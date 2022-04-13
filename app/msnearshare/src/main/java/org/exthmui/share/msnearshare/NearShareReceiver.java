package org.exthmui.share.msnearshare;

import android.content.Context;

import androidx.annotation.NonNull;

import org.exthmui.share.shared.base.receive.Receiver;
import org.exthmui.share.shared.events.ReceiveActionAcceptEvent;
import org.exthmui.share.shared.events.ReceiveActionRejectEvent;
import org.exthmui.share.shared.listeners.BaseEventListener;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.EventObject;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class NearShareReceiver implements Receiver {
    public static NearShareReceiver getInstance(Context context) {
        return new NearShareReceiver();
    }
    
    @NonNull
    @Override
    public Set<String> getPermissionNotGranted() {
        return Collections.emptySet();
    }

    @NonNull
    @Override
    public Set<String> getPermissionsRequired() {
        return Collections.emptySet();
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
    public UUID startWork(Context context) {
        return null;
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
