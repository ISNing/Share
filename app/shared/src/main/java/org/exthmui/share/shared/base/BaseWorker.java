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

import org.exthmui.share.shared.exceptions.trans.ReceiverCancelledException;
import org.exthmui.share.shared.exceptions.trans.RejectedException;
import org.exthmui.share.shared.exceptions.trans.SenderCancelledException;
import org.exthmui.share.shared.exceptions.trans.TransmissionException;
import org.exthmui.share.shared.misc.Constants;
import org.exthmui.share.shared.misc.IConnectionType;

import java.util.concurrent.atomic.AtomicReference;

public abstract class BaseWorker extends Worker {
    public static final String STATUS_CODE = "STATUS_CODE";
    public static final String P_BYTES_TOTAL = "BYTES_TOTAL";
    public static final String P_BYTES_TRANSMITTED = "BYTES_TRANSMITTED";
    public static final String F_MESSAGE = "MESSAGE";
    public static final String F_LOCALIZED_MESSAGE = "LOCALIZED_MESSAGE";

    private final AtomicReference<ForegroundInfo> foregroundInfo = new AtomicReference<>();

    {
        setForegroundInfo(createForegroundInfo(Constants.TransmissionStatus.INITIALIZING.getNumVal(),
                0, 0, new String[]{null,}, null, false));
    }

    public BaseWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    public abstract IConnectionType getConnectionType();

    /**
     * InputData are customized by implements
     *
     * @return See below:
     * Success:
     * MUST contain:Everything in InputData & nothing else
     * @see #genSuccessData()
     * @see #genSuccessResult()
     * ***** Extra values is allowed *****
     *
     * Failure:
     * MUST contain:{@link int}: {@link #STATUS_CODE}
     * {@link String}: {@link #F_MESSAGE}
     * {@link String}: {@link #F_LOCALIZED_MESSAGE}
     * & everything in InputData
     * @see #genFailureData(int, String, String)
     * @see #genFailureResult(TransmissionException)
     * @see #genFailureResult(int, String, String)
     * ***** Extra values is allowed *****
     */
    @NonNull
    public abstract Result doWork();

    protected abstract int getNotificationId();

    protected void updateProgress(int statusCode, long totalBytesToSend, long bytesReceived, @NonNull String[] fileNames, @Nullable PeerInfoTransfer peerInfoTransfer) {
        setProgressAsync(genProgressData(statusCode, totalBytesToSend, bytesReceived));
        boolean indeterminate = bytesReceived == 0;
        setForegroundInfo(createForegroundInfo(statusCode, totalBytesToSend, bytesReceived, fileNames, peerInfoTransfer, indeterminate));
    }

    @NonNull
    private ForegroundInfo createForegroundInfo(int statusCode, long totalBytesToSend, long bytesTransmitted, @NonNull String[] fileNames, @Nullable PeerInfoTransfer peerInfoTransfer, boolean indeterminate) {
        Notification notification = buildProgressNotification(statusCode, totalBytesToSend, bytesTransmitted, fileNames, peerInfoTransfer, indeterminate);
        return new ForegroundInfo(getNotificationId(), notification);
    }

    @NonNull
    protected abstract Notification buildProgressNotification(int statusCode, long totalBytesToSend, long bytesTransmitted, @NonNull String[] fileNames, @Nullable PeerInfoTransfer peerInfoTransfer, boolean indeterminate);

    private void setForegroundInfo(ForegroundInfo foregroundInfo) {
        this.foregroundInfo.set(foregroundInfo);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        notificationManager.notify(getNotificationId(), foregroundInfo.getNotification());
    }

    protected Result genSuccessResult() {
        return Result.success(genSuccessData());
    }

    protected Result genRejectedResult(@NonNull Context context) {
        return genFailureResult(new RejectedException(context));
    }

    protected Result genSenderCancelledResult(@NonNull Context context) {
        return genFailureResult(new SenderCancelledException(context));
    }

    protected Result genReceiverCancelledResult(@NonNull Context context) {
        return genFailureResult(new ReceiverCancelledException(context));
    }

    protected Result genFailureResult(@NonNull TransmissionException e) {
        return genFailureResult(e.getStatusCode(), e.getMessage(), e.getLocalizedMessage());
    }

    private Result genFailureResult(int errCode, @Nullable String message, @Nullable String localizedMessage) {
        return Result.failure(genFailureData(errCode, message, localizedMessage));
    }

    protected Data.Builder genSuccessDataBuilder() {
        return new Data.Builder()
                .putAll(getInputData());
    }

    protected Data genSuccessData() {
        return genSuccessDataBuilder()
                .build();
    }

    protected Data.Builder genProgressDataBuilder(int statusCode, long totalBytesToSend, long bytesTransmitted) {
        return new Data.Builder()
                .putAll(getInputData())
                .putInt(STATUS_CODE, statusCode)
                .putLong(P_BYTES_TOTAL, totalBytesToSend)
                .putLong(P_BYTES_TRANSMITTED, bytesTransmitted);
    }

    protected Data genProgressData(int statusCode, long totalBytesToSend, long bytesTransmitted) {
        return genProgressDataBuilder(statusCode, totalBytesToSend, bytesTransmitted)
                .build();
    }

    protected Data.Builder genFailureDataBuilder(int statusCode, @Nullable String message, @Nullable String localizedMessage) {
        return new Data.Builder()
                .putAll(getInputData())
                .putInt(STATUS_CODE, statusCode)
                .putString(F_MESSAGE, message)
                .putString(F_LOCALIZED_MESSAGE, localizedMessage);
    }

    protected Data genFailureData(int statusCode, @Nullable String message, @Nullable String localizedMessage) {
        return genFailureDataBuilder(statusCode, message, localizedMessage)
                .build();
    }

    @SuppressLint("RestrictedApi")
    @NonNull
    @Override
    public ListenableFuture<ForegroundInfo> getForegroundInfoAsync() {
        SettableFuture<ForegroundInfo> foregroundInfoListenableFuture = SettableFuture.create();
        foregroundInfoListenableFuture.set(foregroundInfo.get());
        return foregroundInfoListenableFuture;
    }

    @Override
    public void onStopped() {
        super.onStopped();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        notificationManager.cancel(getNotificationId());
    }
}
