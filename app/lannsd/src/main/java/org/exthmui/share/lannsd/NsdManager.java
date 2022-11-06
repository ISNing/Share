package org.exthmui.share.lannsd;


import static android.net.nsd.NsdManager.PROTOCOL_DNS_SD;
import static org.exthmui.share.lannsd.Constants.LOCAL_SERVICE_SERVICE_TYPE;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkManager;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.exthmui.share.shared.base.Entity;
import org.exthmui.share.shared.base.IPeer;
import org.exthmui.share.shared.base.discover.Discoverer;
import org.exthmui.share.shared.base.send.Sender;
import org.exthmui.share.shared.events.DiscovererErrorOccurredEvent;
import org.exthmui.share.shared.events.DiscovererStartedEvent;
import org.exthmui.share.shared.events.DiscovererStoppedEvent;
import org.exthmui.share.shared.events.PeerAddedEvent;
import org.exthmui.share.shared.events.PeerRemovedEvent;
import org.exthmui.share.shared.events.PeerUpdatedEvent;
import org.exthmui.share.shared.listeners.BaseEventListener;
import org.exthmui.share.shared.listeners.OnDiscovererErrorOccurredListener;
import org.exthmui.share.shared.listeners.OnDiscovererStartedListener;
import org.exthmui.share.shared.listeners.OnDiscovererStoppedListener;
import org.exthmui.share.shared.listeners.OnPeerAddedListener;
import org.exthmui.share.shared.listeners.OnPeerRemovedListener;
import org.exthmui.share.shared.listeners.OnPeerUpdatedListener;
import org.exthmui.share.shared.listeners.OnSenderErrorOccurredListener;
import org.exthmui.share.shared.misc.BaseEventListenersUtils;
import org.exthmui.share.shared.misc.Constants;

