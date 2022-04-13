package org.exthmui.share.shared.base.send;

import android.app.Notification;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.WorkerParameters;

import org.exthmui.share.shared.base.BaseWorker;
import org.exthmui.share.shared.base.Entity;
import org.exthmui.share.shared.base.PeerInfoTransfer;
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
    protected final Notification buildProgressNotification(int statusCode, long totalBytesToSend, long bytesSent, @NonNull String[] fileNames, @Nullable PeerInfoTransfer receiverInfo, boolean indeterminate) {
        return SenderUtils.buildSendingNotification(getApplicationContext(), getConnectionType(), statusCode, getId(), totalBytesToSend, bytesSent, fileNames, (ReceiverInfo) receiverInfo, indeterminate);
    }

    /**
     * InputData:
     * MUST contain:{@link String[]}: {@link Entity#FILE_URIS},
     * {@link String[]}: {@link Entity#FILE_NAMES},
     * {@link String[]}: {@link Entity#FILE_PATHS},
     * {@link long[]}: {@link Entity#FILE_SIZES},
     * {@link int[]}: {@link Entity#FILE_TYPES}, For more {@link Constants.FileType#getNumVal()}
     * {@link String[]}: {@link Sender#TARGET_PEER_ID}
     * {@link String}: {@link Sender#TARGET_PEER_NAME}
     * ***** Extra values is allowed *****
     *
     * @return Result of work.
     * @see BaseWorker#doWork()
     */
    @NonNull
    @Override
    public abstract Result doWork();
}
