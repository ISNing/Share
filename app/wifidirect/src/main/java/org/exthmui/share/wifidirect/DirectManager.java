package org.exthmui.share.wifidirect;

import static org.exthmui.share.wifidirect.Constants.LOCAL_SERVICE_INSTANCE_NAME;
import static org.exthmui.share.wifidirect.Constants.LOCAL_SERVICE_SERVICE_TYPE;
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
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Build;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkManager;

import org.apache.commons.lang3.StringUtils;
import org.exthmui.share.shared.base.Entity;
import org.exthmui.share.shared.base.IConnectionType;
import org.exthmui.share.shared.base.IPeer;
import org.exthmui.share.shared.base.discover.Discoverer;
import org.exthmui.share.shared.base.send.Sender;
import org.exthmui.share.shared.events.DiscovererErrorOccurredEvent;
import org.exthmui.share.shared.events.DiscovererStartedEvent;
import org.exthmui.share.shared.events.DiscovererStoppedEvent;
import org.exthmui.share.shared.events.PeerAddedEvent;
import org.exthmui.share.shared.events.PeerRemovedEvent;
import org.exthmui.share.shared.events.PeerUpdatedEvent;
import org.exthmui.share.shared.listeners.OnDiscovererErrorOccurredListener;
import org.exthmui.share.shared.listeners.OnDiscovererStartedListener;
import org.exthmui.share.shared.listeners.OnDiscovererStoppedListener;
import org.exthmui.share.shared.listeners.OnPeerAddedListener;
import org.exthmui.share.shared.listeners.OnPeerRemovedListener;
import org.exthmui.share.shared.listeners.OnPeerUpdatedListener;
import org.exthmui.share.shared.listeners.OnSenderErrorOccurredListener;
import org.exthmui.share.shared.misc.Constants;
import org.exthmui.utils.BaseEventListenersUtils;
import org.exthmui.utils.listeners.BaseEventListener;

