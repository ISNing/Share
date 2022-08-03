package org.exthmui.share.shared.base.send;

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
import org.exthmui.share.shared.exceptions.trans.InvalidInputDataException;
import org.exthmui.share.shared.misc.Constants;
import org.exthmui.share.shared.misc.SenderUtils;

public abstract class SendingWorker extends BaseWorker {

    public SendingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @Override
    protected final int getNotificationId() {
        return (Constants.NOTIFICATION_ID_PREFIX_SEND + getId()).hashCode();
    }

    @NonNull
    @Override
    protected final Notification buildProgressNotification(int statusCode, long totalBytesToSend,
                                                           long bytesSent,
                                                           @NonNull FileInfo[] fileInfos,
                                                           @Nullable PeerInfoTransfer receiverInfo,
                                                           @Nullable String curFileId,
                                                           long curFileBytesToSend,
                                                           long curFileBytesSent,
                                                           boolean indeterminate) {
        return SenderUtils.buildSendingNotification(getApplicationContext(), getConnectionType(),
                statusCode, getId(), totalBytesToSend, bytesSent, fileInfos, (ReceiverInfo) receiverInfo,
                curFileId, curFileBytesToSend, curFileBytesSent, indeterminate);
    }

    /**
     * InputData:
     * MUST contain:{@link String[]}: {@link Entity#ENTITIES} (Parceled, Encoded in Base64),
     * {@link String[]}: {@link Sender#TARGET_PEER_ID}
     * {@link String}: {@link Sender#TARGET_PEER_NAME}
     * ***** Extra values is allowed *****
     *
     * @return Result of work.
     * @see BaseWorker#doWork()
     */
    @NonNull
    @Override
    public final Result doWork() {
        Data input = getInputData();
        String[] entitiesStrings = input.getStringArray(Entity.ENTITIES);
        if (entitiesStrings == null || entitiesStrings.length == 0)
            return genFailureResult(new InvalidInputDataException(getApplicationContext()), null);

        Entity[] entities = Entity.stringsToEntities(entitiesStrings);
        String peerId = input.getString(Sender.TARGET_PEER_ID);
        String peerName = input.getString(Sender.TARGET_PEER_NAME);
        return doWork(entities, peerId, peerName);
    }

    @NonNull
    public abstract Result doWork(Entity[] entities, String peerId, String peerName);
}
