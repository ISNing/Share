package org.exthmui.share.shared.base.receive;

import static org.exthmui.share.shared.base.receive.Receiver.FROM_PEER_ID;
import static org.exthmui.share.shared.base.receive.Receiver.FROM_PEER_NAME;

import android.app.Notification;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Data;
import androidx.work.WorkerParameters;

import org.exthmui.share.shared.base.BaseWorker;
import org.exthmui.share.shared.base.Entity;
import org.exthmui.share.shared.base.FileInfo;
import org.exthmui.share.shared.base.PeerInfoTransfer;
import org.exthmui.share.shared.base.results.SilentResult;
import org.exthmui.share.shared.base.results.TransmissionResult;
import org.exthmui.share.shared.exceptions.trans.TransmissionException;
import org.exthmui.share.shared.misc.Constants;
import org.exthmui.share.shared.misc.ReceiverUtils;

import java.util.List;
import java.util.Map;

public abstract class ReceivingWorker extends BaseWorker {

    public ReceivingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @Override
    protected final int getNotificationId() {
        return (Constants.NOTIFICATION_ID_PREFIX_RECEIVE + getId()).hashCode();
    }

    @NonNull
    @Override
    protected final Notification buildProgressNotification(int statusCode, long totalBytesToSend,
                                                           long bytesReceived,
                                                           @NonNull FileInfo[] fileInfos,
                                                           @Nullable PeerInfoTransfer senderInfo,
                                                           @Nullable String curFileId,
                                                           long curFileBytesToSend,
                                                           long curFileBytesSent,
                                                           boolean indeterminate) {
        return ReceiverUtils.buildReceivingNotification(getApplicationContext(), getConnectionType(),
                statusCode, getId(), totalBytesToSend, bytesReceived, fileInfos,
                (SenderInfo) senderInfo, indeterminate);
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
    protected Data genSuccessData() {
        throw new RuntimeException("Stub! Use the genSuccessData(SenderInfo, List<Entity>) defined in ReceivingWorker instead");
    }

    @NonNull
    @Deprecated
    @Override
    protected Data genFailureData(@NonNull TransmissionResult result,
                                  @Nullable Map<String, TransmissionResult> resultMap) {
        throw new RuntimeException("Stub! Use the genFailureData(TransmissionResult, SenderInfo, FileInfo[], Map) defined in ReceivingWorker instead");
    }

    @NonNull
    protected final Data genSuccessData(@NonNull SenderInfo peer, @NonNull List<Entity> entities) {
        return genSuccessDataBuilder()
                .putStringArray(Entity.ENTITIES, Entity.entitiesToStrings(entities.toArray(new Entity[0])))
                .putString(FROM_PEER_ID, peer.getId())
                .putString(FROM_PEER_NAME, peer.getDisplayName())
                .build();
    }

    @NonNull
    protected final Data genFailureData(@NonNull TransmissionResult result,
                                        @Nullable SenderInfo peer, @Nullable FileInfo[] fileInfos,
                                        @Nullable Map<String, TransmissionResult> resultMap) {
        Data.Builder builder = genFailureDataBuilder(result, resultMap);
        if (peer != null) {
            builder.putString(FROM_PEER_ID, peer.getId())
                    .putString(FROM_PEER_NAME, peer.getDisplayName());
        }
        if (fileInfos != null) {
            String[] fileNames = new String[fileInfos.length];
            for (int i = 0; i < fileInfos.length; i++)
                fileNames[i] = fileInfos[i].getFileName();
            builder.putStringArray(Entity.FILE_NAMES, fileNames);
        }
        return builder.build();
    }

    /**
     * InputData:
     * MUST contain: Nothing.
     * ***** Extra values is not allowed *****
     *
     * @return Result of work.
     * @see BaseWorker#doWork()
     *
     * Also see below:
     *
     * Success:
     * MUST contain:{@link String[]}: {@link Entity#ENTITIES} (Parceled, Encoded in Base64),
     * {@link String[]}: {@link Receiver#FROM_PEER_ID}
     * {@link String}: {@link Receiver#FROM_PEER_NAME}
     * & everything defined in super class
     * @see #genSuccessResult(SenderInfo, List)
     * @see #genSuccessData(SenderInfo, List)
     * ***** Extra values is not allowed *****
     *
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
