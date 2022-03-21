package org.exthmui.share.wifidirect;

import static org.exthmui.share.shared.Constants.CONNECTION_CODE_WIFIDIRECT;
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
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import org.exthmui.share.shared.BaseEventListenersUtils;
import org.exthmui.share.shared.Constants;
import org.exthmui.share.shared.base.PeerInfo;
import org.exthmui.share.shared.base.Receiver;
import org.exthmui.share.shared.base.ReceivingWorker;
import org.exthmui.share.shared.base.events.ReceiveActionAcceptEvent;
import org.exthmui.share.shared.base.events.ReceiveActionRejectEvent;
import org.exthmui.share.shared.base.events.ReceiverStartedEvent;
import org.exthmui.share.shared.base.events.ReceiverStoppedEvent;
import org.exthmui.share.shared.base.listeners.BaseEventListener;
import org.exthmui.share.shared.base.listeners.OnReceiveActionAcceptListener;
import org.exthmui.share.shared.base.listeners.OnReceiveActionRejectListener;
import org.exthmui.share.shared.base.listeners.OnReceiverStartedListener;
import org.exthmui.share.shared.base.listeners.OnReceiverStoppedListener;

import java.util.Collection;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class DirectReceiver implements Receiver {

    private static final String TAG = "DirectReceiver";

    @SuppressWarnings("unchecked")
    private static final Class<? extends BaseEventListener>[] LISTENER_TYPES_ALLOWED = (Class<? extends BaseEventListener>[]) new Class<?>[]
            {
                    OnReceiverStartedListener.class,
                    OnReceiverStoppedListener.class,
                    OnReceiveActionAcceptListener.class,
                    OnReceiveActionRejectListener.class
            };

    private static final String WORK_UNIQUE_NAME = Constants.WORK_NAME_PREFIX_RECEIVE + CONNECTION_CODE_WIFIDIRECT;

    private static DirectReceiver instance;

    private final Collection<BaseEventListener> mListeners = new HashSet<>();
    private final Context mContext;
    private final Map<String, PeerInfo> mPeers = new HashMap<>();
    private boolean mReceiverStarted;
    private boolean mInitialized;
    private boolean mStartNotified;

    private WifiP2pManager mWifiP2pManager;
    private WifiP2pManager.Channel mChannel;
    private WifiP2pDnsSdServiceInfo mServiceInfo;
    private DirectBroadcastReceiver mBroadcastReceiver;

    private DirectReceiver(Context context) {
        this.mContext = context.getApplicationContext();
    }

    public static DirectReceiver getInstance(Context context) {
        if (instance == null) instance = new DirectReceiver(context);
        return instance;
    }

    private void notifyListeners(EventObject event) {
        BaseEventListenersUtils.notifyListeners(event, mListeners);
    }

    public WifiP2pManager getWifiP2pManager() {
        return mWifiP2pManager;
    }

    public WifiP2pManager.Channel getChannel() {
        return mChannel;
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
        permissions.add(Manifest.permission.CHANGE_NETWORK_STATE);
        permissions.add(Manifest.permission.INTERNET);
        permissions.add(Manifest.permission.ACCESS_NETWORK_STATE);
        return permissions;
    }

    @Override
    public boolean isReceiverStarted() {
        return mReceiverStarted;
    }

    @Override
    public void registerListener(BaseEventListener listener) {
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
        mChannel = mWifiP2pManager.initialize(mContext, Looper.getMainLooper(), () -> {
        });
        WorkManager.getInstance(mContext).getWorkInfosForUniqueWorkLiveData(WORK_UNIQUE_NAME).observe(ProcessLifecycleOwner.get(), workInfo -> {
            boolean isRunning = false;
            for (WorkInfo info : workInfo) {
                if (!info.getState().isFinished()) {
                    isRunning = true;
                    if (!mStartNotified) {
                        notifyListeners(new ReceiverStartedEvent(this));
                        mStartNotified = true;
                    }
                    break;
                }
            }
            if (isReceiverStarted() & !isRunning) startWork();
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
            // register BroadcastReceiver
            mContext.registerReceiver(mBroadcastReceiver, DirectBroadcastReceiver.getIntentFilter());
            mReceiverStarted = true;
            mStartNotified = false;

            startWork();

            int serverPort = DirectUtils.getServerPort(mContext);

            //  Create a string map containing information about your service.
            Map<String, String> record = new HashMap<>();
            record.put(RECORD_KEY_SHARE_PROTOCOL_VERSION, SHARE_PROTOCOL_VERSION_1);
            record.put(RECORD_KEY_SERVER_PORT, String.valueOf(serverPort));
            record.put(RECORD_KEY_DISPLAY_NAME, "");
            record.put(RECORD_KEY_PEER_ID, "");// Without prefix
            record.put(RECORD_KEY_UID, "");
            record.put(RECORD_KEY_ACCOUNT_SERVER_SIGN, "");

            // Service information
            mServiceInfo = WifiP2pDnsSdServiceInfo.newInstance(
                    org.exthmui.share.wifidirect.Constants.LOCAL_SERVICE_INSTANCE_NAME,
                    org.exthmui.share.wifidirect.Constants.LOCAL_SERVICE_SERVICE_TYPE,
                    record);

            mWifiP2pManager.addLocalService(mChannel, mServiceInfo, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "LocalService successfully added");
                }

                @Override
                public void onFailure(int arg0) {
                    Log.e(TAG, String.format("LocalService add failed: %d", arg0));
                }
            });

            // Peer should be discoverable from now.
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        // Here shouldn't cause any exception, we should only register BroadcastReceiver after permissions are granted.
    }

    private void startWork() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(DirectReceivingWorker.class)
//                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST) TODO: Check it
                .build();
        WorkManager.getInstance(mContext).enqueueUniqueWork(WORK_UNIQUE_NAME, ExistingWorkPolicy.KEEP, work);
    }

    @Override
    public void stopReceive() {
        if (mBroadcastReceiver != null) mContext.unregisterReceiver(mBroadcastReceiver);
        mReceiverStarted = false;
        try {
            List<WorkInfo> workInfo = WorkManager.getInstance(mContext).getWorkInfosForUniqueWork(WORK_UNIQUE_NAME).get();
            if (workInfo == null) return;
            for (WorkInfo info : workInfo) {
                if (!info.getState().isFinished()) {
                    if (info.getProgress().getInt(ReceivingWorker.P_STATUS_CODE, Constants.TransmissionStatus.WAITING_FOR_REQUEST.getNumVal()) == Constants.TransmissionStatus.WAITING_FOR_REQUEST.getNumVal()) {
                        WorkManager.getInstance(mContext).cancelUniqueWork(WORK_UNIQUE_NAME);
                    }
                }
            }
            notifyListeners(new ReceiverStoppedEvent(this));
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        mWifiP2pManager.removeLocalService(mChannel, mServiceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "LocalService successfully removed");
            }

            @Override
            public void onFailure(int arg0) {
                Log.e(TAG, String.format("LocalService remove failed: %d", arg0));
            }
        });
        mServiceInfo = null;
    }

    @Override
    public void onReceiveActionAccept(ReceiveActionAcceptEvent event) {
        notifyListeners(event);
    }

    @Override
    public void onReceiveActionReject(ReceiveActionRejectEvent event) {
        notifyListeners(event);
    }
}
