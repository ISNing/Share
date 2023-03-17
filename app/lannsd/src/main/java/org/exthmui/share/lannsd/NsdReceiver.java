package org.exthmui.share.lannsd;

import static android.net.nsd.NsdManager.PROTOCOL_DNS_SD;
import static org.exthmui.share.lannsd.Constants.RECORD_KEY_ACCOUNT_SERVER_SIGN;
import static org.exthmui.share.lannsd.Constants.RECORD_KEY_DEVICE_TYPE;
import static org.exthmui.share.lannsd.Constants.RECORD_KEY_DISPLAY_NAME;
import static org.exthmui.share.lannsd.Constants.RECORD_KEY_SERVER_PORT;
import static org.exthmui.share.lannsd.Constants.RECORD_KEY_SHARE_PROTOCOL_VERSION;
import static org.exthmui.share.lannsd.Constants.RECORD_KEY_UID;
import static org.exthmui.share.lannsd.Constants.SHARE_PROTOCOL_VERSION_1;
import static org.exthmui.share.lannsd.Constants.WORKER_INPUT_KEY_CONN_ID;
import static org.exthmui.share.shared.misc.Constants.CONNECTION_CODE_LANNSD;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;

import com.google.gson.Gson;

import org.exthmui.share.shared.base.Entity;
import org.exthmui.share.shared.base.IConnectionType;
import org.exthmui.share.shared.base.receive.Receiver;
import org.exthmui.share.shared.events.ReceiveActionAcceptEvent;
import org.exthmui.share.shared.events.ReceiveActionRejectEvent;
import org.exthmui.share.shared.events.ReceiverErrorOccurredEvent;
import org.exthmui.share.shared.events.ReceiverStartedEvent;
import org.exthmui.share.shared.events.ReceiverStoppedEvent;
import org.exthmui.share.shared.exceptions.FailedResolvingUriException;
import org.exthmui.share.shared.listeners.OnReceiveActionAcceptListener;
import org.exthmui.share.shared.listeners.OnReceiveActionRejectListener;
import org.exthmui.share.shared.listeners.OnReceiverErrorOccurredListener;
import org.exthmui.share.shared.listeners.OnReceiverStartedListener;
import org.exthmui.share.shared.listeners.OnReceiverStoppedListener;
import org.exthmui.share.shared.misc.Constants;
import org.exthmui.share.shared.misc.ReceiverUtils;
import org.exthmui.share.shared.misc.Utils;
import org.exthmui.share.taskMgr.Task;
import org.exthmui.share.taskMgr.TaskManager;
import org.exthmui.share.taskMgr.TaskStatus;
import org.exthmui.share.udptransport.UDPReceiver;
import org.exthmui.utils.BaseEventListenersUtils;
import org.exthmui.utils.StackTraceUtils;
import org.exthmui.utils.listeners.BaseEventListener;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class NsdReceiver implements Receiver {

    interface OnListeningPortListener {
        void onListening(int port);
    }

    public static final String TAG = "NsdReceiver";

    @SuppressWarnings("unchecked")
    private static final Class<? extends BaseEventListener>[] LISTENER_TYPES_ALLOWED = (Class<? extends BaseEventListener>[]) new Class<?>[]
            {
                    OnReceiverStartedListener.class,
                    OnReceiverStoppedListener.class,
                    OnReceiveActionAcceptListener.class,
                    OnReceiveActionRejectListener.class,
                    OnReceiverErrorOccurredListener.class
            };

    private static final String WORK_UNIQUE_NAME = Constants.WORK_NAME_PREFIX_RECEIVE + CONNECTION_CODE_LANNSD + "CONN%d";
    private static final String WORK_PROP_CONN_ID = "CONN_ID";

    private static final Gson GSON = new Gson();

    private static volatile NsdReceiver instance;

    private final Collection<BaseEventListener> mListeners = new HashSet<>();
    private final Context mContext;
    private boolean mReceiverStarted;
    private boolean mInitialized;

    private List<String> mTaskIds;

    private UDPReceiver mUdpReceiver;

    private NsdManager mNsdManager;
    private NsdManager.RegistrationListener mRegistrationListener;
    @Nullable
    private NsdServiceInfo mServiceInfo;

    private OnListeningPortListener mOnListeningPortListener;

    final Map<String, Entity> mEntityMap = new HashMap<>();

    private NsdReceiver(@NonNull Context context) {
        this.mContext = context.getApplicationContext();
    }

    public static NsdReceiver getInstance(@NonNull Context context) {
        if (instance == null)
            synchronized (NsdReceiver.class) {
                if (instance == null) instance = new NsdReceiver(context);
            }
        return instance;
    }

    private void notifyListeners(@NonNull EventObject event) {
        BaseEventListenersUtils.notifyListeners(event, mListeners);
    }

    @NonNull
    @Override
    public IConnectionType getConnectionType() {
        return new Metadata();
    }

    @Nullable
    public Entity getEntity(@NonNull String id) {
        return mEntityMap.get(id);
    }

    public UDPReceiver getUdpReceiver() {
        return mUdpReceiver;
    }

    public OnListeningPortListener getOnListeningPortListener() {
        return mOnListeningPortListener;
    }

    @NonNull
    @Override
    public Set<String> getPermissionNotGranted() {
        Set<String> permissions = new HashSet<>();
        for (String permission : getPermissionsRequired())
            if (ContextCompat.checkSelfPermission(mContext, permission) != PackageManager.PERMISSION_GRANTED)
                permissions.add(permission);
        return permissions;
    }

    @NonNull
    @Override
    public Set<String> getPermissionsRequired() {
        Set<String> permissions = new HashSet<>();
        permissions.add(Manifest.permission.INTERNET);
        return permissions;
    }

    @Override
    public boolean isReceiverStarted() {
        return mReceiverStarted;
    }

    @Override
    public void registerListener(@NonNull BaseEventListener listener) {
        if (BaseEventListenersUtils.isThisListenerSuitable(listener, LISTENER_TYPES_ALLOWED))
            mListeners.add(listener);
    }

    @Override
    public void unregisterListener(BaseEventListener listener) {
        mListeners.remove(listener);
    }

    @Override
    public void initialize() {
        int serverPortTcp = NsdUtils.getServerPortTcp(mContext);
        int serverPortUdp = NsdUtils.getServerPortUdp(mContext);
        boolean md5ValidationEnabled = NsdUtils.isMd5ValidationEnabled(mContext);
        mTaskIds = new ArrayList<>();
        try {
            mUdpReceiver = new UDPReceiver(mContext, fileInfo -> {
                Pair<Uri, BufferedOutputStream> p = ReceiverUtils.openFileOutputStream(mContext, fileInfo.getFileName());
                if (p == null) return null;
                try {
                    mEntityMap.put(fileInfo.getId(), new Entity(mContext, p.first));
                } catch (FailedResolvingUriException e) {
                    Log.e(TAG + "/UDPReceiver.OutputStreamFactory", e.getMessage());
                    Log.e(TAG + "/UDPReceiver.OutputStreamFactory", StackTraceUtils.getStackTraceString(e.getStackTrace()));
                    return null;
                }
                return p.second;
            }, fileInfo -> {
                Entity entity = mEntityMap.get(fileInfo.getId());
                if (entity == null) return null;
                try {
                    return entity.getInputStream(mContext);
                } catch (FileNotFoundException e) {
                    Log.e(TAG + "/UDPReceiver.InputStreamFactory", e.getMessage());
                    Log.e(TAG + "/UDPReceiver.InputStreamFactory", StackTraceUtils.getStackTraceString(e.getStackTrace()));
                    return null;
                }
            }, connId -> startWorkWrapped(mContext, Map.of(WORK_PROP_CONN_ID, String.valueOf(connId))), serverPortTcp, serverPortUdp, true, md5ValidationEnabled);
        } catch (IOException e) {
            String message = String.format(Locale.ENGLISH, "UdpReceiver initialize failed: %s", e.getMessage());
            String messageLocalized = mContext.getString(R.string.error_lannsd_udp_receiver_initialize_failed, e.getLocalizedMessage());
            notifyListeners(new ReceiverErrorOccurredEvent(NsdReceiver.this, message, messageLocalized));
            Log.e(TAG, message);
            Log.e(TAG, String.format("Error occurred while receiving: %s(message: %s)", e, e.getMessage()));
            Log.e(TAG, StackTraceUtils.getStackTraceString(e.getStackTrace()));
            return;
        }
        mNsdManager = (android.net.nsd.NsdManager) mContext.getSystemService(Context.NSD_SERVICE);

        mRegistrationListener = new NsdManager.RegistrationListener() {

            @Override
            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
                notifyListeners(new ReceiverStartedEvent(this));
                Log.d(TAG, "NsdService successfully registered");
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                stopReceive();
                String message = String.format(Locale.ENGLISH, "NsdService register failed: %d", errorCode);
                String messageLocalized = mContext.getString(R.string.error_lannsd_local_service_add_failed, errorCode);
                notifyListeners(new ReceiverErrorOccurredEvent(NsdReceiver.this, message, messageLocalized));
                Log.e(TAG, message);
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) {
                Log.d(TAG, "NsdService successfully unregistered");
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                stopReceive();
                String message = String.format(Locale.ENGLISH, "NsdService unregister failed: %d", errorCode);
                String messageLocalized = mContext.getString(R.string.error_lannsd_local_service_remove_failed, errorCode);
                notifyListeners(new ReceiverErrorOccurredEvent(NsdReceiver.this, message, messageLocalized));
                Log.e(TAG, message);
            }
        };

        mOnListeningPortListener = port -> {
            try {
                mNsdManager.unregisterService(mRegistrationListener);
            } catch (IllegalArgumentException ignored) {
            }
            String serviceName = Utils.getSelfId(mContext);

            // Service information
            mServiceInfo = new NsdServiceInfo();

            mServiceInfo.setServiceName(serviceName);
            mServiceInfo.setServiceType(org.exthmui.share.lannsd.Constants.LOCAL_SERVICE_SERVICE_TYPE);
            mServiceInfo.setPort(port);
            mServiceInfo.setAttribute(RECORD_KEY_DEVICE_TYPE, String.valueOf(Utils.getSelfDeviceType(mContext)));
            mServiceInfo.setAttribute(RECORD_KEY_DISPLAY_NAME, Utils.getSelfName(mContext));
            mServiceInfo.setAttribute(RECORD_KEY_SHARE_PROTOCOL_VERSION, SHARE_PROTOCOL_VERSION_1);
            mServiceInfo.setAttribute(RECORD_KEY_SERVER_PORT, String.valueOf(port));
            mServiceInfo.setAttribute(RECORD_KEY_UID, "0");// TODO: get it from account sdk
            mServiceInfo.setAttribute(RECORD_KEY_ACCOUNT_SERVER_SIGN, ""); // TODO: get it from account sdk

            mNsdManager.registerService(mServiceInfo, PROTOCOL_DNS_SD, mRegistrationListener);
            // Peer should be discoverable from now.
        };

        this.mInitialized = true;
    }

    @Override
    public boolean isInitialized() {
        return mInitialized;
    }

    @Override
    public boolean isFeatureAvailable() {
        return mContext.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_WIFI_DIRECT);
    }

    @Override
    public void startReceive() {
        try {
            mUdpReceiver.startReceive();
            mOnListeningPortListener.onListening(mUdpReceiver.getServerPortTcp());
        } catch (IOException e) {
            String message = String.format(Locale.ENGLISH, "UdpReceiver start failed: %s", e.getMessage());
            String messageLocalized = mContext.getString(R.string.error_lannsd_udp_receiver_start_failed, e.getLocalizedMessage());
            notifyListeners(new ReceiverErrorOccurredEvent(NsdReceiver.this, message, messageLocalized));
            Log.e(TAG, message);
            Log.e(TAG, String.format("Error occurred while starting receiver: %s(message: %s)", e, e.getMessage()));
            Log.e(TAG, StackTraceUtils.getStackTraceString(e.getStackTrace()));
            return;
        }
        mReceiverStarted = true;
    }

    @NonNull
    @Override
    public UUID startWork(@NonNull Context context, Map<String, String> properties) {
        String connIdStr = properties.get(WORK_PROP_CONN_ID);
        if (connIdStr == null) {
            Log.e(TAG, "Got null connection id from properties when to start work, ignoring");
            throw new IllegalArgumentException(); // TODO
        }
        byte connId;
        try {
            connId = Byte.parseByte(connIdStr);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Got invalid connection id from properties when to start work, ignoring");
            throw new IllegalArgumentException(); // TODO
        }
        Bundle input = new Bundle();
        input.putByte(WORKER_INPUT_KEY_CONN_ID, connId);
        NsdReceivingTask task = new NsdReceivingTask(mContext, input);
        TaskManager.getInstance(mContext).enqueueTask(String.format(Locale.ROOT, WORK_UNIQUE_NAME, connId), task);
        mTaskIds.add(task.getTaskId());
        return UUID.fromString(task.getTaskId());
    }

    @Override
    public void stopReceive() {
        boolean workerRunning = false;
        for (String taskId : mTaskIds) {
            Task task = TaskManager.getInstance(mContext).getTask(taskId);
            if (task == null) return;
            if (task.getStatus() == TaskStatus.RUNNING) {
                workerRunning = true;
                break;
            }
        }
        if (!workerRunning && mUdpReceiver != null) mUdpReceiver.stopReceive();
        notifyListeners(new ReceiverStoppedEvent(this));

        if (mServiceInfo == null) return;
        try {
            mNsdManager.unregisterService(mRegistrationListener);
        } catch (IllegalArgumentException ignored) {
        }
        mServiceInfo = null;
        mReceiverStarted = false;
    }

    @Override
    public void onReceiveActionAccept(@NonNull ReceiveActionAcceptEvent event) {
        notifyListeners(event);
    }

    @Override
    public void onReceiveActionReject(@NonNull ReceiveActionRejectEvent event) {
        notifyListeners(event);
    }
}