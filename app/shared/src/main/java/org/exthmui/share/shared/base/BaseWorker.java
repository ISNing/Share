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
import org.exthmui.share.shared.base.exceptions.trans.ReceiverCancelledException;
import org.exthmui.share.shared.base.exceptions.trans.RejectedException;
import org.exthmui.share.shared.base.exceptions.trans.SenderCancelledException;
import org.exthmui.share.shared.base.exceptions.trans.TransmissionException;
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
     * InputData are cus
     *
     * @return See below:
     * <p>
     * Success:
     * MUST contain:Everything in InputData & nothing else
     * @see #genSuccessData()
     * @see #genSuccessResult()
     * ***** Extra values is not allowed *****
     * <p>
     * Failure:
     * MUST contain:{@link int}: {@link #STATUS_CODE}
     * {@link String}: {@link #F_MESSAGE}
     * {@link String}: {@link #F_LOCALIZED_MESSAGE}
     * & everything in InputData
     * @see #genFailureData(int, String, String)
     * @see #genFailureResult(TransmissionException)
     * @see #genFailureResult(int, String, String)
     * ***** Extra values is not allowed *****
     */
    @NonNull
    public abstract Result doWork();

    abstract int getNotificationId();

    protected void updateProgress(int statusCode, long totalBytesToSend, long bytesTransmitted, @Nullable String fileName, @Nullable String peerName) {
        updateProgress(statusCode, totalBytesToSend, bytesTransmitted, new String[]{fileName,}, peerName);
    }

    protected void updateProgress(int statusCode, long totalBytesToSend, long bytesReceived, @NonNull String[] fileNames, @Nullable String peerName) {
        setProgressAsync(genProgressData(statusCode, totalBytesToSend, bytesReceived));
        boolean indeterminate = bytesReceived == 0;
        setForegroundInfo(createForegroundInfo(statusCode, totalBytesToSend, bytesReceived, fileNames, peerName, indeterminate));
    }

    @NonNull
    private ForegroundInfo createForegroundInfo(int statusCode, long totalBytesToSend, long bytesTransmitted, @NonNull String[] fileNames, @Nullable String peerName, boolean indeterminate) {
        Notification notification = buildProgressNotification(statusCode, totalBytesToSend, bytesTransmitted, fileNames, peerName, indeterminate);
        return new ForegroundInfo(getNotificationId(), notification);
    }

    @NonNull
    abstract Notification buildProgressNotification(int statusCode, long totalBytesToSend, long bytesTransmitted, @NonNull String[] fileNames, @Nullable String peerName, boolean indeterminate);

    private void setForegroundInfo(ForegroundInfo foregroundInfo) {
        this.foregroundInfo.set(foregroundInfo);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        notificationManager.notify(getNotificationId(), foregroundInfo.getNotification());
    }

    protected final Result genSuccessResult() {
        return Result.failure(genSuccessData());
    }

    protected final Result genRejectedResult(Context context) {
        return genFailureResult(new RejectedException(context));
    }

    protected final Result genSenderCancelledResult(Context context) {
        return genFailureResult(new SenderCancelledException(context));
    }

    protected final Result genReceiverCancelledResult(Context context) {
        return genFailureResult(new ReceiverCancelledException(context));
    }

    protected final Result genFailureResult(TransmissionException e) {
        return genFailureResult(e.getStatusCode(), e.getMessage(), e.getLocalizedMessage());
    }

    private Result genFailureResult(int errCode, String message, String localizedMessage) {
        return Result.failure(genFailureData(errCode, message, localizedMessage));
    }

    private Data genSuccessData() {
        return new Data.Builder()
                .putAll(getInputData())
                .build();
    }

    private Data genProgressData(int statusCode, long totalBytesToSend, long bytesReceived) {
        return new Data.Builder()
                .putAll(getInputData())
                .putInt(STATUS_CODE, statusCode)
                .putLong(P_BYTES_TOTAL, totalBytesToSend)
                .putLong(P_BYTES_TRANSMITTED, bytesReceived)
                .build();
    }

    private Data genFailureData(int statusCode, String message, String localizedMessage) {
        return new Data.Builder()
                .putAll(getInputData())
                .putInt(STATUS_CODE, statusCode)
                .putString(F_MESSAGE, message)
                .putString(F_LOCALIZED_MESSAGE, localizedMessage)
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
