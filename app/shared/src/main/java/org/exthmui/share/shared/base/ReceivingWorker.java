package org.exthmui.share.shared.base;

import android.app.Notification;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.exthmui.share.shared.Constants;
import org.exthmui.share.shared.ReceiverUtils;

public abstract class ReceivingWorker extends Worker {
    public static final String P_PROGRESS = "PROGRESS";
    public static final String P_ACCEPTED = "ACCEPTED";
    public static final String P_BYTES_TOTAL = "BYTES_TOTAL";
    public static final String P_BYTES_RECEIVED = "BYTES_SENT";
    public static final String F_STATUS_CODE = "STATUS_CODE";
    public static final String F_MESSAGE = "MESSAGE";

    public ReceivingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    protected void updateProgress(long totalBytesToSend, long bytesReceived, String fileName, String senderName) {
        setProgressAsync(genProgressData(totalBytesToSend, bytesReceived));
        boolean indeterminate = bytesReceived == 0;
        setForegroundAsync(createForegroundInfo((Constants.NOTIFICATION_ID_PREFIX_RECEIVE + getId()).hashCode(), bytesReceived, totalBytesToSend, fileName, senderName, indeterminate));
    }

    protected Result genFailureResult(int errCode, String message) {
        return Result.failure(genFailureData(errCode, message));
    }

    private Data genProgressData(long totalBytesToSend, long bytesReceived) {
        return new Data.Builder()
                .putAll(getInputData())
                .putLong(P_BYTES_TOTAL, totalBytesToSend)
                .putLong(P_BYTES_RECEIVED, bytesReceived)
                .putInt(P_PROGRESS, (int) (bytesReceived / totalBytesToSend) * 100)
                .build();
    }

    private Data genFailureData(int statusCode, String message) {
        return new Data.Builder()
                .putAll(getInputData())
                .putInt(F_STATUS_CODE, statusCode)
                .putString(F_MESSAGE, message)
                .build();
    }

    /**
     * InputData:
     * MUST contain: Nothing.
     * <p>
     * ***** Extra values is allowed *****
     *
     * @return Result of work.
     * <p>
     * Success:
     * MUST contain:Everything in InputData & nothing else
     * ***** Extra values is not allowed *****
     * <p>
     * Failure:
     * MUST contain:{@link int}: {@link #F_STATUS_CODE}
     * {@link String}: {@link #F_MESSAGE}
     * ***** Extra values is not allowed *****
     */
    @NonNull
    @Override
    public abstract Result doWork();

    @NonNull
    protected ForegroundInfo createForegroundInfo(int notificationId, long totalBytesToSend, long bytesReceived, String fileName, String targetName, boolean indeterminate) {
        Notification notification = ReceiverUtils.buildSendingNotification(getApplicationContext(), getId(), totalBytesToSend, bytesReceived, fileName, targetName, indeterminate);
        return new ForegroundInfo(notificationId, notification);
    }
}
