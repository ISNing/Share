package org.exthmui.share.shared.base.exceptions.trans;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exthmui.share.shared.Constants;

public class UnknownErrorException extends TransmissionException {

    public UnknownErrorException(@NonNull Context context) {
        super(context);
    }

    public UnknownErrorException(@NonNull Context context, Throwable cause) {
        super(context, cause);
    }

    public UnknownErrorException(@NonNull String message) {
        super(message);
    }

    public UnknownErrorException(@NonNull String message, @Nullable String localizedMessage) {
        super(message, localizedMessage);
    }

    public UnknownErrorException(@NonNull String message, Throwable cause) {
        super(message, cause);
    }

    public UnknownErrorException(@NonNull String message, @Nullable String localizedMessage, Throwable cause) {
        super(message, localizedMessage, cause);
    }

    public UnknownErrorException(@NonNull String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public UnknownErrorException(@NonNull String message, @Nullable String localizedMessage, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, localizedMessage, cause, enableSuppression, writableStackTrace);
    }

    @NonNull
    @Override
    public Constants.TransmissionStatus getStatus() {
        return Constants.TransmissionStatus.UNKNOWN_ERROR;
    }

    @NonNull
    @Override
    protected String getDefaultMessage() {
        return "Unknown error occurred";
    }
}
