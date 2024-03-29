package org.exthmui.share.msnearshare;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
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
import com.microsoft.connecteddevices.ConnectedDevicesAccountAddedStatus;
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

import java.lang.ref.WeakReference;
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


public class NearShareManager implements Sender<NearSharePeer>, Discoverer {

    public static final String TAG = "NearShareManager";

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

    private final Collection<BaseEventListener> mListeners = new HashSet<>();

    private static volatile NearShareManager instance;
    private final Context mContext;
    private final Map<String, IPeer> mPeers = new HashMap<>();
    private boolean mDiscovererStarted;
    private boolean mInitialized;


    private ConnectedDevicesPlatform mPlatform;
    @Nullable
    private RemoteSystemWatcher mRemoteSystemWatcher;

    @Override
    public void registerListener(@NonNull BaseEventListener listener) {
        if (BaseEventListenersUtils.INSTANCE.isThisListenerSuitable(listener, LISTENER_TYPES_ALLOWED))
            mListeners.add(listener);
    }

    @Override
    public void unregisterListener(BaseEventListener listener) {
        mListeners.remove(listener);
    }

    private void notifyListeners(@NonNull EventObject event){
        BaseEventListenersUtils.INSTANCE.notifyListeners(event, mListeners);
    }

    @NonNull
    @Override
    public IConnectionType getConnectionType() {
        return new Metadata();
    }

    public static NearShareManager getInstance(@NonNull Context context){
        if(instance == null)
            synchronized (NearShareManager.class){
                if(instance == null) instance = new NearShareManager(context);
            }
        return instance;
    }

    private NearShareManager(@NonNull Context context) {
        this.mContext = context.getApplicationContext();
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
    private void createAndAddAnonymousAccount(@NonNull ConnectedDevicesPlatform platform) {
        ConnectedDevicesAccount account = ConnectedDevicesAccount.getAnonymousAccount();
        platform.getAccountManager().addAccountAsync(account).whenComplete((ConnectedDevicesAddAccountResult result, Throwable throwable) -> {
            if (result.getStatus() == ConnectedDevicesAccountAddedStatus.SUCCESS) {
                Log.d(TAG, "AccountManager: Added account successfully");
            } else {
                String message;
                String messageLocalized;
                if (throwable == null) {
                    message = String.format(Locale.ENGLISH, "AccountManager addAccountAsync error: %1$s", result.getStatus());
                    messageLocalized = mContext.getString(R.string.error_msnearshare_create_add_anonymous_account_failed, result.getStatus());
                } else {
                    message = String.format(Locale.ENGLISH, "AccountManager addAccountAsync error: %1$s, with throwable: %2$s", result.getStatus(), throwable.getMessage());
                    messageLocalized = mContext.getString(R.string.error_msnearshare_create_add_anonymous_account_failed_with_throwable, result.getStatus(), throwable.getLocalizedMessage());
                }
                notifyListeners(new DiscovererErrorOccurredEvent(this, message, messageLocalized));
                Log.e(TAG, message);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }
        permissions.add(Manifest.permission.ACCESS_NETWORK_STATE);
        return permissions;
    }

    @Override
    public void initialize() {
        initializePlatform();
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
            mPeers.put(peer.getId(), peer);
            notifyListeners(new PeerAddedEvent(this, peer));
            Log.d(TAG, String.format("Peer added: %s", peer.getDisplayName()));
        });
        weakRemoteSystemWatcher.get().remoteSystemUpdated().subscribe((remoteSystemWatcher, args) -> {
            final RemoteSystem remoteSystem = args.getRemoteSystem();
            IPeer peer = new NearSharePeer(remoteSystem);
            mPeers.remove(peer.getId());
            mPeers.put(peer.getId(), peer);
            notifyListeners(new PeerUpdatedEvent(this, peer));
            Log.d(TAG, String.format("Peer updated: %s", peer.getDisplayName()));
        });
        weakRemoteSystemWatcher.get().remoteSystemRemoved().subscribe((remoteSystemWatcher, args) -> {
            final RemoteSystem remoteSystemParam = args.getRemoteSystem();
            NearSharePeer peer = new NearSharePeer(remoteSystemParam);
            notifyListeners(new PeerRemovedEvent(this, mPeers.get(peer.getId())));
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

    @NonNull
    @Override
    public Map<String, IPeer> getPeers() {
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

    @NonNull
    @Override
    public UUID send(@NonNull NearSharePeer peer, @NonNull List<Entity> entities) {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(NearShareMultiSendingWorker.class)
                .setInputData(genSendingInputData(peer, entities))
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build();
        WorkManager.getInstance(mContext).enqueueUniqueWork(Constants.WORK_NAME_PREFIX_SEND + peer.getId(), ExistingWorkPolicy.APPEND_OR_REPLACE, work);
        return work.getId();
    }

    @Override
    public boolean isFeatureAvailable() {
        return mContext.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH) &&
                mContext.getPackageManager().hasSystemFeature(
                        PackageManager.FEATURE_WIFI);
    }

    @Override
    public boolean isInitialized() {
        return mInitialized;
    }
}