import java.util.Collection;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class NsdManager implements Discoverer, Sender<NsdPeer> {

    public static final String TAG = "NsdManager";

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

    private static final Gson GSON = new Gson();

    private static volatile NsdManager instance;

    private final Collection<BaseEventListener> mListeners = new HashSet<>();
    private final Context mContext;
    private final Map<String, IPeer> mPeers = new HashMap<>();
    private boolean mDiscovererStarted;
    private boolean mInitialized;

    private android.net.nsd.NsdManager mNsdManager;
    private android.net.nsd.NsdManager.DiscoveryListener mDiscoveryListener;

    private NsdManager(@NonNull Context context) {
        this.mContext = context.getApplicationContext();
    }

    public static NsdManager getInstance(@NonNull Context context) {
        if (instance == null)
            synchronized (NsdManager.class) {
                if (instance == null) instance = new NsdManager(context);
            }
        return instance;
    }

    private void notifyListeners(@NonNull EventObject event) {
        BaseEventListenersUtils.notifyListeners(event, mListeners);
    }

    public android.net.nsd.NsdManager getNsdManager() {
        return mNsdManager;
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
    public boolean isDiscovererStarted() {
        return mDiscovererStarted;
    }

    @NonNull
    @Override
    public UUID send(@NonNull NsdPeer peer, @NonNull List<Entity> entities) {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(NsdMultiSendingWorker.class)
                .setInputData(genSendingInputData(peer, entities))
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build();
        WorkManager.getInstance(mContext).enqueueUniqueWork(Constants.WORK_NAME_PREFIX_SEND + peer.getId() + work.hashCode(), ExistingWorkPolicy.KEEP, work);
        return work.getId();
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
        this.mNsdManager = (android.net.nsd.NsdManager) mContext.getSystemService(Context.NSD_SERVICE);
        this.mDiscoveryListener = new android.net.nsd.NsdManager.DiscoveryListener() {

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                String message = String.format(Locale.ENGLISH, "Service discovering start failed: %d", errorCode);
                String messageLocalized = mContext.getString(R.string.error_lannsd_service_discovering_start_failed, errorCode);
                notifyListeners(new DiscovererErrorOccurredEvent(NsdManager.this, message, messageLocalized));
                Log.e(TAG, message);
                stopDiscover();
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                String message = String.format(Locale.ENGLISH, "Service discovering stop failed: %d", errorCode);
                String messageLocalized = mContext.getString(R.string.error_lannsd_service_discovering_stop_failed, errorCode);
                notifyListeners(new DiscovererErrorOccurredEvent(NsdManager.this, message, messageLocalized));
                Log.e(TAG, message);
            }

            @Override
            public void onDiscoveryStarted(String serviceType) {
                mDiscovererStarted = true;
                notifyListeners(new DiscovererStartedEvent(this));
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                mDiscovererStarted = false;
                notifyListeners(new DiscovererStoppedEvent(this));
            }

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "LAN Network Service Discovery discovered service: " + serviceInfo.getServiceName());

                ServiceNameModel serviceNameModel;
                try {
                    serviceNameModel = GSON.fromJson(serviceInfo.getServiceName(), ServiceNameModel.class);
                } catch (JsonSyntaxException ignored) {
                    Log.d(TAG, String.format("Illegal json syntax of service name: %s, ignoring...", serviceInfo.getServiceName()));
                    return;
                }
//                if (serviceNameModel.getPeerId().equals(NsdUtils.genNsdId(Utils.getSelfId(mContext)))) {
//                    Log.d(TAG, String.format("Found self device: %s, ignoring...", serviceNameModel.getPeerId()));
//                    return;TODO
//                }

                NsdPeer peer = new NsdPeer(serviceInfo, serviceNameModel.getDeviceType(),
                        serviceNameModel.getPeerId(), serviceNameModel.getDisplayName());

                mPeers.put(peer.getId(), peer);
                notifyListeners(new PeerAddedEvent(NsdManager.this, peer));
                Log.d(TAG, String.format("New peer added: %s(%s)", peer.getDisplayName(), peer.getId()));

                // Try to resolve peer, if failed or unsupported, remove it
                NsdUtils.resolvePeer(mContext, peer, new NsdUtils.ResolveListener() {
                    @Override
                    public void onResolveFailed(NsdPeer peer, int errorCode) {
                        Log.d(TAG, String.format("Failed resolving peer: %s, code: %d, removing peer", peer.getId(), errorCode));

                        mPeers.remove(peer.getId());
                        notifyListeners(new PeerRemovedEvent(NsdManager.this, peer));
                    }

                    @Override
                    public void onServiceResolved(NsdPeer peer) {
                        Log.d(TAG, String.format("Successfully resolved peer: %s", peer.getId()));

                        mPeers.put(peer.getId(), peer);
                        notifyListeners(new PeerUpdatedEvent(NsdManager.this, peer));
                    }
                });
            }


            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {ServiceNameModel serviceNameModel;
                try {
                    serviceNameModel = GSON.fromJson(serviceInfo.getServiceName(), ServiceNameModel.class);
                } catch (JsonSyntaxException ignored) {
                    Log.d(TAG, String.format("Illegal json syntax of service name: %s, ignoring...", serviceInfo.getServiceName()));
                    return;
                }
                String peerId = serviceNameModel.getPeerId();
                if (peerId == null) {
                    Log.d(TAG, "Peer id not found in service name model, ignoring...");
                }

                IPeer origPeer = mPeers.get(NsdUtils.genNsdId(peerId));
                if (origPeer == null) {
                    Log.d(TAG, String.format("Notifying service disappeared, but found no original peer: %s, ingoring...", peerId));
                } else {
                    mPeers.remove(origPeer.getId());
                    notifyListeners(new PeerRemovedEvent(NsdManager.this, origPeer));
                }
            }
        };

        this.mInitialized = true;
    }

    @Override
    public boolean isInitialized() {
        return mInitialized;
    }

    @Override
    public boolean isFeatureAvailable() {
        return true;
    }

    @Override
    public void startDiscover() {
        mNsdManager.discoverServices(LOCAL_SERVICE_SERVICE_TYPE, PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    @Override
    public void stopDiscover() {
        mNsdManager.stopServiceDiscovery(mDiscoveryListener);
    }

    @NonNull
    @Override
    public Map<String, IPeer> getPeers() {
        return mPeers;
    }
}
