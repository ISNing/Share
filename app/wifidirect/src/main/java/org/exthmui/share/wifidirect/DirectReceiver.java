package org.exthmui.share.wifidirect;

import static org.exthmui.share.shared.misc.Constants.CONNECTION_CODE_WIFIDIRECT;
import static org.exthmui.share.wifidirect.Constants.RECORD_KEY_ACCOUNT_SERVER_SIGN;
import static org.exthmui.share.wifidirect.Constants.RECORD_KEY_DISPLAY_NAME;
import static org.exthmui.share.wifidirect.Constants.RECORD_KEY_PEER_ID;
import static org.exthmui.share.wifidirect.Constants.RECORD_KEY_SERVER_PORT;
import static org.exthmui.share.wifidirect.Constants.RECORD_KEY_SHARE_PROTOCOL_VERSION;
import static org.exthmui.share.wifidirect.Constants.RECORD_KEY_UID;
import static org.exthmui.share.wifidirect.Constants.SHARE_PROTOCOL_VERSION_1;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.os.Build;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class DirectReceiver implements Receiver {

    public static final String TAG = "DirectReceiver";

    @SuppressWarnings("unchecked")
    private static final Class<? extends BaseEventListener>[] LISTENER_TYPES_ALLOWED = (Class<? extends BaseEventListener>[]) new Class<?>[]
            {
                    OnReceiverStartedListener.class,
                    OnReceiverStoppedListener.class,
                    OnReceiveActionAcceptListener.class,
                    OnReceiveActionRejectListener.class,
                    OnReceiverErrorOccurredListener.class
            };

    private static final String WORK_UNIQUE_NAME = Constants.WORK_NAME_PREFIX_RECEIVE + CONNECTION_CODE_WIFIDIRECT;

    private static DirectReceiver instance;

    private final Collection<BaseEventListener> mListeners = new HashSet<>();
    private final Context mContext;
    private boolean mReceiverStarted;
    private boolean mInitialized;

    private WifiP2pManager mWifiP2pManager;
    private WifiP2pManager.Channel mChannel;
    @Nullable
    private WifiP2pDnsSdServiceInfo mServiceInfo;

    private DirectReceiver(@NonNull Context context) {
        this.mContext = context.getApplicationContext();
    }

    public static DirectReceiver getInstance(@NonNull Context context) {
        if (instance == null)
            synchronized (DirectReceiver.class) {
                if (instance == null) instance = new DirectReceiver(context);
            }
        return instance;
    }

    private void notifyListeners(@NonNull EventObject event) {
        BaseEventListenersUtils.notifyListeners(event, mListeners);
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
        permissions.add(Manifest.permission.ACCESS_WIFI_STATE);
        permissions.add(Manifest.permission.CHANGE_WIFI_STATE);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }
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
        mWifiP2pManager = (WifiP2pManager) mContext.getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mWifiP2pManager.initialize(mContext, Looper.getMainLooper(), () -> {});
        WorkManager.getInstance(mContext).getWorkInfosForUniqueWorkLiveData(WORK_UNIQUE_NAME).observeForever(workInfo -> {
            boolean isRunning = false;
            for (WorkInfo info : workInfo) {
                if (!info.getState().isFinished()) {
                    isRunning = true;
                    break;
                }
            }
            if (isReceiverStarted() && !isRunning)
                startWorkWrapped(mContext);// Restart when finished but receiver not stopped
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
        try {
            mReceiverStarted = true;

            startWorkWrapped(mContext);

            int serverPort = DirectUtils.getServerPort(mContext);

            //  Create a string map containing information about your service.
            Map<String, String> record = new HashMap<>(6);
            record.put(RECORD_KEY_SHARE_PROTOCOL_VERSION, SHARE_PROTOCOL_VERSION_1);
            record.put(RECORD_KEY_SERVER_PORT, String.valueOf(serverPort));
            record.put(RECORD_KEY_DISPLAY_NAME, Utils.getSelfName(mContext));
            record.put(RECORD_KEY_PEER_ID, Utils.getSelfId(mContext));// Without prefix
            record.put(RECORD_KEY_UID, "");// TODO: get it from account sdk
            record.put(RECORD_KEY_ACCOUNT_SERVER_SIGN, ""); // TODO: get it from account sdk

            // Service information
            mServiceInfo = WifiP2pDnsSdServiceInfo.newInstance(
                    org.exthmui.share.wifidirect.Constants.LOCAL_SERVICE_INSTANCE_NAME,
                    org.exthmui.share.wifidirect.Constants.LOCAL_SERVICE_SERVICE_TYPE,
                    record);

            mWifiP2pManager.addLocalService(mChannel, mServiceInfo, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    notifyListeners(new ReceiverStartedEvent(this));
                    Log.d(TAG, "LocalService successfully added");
                }

                @Override
                public void onFailure(int arg0) {
                    stopReceive();
                    String message = String.format(Locale.ENGLISH, "LocalService add failed: %d", arg0);
                    String messageLocalized = mContext.getString(R.string.error_wifidirect_local_service_add_failed, arg0);
                    notifyListeners(new ReceiverErrorOccurredEvent(DirectReceiver.this, message, messageLocalized));
                    Log.e(TAG, message);
                }
            });

            // Peer should be discoverable from now.
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        // Here shouldn't cause any exception, we should only register BroadcastReceiver after permissions are granted.
    }

    @NonNull
    @Override
    public UUID startWork(@NonNull Context context) {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(DirectReceivingWorker.class)
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
        mWifiP2pManager.removeLocalService(mChannel, mServiceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "LocalService successfully removed");
            }

            @Override
            public void onFailure(int arg0) {
                String message = String.format(Locale.ENGLISH, "LocalService remove failed: %d", arg0);
                String messageLocalized = mContext.getString(R.string.error_wifidirect_local_service_remove_failed, arg0);
                notifyListeners(new ReceiverErrorOccurredEvent(DirectReceiver.this, message, messageLocalized));
                Log.e(TAG, message);
            }
        });
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
