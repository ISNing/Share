package org.exthmui.share.shared.base;

import androidx.annotation.NonNull;

import org.exthmui.share.shared.base.events.ReceiveActionAcceptEvent;
import org.exthmui.share.shared.base.events.ReceiveActionRejectEvent;
import org.exthmui.share.shared.base.listeners.BaseEventListener;
import org.exthmui.share.shared.base.listeners.OnReceiveActionAcceptListener;
import org.exthmui.share.shared.base.listeners.OnReceiveActionRejectListener;

import java.lang.reflect.Method;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * IMPORTANT: Should have a static method "getInstance({@link android.content.Context} context)"
 */
public interface Receiver extends OnReceiveActionAcceptListener, OnReceiveActionRejectListener {
    @NonNull Set<String> getPermissionNotGranted();
    @NonNull Set<String> getPermissionsRequired();
    boolean isReceiverStarted();
    void registerListener(BaseEventListener listener);
    void unregisterListener(BaseEventListener listener);
    void initialize();
    boolean isInitialized();
    boolean isFeatureAvailable();
    void startReceive();
    void stopReceive();

    default Map<Class<? extends EventObject>, Method[]> getEventTMethodMap(){
        return this._getEventTMethodMap() == null ? new HashMap<>() : this._getEventTMethodMap();
    }

    @Override
    default Map<Class<? extends EventObject>, Method[]> _getEventTMethodMap() {
        Map<Class<? extends EventObject>, Method[]> map = new HashMap<>();
        map.putAll(OnReceiveActionAcceptListener.super._getEventTMethodMap());
        map.putAll(OnReceiveActionRejectListener.super._getEventTMethodMap());
        return map;
    }

    @Override
    void onReceiveActionAccept(ReceiveActionAcceptEvent event);

    @Override
    void onReceiveActionReject(ReceiveActionRejectEvent event);
}
