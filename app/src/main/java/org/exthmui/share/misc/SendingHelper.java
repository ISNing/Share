package org.exthmui.share.misc;

import android.app.Notification;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import org.exthmui.share.shared.base.Entity;
import org.exthmui.share.shared.base.IPeer;
import org.exthmui.share.shared.base.send.Sender;
import org.exthmui.share.shared.exceptions.FailedInvokingSendingMethodException;
import org.exthmui.share.shared.exceptions.FailedStartSendingException;
import org.exthmui.share.shared.exceptions.InvalidSenderException;
import org.exthmui.share.shared.misc.IConnectionType;
import org.exthmui.share.shared.misc.SenderUtils;
import org.exthmui.share.shared.misc.StackTraceUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

public class SendingHelper {

    @NonNull
    private final Context mContext;
    @NonNull
    private final WorkManager mWorkManager;

    public static final String TAG = "SendingHelper";

    public SendingHelper(@NonNull Context context) {
        mContext = context.getApplicationContext();
        mWorkManager = WorkManager.getInstance(mContext);
    }

    public UUID send(@NonNull IPeer target, @NonNull List<Entity> entities)
        throws FailedStartSendingException {
        IConnectionType connectionType = target.getConnectionType();
        try {
            Method method = connectionType.getSenderClass()
                    .getDeclaredMethod("getInstance", Context.class);
            Sender<?> sender = (Sender<?>) method.invoke(null, mContext);
            if (sender == null) {
                throw new InvalidSenderException(mContext);
            }
            UUID workId = sender.sendToPeerInfo(mContext, target, entities);
            mWorkManager.getWorkInfoByIdLiveData(workId).observeForever(workInfo -> {
                if (workInfo == null) return;
                if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                    Notification notification =
                            SenderUtils.buildSendingSucceededNotification(mContext, workInfo.getOutputData());
                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mContext);
                    notificationManager.notify(UUID.randomUUID().hashCode(), notification);
                } else if (workInfo.getState() == WorkInfo.State.FAILED) {
                    Notification notification =
                            SenderUtils.buildSendingFailedNotification(mContext, workInfo.getOutputData());
                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mContext);
                    notificationManager.notify(UUID.randomUUID().hashCode(), notification);
                }
            });
            return workId;
        } catch (@NonNull IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            Log.e(TAG, StackTraceUtils.getStackTraceString(e.getStackTrace()));
            throw new FailedInvokingSendingMethodException(mContext, e);
        }
    }
}
