package org.exthmui.share.shared.services;

import android.app.Activity;
import android.content.ComponentCallbacks2;
import android.content.Intent;

import androidx.annotation.IntRange;
import androidx.annotation.Nullable;

import org.exthmui.share.shared.base.Discoverer;
import org.exthmui.share.shared.base.PeerInfo;
import org.exthmui.share.shared.base.listeners.BaseEventListener;

import java.util.Collection;
import java.util.EventObject;
import java.util.Map;
import java.util.Set;

public interface IDiscoverService extends ComponentCallbacks2 {
    int REQUEST_CODE_GRANT_PERMISSIONS = 0;

    void registerListener(BaseEventListener listener);

    void unregisterListener(BaseEventListener listener);

    void onCreate();

    int onStartCommand(Intent intent, int flags, int startId);

    void onDestroy();

    void unregisterDiscoverersListeners(Collection<BaseEventListener> listeners);

    @Nullable
    Discoverer getDiscoverer(String code);

    void stopDiscoverers();

    void addDiscoverers();

    void registerInternalListeners(Discoverer discoverer);

    void registerInternalListeners();

    void notifyListeners(EventObject event);

    boolean isAllDiscoverersStopped();

    void registerDiscoverersListeners(Collection<? extends BaseEventListener> listeners);

    boolean isAnyDiscovererStarted();

    void addDiscoverer(String code);

    void removeDiscoverer(String code);

    void startDiscoverer(String code);

    void stopDiscoverer(String code);

    void restartDiscoverers();

    void startDiscoverers();

    void registerDiscovererListeners(Collection<? extends BaseEventListener> listeners, String code);

    void unregisterDiscovererListeners(Collection<? extends BaseEventListener> listeners, String code);

    boolean isAnyDiscovererStopped();

    boolean isAllDiscoverersStarted();

    boolean isDiscovererStopped(String code);

    boolean isDiscovererStarted(String code);

    boolean isDiscoverersAvailable();

    boolean isDiscovererAvailable(String code);

    Set<String> getDiscoverersPermissionsNotGranted();

    Set<String> getDiscovererPermissionsNotGranted(String code);

    void grantDiscoverPermissions(Activity activity);

    void grantDiscovererPermissions(String code, Activity activity, @IntRange(from = 0) int requestCode);

    Map<String, PeerInfo> getPeerInfoMap();
}
