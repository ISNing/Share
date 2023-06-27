package org.exthmui.share.misc;

import android.app.Notification;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import org.exthmui.share.shared.base.Entity;
import org.exthmui.share.shared.base.IConnectionType;
import org.exthmui.share.shared.base.IPeer;
import org.exthmui.share.shared.base.send.Sender;
import org.exthmui.share.shared.exceptions.FailedInvokingSendingMethodException;
import org.exthmui.share.shared.exceptions.FailedStartSendingException;
import org.exthmui.share.shared.exceptions.InvalidSenderException;
import org.exthmui.share.shared.misc.NotificationUtils;
import org.exthmui.share.shared.misc.SenderUtils;
import org.exthmui.share.taskMgr.Result;
import org.exthmui.share.taskMgr.TaskManager;
import org.exthmui.share.taskMgr.listeners.OnResultListener;
import org.exthmui.utils.StackTraceUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

public class SendingHelper {

    @NonNull
    private final Context mContext;
    @NonNull
    private final TaskManager mTaskManager;
    @NonNull
    private final WorkManager mWorkManager;

    public static final String TAG = "SendingHelper";

    public SendingHelper(@NonNull Context context) {
        mContext = context.getApplicationContext();
        mTaskManager = TaskManager.getInstance(mContext);
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
            mTaskManager.getTask(workId.toString()).registerListener((OnResultListener) event -> {
                Result result = event.getResult();
                if (result.getStatus() == Result.Status.SUCCESS) {
                    Notification notification =
                            SenderUtils.buildSendingSucceededNotification(mContext, result.getData());
                    NotificationUtils.postNotification(mContext, UUID.randomUUID().hashCode(), notification);
                } else if (result.getStatus() == Result.Status.ERROR) {
                    Notification notification =
                            SenderUtils.buildSendingFailedNotification(mContext, result.getData());
                    NotificationUtils.postNotification(mContext, UUID.randomUUID().hashCode(), notification);
                }
            });
            mWorkManager.getWorkInfoByIdLiveData(workId).observeForever(workInfo -> {
                if (workInfo == null) return;
                if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                    Notification notification =
                            SenderUtils.buildSendingSucceededNotification(mContext, workInfo.getOutputData());
                    NotificationUtils.postNotification(mContext, UUID.randomUUID().hashCode(), notification);
                } else if (workInfo.getState() == WorkInfo.State.FAILED) {
                    Notification notification =
                            SenderUtils.buildSendingFailedNotification(mContext, workInfo.getOutputData());
                    NotificationUtils.postNotification(mContext, UUID.randomUUID().hashCode(), notification);
                }
            });
            return workId;
        } catch (@NonNull IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            Log.e(TAG, StackTraceUtils.getStackTraceString(e.getStackTrace()));
            throw new FailedInvokingSendingMethodException(mContext, e);
        }
    }
}
