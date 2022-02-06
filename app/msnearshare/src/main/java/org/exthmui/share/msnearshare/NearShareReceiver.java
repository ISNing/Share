package org.exthmui.share.msnearshare;

import androidx.annotation.NonNull;

import org.exthmui.share.shared.base.Receiver;
import org.exthmui.share.shared.base.events.ReceiveActionAcceptEvent;
import org.exthmui.share.shared.base.events.ReceiveActionRejectEvent;
import org.exthmui.share.shared.base.listeners.BaseEventListener;

import java.lang.reflect.Method;
import java.util.EventObject;
import java.util.Map;
import java.util.Set;

public class NearShareReceiver implements Receiver {
    @NonNull
    @Override
    public Set<String> getPermissionNotGranted() {
        return null;
    }

    @NonNull
    @Override
    public Set<String> getPermissionsRequired() {
        return null;
    }

    @Override
    public boolean isReceiverStarted() {
        return false;
    }

    @Override
    public void registerListener(BaseEventListener listener) {

    }

    @Override
    public void unregisterListener(BaseEventListener listener) {

    }

    @Override
    public void initialize() {

    }

    @Override
    public boolean isInitialized() {
        return false;
    }

    @Override
    public boolean isFeatureAvailable() {
        return false;
    }

    @Override
    public void startReceive() {

    }

    @Override
    public void stopReceive() {

    }

    @Override
    public Map<Class<? extends EventObject>, Method[]> getEventTMethodMap() {
        return null;
    }

    @Override
    public void onReceiveActionAccept(ReceiveActionAcceptEvent event) {

    }

    @Override
    public void onReceiveActionReject(ReceiveActionRejectEvent event) {

    }
}