import java.util.Collection;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class DirectManager implements Discoverer, Sender<DirectPeer> {

    public static final String TAG = "DirectManager";

    @SuppressWarnings("unchecked")
    private static final Class<? extends BaseEventListener>[] LISTENER_TYPES_ALLOWED = (Class<? extends BaseEventListener>[]) new Class<?>[]
            {
                    OnDiscovererStartedListener.class,
                    OnDiscovererStoppedListener.class,
                    OnPeerAddedListener.class,
                    OnPeerUpdatedListener.class,
                    OnPeerRemovedListener.class,
                    OnDiscovererErrorOccurredListener.class,
                    OnSenderErrorOccurredListener.class
            };
    private static volatile DirectManager instance;

    private final Collection<BaseEventListener> mListeners = new HashSet<>();
    private final Context mContext;
    private final Map<String, IPeer> mPeers = new HashMap<>();
    private final Map<String, Map<String, String>> mPeerRecords = new HashMap<>();
    private boolean mDiscovererStarted;
    private boolean mInitialized;

    private WifiP2pManager mWifiP2pManager;
    private WifiP2pManager.Channel mChannel;
    private DirectBroadcastReceiver mBroadcastReceiver;
    private final WifiP2pDnsSdServiceRequest mServiceRequest = WifiP2pDnsSdServiceRequest.newInstance(LOCAL_SERVICE_INSTANCE_NAME, LOCAL_SERVICE_SERVICE_TYPE);

    private DirectManager(@NonNull Context context) {
        this.mContext = context.getApplicationContext();
    }

    public static DirectManager getInstance(@NonNull Context context) {
        if (instance == null)
            synchronized (DirectManager.class) {
                if (instance == null) instance = new DirectManager(context);
            }
        return instance;
    }

    private void notifyListeners(@NonNull EventObject event) {
        BaseEventListenersUtils.INSTANCE.notifyListeners(event, mListeners);
    }

    @NonNull
    @Override
    public IConnectionType getConnectionType() {
        return new Metadata();
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }
        permissions.add(Manifest.permission.INTERNET);
        return permissions;
    }

    @Override
    public boolean isDiscovererStarted() {
        return mDiscovererStarted;
    }

    @NonNull
    @Override
    public UUID send(@NonNull DirectPeer peer, @NonNull List<Entity> entities) {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(DirectMultiSendingWorker.class)
                .setInputData(genSendingInputData(peer, entities))
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build();
        WorkManager.getInstance(mContext).enqueueUniqueWork(Constants.WORK_NAME_PREFIX_SEND + peer.getId(), ExistingWorkPolicy.APPEND_OR_REPLACE, work);
        return work.getId();
    }

    @Override
    public void registerListener(@NonNull BaseEventListener listener) {
        if (BaseEventListenersUtils.INSTANCE.isThisListenerSuitable(listener, LISTENER_TYPES_ALLOWED))
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
            public void onPeersListChanged(@NonNull Collection<WifiP2pDevice> wifiP2pDeviceList) {
                // Do not add peer here
                Set<IPeer> notChanged = new HashSet<>();
                Set<IPeer> updated = new HashSet<>();
                Set<IPeer> removed = new HashSet<>(mPeers.values());

                for (WifiP2pDevice wifiP2pDevice : wifiP2pDeviceList) {
                    Map<String, String> record = mPeerRecords.get(wifiP2pDevice.deviceAddress);
                    if (record == null) return;
                    DirectPeer oldPeer = (DirectPeer) mPeers.get(record.get(RECORD_KEY_PEER_ID));
                    if (oldPeer != null) {
                        if (wifiP2pDevice.equals(oldPeer.getWifiP2pDevice()))
                            notChanged.add(oldPeer);
                        else {
                            wifiP2pDevice.deviceName = mPeerRecords.containsKey(wifiP2pDevice.deviceAddress) ?
                                    Objects.requireNonNull(mPeerRecords.get(wifiP2pDevice.deviceAddress)).get(RECORD_KEY_DISPLAY_NAME) : wifiP2pDevice.deviceName;
                            oldPeer.setWifiP2pDevice(wifiP2pDevice);
                            updated.add(oldPeer);
                        }
                    }
                }
                removed.removeAll(notChanged);
                removed.removeAll(updated);
                for (IPeer peer : updated) {
                    peer.notifyPeerUpdated();
                    notifyListeners(new PeerUpdatedEvent(this, peer));
                }
                for (IPeer peer : removed) {
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
                        String fullDomain, @NonNull Map record, @NonNull WifiP2pDevice device) {
                    Log.d(TAG, "DnsSdTxtRecord available: " + record);
                    try {
                        @SuppressWarnings("unchecked") Map<String, String> rec = (Map<String, String>) record;
                        mPeerRecords.put(device.deviceAddress, rec);
                        // Update the device name with the human-friendly version from
                        // the DnsTxtRecord, assuming one arrived.
                        device.deviceName =
                                mPeerRecords.containsKey(device.deviceAddress) ?
                                        Objects.requireNonNull(mPeerRecords.get(device.deviceAddress)).get(RECORD_KEY_DISPLAY_NAME) : device.deviceName;

                        String shareProtocolVersion = rec.get(RECORD_KEY_SHARE_PROTOCOL_VERSION);
                        if (shareProtocolVersion == null) {
                            Log.d(TAG, String.format("The key %s for protocol version not found in DnsSdTxtRecord, ignoring...", RECORD_KEY_SHARE_PROTOCOL_VERSION));
                        } else if (shareProtocolVersion.equals(SHARE_PROTOCOL_VERSION_1)) {
                            String serverPortStr = rec.get(RECORD_KEY_SERVER_PORT);
                            String peerId = rec.get(RECORD_KEY_PEER_ID);
                            String uidStr = rec.get(RECORD_KEY_UID);
                            String serverSign = rec.get(RECORD_KEY_ACCOUNT_SERVER_SIGN);
                            if (peerId == null || serverSign == null || !StringUtils.isNumeric(serverPortStr) ||
                                    !StringUtils.isNumeric(uidStr)) {
                                Log.d(TAG, "Invalid DnsSdTxtRecord, ignoring...");
                                Log.d(TAG, "Share protocol version: " + shareProtocolVersion);
                                Log.d(TAG, "DnsSdTxtRecord: " + rec);
                                return;
                            }
                            Log.d(TAG, "Share protocol version: " + shareProtocolVersion);
                            Log.d(TAG, "Valid DnsSdTxtRecord, adding peer...");
                            IPeer peer = new DirectPeer(device, shareProtocolVersion,
                                    Integer.parseInt(serverPortStr),
                                    peerId,
                                    Integer.parseInt(uidStr),
                                    serverSign);
                            Log.d(TAG, String.format("New peer added: %s(%s)", peer.getDisplayName(), peer.getId()));
                            mPeers.put(peer.getId(), peer);
                            notifyListeners(new PeerAddedEvent(this, peer));
                        } else {
                            // Unsupported
                            Log.w(TAG, "Unsupported protocol version: %s, ignoring...");
                        }
                    } catch (ClassCastException e) {
                        Log.d(TAG, "Failed casting DnsSdTxtRecord,ignoring...");
                    }
                }
            };

            WifiP2pManager.DnsSdServiceResponseListener servListener = (instanceName, registrationType, device) -> Log.d(TAG, "Wifi Direct discovered service: " + instanceName);

            mWifiP2pManager.setDnsSdResponseListeners(mChannel, servListener, txtListener);

            mWifiP2pManager.addServiceRequest(mChannel, mServiceRequest, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    mDiscovererStarted = true;
                    notifyListeners(new DiscovererStartedEvent(this));
                }

                @Override
                public void onFailure(int reasonCode) {
                    String message = String.format(Locale.ENGLISH, "Service request adding failed: %d", reasonCode);
                    String messageLocalized = mContext.getString(R.string.error_wifidirect_service_request_add_failed, reasonCode);
                    notifyListeners(new DiscovererErrorOccurredEvent(DirectManager.this, message, messageLocalized));
                    Log.e(TAG, message);
                    stopDiscover();
                }
            });

            mWifiP2pManager.discoverServices(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    mDiscovererStarted = true;
                    notifyListeners(new DiscovererStartedEvent(this));
                }

                @Override
                public void onFailure(int reasonCode) {
                    String message = String.format(Locale.ENGLISH, "Service discovering start failed: %d", reasonCode);
                    String messageLocalized = mContext.getString(R.string.error_wifidirect_service_discovering_start_failed, reasonCode);
                    notifyListeners(new DiscovererErrorOccurredEvent(DirectManager.this, message, messageLocalized));
                    Log.e(TAG, message);
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
        try {
            mContext.unregisterReceiver(mBroadcastReceiver);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Failed unregistering BroadcastReceiver");
        }

        mWifiP2pManager.removeServiceRequest(mChannel, mServiceRequest, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                mDiscovererStarted = false;
                notifyListeners(new DiscovererStoppedEvent(this));
            }

            @Override
            public void onFailure(int reasonCode) {
                String message = String.format(Locale.ENGLISH, "Service request remove failed: %d", reasonCode);
                String messageLocalized = mContext.getString(R.string.error_wifidirect_service_request_remove_failed, reasonCode);
                notifyListeners(new DiscovererErrorOccurredEvent(DirectManager.this, message, messageLocalized));
                Log.e(TAG, message);
            }
        });

        mWifiP2pManager.stopPeerDiscovery(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                mDiscovererStarted = false;
                notifyListeners(new DiscovererStoppedEvent(this));
            }

            @Override
            public void onFailure(int reasonCode) {
                String message = String.format(Locale.ENGLISH, "Service discovering stop failed: %d", reasonCode);
                String messageLocalized = mContext.getString(R.string.error_wifidirect_service_discovering_stop_failed, reasonCode);
                notifyListeners(new DiscovererErrorOccurredEvent(DirectManager.this, message, messageLocalized));
                Log.e(TAG, message);
            }
        });
    }

    @NonNull
    @Override
    public Map<String, IPeer> getPeers() {
        return mPeers;
    }
}
