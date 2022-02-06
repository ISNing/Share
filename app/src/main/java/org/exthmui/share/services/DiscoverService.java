package org.exthmui.share.services;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.service.quicksettings.TileService;

import androidx.annotation.IntRange;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import org.exthmui.share.BuildConfig;
import org.exthmui.share.R;
import org.exthmui.share.misc.Constants;
import org.exthmui.share.shared.BaseEventListenersUtils;
import org.exthmui.share.shared.ServiceUtils;
import org.exthmui.share.shared.base.Discoverer;
import org.exthmui.share.shared.base.PeerInfo;
import org.exthmui.share.shared.base.listeners.BaseEventListener;
import org.exthmui.share.shared.base.listeners.OnDiscovererStartedListener;
import org.exthmui.share.shared.base.listeners.OnDiscovererStoppedListener;
import org.exthmui.share.shared.base.listeners.OnPeerAddedListener;
import org.exthmui.share.shared.base.listeners.OnPeerRemovedListener;
import org.exthmui.share.shared.base.listeners.OnPeerUpdatedListener;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DiscoverService extends ServiceUtils.MyService {

    private static final String TAG = "DiscoverService";

    public static final int REQUEST_CODE_GRANT_PERMISSIONS = 0;

    private static final Class<? extends BaseEventListener>[] LISTENER_TYPES_ALLOWED;
    private static DiscoverService instance;

    static {
        LISTENER_TYPES_ALLOWED = (Class<? extends BaseEventListener>[]) new Class<?>[]
                {
                        OnDiscovererStartedListener.class,
                        OnDiscovererStoppedListener.class
                };
    }

    private final Collection<BaseEventListener> mListeners = new HashSet<>();

    private final Set<Discoverer> mDiscovererList = new HashSet<>();
    private final Set<BaseEventListener> mDiscovererListenerList = new HashSet<>();
    private final Map<String, PeerInfo> mPeerInfoMap = new HashMap<>();

    public DiscoverService() {
    }

    public static DiscoverService getInstance() {
        if (instance == null) instance = new DiscoverService();
        return instance;
    }

    public void registerListener(BaseEventListener listener) {
        if (BaseEventListenersUtils.isThisListenerSuitable(listener, LISTENER_TYPES_ALLOWED))
            mListeners.add(listener);
    }

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
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterDiscoverersListeners(mDiscovererListenerList);
        stopDiscoverers();
    }

    public void unregisterDiscoverersListeners(Collection<BaseEventListener> listeners) {
        for (Discoverer discoverer : mDiscovererList) {
            for (BaseEventListener listener : listeners) {
                discoverer.unregisterListener(listener);
            }
        }
        registerInternalListeners();
    }

    @Nullable
    private Discoverer getDiscoverer(String code) {
        Constants.ConnectionType type = Constants.ConnectionType.parseFromCode(code);
        if (type == null) return null;
        for (Discoverer discoverer : mDiscovererList) {
            if (!discoverer.getClass().isAssignableFrom(type.getDiscovererClass())) continue;
            return discoverer;
        }
        return null;
    }

    public void stopDiscoverers() {
        for (Discoverer discoverer : mDiscovererList) {
            if (discoverer.isDiscovererStarted())
                discoverer.stopDiscover();
        }
    }

    public void addDiscoverers() {
        Set<String> codes = PreferenceManager.getDefaultSharedPreferences(this).getStringSet(getString(R.string.prefs_key_global_plugins_enabled), Collections.emptySet());
        for (String code : codes) {
            addDiscoverer(code);
        }
    }

    private void registerInternalListeners() {
        Set<BaseEventListener> listeners = new HashSet<>();
        listeners.add((OnDiscovererStartedListener) event -> {
            TileService.requestListeningState(this, new ComponentName(BuildConfig.APPLICATION_ID, DiscoveringTileService.class.getName()));
            notifyListeners(event);
        });
        listeners.add((OnDiscovererStoppedListener) event -> {
            TileService.requestListeningState(this, new ComponentName(BuildConfig.APPLICATION_ID, DiscoveringTileService.class.getName()));
            if (isAllDiscoverersStopped()) notifyListeners(event);
        });
        listeners.add((OnPeerAddedListener) event -> mPeerInfoMap.put(event.getPeer().getId(), event.getPeer()));
        listeners.add((OnPeerUpdatedListener) event -> mPeerInfoMap.replace(event.getPeer().getId(), event.getPeer()));
        listeners.add((OnPeerRemovedListener) event -> mPeerInfoMap.remove(event.getPeer().getId(), event.getPeer()));
        registerDiscoverersListeners(listeners);
    }

    private void notifyListeners(EventObject event) {
        BaseEventListenersUtils.notifyListeners(event, mListeners);
    }

    public boolean isAllDiscoverersStopped() {
        return !isAnyDiscovererStarted();
    }

    public void registerDiscoverersListeners(Collection<? extends BaseEventListener> listeners) {
        for (Discoverer discoverer : mDiscovererList) {
            for (BaseEventListener listener : listeners) {
                discoverer.registerListener(listener);
            }
        }
    }

    public boolean isAnyDiscovererStarted() {
        for (Discoverer discoverer : mDiscovererList) {
            if (discoverer.isDiscovererStarted()) return true;
        }
        return false;
    }

    public void addDiscoverer(String code) {
        Constants.ConnectionType type = Constants.ConnectionType.parseFromCode(code);
        if (type == null) return;
        try {
            Method method = type.getDiscovererClass().getDeclaredMethod("getInstance", Context.class);
            Discoverer discoverer = (Discoverer) method.invoke(null, this);
            mDiscovererList.add(discoverer);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException exception) {
            exception.printStackTrace();
        }
    }

    public void removeDiscoverer(String code) {
        Discoverer discoverer = getDiscoverer(code);
        if (discoverer == null) return;
        if (discoverer.isDiscovererStarted()) {
            discoverer.registerListener((OnDiscovererStoppedListener) event -> mDiscovererList.remove(discoverer));
            discoverer.stopDiscover();
        } else {
            mDiscovererList.remove(discoverer);
        }
    }

    public void startDiscoverer(String code) {
        Discoverer discoverer = getDiscoverer(code);
        if (discoverer == null) return;
        discoverer.startDiscover();
    }

    public void stopDiscoverer(String code) {
        Discoverer discoverer = getDiscoverer(code);
        if (discoverer == null) return;
        discoverer.stopDiscover();
    }

    public void restartDiscoverers() {
        stopDiscoverers();
        startDiscoverers();
    }

    public void startDiscoverers() {
        for (Discoverer discoverer : mDiscovererList) {
            if (!discoverer.isInitialized()) discoverer.initialize();
            discoverer.startDiscover();
        }
    }

    public void registerDiscovererListeners(Collection<? extends BaseEventListener> listeners, String code) {
        Discoverer discoverer = getDiscoverer(code);
        if (discoverer == null) return;
        for (BaseEventListener listener : listeners) {
            discoverer.registerListener(listener);
        }
    }

    public void unregisterDiscovererListeners(Collection<? extends BaseEventListener> listeners, String code) {
        Discoverer discoverer = getDiscoverer(code);
        if (discoverer == null) return;
        for (BaseEventListener listener : listeners) {
            discoverer.unregisterListener(listener);
        }
    }

    public boolean isAnyDiscovererStopped() {
        return !isAllDiscoverersStarted();
    }

    public boolean isAllDiscoverersStarted() {
        for (Discoverer discoverer : mDiscovererList) {
            if (!discoverer.isDiscovererStarted()) return false;
        }
        return true;
    }

    public boolean isDiscovererStopped(String code) {
        return !isDiscovererStarted(code);
    }

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

    public boolean isDiscoverersAvailable() {
        for (Discoverer discoverer : mDiscovererList) {
            if (discoverer.isFeatureAvailable() & discoverer.getPermissionNotGranted().isEmpty())
                return true;
        }
        return false;
    }

    public boolean isDiscovererAvailable(String code) {
        Discoverer discoverer = getDiscoverer(code);
        if (discoverer == null) return false;
        return discoverer.isFeatureAvailable() & discoverer.getPermissionNotGranted().isEmpty();
    }

    public Set<String> getDiscoverersPermissionsNotGranted() {
        Set<String> permissions = new HashSet<>();
        for (Discoverer discoverer : mDiscovererList) {
            permissions.addAll(discoverer.getPermissionNotGranted());
        }
        return permissions;
    }

    public Set<String> getDiscovererPermissionsNotGranted(String code) {
        Discoverer discoverer = getDiscoverer(code);
        if (discoverer == null) return Collections.emptySet();
        return discoverer.getPermissionNotGranted();
    }

    public void grantDiscoverPermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity, getDiscoverersPermissionsNotGranted().toArray(new String[0]), REQUEST_CODE_GRANT_PERMISSIONS);
    }

    public void grantDiscovererPermissions(String code, Activity activity, @IntRange(from = 0) int requestCode) {
        ActivityCompat.requestPermissions(activity, getDiscovererPermissionsNotGranted(code).toArray(new String[0]), requestCode);
    }

    public Map<String, PeerInfo> getPeerInfoMap() {
        return mPeerInfoMap;
    }
}