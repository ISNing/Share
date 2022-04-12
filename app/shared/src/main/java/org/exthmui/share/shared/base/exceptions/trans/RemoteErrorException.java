package org.exthmui.share.shared.base.exceptions.trans;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exthmui.share.shared.Constants;

public class RemoteErrorException extends TransmissionException {

    public RemoteErrorException(@NonNull Context context) {
        super(context);
    }

    public RemoteErrorException(@NonNull Context context, Throwable cause) {
        super(context, cause);
    }

    public RemoteErrorException(@NonNull String message) {
        super(message);
    }

    public RemoteErrorException(@NonNull String message, @Nullable String localizedMessage) {
        super(message, localizedMessage);
    }

    public RemoteErrorException(@NonNull String message, Throwable cause) {
        super(message, cause);
    }

    public RemoteErrorException(@NonNull String message, @Nullable String localizedMessage, Throwable cause) {
        super(message, localizedMessage, cause);
    }

    public RemoteErrorException(@NonNull String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public RemoteErrorException(@NonNull String message, @Nullable String localizedMessage, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, localizedMessage, cause, enableSuppression, writableStackTrace);
    }

    @NonNull
    @Override
    public Constants.TransmissionStatus getStatus() {
        return Constants.TransmissionStatus.REMOTE_ERROR;
    }

    @NonNull
    @Override
    protected String getDefaultMessage() {
        return "Unknown error from remote side";
    }
}
