package org.exthmui.share.wifidirect;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkManager;

import org.exthmui.share.shared.BaseEventListenersUtils;
import org.exthmui.share.shared.Constants;
import org.exthmui.share.shared.base.PeerInfo;
import org.exthmui.share.shared.base.Receiver;
import org.exthmui.share.shared.base.events.PeerAddedEvent;
import org.exthmui.share.shared.base.events.PeerRemovedEvent;
import org.exthmui.share.shared.base.events.PeerUpdatedEvent;
import org.exthmui.share.shared.base.events.ReceiveActionAcceptEvent;
import org.exthmui.share.shared.base.events.ReceiveActionRejectEvent;
import org.exthmui.share.shared.base.listeners.BaseEventListener;
import org.exthmui.share.shared.base.listeners.OnReceiveActionAcceptListener;
import org.exthmui.share.shared.base.listeners.OnReceiveActionRejectListener;
import org.exthmui.share.shared.base.listeners.OnReceiverStartedListener;
import org.exthmui.share.shared.base.listeners.OnReceiverStoppedListener;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DirectReceiver implements Receiver {

    private static final String TAG = "DirectReceiver";

    private static final Class<? extends BaseEventListener>[] LISTENER_TYPES_ALLOWED;

    private static DirectReceiver instance;

    static {
        LISTENER_TYPES_ALLOWED = (Class<? extends BaseEventListener>[]) new Class<?>[]
                {
                        OnReceiverStartedListener.class,
                        OnReceiverStoppedListener.class,
                        OnReceiveActionAcceptListener.class,
                        OnReceiveActionRejectListener.class
                };
    }

    private final Collection<BaseEventListener> mListeners = new HashSet<>();
    private final Context mContext;
    private final Map<String, PeerInfo> mPeers = new HashMap<>();
    private boolean mReceiverStarted;
    private boolean mInitialized;

    private WifiP2pManager mWifiP2pManager;
    private WifiP2pManager.Channel mChannel;
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
        for (String permission: getPermissionsRequired())
            if (ContextCompat.checkSelfPermission(mContext, permission) != PackageManager.PERMISSION_GRANTED) permissions.add(permission);
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
        this.mInitialized = true;
    }

    @Override
    public boolean isInitialized() {
        return false;
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

            OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(DirectReceivingWorker.class)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build();
            WorkManager.getInstance(mContext).enqueueUniqueWork(Constants.WORK_NAME_PREFIX_RECEIVE + new DirectPeer(null).getConnectionType(), ExistingWorkPolicy.APPEND_OR_REPLACE, work);
            mReceiverStarted = true;
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        // Here shouldn't cause any exception, we should only register BroadcastReceiver after permissions are granted.
    }

    @Override
    public void stopReceive() {
        mContext.unregisterReceiver(mBroadcastReceiver);
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
