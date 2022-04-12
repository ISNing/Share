package org.exthmui.share.shared.base;

import android.app.Notification;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.WorkerParameters;

import org.exthmui.share.shared.Constants;
import org.exthmui.share.shared.ReceiverUtils;

public abstract class ReceivingWorker extends BaseWorker {

    public ReceivingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    public int getNotificationId() {
        return (Constants.NOTIFICATION_ID_PREFIX_RECEIVE + getId()).hashCode();
    }

    @NonNull
    @Override
    Notification buildProgressNotification(int statusCode, long totalBytesToSend, long bytesReceived, @NonNull String[] fileNames, @Nullable String senderName, boolean indeterminate) {
        return ReceiverUtils.buildReceivingNotification(getApplicationContext(), getConnectionType(), statusCode, getId(), totalBytesToSend, bytesReceived, fileNames, senderName, indeterminate);
    }

    /**
     * InputData:
     * MUST contain: Nothing.
     * ***** Extra values is allowed *****
     *
     * @return Result of work.
     * @see BaseWorker#doWork()
     */
    @NonNull
    @Override
    public abstract Result doWork();
}
