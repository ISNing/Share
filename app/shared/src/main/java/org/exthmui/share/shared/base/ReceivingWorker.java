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
import org.exthmui.share.shared.ReceiverUtils;

public abstract class ReceivingWorker extends Worker {
    public static final String P_BYTES_TOTAL = "BYTES_TOTAL";
    public static final String P_BYTES_RECEIVED = "BYTES_SENT";
    public static final String P_STATUS_CODE = "STATUS_CODE";
    public static final String F_STATUS_CODE = "STATUS_CODE";
    public static final String F_MESSAGE = "MESSAGE";

    public ReceivingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    /**
     * Update progress
     * <p>
     * Note: (fileName == null && senderName == null) == true => means ready for receiving file, but no acceptation received.
     *
     * @param totalBytesToSend As name
     * @param bytesReceived    As name
     * @param fileName         File name
     * @param senderName       Sender's display name
     */
    protected void updateProgress(int statusCode, long totalBytesToSend, long bytesReceived, @Nullable String fileName, @Nullable String senderName) {
        setProgressAsync(genProgressData(statusCode, totalBytesToSend, bytesReceived));
        boolean indeterminate = bytesReceived == 0;
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        Notification notification = ReceiverUtils.buildReceivingNotification(getApplicationContext(), statusCode, getId(), totalBytesToSend, bytesReceived, fileName, senderName, indeterminate);
        notificationManager.notify((Constants.NOTIFICATION_ID_PREFIX_RECEIVE + getId()).hashCode(), notification);
    }

    protected void updateProgress(int statusCode, long totalBytesToSend, long bytesReceived, @NonNull String[] fileNames, @Nullable String senderName) {
        setProgressAsync(genProgressData(statusCode, totalBytesToSend, bytesReceived));
        boolean indeterminate = bytesReceived == 0;
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        Notification notification = ReceiverUtils.buildReceivingNotification(getApplicationContext(), statusCode, getId(), totalBytesToSend, bytesReceived, fileNames, senderName, indeterminate);
        notificationManager.notify((Constants.NOTIFICATION_ID_PREFIX_RECEIVE + getId()).hashCode(), notification);
    }

    protected Result genFailureResult(int errCode, String message) {
        return Result.failure(genFailureData(errCode, message));
    }

    protected Result genRejectedResult() {
        return genFailureResult(Constants.TransmissionStatus.REJECTED.getNumVal(), "User rejected receiving file");
    }

    protected Result genSenderCancelledResult() {
        return genFailureResult(Constants.TransmissionStatus.SENDER_CANCELLED.getNumVal(), "Remote system(aka sender) canceled sending file");
    }

    protected Result genReceiverCancelledResult() {
        return genFailureResult(Constants.TransmissionStatus.RECEIVER_CANCELLED.getNumVal(), "User(aka receiver) canceled receiving file");
    }

    private Data genProgressData(int statusCode, long totalBytesToSend, long bytesReceived) {
        return new Data.Builder()
                .putAll(getInputData())
                .putInt(P_STATUS_CODE, statusCode)
                .putLong(P_BYTES_TOTAL, totalBytesToSend)
                .putLong(P_BYTES_RECEIVED, bytesReceived)
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
    protected ForegroundInfo createForegroundInfo(int statusCode, int notificationId, long totalBytesToSend, long bytesReceived, @Nullable String fileName, @Nullable String senderName, boolean indeterminate) {
        Notification notification = ReceiverUtils.buildReceivingNotification(getApplicationContext(), statusCode, getId(), totalBytesToSend, bytesReceived, fileName, senderName, indeterminate);
        return new ForegroundInfo(notificationId, notification);
    }

    @NonNull
    protected ForegroundInfo createForegroundInfo(int statusCode, int notificationId, long totalBytesToSend, long bytesReceived, @NonNull String[] fileNames, @Nullable String senderName, boolean indeterminate) {
        Notification notification = ReceiverUtils.buildReceivingNotification(getApplicationContext(), statusCode, getId(), totalBytesToSend, bytesReceived, fileNames, senderName, indeterminate);
        return new ForegroundInfo(notificationId, notification);
    }

    @SuppressLint("RestrictedApi")
    @NonNull
    @Override
    public ListenableFuture<ForegroundInfo> getForegroundInfoAsync() {
        SettableFuture<ForegroundInfo> foregroundInfoListenableFuture = SettableFuture.create();
        foregroundInfoListenableFuture.set(createForegroundInfo(Constants.TransmissionStatus.UNKNOWN.getNumVal(), (Constants.NOTIFICATION_ID_PREFIX_RECEIVE + getId()).hashCode(), 0, 0, (String) null, null, false));
        return foregroundInfoListenableFuture;
    }
}
