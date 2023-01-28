package org.exthmui.share.shared.base.receive;

import static org.exthmui.share.shared.base.receive.Receiver.FROM_PEER_ID;
import static org.exthmui.share.shared.base.receive.Receiver.FROM_PEER_NAME;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exthmui.share.shared.base.BaseTask;
import org.exthmui.share.shared.base.Entity;
import org.exthmui.share.shared.base.FileInfo;
import org.exthmui.share.shared.base.results.SilentResult;
import org.exthmui.share.shared.base.results.TransmissionResult;
import org.exthmui.share.shared.exceptions.trans.TransmissionException;
import org.exthmui.share.taskMgr.Result;

import java.util.List;
import java.util.Map;

public abstract class ReceivingTask extends BaseTask {

    public ReceivingTask(Context context, String taskId, Bundle inputData) {
        super(context, taskId, inputData);
    }

    @NonNull
    @Deprecated
    @Override
    protected final Result genSuccessResult() {
        throw new RuntimeException("Stub! Use the genSuccessResult(SenderInfo, List<Entity>) defined in ReceivingWorker instead");
    }

    @NonNull
    protected final Result genSuccessResult(@NonNull SenderInfo peer, @NonNull List<Entity> entities) {
        return Result.success(genSuccessData(peer, entities));
    }

    @NonNull
    protected Result genFailureResult(@NonNull TransmissionException e, @Nullable SenderInfo peer,
                                      @Nullable FileInfo[] fileInfos,
                                      @Nullable Map<String, TransmissionResult> resultMap) {
        return genFailureResult((TransmissionResult) e, peer, fileInfos, resultMap);
    }

    @NonNull
    private Result genFailureResult(@NonNull TransmissionResult result, @Nullable SenderInfo peer,
                                    @Nullable FileInfo[] fileInfos,
                                    @Nullable Map<String, TransmissionResult> resultMap) {
        return Result.failure(genFailureData(result, peer, fileInfos,
                resultMap));
    }

    @NonNull
    @Override
    protected Result genSilentResult() {
        return Result.failure(genFailureData(new SilentResult(), null, null, null));
    }

    @NonNull
    @Deprecated
    @Override
    protected Bundle genSuccessData() {
        throw new RuntimeException("Stub! Use the genSuccessData(SenderInfo, List<Entity>) defined in ReceivingWorker instead");
    }

    @NonNull
    @Deprecated
    @Override
    protected Bundle genFailureData(@NonNull TransmissionResult result,
                                  @Nullable Map<String, TransmissionResult> resultMap) {
        throw new RuntimeException("Stub! Use the genFailureData(TransmissionResult, SenderInfo, FileInfo[], Map) defined in ReceivingWorker instead");
    }

    @NonNull
    protected final Bundle genSuccessData(@NonNull SenderInfo peer, @NonNull List<Entity> entities) {
        Bundle bundle = super.genSuccessData();
        bundle.putStringArray(Entity.ENTITIES, Entity.entitiesToStrings(entities.toArray(new Entity[0])));
        bundle.putString(FROM_PEER_ID, peer.getId());
        bundle.putString(FROM_PEER_NAME, peer.getDisplayName());
        return bundle;
    }

    @NonNull
    protected final Bundle genFailureData(@NonNull TransmissionResult result,
                                          @Nullable SenderInfo peer, @Nullable FileInfo[] fileInfos,
                                          @Nullable Map<String, TransmissionResult> resultMap) {
        Bundle bundle = super.genFailureData(result, resultMap);
        if (peer != null) {
            bundle.putString(FROM_PEER_ID, peer.getId());
            bundle.putString(FROM_PEER_NAME, peer.getDisplayName());
        }
        if (fileInfos != null) {
            String[] fileNames = new String[fileInfos.length];
            for (int i = 0; i < fileInfos.length; i++)
                fileNames[i] = fileInfos[i].getFileName();
            bundle.putStringArray(Entity.FILE_NAMES, fileNames);
        }
        return bundle;
    }

    /**
     * InputData:
     * MUST contain: Nothing.
     * ***** Extra values is not allowed *****
     *
     * @return Result of work.
     * @see BaseTask#doWork()
     *
     * Also see below:
     * <p>
     * Success:
     * MUST contain:{@link String[]}: {@link Entity#ENTITIES} (Parceled, Encoded in Base64),
     * {@link String[]}: {@link Receiver#FROM_PEER_ID}
     * {@link String}: {@link Receiver#FROM_PEER_NAME}
     * & everything defined in super class
     * @see #genSuccessResult(SenderInfo, List)
     * @see #genSuccessData(SenderInfo, List)
     * ***** Extra values is not allowed *****
     * <p>
     * Failure:
     * MUST contain:{@link String[]}: {@link String[]}: {@link Entity#FILE_NAMES}
     * {@link String[]}: {@link Receiver#FROM_PEER_ID}
     * {@link String}: {@link Receiver#FROM_PEER_NAME}
     * & everything defined in super class
     * @see #genFailureResult(TransmissionException, SenderInfo, FileInfo[], Map)
     * @see #genFailureData(TransmissionResult, SenderInfo, FileInfo[], Map)
     * ***** Extra values is not allowed *****
     */
    @NonNull
    @Override
    public abstract Result doWork();
}
