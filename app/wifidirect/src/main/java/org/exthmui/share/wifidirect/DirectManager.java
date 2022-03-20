package org.exthmui.share.wifidirect;

import static org.exthmui.share.wifidirect.Constants.RECORD_KEY_DISPLAY_NAME;
import static org.exthmui.share.wifidirect.Constants.RECORD_KEY_SHARE_PROTOCOL_VERSION;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import org.exthmui.share.shared.BaseEventListenersUtils;
import org.exthmui.share.shared.Constants;
import org.exthmui.share.shared.base.Discoverer;
import org.exthmui.share.shared.base.Entity;
import org.exthmui.share.shared.base.PeerInfo;
import org.exthmui.share.shared.base.Sender;
import org.exthmui.share.shared.base.events.DiscovererStartedEvent;
import org.exthmui.share.shared.base.events.DiscovererStoppedEvent;
import org.exthmui.share.shared.base.events.PeerAddedEvent;
import org.exthmui.share.shared.base.events.PeerRemovedEvent;
import org.exthmui.share.shared.base.events.PeerUpdatedEvent;
import org.exthmui.share.shared.base.listeners.BaseEventListener;
import org.exthmui.share.shared.base.listeners.OnDiscovererStartedListener;
import org.exthmui.share.shared.base.listeners.OnDiscovererStoppedListener;
import org.exthmui.share.shared.base.listeners.OnPeerAddedListener;
import org.exthmui.share.shared.base.listeners.OnPeerRemovedListener;
import org.exthmui.share.shared.base.listeners.OnPeerUpdatedListener;

