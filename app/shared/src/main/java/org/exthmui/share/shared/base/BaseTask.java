package org.exthmui.share.shared.base;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exthmui.share.shared.base.results.BasicTransmissionResult;
import org.exthmui.share.shared.base.results.SilentResult;
import org.exthmui.share.shared.base.results.TransmissionResult;
import org.exthmui.share.shared.exceptions.trans.TransmissionException;
import org.exthmui.share.taskMgr.Result;
import org.exthmui.share.taskMgr.Task;
import org.exthmui.share.taskMgr.entities.TaskEntity;

import java.util.Map;

public abstract class BaseTask extends Task {
    public static final String STATUS_CODE = "STATUS_CODE";
    public static final String P_BYTES_TOTAL = "BYTES_TOTAL";
    public static final String P_BYTES_TRANSMITTED = "BYTES_TRANSMITTED";
    public static final String P_FILE_INFOS = "FILE_INFOS";
    public static final String P_PEER_INFO_TRANSFER = "PEER_INFO_TRANSFER";
    public static final String P_CUR_FILE_ID = "CUR_FILE_ID";
    public static final String P_CUR_FILE_BYTES_TOTAL = "CUR_FILE_BYTES_TOTAL";
    public static final String P_CUR_FILE_BYTES_TRANSMITTED = "CUR_FILE_BYTES_TRANSMITTED";
    public static final String P_INDETERMINATE = "INDETERMINATE";
    public static final String F_MESSAGE = "MESSAGE";
    public static final String F_LOCALIZED_MESSAGE = "LOCALIZED_MESSAGE";
    public static final String F_RESULT_MAP = "LOCALIZED_MESSAGE";

    private Context mAppContext;

    public BaseTask(@NonNull Context context, String taskId, Bundle inputData) {
        super(taskId, inputData);
        mAppContext = context.getApplicationContext();
    }

    public BaseTask(@NonNull TaskEntity taskEntity) {
        super(taskEntity);
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

    protected Context getApplicationContext() {
        return mAppContext;
    }

    protected void updateProgress(int statusCode, long totalBytesToSend, long bytesReceived,
                                  @NonNull FileInfo[] fileInfos,
                                  @Nullable PeerInfoTransfer peerInfoTransfer,
                                  @Nullable String curFileId, long curFileBytesToSend,
                                  long curFileBytesSent, boolean indeterminate) {
        super.updateProgress(genProgressData(statusCode, totalBytesToSend, bytesReceived,
                fileInfos, peerInfoTransfer, curFileId, curFileBytesToSend, curFileBytesSent,
                indeterminate));
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
    protected Bundle genSuccessData() {
        return new Bundle(getInputData());
    }

    @NonNull
    protected Bundle genProgressData(int statusCode, long totalBytesToSend,
                                            long bytesTransmitted,
                                            @NonNull FileInfo[] fileInfos,
                                            @Nullable PeerInfoTransfer peerInfoTransfer,
                                            @Nullable String curFileId,
                                            long curFileBytesToSend,
                                            long curFileBytesTransmitted, boolean indeterminate) {
        Bundle bundle = new Bundle(getInputData());
        bundle.putInt(STATUS_CODE, statusCode);
        bundle.putLong(P_BYTES_TOTAL, totalBytesToSend);
        bundle.putLong(P_BYTES_TRANSMITTED, bytesTransmitted);
        bundle.putSerializable(P_FILE_INFOS, fileInfos);
        bundle.putSerializable(P_PEER_INFO_TRANSFER, peerInfoTransfer);
        bundle.putString(P_CUR_FILE_ID, curFileId);
        bundle.putLong(P_CUR_FILE_BYTES_TOTAL, curFileBytesToSend);
        bundle.putLong(P_CUR_FILE_BYTES_TRANSMITTED, curFileBytesTransmitted);
        bundle.putBoolean(P_INDETERMINATE, indeterminate);
        return bundle;
    }


    /**
     * Generate Failure Data
     *
     * @param result    General result
     * @param resultMap Result map
     * @return Bundle
     */
    @NonNull
    protected Bundle genFailureData(@NonNull TransmissionResult result,
                                                 @Nullable Map<String, TransmissionResult> resultMap) {
        Bundle bundle = new Bundle(getInputData());
        bundle.putInt(STATUS_CODE, result.getStatusCode());
        bundle.putString(F_MESSAGE, result.getMessage());
        bundle.putString(F_MESSAGE, result.getMessage());
        bundle.putString(F_LOCALIZED_MESSAGE, result.getLocalizedMessage());
        bundle.putString(F_RESULT_MAP, resultMap == null ?
                null : BasicTransmissionResult.mapToString(resultMap));
        return bundle;
    }
}
