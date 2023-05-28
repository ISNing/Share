package org.exthmui.share.services;

import android.app.Activity;
import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.TileService;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import org.exthmui.share.BuildConfig;
import org.exthmui.share.R;
import org.exthmui.share.misc.Constants;
import org.exthmui.share.shared.base.IPeer;
import org.exthmui.share.shared.base.discover.Discoverer;
import org.exthmui.share.shared.events.DiscovererStoppedEvent;
import org.exthmui.share.shared.events.PeerAddedEvent;
import org.exthmui.share.shared.events.PeerUpdatedEvent;
import org.exthmui.share.shared.listeners.OnDiscovererErrorOccurredListener;
import org.exthmui.share.shared.listeners.OnDiscovererStartedListener;
import org.exthmui.share.shared.listeners.OnDiscovererStoppedListener;
import org.exthmui.share.shared.listeners.OnPeerAddedListener;
import org.exthmui.share.shared.listeners.OnPeerRemovedListener;
import org.exthmui.share.shared.listeners.OnPeerUpdatedListener;
import org.exthmui.share.shared.listeners.OnSenderErrorOccurredListener;
import org.exthmui.share.shared.misc.SenderUtils;
import org.exthmui.utils.BaseEventListenersUtils;
import org.exthmui.utils.ServiceUtils;
import org.exthmui.utils.listeners.BaseEventListener;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DiscoverService extends ServiceUtils.MyService implements org.exthmui.share.shared.services.IDiscoverService {

    public static final String TAG = "DiscoverService";

    @SuppressWarnings("unchecked")
    private static final Class<? extends BaseEventListener>[] LISTENER_TYPES_ALLOWED = (Class<? extends BaseEventListener>[]) new Class<?>[]
            {
                    OnDiscovererStartedListener.class,
                    OnDiscovererStoppedListener.class,
                    OnPeerAddedListener.class,
                    OnPeerUpdatedListener.class,
                    OnPeerRemovedListener.class
            };

    private final Collection<BaseEventListener> mListeners = new HashSet<>();

    @IntRange(from = 0)
    private int mBindNumber = 0;

    private final Set<Discoverer> mDiscovererList = new HashSet<>();
    private final Set<BaseEventListener> mInternalListenerList = new HashSet<>();
    private final Set<BaseEventListener> mDiscovererListenerList = new HashSet<>();
    private final Map<String, IPeer> mPeerInfoMap = new HashMap<>();

    public DiscoverService() {
        initializeInternalListeners();
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
    public void onCreate() {
        super.onCreate();
        addDiscoverers();
        registerInternalListeners();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        unregisterDiscoverersListeners(mInternalListenerList);// Avoid memory leaking
        unregisterDiscoverersListeners(mDiscovererListenerList);
        Log.d(TAG, "Service going down(onDestroy), stopping discoverers");
        stopDiscoverers();
        super.onDestroy();
    }

    private void initializeInternalListeners() {
        mInternalListenerList.add((OnDiscovererStartedListener) event -> {
            updateTileState();
            notifyListeners(event);
        });
        mInternalListenerList.add((OnDiscovererStoppedListener) event -> {
            updateTileState();
            if (isAllDiscoverersStopped()) {
                notifyListeners(new DiscovererStoppedEvent(this));
                stopForeground();
            }
        });
        mInternalListenerList.add((OnPeerAddedListener) event -> {
            IPeer origPeer = mPeerInfoMap.get(event.getPeer().getId());
            if (origPeer == null) {
                mPeerInfoMap.put(event.getPeer().getId(), event.getPeer());// Then add it.
                notifyListeners(event);
            } else {// Add event notified, but a peer with same id found, handle as peer updated
                // Connection type is same
                if (origPeer.getConnectionType().getCode().equals(event.getPeer().getConnectionType().getCode())){
                    Log.w(TAG, "Got PeerAddedEvent, but a peer with same id found (with same " +
                            "connection type), dropping a new PeerUpdatedEvent");
                    mPeerInfoMap.replace(event.getPeer().getId(), event.getPeer());// Then replace it.
                }

                // Connection type are not same, but the new one's connection type's priority
                // is grater than the older one's
                else if (origPeer.getConnectionType().getPriority() < event.getPeer().getConnectionType().getPriority()) {
                    Log.d(TAG, String.format("Got PeerAddedEvent, the original peer has " +
                                    "connection type \"%s\"(Priority: %d), the newer one has " +
                                    "connection type \"%s\"(Priority: %d), replacing the older one," +
                                    "dropping a new PeerUpdatedEvent",
                            origPeer.getConnectionType().getCode(),
                            origPeer.getConnectionType().getPriority(),
                            event.getPeer().getConnectionType().getCode(),
                            event.getPeer().getConnectionType().getPriority()));
                    mPeerInfoMap.replace(event.getPeer().getId(), event.getPeer());// Then replace it.
                }

                notifyListeners(new PeerUpdatedEvent(event, event.getPeer()));
            }
        });
        mInternalListenerList.add((OnPeerUpdatedListener) event -> {
            IPeer origPeer = mPeerInfoMap.get(event.getPeer().getId());
            if (origPeer != null) {
                // Connection type is same
                if (origPeer.getConnectionType().getCode().equals(event.getPeer().getConnectionType().getCode()))
                    mPeerInfoMap.replace(event.getPeer().getId(), event.getPeer());// Then replace it.

                // Connection type are not same, but the new one's connection type's priority
                // is grater than the older one's
                else if (origPeer.getConnectionType().getPriority() <
                        event.getPeer().getConnectionType().getPriority()) {
                    Log.d(TAG, String.format("Got PeerUpdatedEvent, the original peer has " +
                            "connection type \"%s\"(Priority: %d), the newer one has " +
                            "connection type \"%s\"(Priority: %d), replacing the older one.",
                            origPeer.getConnectionType().getCode(),
                            origPeer.getConnectionType().getPriority(),
                            event.getPeer().getConnectionType().getCode(),
                            event.getPeer().getConnectionType().getPriority()));
                    mPeerInfoMap.replace(event.getPeer().getId(), event.getPeer());// Then replace it.
                }

                notifyListeners(event);
            } else {// Update event notified, but no original peer found, handle as new peer added
                Log.w(TAG, "Got PeerUpdatedEvent, but no original peer found, dropping a new" +
                        "PeerAddedEvent");
                mPeerInfoMap.put(event.getPeer().getId(), event.getPeer());// Then add it.
                notifyListeners(new PeerAddedEvent(event, event.getPeer()));
            }
        });
        mInternalListenerList.add((OnPeerRemovedListener) event -> {
            mPeerInfoMap.remove(event.getPeer().getId(), event.getPeer());
            notifyListeners(event);
        });
        mInternalListenerList.add((OnDiscovererErrorOccurredListener) event -> {
            int duration = Toast.LENGTH_LONG;

            Toast toast = Toast.makeText(getApplicationContext(),
                    getString(R.string.toast_discover_error_occurred_with_message,
                            event.getMessageLocalized()), duration);
            toast.show();
        });
        mInternalListenerList.add((OnSenderErrorOccurredListener) event -> {
            int duration = Toast.LENGTH_LONG;

            Toast toast = Toast.makeText(getApplicationContext(),
                    getString(R.string.toast_send_error_occurred_with_message,
                            event.getMessageLocalized()), duration);
            toast.show();
        });
    }

    @Override
    public void unregisterDiscoverersListeners(@NonNull Collection<BaseEventListener> listeners) {
        for (Discoverer discoverer : mDiscovererList) {
            for (BaseEventListener listener : listeners) {
                discoverer.unregisterListener(listener);
            }
        }
    }

    @Override
    @Nullable
    public Discoverer getDiscoverer(String code) {
        Constants.ConnectionType type = Constants.ConnectionType.parseFromCode(code);
        if (type == null) return null;
        for (Discoverer discoverer : mDiscovererList) {
            if (!discoverer.getClass().isAssignableFrom(type.getDiscovererClass())) continue;
            return discoverer;
        }
        return null;
    }

    @Override
    public void addDiscoverers() {
        Set<String> codes = PreferenceManager.getDefaultSharedPreferences(this).getStringSet(getString(R.string.prefs_key_global_plugins_enabled), Collections.emptySet());
        for (String code : codes) {
            addDiscoverer(code);
        }
    }

    @Override
    public void registerInternalListeners(@NonNull Discoverer discoverer) {
        for (BaseEventListener listener: mInternalListenerList) {
            discoverer.registerListener(listener);
        }
    }

    @Override
    public void registerInternalListeners() {
        for (Discoverer discoverer : mDiscovererList) {
            registerInternalListeners(discoverer);
        }
    }

    @Override
    public void notifyListeners(@NonNull EventObject event) {
        BaseEventListenersUtils.notifyListeners(event, mListeners);
    }

    @Override
    public boolean isAllDiscoverersStopped() {
        return !isAnyDiscovererStarted();
    }

    @Override
    public void registerDiscoverersListeners(@NonNull Collection<? extends BaseEventListener> listeners) {
        for (BaseEventListener listener : listeners) {
            for (Discoverer discoverer : mDiscovererList) {
                discoverer.registerListener(listener);
            }
            mDiscovererListenerList.add(listener);
        }
    }

    @Override
    public boolean isAnyDiscovererStarted() {
        for (Discoverer discoverer : mDiscovererList) {
            if (discoverer.isDiscovererStarted()) return true;
        }
        return false;
    }

    @Override
    public void addDiscoverer(String code) {
        Constants.ConnectionType type = Constants.ConnectionType.parseFromCode(code);
        if (type == null) return;
        try {
            Method method = type.getDiscovererClass().getDeclaredMethod("getInstance", Context.class);
            Discoverer discoverer = (Discoverer) method.invoke(null, this);
            assert discoverer != null;
            mDiscovererList.add(discoverer);
            registerInternalListeners(discoverer);
            updateTileState();
        } catch (@NonNull IllegalAccessException | NoSuchMethodException | InvocationTargetException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void removeDiscoverer(String code) {
        Discoverer discoverer = getDiscoverer(code);
        if (discoverer == null) return;
        if (discoverer.isDiscovererStarted()) {
            discoverer.registerListener((OnDiscovererStoppedListener) event -> {
                mDiscovererList.remove(discoverer);
                updateTileState();
            });
            discoverer.stopDiscover();
        } else {
            mDiscovererList.remove(discoverer);
        }
    }

    private void startDiscoverer(Discoverer discoverer) {
        if (discoverer == null) return;
        if (!discoverer.isInitialized()) discoverer.initialize();
        if (!discoverer.isInitialized()) Log.e(TAG, String.format("Failed initializing Discoverer %s", discoverer.getConnectionType().getCode()));
        if (discoverer.isDiscovererStarted()) {
            Log.e(TAG, String.format("Discoverer %s has already been started, " +
                    "ignoring starting request", discoverer.getConnectionType().getCode()));
            return;
        }
        discoverer.startDiscover();
    }

    private void stopDiscoverer(Discoverer discoverer) {
        if (discoverer == null) return;
        if (!discoverer.isInitialized()) discoverer.initialize();
        if (!discoverer.isInitialized()) Log.e(TAG, String.format("Failed initializing Discoverer %s", discoverer.getConnectionType().getCode()));
        if (!discoverer.isDiscovererStarted()) {
            Log.e(TAG, String.format("Discoverer %s has already been stopped, " +
                    "ignoring stopping request", discoverer.getConnectionType().getCode()));
            return;
        }
        discoverer.stopDiscover();
    }

    @Override
    public void startDiscoverer(String code) {
        Discoverer discoverer = getDiscoverer(code);
        startDiscoverer(discoverer);
        updateTileState();
    }

    @Override
    public void stopDiscoverer(String code) {
        Discoverer discoverer = getDiscoverer(code);
        stopDiscoverer(discoverer);
        updateTileState();
    }

    @Override
    public void restartDiscoverers() {
        stopDiscoverers();
        startDiscoverers();
    }

    @Override
    public void startDiscoverers() {
        for (Discoverer discoverer : mDiscovererList) {
            startDiscoverer(discoverer);
        }
    }

    @Override
    public void stopDiscoverers() {
        for (Discoverer discoverer : mDiscovererList) {
            stopDiscoverer(discoverer);
        }
    }

    @Override
    public void registerDiscovererListeners(@NonNull Collection<? extends BaseEventListener> listeners, String code) {
        Discoverer discoverer = getDiscoverer(code);
        if (discoverer == null) return;
        for (BaseEventListener listener : listeners) {
            discoverer.registerListener(listener);
        }
    }

    @Override
    public void unregisterDiscovererListeners(@NonNull Collection<? extends BaseEventListener> listeners, String code) {
        Discoverer discoverer = getDiscoverer(code);
        if (discoverer == null) return;
        for (BaseEventListener listener : listeners) {
            discoverer.unregisterListener(listener);
        }
    }

    @Override
    public boolean isAnyDiscovererStopped() {
        return !isAllDiscoverersStarted();
    }

    @Override
    public boolean isAllDiscoverersStarted() {
        for (Discoverer discoverer : mDiscovererList) {
            if (!discoverer.isDiscovererStarted()) return false;
        }
        return true;
    }

    @Override
    public boolean isDiscovererStopped(String code) {
        return !isDiscovererStarted(code);
    }

    @Override
    public boolean isDiscovererStarted(String code) {
        Constants.ConnectionType connectionType = Constants.ConnectionType.parseFromCode(code);
        if (connectionType == null) return false;
        for (Discoverer discoverer : mDiscovererList) {
            if (!discoverer.getClass().isAssignableFrom(connectionType.getDiscovererClass()))
                continue;
            if (discoverer.isDiscovererStarted()) return true;
        }
        return false;
    }

    @Override
    public boolean isDiscoverersAvailable() {
        for (Discoverer discoverer : mDiscovererList) {
            if (discoverer.isFeatureAvailable() && discoverer.getPermissionNotGranted().isEmpty())
                return true;
        }
        return false;
    }

    @Override
    public boolean isDiscovererAvailable(String code) {
        Discoverer discoverer = getDiscoverer(code);
        if (discoverer == null) return false;
        return discoverer.isFeatureAvailable() && discoverer.getPermissionNotGranted().isEmpty();
    }

    @NonNull
    @Override
    public Set<String> getDiscoverersPermissionsNotGranted() {
        Set<String> permissions = new HashSet<>();
        for (Discoverer discoverer : mDiscovererList) {
            permissions.addAll(discoverer.getPermissionNotGranted());
        }
        return permissions;
    }

    @NonNull
    @Override
    public Set<String> getDiscovererPermissionsNotGranted(String code) {
        Discoverer discoverer = getDiscoverer(code);
        if (discoverer == null) return Collections.emptySet();
        return discoverer.getPermissionNotGranted();
    }

    @Override
    public void grantDiscoverPermissions(@NonNull Activity activity) {
        String[] permissions = getDiscoverersPermissionsNotGranted().toArray(new String[0]);
        if (permissions.length == 0) return;
        ActivityCompat.requestPermissions(activity, permissions, REQUEST_CODE_GRANT_PERMISSIONS);
    }

    @Override
    public void grantDiscovererPermissions(String code, @NonNull Activity activity, @IntRange(from = 0) int requestCode) {
        String[] permissions = getDiscovererPermissionsNotGranted(code).toArray(new String[0]);
        if (permissions.length == 0) return;
        ActivityCompat.requestPermissions(activity, permissions, requestCode);
    }

    @NonNull
    @Override
    public Map<String, IPeer> getPeerInfoMap() {
        return mPeerInfoMap;
    }

    private void updateTileState() {
        TileService.requestListeningState(this, new ComponentName(BuildConfig.APPLICATION_ID, DiscoveringTileService.class.getName()));
    }

    private void startForeground() {
        Notification notification = SenderUtils.buildServiceNotification(this);
        startForeground(this.hashCode(), notification);
    }

    private void stopForeground() {
        stopForeground(true);
    }

    @Override
    public void onBind(Intent intent, Object ignored) {
        Log.d(TAG, "Service going to be bound(onBind)");
        mBindNumber++;
        stopForeground();
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d(TAG, "Service going to be rebound(onRebind)");
        super.onRebind(intent);
        mBindNumber++;
        stopForeground();
    }

    /**
     * IMPORTANT: To keep service alive when discoverer running, MUST be called before unbind service.
     */
    public void beforeUnbind() {
        if (mBindNumber > 1) return;
        Log.d(TAG, "Checking whether to start service and in foreground or in background(beforeUnbind)");
        if (isAnyDiscovererStarted()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d(TAG, String.format("Discoverer running, API newer than %d, starting service in foreground", Build.VERSION_CODES.O));
                startForegroundService(new Intent(getApplicationContext(), DiscoverService.class));
                startForeground();
            } else {
                Log.d(TAG, String.format("Discoverer running, API lower than %d, starting service in background", Build.VERSION_CODES.O));
                startService(new Intent(getApplicationContext(), DiscoverService.class));
            }
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Service going to be unbound(onUnbind)");
        mBindNumber--;
        return true;
    }
}