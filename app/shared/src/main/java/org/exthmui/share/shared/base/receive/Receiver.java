package org.exthmui.share.shared.base.receive;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exthmui.share.shared.base.IConnectionType;
import org.exthmui.share.shared.events.ReceiveActionAcceptEvent;
import org.exthmui.share.shared.events.ReceiveActionRejectEvent;
import org.exthmui.share.shared.listeners.OnReceiveActionAcceptListener;
import org.exthmui.share.shared.listeners.OnReceiveActionRejectListener;
import org.exthmui.utils.listeners.BaseEventListener;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * IMPORTANT: Should have a static method "getInstance({@link android.content.Context} context)"
 */
public interface Receiver extends OnReceiveActionAcceptListener, OnReceiveActionRejectListener {
    String TAG = "Sender";
    String FROM_PEER_ID = "FROM_PEER_ID";
    String FROM_PEER_NAME = "FROM_PEER_NAME";

    @NonNull IConnectionType getConnectionType();
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

    @Nullable
    default UUID startWork(Context context) {
        return startWork(context, Collections.emptyMap());
    }

    @Nullable
    UUID startWork(Context context, Map<String, String> properties);

    @NonNull
    @Override
    default Map<Class<? extends EventObject>, Method[]> getEventToMethodMap(){
        this._getEventToMethodMap();
        return this._getEventToMethodMap();
    }

    @NonNull
    @Override
    default Map<Class<? extends EventObject>, Method[]> _getEventToMethodMap() {
        Map<Class<? extends EventObject>, Method[]> map = new HashMap<>();
        map.putAll(OnReceiveActionAcceptListener.super._getEventToMethodMap());
        map.putAll(OnReceiveActionRejectListener.super._getEventToMethodMap());
        return map;
    }

    @Nullable
    default UUID startWorkWrapped(@NonNull Context context) {
        return startWorkWrapped(context, Collections.emptyMap());
    }

    @Nullable
    default UUID startWorkWrapped(@NonNull Context context, @NonNull Map<String, String> properties) {
        UUID workId = startWork(context, properties);
        //        WorkManager.getInstance(context).getWorkInfoByIdLiveData(workId).observeForever(workInfo -> {
//            if (workInfo == null) return;
//            if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
//                Notification notification =
//                        ReceiverUtils.buildReceivingSucceededNotification(context, workInfo.getOutputData());
//                NotificationUtils.postNotification(context, notificationId, notification);
//            } else if (workInfo.getState() == WorkInfo.State.FAILED) {
//                Notification notification =
//                        ReceiverUtils.buildReceivingFailedNotification(context, workInfo.getOutputData());
//                NotificationUtils.postNotification(context, notificationId, notification);
//            }
//        });TODO
        return workId;
    }

    @Override
    void onReceiveActionAccept(ReceiveActionAcceptEvent event);

    @Override
    void onReceiveActionReject(ReceiveActionRejectEvent event);
}
