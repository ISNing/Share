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
import org.exthmui.share.shared.exceptions.trans.ReceiverCancelledException;
import org.exthmui.share.shared.exceptions.trans.RejectedException;
import org.exthmui.share.shared.exceptions.trans.SenderCancelledException;
import org.exthmui.share.shared.exceptions.trans.TransmissionException;
import org.exthmui.share.shared.misc.Constants;
import org.exthmui.share.shared.misc.ReceiverUtils;

import java.util.List;

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
    protected final Notification buildProgressNotification(int statusCode, long totalBytesToSend, long bytesReceived, @NonNull FileInfo[] fileInfos, @Nullable PeerInfoTransfer senderInfo, boolean indeterminate) {
        return ReceiverUtils.buildReceivingNotification(getApplicationContext(), getConnectionType(), statusCode, getId(), totalBytesToSend, bytesReceived, fileInfos, (SenderInfo) senderInfo, indeterminate);
    }

    @NonNull
    @Deprecated
    @Override
    protected final Result genSuccessResult() {
        throw new RuntimeException("Stub! Use the genSuccessResult(SenderInfo, List<Entity>) defined in ReceivingWorker instead");
    }

    @NonNull
    @Deprecated
    @Override
    protected final Result genRejectedResult(@NonNull Context context) {
        throw new RuntimeException("Stub! Use the genRejectedResult(Context, SenderInfo, String[]) defined in ReceivingWorker instead");
    }

    @NonNull
    @Deprecated
    @Override
    protected final Result genSenderCancelledResult(@NonNull Context context) {
        throw new RuntimeException("Stub! Use the genSenderCancelledResult(Context, SenderInfo, String[]) defined in ReceivingWorker instead");
    }

    @NonNull
    @Deprecated
    @Override
    protected final Result genReceiverCancelledResult(@NonNull Context context) {
        throw new RuntimeException("Stub! Use the genReceiverCancelledResult(Context, SenderInfo, String[]) defined in ReceivingWorker instead");
    }

    @NonNull
    @Deprecated
    @Override
    protected Result genFailureResult(@NonNull TransmissionException e) {
        throw new RuntimeException("Stub! Use the genFailureResult(TransmissionException, SenderInfo, String[]) defined in ReceivingWorker instead");
    }

    @NonNull
    protected final Result genSuccessResult(@NonNull SenderInfo peer, @NonNull List<Entity> entities) {
        return Result.failure(genSuccessData(peer, entities));
    }

    @NonNull
    protected final Result genRejectedResult(@NonNull Context context, @Nullable SenderInfo peer, @Nullable FileInfo[] fileInfos) {
        return genFailureResult(new RejectedException(context), peer, fileInfos);
    }

    @NonNull
    protected final Result genSenderCancelledResult(@NonNull Context context, @Nullable SenderInfo peer, @Nullable FileInfo[] fileInfos) {
        return genFailureResult(new SenderCancelledException(context), peer, fileInfos);
    }

    @NonNull
    protected final Result genReceiverCancelledResult(@NonNull Context context, @Nullable SenderInfo peer, @Nullable FileInfo[] fileInfos) {
        return genFailureResult(new ReceiverCancelledException(context), peer, fileInfos);
    }

    @NonNull
    protected Result genFailureResult(@NonNull TransmissionException e, @Nullable SenderInfo peer, @Nullable FileInfo[] fileInfos) {
        return genFailureResult(e.getStatusCode(), e.getMessage(), e.getLocalizedMessage(), peer, fileInfos);
    }

    @NonNull
    private Result genFailureResult(int errCode, @Nullable String message, @Nullable String localizedMessage, @Nullable SenderInfo peer, @Nullable FileInfo[] fileInfos) {
        return Result.failure(genFailureData(errCode, message, localizedMessage, peer, fileInfos));
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
    protected Data genFailureData(int statusCode, @Nullable String message, @Nullable String localizedMessage) {
        throw new RuntimeException("Stub! Use the genFailureData(int, String, String, SenderInfo, String[]) defined in ReceivingWorker instead");
    }

    @NonNull
    protected final Data genSuccessData(@NonNull SenderInfo peer, @NonNull List<Entity> entities) {
        String[] uriStrings = new String[entities.size()];
        String[] fileNames = new String[entities.size()];
        String[] filePaths = new String[entities.size()];
        long[] fileSizes = new long[entities.size()];
        int[] fileTypes = new int[entities.size()];
        for (int i = 0; i < entities.size(); i++) {
            uriStrings[i] = entities.get(i).getUri().toString();
            fileNames[i] = entities.get(i).getFileName();
            filePaths[i] = entities.get(i).getFilePath();
            fileSizes[i] = entities.get(i).getFileSize();
            fileTypes[i] = entities.get(i).getFileType();
        }
        return genSuccessDataBuilder()
                .putStringArray(Entity.FILE_URIS, uriStrings)
                .putStringArray(Entity.FILE_NAMES, fileNames)
                .putStringArray(Entity.FILE_PATHS, filePaths)
                .putLongArray(Entity.FILE_SIZES, fileSizes)
                .putIntArray(Entity.FILE_TYPES, fileTypes)
                .putString(FROM_PEER_ID, peer.getId())
                .putString(FROM_PEER_NAME, peer.getDisplayName())
                .build();
    }

    @NonNull
    protected final Data genFailureData(int errCode, @Nullable String message, @Nullable String localizedMessage, @Nullable SenderInfo peer, @Nullable FileInfo[] fileInfos) {
        Data.Builder builder = genFailureDataBuilder(errCode, message, localizedMessage);
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
     * MUST contain:{@link String[]}: {@link Entity#FILE_URIS},
     * {@link String[]}: {@link Entity#FILE_NAMES},
     * {@link String[]}: {@link Entity#FILE_PATHS},
     * {@link long[]}: {@link Entity#FILE_SIZES},
     * {@link int[]}: {@link Entity#FILE_TYPES}, For more {@link Constants.FileType#getNumVal()}
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
     * @see #genFailureResult(TransmissionException, SenderInfo, FileInfo[])
     * @see #genFailureData(int, String, String, SenderInfo, FileInfo[])
     * ***** Extra values is not allowed *****
     */
    @NonNull
    @Override
    public abstract Result doWork();
}
