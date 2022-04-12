package org.exthmui.share.shared.base;

import android.app.Notification;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.WorkerParameters;

import org.exthmui.share.shared.Constants;
import org.exthmui.share.shared.SenderUtils;

public abstract class SendingWorker extends BaseWorker {

    public SendingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @Override
    int getNotificationId() {
        return (Constants.NOTIFICATION_ID_PREFIX_SEND + getId()).hashCode();
    }

    @NonNull
    @Override
    Notification buildProgressNotification(int statusCode, long totalBytesToSend, long bytesSent, @NonNull String[] fileNames, @Nullable String targetName, boolean indeterminate) {
        return SenderUtils.buildSendingNotification(getApplicationContext(), getConnectionType(), statusCode, getId(), totalBytesToSend, bytesSent, fileNames, targetName, indeterminate);
    }

    /**
     * InputData:
     * MUST contain:{@link String}: {@link Entity#FILE_URI},
     * {@link String}: {@link Entity#FILE_NAME},
     * {@link String}: {@link Entity#FILE_PATH},
     * {@link long}: {@link Entity#FILE_SIZE},
     * {@link int}: {@link Entity#FILE_TYPE}, For more {@link Constants.FileType#getNumVal()}
     * {@link String}: {@link Sender#TARGET_PEER_ID}
     * {@link String}: {@link Sender#TARGET_PEER_NAME}
     * <p>
     * OR:{@link String[]}: {@link Entity#FILE_URIS},
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
