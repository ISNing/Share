package org.exthmui.share.shared.services;

import android.app.Activity;
import android.content.ComponentCallbacks2;
import android.content.Intent;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exthmui.share.shared.base.receive.Receiver;
import org.exthmui.share.shared.listeners.BaseEventListener;

import java.util.Collection;
import java.util.EventObject;
import java.util.Set;

public interface IReceiveService extends ComponentCallbacks2 {
    int REQUEST_CODE_GRANT_PERMISSIONS = 0;

    void registerListener(BaseEventListener listener);

    void unregisterListener(BaseEventListener listener);

    void onCreate();

    int onStartCommand(Intent intent, int flags, int startId);

    void onDestroy();

    void unregisterReceiverListeners(Collection<BaseEventListener> listeners);

    @Nullable
    Receiver getReceiver(String code);

    void stopReceivers();

    void addReceivers();

    void registerInternalListeners();

    void registerInternalListeners(Receiver receiver);

    void addReceiver(String code);

    void notifyListeners(EventObject event);

    boolean isAllReceiversStopped();

    void registerReceiverListeners(Collection<? extends BaseEventListener> listeners);

    boolean isAnyReceiverStarted();

    void removeReceiver(String code);

    void startReceiver(String code);

    void stopReceiver(String code);

    void restartReceiver();

    void startReceivers();

    void registerReceiverListeners(Collection<? extends BaseEventListener> listeners, String code);

    void unregisterReceiverListeners(Collection<? extends BaseEventListener> listeners, String code);

    boolean isAnyReceiverStopped();

    boolean isAllReceiversStarted();

    boolean isReceiverStopped(String code);

    boolean isReceiverStarted(String code);

    boolean isReceiversAvailable();

    boolean isReceiverAvailable(String code);

    void grantReceiversPermissions(Activity activity);

    @NonNull
    Set<String> getReceiversPermissionsNotGranted();

    void grantReceiverPermissions(String code, Activity activity, @IntRange(from = 0) int requestCode);

    @NonNull
    Set<String> getReceiverPermissionsNotGranted(String code);
}