import java.util.Collection;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class DirectManager implements Discoverer, Sender<DirectPeer> {

    private static final String TAG = "DirectManager";

    @SuppressWarnings("unchecked")
    private static final Class<? extends BaseEventListener>[] LISTENER_TYPES_ALLOWED = (Class<? extends BaseEventListener>[]) new Class<?>[]
            {
                    OnDiscovererStartedListener.class,
                    OnDiscovererStoppedListener.class,
                    OnPeerAddedListener.class,
                    OnPeerUpdatedListener.class,
                    OnPeerRemovedListener.class
            };
    private static DirectManager instance;

    private final Collection<BaseEventListener> mListeners = new HashSet<>();
    private final Context mContext;
    private final Map<String, PeerInfo> mPeers = new HashMap<>();
    private final Map<String, Map<String, String>> mPeerRecords = new HashMap<>();
    private boolean mDiscovererStarted;
    private boolean mInitialized;

    private WifiP2pManager mWifiP2pManager;
    private WifiP2pManager.Channel mChannel;
    private DirectBroadcastReceiver mBroadcastReceiver;

    private DirectManager(Context context) {
        this.mContext = context.getApplicationContext();
    }

    public static DirectManager getInstance(Context context) {
        if (instance == null) instance = new DirectManager(context);
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
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        return permissions;
    }

    @Override
    public boolean isDiscovererStarted() {
        return mDiscovererStarted;
    }

    @Override
    public UUID send(DirectPeer peer, Entity entity) {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(DirectSendingWorker.class)
                .setInputData(genSendingInputData(peer, entity))
//                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)// FIXME: 3/20/22
                .build();
        WorkManager.getInstance(mContext).enqueueUniqueWork(Constants.WORK_NAME_PREFIX_SEND + peer.getId(), ExistingWorkPolicy.APPEND_OR_REPLACE, work);
        return work.getId();
    }

    @Override
    public UUID send(DirectPeer peer, List<Entity> entities) {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(DirectMultiSendingWorker.class)
                .setInputData(genSendingInputData(peer, (Entity[]) entities.toArray()))
//                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)// FIXME: 3/20/22
                .build();
        WorkManager.getInstance(mContext).enqueueUniqueWork(Constants.WORK_NAME_PREFIX_SEND + peer.getId(), ExistingWorkPolicy.APPEND_OR_REPLACE, work);
        return work.getId();
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
        mBroadcastReceiver = new DirectBroadcastReceiver(mWifiP2pManager, mChannel, new DirectActionListener() {
            @Override
            public void onWifiP2pStateChanged(boolean enabled) {

            }

            @Override
            public void onPeersListChanged(Collection<WifiP2pDevice> wifiP2pDeviceList) {
                // Do not add peer here
//                Set<PeerInfo> newlyAdded = new HashSet<>();
                Set<PeerInfo> notChanged = new HashSet<>();
                Set<PeerInfo> updated = new HashSet<>();
                Set<PeerInfo> removed = new HashSet<>(mPeers.values());

                for (WifiP2pDevice wifiP2pDevice : wifiP2pDeviceList) {
                    DirectPeer newPeer = new DirectPeer(wifiP2pDevice);
                    DirectPeer oldPeer = (DirectPeer) mPeers.get(newPeer.getId());
                    if (oldPeer != null) {
                        if (newPeer.getWifiP2pDevice().equals(oldPeer.getWifiP2pDevice()))
                            notChanged.add(oldPeer);
                        else {
                            wifiP2pDevice.deviceName = mPeerRecords.containsKey(wifiP2pDevice.deviceAddress) ?
                                    Objects.requireNonNull(mPeerRecords.get(wifiP2pDevice.deviceAddress)).get(RECORD_KEY_DISPLAY_NAME) : wifiP2pDevice.deviceName;
                            oldPeer.setWifiP2pDevice(wifiP2pDevice);
                            updated.add(oldPeer);
                        }
                    }
//                    else newlyAdded.add(newPeer);
                }
                removed.removeAll(notChanged);
                removed.removeAll(updated);
//                for (PeerInfo peer: newlyAdded){
//                    mPeers.put(peer.getId(), peer);
//                    notifyListeners(new PeerAddedEvent(this, peer));
//                }
                for (PeerInfo peer : updated) {
                    peer.notifyPeerUpdated();
                    notifyListeners(new PeerUpdatedEvent(this, peer));
                }
                for (PeerInfo peer : removed) {
                    mPeers.remove(peer.getId());
                    notifyListeners(new PeerRemovedEvent(this, peer));
                }
            }

            @Override
            public void onConnectionInfoChanged(WifiP2pInfo wifiP2pInfo) {

            }

            @Override
            public void onSelfDeviceChanged(WifiP2pDevice self) {

            }

            @Override
            public void onDisconnected() {

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
    public void startDiscover() {
        try {
            // register BroadcastReceiver
            mContext.registerReceiver(mBroadcastReceiver, DirectBroadcastReceiver.getIntentFilter());

            WifiP2pManager.DnsSdTxtRecordListener txtListener = new WifiP2pManager.DnsSdTxtRecordListener() {
                @Override
                public void onDnsSdTxtRecordAvailable(
                        String fullDomain, Map record, WifiP2pDevice device) {
                    Log.d(TAG, "DnsSdTxtRecord available: " + record.toString());
                    try {
                        @SuppressWarnings("unchecked") Map<String, String> rec = (Map<String, String>) record;
                        if (rec.get(RECORD_KEY_SHARE_PROTOCOL_VERSION) == null) {
                            Log.d(TAG, String.format("The key %s for protocol version not found in DnsSdTxtRecord,ignoring...", RECORD_KEY_SHARE_PROTOCOL_VERSION));
                        } else {
                            mPeerRecords.put(device.deviceAddress, rec);
                            // Update the device name with the human-friendly version from
                            // the DnsTxtRecord, assuming one arrived.
                            device.deviceName =
                                    mPeerRecords.containsKey(device.deviceAddress) ?
                                            Objects.requireNonNull(mPeerRecords.get(device.deviceAddress)).get(RECORD_KEY_DISPLAY_NAME) : device.deviceName;

                            PeerInfo peer = new DirectPeer(device);
                            Log.d(TAG, String.format("New peer added: %s(%s)", peer.getDisplayName(), peer.getId()));
                            mPeers.put(peer.getId(), peer);
                            notifyListeners(new PeerAddedEvent(this, peer));
                        }
                    } catch (ClassCastException e) {
                        Log.d(TAG, "Failed casting DnsSdTxtRecord,ignoring...");
                    }
                }
            };

            WifiP2pManager.DnsSdServiceResponseListener servListener = (instanceName, registrationType, device) -> Log.d(TAG, "Wifi Direct discovered service: " + instanceName);

            mWifiP2pManager.setDnsSdResponseListeners(mChannel, servListener, txtListener);

            mWifiP2pManager.discoverServices(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    mDiscovererStarted = true;
                    notifyListeners(new DiscovererStartedEvent(this));
                }

                @Override
                public void onFailure(int reasonCode) {
                    stopDiscover();
                }
            });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        // Here shouldn't cause any exception, we should only register BroadcastReceiver after permissions are granted.
    }

    @Override
    public void stopDiscover() {
        mContext.unregisterReceiver(mBroadcastReceiver);
        mWifiP2pManager.stopPeerDiscovery(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                mDiscovererStarted = false;
                notifyListeners(new DiscovererStoppedEvent(this));
            }

            @Override
            public void onFailure(int i) {
            }
        });
    }

    @Override
    public Map<String, PeerInfo> getPeers() {
        return mPeers;
    }
}