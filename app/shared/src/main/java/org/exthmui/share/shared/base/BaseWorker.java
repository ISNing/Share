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

import org.exthmui.share.shared.base.results.BasicTransmissionResult;
import org.exthmui.share.shared.base.results.SilentResult;
import org.exthmui.share.shared.base.results.TransmissionResult;
import org.exthmui.share.shared.exceptions.trans.TransmissionException;
import org.exthmui.share.shared.misc.Constants;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public abstract class BaseWorker extends Worker {
    public static final String STATUS_CODE = "STATUS_CODE";
    public static final String P_BYTES_TOTAL = "BYTES_TOTAL";
    public static final String P_BYTES_TRANSMITTED = "BYTES_TRANSMITTED";
    public static final String F_MESSAGE = "MESSAGE";
    public static final String F_LOCALIZED_MESSAGE = "LOCALIZED_MESSAGE";
    public static final String F_RESULT_MAP = "LOCALIZED_MESSAGE";

    private final AtomicReference<ForegroundInfo> foregroundInfo = new AtomicReference<>();

    {
        setForegroundInfo(createForegroundInfo(Constants.TransmissionStatus.INITIALIZING.getNumVal(),
                0, 0, new FileInfo[]{null,}, null,
                null, 0, 0, false));
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
     * <p>
     * Failure:
     * MUST contain:{@link int}: {@link #STATUS_CODE}
     * {@link String}: {@link #F_MESSAGE}
     * {@link String}: {@link #F_LOCALIZED_MESSAGE}
     * & everything in InputData
     * @see #genFailureData(TransmissionResult, Map)
     * @see #genFailureResult(TransmissionException, Map)
     * @see #genFailureResult(TransmissionResult, Map)
     * ***** Extra values is allowed *****
     */
    @Override
    @NonNull
    public abstract Result doWork();

    protected abstract int getNotificationId();

    protected void updateProgress(int statusCode, long totalBytesToSend, long bytesReceived,
                                  @NonNull FileInfo[] fileInfos,
                                  @Nullable PeerInfoTransfer peerInfoTransfer,
                                  @Nullable String curFileId, long curFileBytesToSend,
                                  long curFileBytesSent) {
        setProgressAsync(genProgressData(statusCode, totalBytesToSend, bytesReceived));
        boolean indeterminate = bytesReceived == 0;
        setForegroundInfo(createForegroundInfo(statusCode, totalBytesToSend, bytesReceived,
                fileInfos, peerInfoTransfer, curFileId, curFileBytesToSend, curFileBytesSent,
                indeterminate));
    }

    @NonNull
    private ForegroundInfo createForegroundInfo(int statusCode, long totalBytesToSend,
                                                long bytesTransmitted,
                                                @NonNull FileInfo[] fileInfos,
                                                @Nullable PeerInfoTransfer peerInfoTransfer,
                                                @Nullable String curFileId,
                                                long curFileBytesToSend,
                                                long curFileBytesSent, boolean indeterminate) {
        Notification notification = buildProgressNotification(statusCode, totalBytesToSend,
                bytesTransmitted, fileInfos, peerInfoTransfer, curFileId, curFileBytesToSend,
                curFileBytesSent, indeterminate);
        return new ForegroundInfo(getNotificationId(), notification);
    }

    @NonNull
    protected abstract Notification buildProgressNotification(int statusCode, long totalBytesToSend,
                                                              long bytesTransmitted,
                                                              @NonNull FileInfo[] fileInfos,
                                                              @Nullable PeerInfoTransfer peerInfoTransfer,
                                                              @Nullable String curFileId,
                                                              long curFileBytesToSend,
                                                              long curFileBytesTransmitted,
                                                              boolean indeterminate);

    private void setForegroundInfo(@NonNull ForegroundInfo foregroundInfo) {
        this.foregroundInfo.set(foregroundInfo);
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(getApplicationContext());
        notificationManager.notify(getNotificationId(), foregroundInfo.getNotification());
    }

    @NonNull
    protected Result genSuccessResult() {
        return Result.success(genSuccessData());
    }

    @NonNull
    protected Result genFailureResult(@NonNull TransmissionException e, @Nullable Map<String, TransmissionResult> resultMap) {
        return genFailureResult((TransmissionResult) e, resultMap);
    }

    @NonNull
    private Result genFailureResult(@NonNull TransmissionResult result, @Nullable Map<String, TransmissionResult> resultMap) {
        return Result.failure(genFailureData(result, resultMap));
    }

    @NonNull
    protected Result genSilentResult() {
        return Result.failure(genFailureData(new SilentResult(), null));
    }

    @NonNull
    protected Data.Builder genSuccessDataBuilder() {
        return new Data.Builder()
                .putAll(getInputData());
    }

    @NonNull
    protected Data genSuccessData() {
        return genSuccessDataBuilder()
                .build();
    }

    @NonNull
    protected Data.Builder genProgressDataBuilder(int statusCode, long totalBytesToSend, long bytesTransmitted) {
        return new Data.Builder()
                .putAll(getInputData())
                .putInt(STATUS_CODE, statusCode)
                .putLong(P_BYTES_TOTAL, totalBytesToSend)
                .putLong(P_BYTES_TRANSMITTED, bytesTransmitted);
    }

    @NonNull
    protected Data genProgressData(int statusCode, long totalBytesToSend, long bytesTransmitted) {
        return genProgressDataBuilder(statusCode, totalBytesToSend, bytesTransmitted)
                .build();
    }

    /**
     * Generate Failure Data Builder
     *
     * @param result    General result
     * @param resultMap Result map
     * @return Data.Builder
     */
    @NonNull
    protected Data.Builder genFailureDataBuilder(@NonNull TransmissionResult result,
                                                 @Nullable Map<String, TransmissionResult> resultMap) {

        return new Data.Builder()
                .putAll(getInputData())
                .putInt(STATUS_CODE, result.getStatusCode())
                .putString(F_MESSAGE, result.getMessage())
                .putString(F_LOCALIZED_MESSAGE, result.getLocalizedMessage())
                .putString(F_RESULT_MAP, resultMap == null ?
                        null : BasicTransmissionResult.mapToString(resultMap));
    }

    @NonNull
    protected Data genFailureData(TransmissionResult result,
                                  @Nullable Map<String, TransmissionResult> resultMap) {
        return genFailureDataBuilder(result, resultMap)
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
