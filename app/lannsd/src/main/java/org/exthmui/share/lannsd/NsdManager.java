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

import com.google.gson.Gson;

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
import org.exthmui.share.shared.listeners.OnDiscovererErrorOccurredListener;
import org.exthmui.share.shared.listeners.OnDiscovererStartedListener;
import org.exthmui.share.shared.listeners.OnDiscovererStoppedListener;
import org.exthmui.share.shared.listeners.OnPeerAddedListener;
import org.exthmui.share.shared.listeners.OnPeerRemovedListener;
import org.exthmui.share.shared.listeners.OnPeerUpdatedListener;
import org.exthmui.share.shared.listeners.OnSenderErrorOccurredListener;
import org.exthmui.share.shared.misc.Constants;
import org.exthmui.share.shared.misc.Utils;
import org.exthmui.share.taskMgr.TaskManager;
import org.exthmui.utils.BaseEventListenersUtils;
import org.exthmui.utils.listeners.BaseEventListener;

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
        BaseEventListenersUtils.INSTANCE.notifyListeners(event, mListeners);
    }

    @NonNull
    @Override
    public IConnectionType getConnectionType() {
        return new Metadata();
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
        NsdSendingTask task = new NsdSendingTask(mContext, genSendingInputDataBundle(peer, entities));
        TaskManager.Companion.getInstance(mContext).enqueueTaskBlocking(Constants.WORK_NAME_PREFIX_SEND + peer.getId(), task);
        return UUID.fromString(task.getTaskId());
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

                String serviceName = serviceInfo.getServiceName();

                // Ignore self device, except for debug build
                if (StringUtils.equals(serviceName, NsdUtils.genNsdId(Utils.getSelfId(mContext))) && !BuildConfig.DEBUG) {
                    Log.d(TAG, String.format("Found self device: %s, ignoring...", serviceName));
                    return;
                }

                NsdPeer peer = new NsdPeer(serviceInfo, serviceName);

                Log.d(TAG, String.format("New service found, but it will not be added because it hasn't been resolved: %s", peer.getId()));

                // Try to resolve peer, if failed or unsupported, remove it
                NsdUtils.resolvePeer(mContext, peer, new NsdUtils.ResolveListener() {
                    @Override
                    public void onResolveFailed(NsdPeer peer, int errorCode) {
                        Log.d(TAG, String.format("Failed resolving peer: %s, code: %d, ignoring peer", peer.getId(), errorCode));
                    }

                    @Override
                    public void onServiceResolved(NsdPeer peer) {
                        Log.d(TAG, String.format("Successfully resolved peer, adding it: %s(%s)", peer.getDisplayName(), peer.getId()));

                        mPeers.put(peer.getId(), peer);
                        notifyListeners(new PeerAddedEvent(NsdManager.this, peer));
                    }
                });
            }


            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                String serviceName;
                serviceName = serviceInfo.getServiceName();

                String peerId = serviceName;
                if (StringUtils.isEmpty(serviceName)) {
                    Log.d(TAG, "Peer id, as well as service name is empty, ignoring...");
                }

                IPeer origPeer = mPeers.get(NsdUtils.genNsdId(peerId));
                if (origPeer == null) {
                    Log.d(TAG, String.format("Notifying service disappeared, but found no original peer: %s, ignoring...", peerId));
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
