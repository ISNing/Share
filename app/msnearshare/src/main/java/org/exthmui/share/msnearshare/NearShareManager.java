package org.exthmui.share.msnearshare;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkManager;

import com.microsoft.connecteddevices.ConnectedDevicesAccount;
import com.microsoft.connecteddevices.ConnectedDevicesAccountManager;
import com.microsoft.connecteddevices.ConnectedDevicesAddAccountResult;
import com.microsoft.connecteddevices.ConnectedDevicesNotificationRegistrationManager;
import com.microsoft.connecteddevices.ConnectedDevicesPlatform;
import com.microsoft.connecteddevices.remotesystems.RemoteSystem;
import com.microsoft.connecteddevices.remotesystems.RemoteSystemAuthorizationKind;
import com.microsoft.connecteddevices.remotesystems.RemoteSystemAuthorizationKindFilter;
import com.microsoft.connecteddevices.remotesystems.RemoteSystemDiscoveryType;
import com.microsoft.connecteddevices.remotesystems.RemoteSystemDiscoveryTypeFilter;
import com.microsoft.connecteddevices.remotesystems.RemoteSystemFilter;
import com.microsoft.connecteddevices.remotesystems.RemoteSystemStatusType;
import com.microsoft.connecteddevices.remotesystems.RemoteSystemStatusTypeFilter;
import com.microsoft.connecteddevices.remotesystems.RemoteSystemWatcher;

import org.exthmui.share.shared.BaseEventListenersUtils;
import org.exthmui.share.shared.Constants;
import org.exthmui.share.shared.base.Discoverer;
import org.exthmui.share.shared.base.Entity;
import org.exthmui.share.shared.base.PeerInfo;
import org.exthmui.share.shared.base.Sender;
import org.exthmui.share.shared.base.events.DiscovererStartedEvent;
import org.exthmui.share.shared.base.events.DiscovererStoppedEvent;
import org.exthmui.share.shared.base.events.PeerAddedEvent;
import org.exthmui.share.shared.base.events.PeerUpdatedEvent;
import org.exthmui.share.shared.base.listeners.BaseEventListener;
import org.exthmui.share.shared.base.listeners.OnDiscovererStartedListener;
import org.exthmui.share.shared.base.listeners.OnDiscovererStoppedListener;
import org.exthmui.share.shared.base.listeners.OnPeerAddedListener;
import org.exthmui.share.shared.base.listeners.OnPeerRemovedListener;
import org.exthmui.share.shared.base.listeners.OnPeerUpdatedListener;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings("unchecked")
public class NearShareManager implements Sender<NearSharePeer>, Discoverer {

    private static final String TAG = "NearShareManager";

    private static final Class<? extends BaseEventListener>[] LISTENER_TYPES_ALLOWED;

    private final Collection<BaseEventListener> mListeners = new HashSet<>();

    static {
        LISTENER_TYPES_ALLOWED = (Class<? extends BaseEventListener>[]) new Class<?>[]
                {
                        OnDiscovererStartedListener.class,
                        OnDiscovererStoppedListener.class,
                        OnPeerAddedListener.class,
                        OnPeerUpdatedListener.class,
                        OnPeerRemovedListener.class
                };
    }


    private static NearShareManager instance;
    private final Context mContext;
    private final Map<String, PeerInfo> mPeers = new HashMap<>();
    private boolean mDiscovererStarted;
    private boolean mInitialized;


    private ConnectedDevicesPlatform mPlatform;
    @Nullable private RemoteSystemWatcher mRemoteSystemWatcher;

    @Override
    public void registerListener(BaseEventListener listener) {
        if (BaseEventListenersUtils.isThisListenerSuitable(listener, LISTENER_TYPES_ALLOWED))
            mListeners.add(listener);
    }
    @Override
    public void unregisterListener(BaseEventListener listener) {
        mListeners.remove(listener);
    }

    private void notifyListeners(EventObject event){
        BaseEventListenersUtils.notifyListeners(event, mListeners);
    }

    public static NearShareManager getInstance(Context context){
        if(instance == null) instance = new NearShareManager(context);
        return instance;
    }

    private NearShareManager(Context context) {
        this.mContext = context.getApplicationContext();
        initializePlatform();
        initialize();
    }

    private void initializePlatform() {
        mPlatform = new ConnectedDevicesPlatform(mContext);

        final ConnectedDevicesAccountManager accountManager = mPlatform.getAccountManager();

        accountManager.accessTokenRequested().subscribe((manager, args) -> {
        });

        accountManager.accessTokenInvalidated().subscribe((manager, args) -> {
        });

        final ConnectedDevicesNotificationRegistrationManager registrationManager
                = mPlatform.getNotificationRegistrationManager();

        registrationManager.notificationRegistrationStateChanged().subscribe((manager, args) -> {
        });

        mPlatform.start();

        createAndAddAnonymousAccount(mPlatform);
    }

