package org.exthmui.share.shared.base;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.impl.utils.futures.SettableFuture;

import com.google.common.util.concurrent.ListenableFuture;

import org.exthmui.share.shared.Constants;
import org.exthmui.share.shared.SenderUtils;

public abstract class SendingWorker extends Worker {
    public static final String P_STATUS_CODE = "STATUS_CODE";
    public static final String P_BYTES_TOTAL = "BYTES_TOTAL";
    public static final String P_BYTES_SENT = "BYTES_SENT";
    public static final String F_STATUS_CODE = "STATUS_CODE";
    public static final String F_MESSAGE = "MESSAGE";

    public SendingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    protected void updateProgress(int statusCode, long totalBytesToSend, long bytesSent, @Nullable String fileName, @Nullable String targetName) {
        setProgressAsync(genProgressData(statusCode, totalBytesToSend, bytesSent));
        boolean indeterminate = bytesSent == 0;
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        Notification notification = SenderUtils.buildSendingNotification(getApplicationContext(), statusCode, getId(), totalBytesToSend, bytesSent, fileName, targetName, indeterminate);
        notificationManager.notify((Constants.NOTIFICATION_ID_PREFIX_SEND + getId()).hashCode(), notification);
    }

    protected void updateProgress(int statusCode, long totalBytesToSend, long bytesSent, @NonNull String[] fileNames, @Nullable String targetName) {
        setProgressAsync(genProgressData(statusCode, totalBytesToSend, bytesSent));
        boolean indeterminate = bytesSent == 0;
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        Notification notification = SenderUtils.buildSendingNotification(getApplicationContext(), statusCode, getId(), totalBytesToSend, bytesSent, fileNames, targetName, indeterminate);
        notificationManager.notify((Constants.NOTIFICATION_ID_PREFIX_SEND + getId()).hashCode(), notification);
    }


    protected Result genRejectedResult() {
        return genFailureResult(Constants.TransmissionStatus.REJECTED.getNumVal(), "Remote system rejected receiving file");
    }

    protected Result genSenderCancelledResult() {
        return genFailureResult(Constants.TransmissionStatus.SENDER_CANCELLED.getNumVal(), "User(aka sender) canceled sending file");
    }

    protected Result genReceiverCancelledResult() {
        return genFailureResult(Constants.TransmissionStatus.RECEIVER_CANCELLED.getNumVal(), "Remote system(aka receiver) canceled receiving file");
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
     * {@link int}: {@link Entity#FILE_TYPE}, For more {@link Constants.FileType#getNumVal()}
     * {@link String}: {@link Sender#TARGET_PEER_ID}
     *
     * OR:{@link String[]}: {@link Entity#FILE_URIS},
     * {@link String[]}: {@link Entity#FILE_NAMES},
     * {@link String[]}: {@link Entity#FILE_PATHS},
     * {@link long[]}: {@link Entity#FILE_SIZES},
     * {@link int[]}: {@link Entity#FILE_TYPES}, For more {@link Constants.FileType#getNumVal()}
     * {@link String[]}: {@link Sender#TARGET_PEER_ID}
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
    protected ForegroundInfo createForegroundInfo(int statusCode, int notificationId, long totalBytesToSend, long bytesSent, @Nullable String fileName, @Nullable String targetName, boolean indeterminate) {
        Notification notification = SenderUtils.buildSendingNotification(getApplicationContext(), statusCode, getId(), totalBytesToSend, bytesSent, fileName, targetName, indeterminate);
        return new ForegroundInfo(notificationId, notification);
    }

    @NonNull
    protected ForegroundInfo createForegroundInfo(int statusCode, int notificationId, long totalBytesToSend, long bytesSent, @NonNull String[] fileNames, @Nullable String targetName, boolean indeterminate) {
        Notification notification = SenderUtils.buildSendingNotification(getApplicationContext(), statusCode, getId(), totalBytesToSend, bytesSent, fileNames, targetName, indeterminate);
        return new ForegroundInfo(notificationId, notification);
    }

    @SuppressLint("RestrictedApi")
    @NonNull
    @Override
    public ListenableFuture<ForegroundInfo> getForegroundInfoAsync() {
        SettableFuture<ForegroundInfo> foregroundInfoListenableFuture = SettableFuture.create();
        foregroundInfoListenableFuture.set(createForegroundInfo(Constants.TransmissionStatus.INITIALIZING.getNumVal(), (Constants.NOTIFICATION_ID_PREFIX_SEND + getId()).hashCode(), 0, 0, (String) null, null, false));
        return foregroundInfoListenableFuture;
    }
}
