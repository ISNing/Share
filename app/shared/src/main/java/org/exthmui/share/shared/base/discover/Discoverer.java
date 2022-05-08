package org.exthmui.share.shared.base.discover;

import androidx.annotation.NonNull;

import org.exthmui.share.shared.base.IPeer;
import org.exthmui.share.shared.listeners.BaseEventListener;

import java.util.Map;
import java.util.Set;

/**
 * IMPORTANT: Should have a static method "getInstance({@link android.content.Context} context)"
  */
public interface Discoverer {
    @NonNull Set<String> getPermissionNotGranted();
    @NonNull Set<String> getPermissionsRequired();
    boolean isDiscovererStarted();
    void registerListener(BaseEventListener listener);
    void unregisterListener(BaseEventListener listener);
    void initialize();
    boolean isInitialized();
    boolean isFeatureAvailable();
    void startDiscover();
    void stopDiscover();
    @NonNull
    Map<String, IPeer> getPeers();
}