    /**
     * NearShare just works with anonymous account.
     */
    private void createAndAddAnonymousAccount(ConnectedDevicesPlatform platform) {
        ConnectedDevicesAccount account = ConnectedDevicesAccount.getAnonymousAccount();
        platform.getAccountManager().addAccountAsync(account).whenComplete((ConnectedDevicesAddAccountResult result, Throwable throwable) -> {
            if (throwable != null) {
                Log.d(TAG, String.format("AccountManager addAccountAsync returned a throwable: %1$s", throwable.getMessage()));
            } else {
                Log.d(TAG, "AccountManager : Added account successfully");
            }
        });
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
        permissions.add(Manifest.permission.INTERNET);
        permissions.add(Manifest.permission.BLUETOOTH);
        permissions.add(Manifest.permission.BLUETOOTH_ADMIN);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_NETWORK_STATE);
        return permissions;
    }

    @Override
    public void initialize() {
        this.mInitialized = true;
    }

    @Override
    public boolean isDiscovererStarted() {
        return mDiscovererStarted;
    }

    @Override
    public void startDiscover() {
        ArrayList<RemoteSystemFilter> filters = new ArrayList<>();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        // Config filters
        boolean proximalMode = preferences.getBoolean("proximal_mode", false);
        if (proximalMode) {
            filters.add(new RemoteSystemDiscoveryTypeFilter(RemoteSystemDiscoveryType.PROXIMAL));
        } else {
            filters.add(new RemoteSystemDiscoveryTypeFilter(RemoteSystemDiscoveryType.SPATIALLY_PROXIMAL));
        }

        filters.add(new RemoteSystemStatusTypeFilter(RemoteSystemStatusType.ANY));
        filters.add(new RemoteSystemAuthorizationKindFilter(RemoteSystemAuthorizationKind.ANONYMOUS));

        mRemoteSystemWatcher = new RemoteSystemWatcher(filters);
        final WeakReference<RemoteSystemWatcher> weakRemoteSystemWatcher = new WeakReference<>(mRemoteSystemWatcher);
        // Add Listeners
        weakRemoteSystemWatcher.get().remoteSystemAdded().subscribe((remoteSystemWatcher, args) -> {
            final RemoteSystem remoteSystem = args.getRemoteSystem();
            NearSharePeer peer = new NearSharePeer(remoteSystem);
            notifyListeners(new PeerAddedEvent(this, peer));
            mPeers.put(peer.getId(), peer);
            Log.d(TAG, String.format("Peer added: %s", peer.getDisplayName()));
        });
        weakRemoteSystemWatcher.get().remoteSystemUpdated().subscribe((remoteSystemWatcher, args) -> {
            final RemoteSystem remoteSystem = args.getRemoteSystem();
            PeerInfo peer = new NearSharePeer(remoteSystem);
            mPeers.remove(peer.getId());
            mPeers.put(peer.getId(), peer);
            notifyListeners(new PeerUpdatedEvent(this, peer));
            Log.d(TAG, String.format("Peer updated: %s", peer.getDisplayName()));
        });
        weakRemoteSystemWatcher.get().remoteSystemRemoved().subscribe((remoteSystemWatcher, args) -> {
            final RemoteSystem remoteSystemParam = args.getRemoteSystem();
            NearSharePeer peer = new NearSharePeer(remoteSystemParam);
            mPeers.remove(peer.getId());
            Log.d(TAG, String.format("Peer removed: %s", peer.getDisplayName()));
        });
        weakRemoteSystemWatcher.get().errorOccurred().subscribe((remoteSystemWatcher, args) -> Log.e(TAG, String.format("Discovery error: %1$s", args.getError())));

        // Stop watcher if it has already started
        if (mDiscovererStarted) {
            weakRemoteSystemWatcher.get().stop();
            mDiscovererStarted = false;
        }

        weakRemoteSystemWatcher.get().start();
        mDiscovererStarted = true;
        notifyListeners(new DiscovererStartedEvent(this));
    }

    @Override
    public void stopDiscover() {
        if (mRemoteSystemWatcher != null)
            mRemoteSystemWatcher.stop();
        mDiscovererStarted = false;
        notifyListeners(new DiscovererStoppedEvent(this));
    }

    @Override
    public Map<String, PeerInfo> getPeers() {
        return mPeers;
    }

    /**TODO:Make use of it
     * Send URI to the target device using nearshare
     */
//    private void sendUri(PeerInfo t, String uriText) {
//        RemoteSystemConnectionRequest remoteSystemConnectionRequest = new RemoteSystemConnectionRequest(mSelectedRemoteSystem);
//
//        if (mNearShareSender.isNearShareSupported(remoteSystemConnectionRequest)) {
//            mNearShareSender.sendUriAsync(remoteSystemConnectionRequest, uriText);
//        } else {
//            Log.d(TAG, "NearShare is not supported in this device");
//        }
//    }

    @Override
    public UUID send(NearSharePeer peer, Entity entity) {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(NearShareSendingWorker.class)
                .setInputData(genSendingInputData(peer, entity))
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build();
        WorkManager.getInstance(mContext).enqueueUniqueWork(Constants.WORK_NAME_PREFIX_SEND + peer.getId(), ExistingWorkPolicy.APPEND_OR_REPLACE, work);
        return work.getId();
    }

    @Override
    public UUID[] send(NearSharePeer peer, List<Entity> entities) {
        UUID[] uuids = new UUID[entities.size()];
        int i = 0;
        for (Entity entity :entities) {
            uuids[i] = send(peer, entity);
            i++;
        }
        return uuids;
    }

    @Override
    public boolean isFeatureAvailable() {
        return mContext.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH) &
                mContext.getPackageManager().hasSystemFeature(
                        PackageManager.FEATURE_WIFI);
    }

    @Override
    public boolean isInitialized() {
        return mInitialized;
    }


}
