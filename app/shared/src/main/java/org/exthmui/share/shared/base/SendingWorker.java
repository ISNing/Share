package org.exthmui.share.shared.base;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.exthmui.share.shared.Constants;
import org.exthmui.share.shared.R;
import org.exthmui.share.shared.SenderUtils;

public abstract class SendingWorker extends Worker {
    public static final String P_STATUS_CODE = "STATUS_CODE";
    public static final String P_PROGRESS = "PROGRESS";
    public static final String P_ACCEPTED = "ACCEPTED";
    public static final String P_BYTES_TOTAL = "BYTES_TOTAL";
    public static final String P_BYTES_SENT = "BYTES_SENT";
    public static final String F_STATUS_CODE = "STATUS_CODE";
    public static final String F_MESSAGE = "MESSAGE";
    public SendingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    protected void updateProgress(int statusCode, long totalBytesToSend, long bytesSent, String fileName, String targetName) {
        setProgressAsync(genProgressData(statusCode, totalBytesToSend, bytesSent));
        boolean indeterminate = bytesSent == 0;
        setForegroundAsync(createForegroundInfo((Constants.NOTIFICATION_ID_PREFIX_SEND + getId()).hashCode(), bytesSent, totalBytesToSend, fileName, targetName, indeterminate));
    }
    protected Result genFailureResult(int errCode, String message) {
        return Result.failure(genFailureData(errCode, message));
    }
    private Data genProgressData(int statusCode, long totalBytesToSend, long bytesSent) {
        return new Data.Builder()
                .putAll(getInputData())
                .putInt(P_STATUS_CODE, statusCode)
                .putLong(P_BYTES_TOTAL, totalBytesToSend)
                .putLong(P_BYTES_SENT, bytesSent)
                .putInt(P_PROGRESS, (int) (bytesSent / totalBytesToSend) * 100)
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
     * MUST contain:{@link String}: {@link Entity#FILE_URI},
     * {@link String}: {@link Entity#FILE_NAME},
     * {@link String}: {@link Entity#FILE_PATH},
     * {@link long}: {@link Entity#FILE_SIZE},
     * {@link int}: {@link Entity#FILE_TYPE}, For more {@link Constants.FileTypes#getNumVal()}
     * {@link String}: {@link Sender#TARGET_PEER_ID}
     *
     * ***** Extra values is allowed *****
     *
     *
     * @return Result of work.
     *
     * Success:
     * MUST contain:Everything in InputData & nothing else
     * ***** Extra values is not allowed *****
     *
     * Failure:
     * MUST contain:{@link int}: {@link #F_STATUS_CODE}
     * {@link String}: {@link #F_MESSAGE}
     * & everything in InputData
     * ***** Extra values is not allowed *****
     */
    @NonNull
    @Override
    public abstract Result doWork();

    @NonNull
    protected ForegroundInfo createForegroundInfo(int notificationId, long totalBytesToSend, long bytesSent, String fileName, String targetName, boolean indeterminate) {
        Notification notification = SenderUtils.buildSendingNotification(getApplicationContext(), getId(), totalBytesToSend, bytesSent, fileName, targetName, indeterminate);
        return new ForegroundInfo(notificationId, notification);
    }
}
