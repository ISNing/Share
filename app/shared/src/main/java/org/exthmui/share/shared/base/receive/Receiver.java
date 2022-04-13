package org.exthmui.share.shared.base.receive;

import android.app.Notification;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import org.exthmui.share.shared.events.ReceiveActionAcceptEvent;
import org.exthmui.share.shared.events.ReceiveActionRejectEvent;
import org.exthmui.share.shared.listeners.BaseEventListener;
import org.exthmui.share.shared.listeners.OnReceiveActionAcceptListener;
import org.exthmui.share.shared.listeners.OnReceiveActionRejectListener;
import org.exthmui.share.shared.misc.ReceiverUtils;

import java.lang.reflect.Method;
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
    UUID startWork(Context context);

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

    default UUID startWorkWrapped(Context context) {
        UUID workId = startWork(context);
        WorkManager.getInstance(context).getWorkInfoByIdLiveData(workId).observeForever(workInfo -> {
            if (workInfo == null) return;
            if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                Notification notification =
                        ReceiverUtils.buildReceivingSucceededNotification(context, workInfo.getOutputData());
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.notify(UUID.randomUUID().hashCode(), notification);
            } else if (workInfo.getState() == WorkInfo.State.FAILED) {
                Notification notification =
                        ReceiverUtils.buildReceivingFailedNotification(context, workInfo.getOutputData());
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.notify(UUID.randomUUID().hashCode(), notification);
            }
        });
        return workId;
    }

    @Override
    void onReceiveActionAccept(ReceiveActionAcceptEvent event);

    @Override
    void onReceiveActionReject(ReceiveActionRejectEvent event);
}
