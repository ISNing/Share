package org.exthmui.share.lannsd;

import static android.net.nsd.NsdManager.PROTOCOL_DNS_SD;
import static org.exthmui.share.lannsd.Constants.RECORD_KEY_ACCOUNT_SERVER_SIGN;
import static org.exthmui.share.lannsd.Constants.RECORD_KEY_SERVER_PORT;
import static org.exthmui.share.lannsd.Constants.RECORD_KEY_SHARE_PROTOCOL_VERSION;
import static org.exthmui.share.lannsd.Constants.RECORD_KEY_UID;
import static org.exthmui.share.lannsd.Constants.SHARE_PROTOCOL_VERSION_1;
import static org.exthmui.share.shared.misc.Constants.CONNECTION_CODE_LANNSD;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.gson.Gson;

import org.exthmui.share.shared.base.receive.Receiver;
import org.exthmui.share.shared.base.receive.ReceivingWorker;
import org.exthmui.share.shared.events.ReceiveActionAcceptEvent;
import org.exthmui.share.shared.events.ReceiveActionRejectEvent;
import org.exthmui.share.shared.events.ReceiverErrorOccurredEvent;
import org.exthmui.share.shared.events.ReceiverStartedEvent;
import org.exthmui.share.shared.events.ReceiverStoppedEvent;
import org.exthmui.share.shared.listeners.BaseEventListener;
import org.exthmui.share.shared.listeners.OnReceiveActionAcceptListener;
import org.exthmui.share.shared.listeners.OnReceiveActionRejectListener;
import org.exthmui.share.shared.listeners.OnReceiverErrorOccurredListener;
import org.exthmui.share.shared.listeners.OnReceiverStartedListener;
import org.exthmui.share.shared.listeners.OnReceiverStoppedListener;
import org.exthmui.share.shared.misc.BaseEventListenersUtils;
import org.exthmui.share.shared.misc.Constants;
import org.exthmui.share.shared.misc.Utils;

import java.util.Collection;
import java.util.EventObject;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

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

    private static final String WORK_UNIQUE_NAME = Constants.WORK_NAME_PREFIX_RECEIVE + CONNECTION_CODE_LANNSD;

    private static final Gson GSON = new Gson();

    private static NsdReceiver instance;

    private final Collection<BaseEventListener> mListeners = new HashSet<>();
    private final Context mContext;
    private boolean mReceiverStarted;
    private boolean mInitialized;

    private NsdManager mNsdManager;
    private NsdManager.RegistrationListener mRegistrationListener;
    @Nullable
    private NsdServiceInfo mServiceInfo;

    private OnListeningPortListener mOnListeningPortListener;

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
            ServiceNameModel serviceName = new ServiceNameModel()
                    .setDeviceType(Utils.getSelfDeviceType(mContext))
                    .setDisplayName(Utils.getSelfName(mContext))
                    .setPeerId(Utils.getSelfId(mContext));

            // Service information
            mServiceInfo = new NsdServiceInfo();

            mServiceInfo.setServiceName(GSON.toJson(serviceName));
            mServiceInfo.setServiceType(org.exthmui.share.lannsd.Constants.LOCAL_SERVICE_SERVICE_TYPE);
            mServiceInfo.setPort(port);
            mServiceInfo.setAttribute(RECORD_KEY_SHARE_PROTOCOL_VERSION, SHARE_PROTOCOL_VERSION_1);
            mServiceInfo.setAttribute(RECORD_KEY_SERVER_PORT, String.valueOf(port));
            mServiceInfo.setAttribute(RECORD_KEY_UID, "0");// TODO: get it from account sdk
            mServiceInfo.setAttribute(RECORD_KEY_ACCOUNT_SERVER_SIGN, "jj"); // TODO: get it from account sdk

            mNsdManager.registerService(mServiceInfo, PROTOCOL_DNS_SD, mRegistrationListener);
            // Peer should be discoverable from now.
        };

        WorkManager.getInstance(mContext).getWorkInfosForUniqueWorkLiveData(WORK_UNIQUE_NAME).observeForever(workInfo -> {
            boolean isRunning = false;
            for (WorkInfo info : workInfo) {
                if (!info.getState().isFinished()) {
                    isRunning = true;
                    break;
                }
            }
            if (isReceiverStarted() && !isRunning) {
                startWorkWrapped(mContext);// Restart when finished but receiver not stopped
            }
        });
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
        mReceiverStarted = true;

        startWorkWrapped(mContext);
    }

    @NonNull
    @Override
    public UUID startWork(@NonNull Context context) {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(NsdReceivingWorker.class)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_UNIQUE_NAME, ExistingWorkPolicy.REPLACE, work);
        return work.getId();
    }

    @Override
    public void stopReceive() {
        mReceiverStarted = false;
        try {
            List<WorkInfo> workInfo = WorkManager.getInstance(mContext).getWorkInfosForUniqueWork(WORK_UNIQUE_NAME).get();
            if (workInfo == null) return;
            for (WorkInfo info : workInfo) {
                if (!info.getState().isFinished()) {
                    if (info.getProgress().getInt(ReceivingWorker.STATUS_CODE, Constants.TransmissionStatus.WAITING_FOR_REQUEST.getNumVal()) == Constants.TransmissionStatus.WAITING_FOR_REQUEST.getNumVal()) {
                        WorkManager.getInstance(mContext).cancelUniqueWork(WORK_UNIQUE_NAME);
                    }
                }
            }
            notifyListeners(new ReceiverStoppedEvent(this));
        } catch (@NonNull ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        if (mServiceInfo == null) return;
        try {
            mNsdManager.unregisterService(mRegistrationListener);
        } catch (IllegalArgumentException ignored) {
        }
        mServiceInfo = null;
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