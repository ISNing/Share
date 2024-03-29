package org.exthmui.share.shared.exceptions.trans;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exthmui.share.shared.misc.Constants;

public class UnknownErrorException extends TransmissionException {

    public UnknownErrorException(@NonNull Context context) {
        super(context);
    }

    public UnknownErrorException(@NonNull Context context, Throwable cause) {
        super(context, cause);
    }

    public UnknownErrorException(@Nullable String message) {
        super(message);
    }

    public UnknownErrorException(@Nullable String message, @Nullable String localizedMessage) {
        super(message, localizedMessage);
    }

    public UnknownErrorException(@Nullable String message, Throwable cause) {
        super(message, cause);
    }

    public UnknownErrorException(@Nullable String message, @Nullable String localizedMessage, Throwable cause) {
        super(message, localizedMessage, cause);
    }

    public UnknownErrorException(@Nullable String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public UnknownErrorException(@Nullable String message, @Nullable String localizedMessage, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, localizedMessage, cause, enableSuppression, writableStackTrace);
    }

    @NonNull
    @Override
    public Constants.TransmissionStatus getStatus() {
        return Constants.TransmissionStatus.ERROR;
    }

    @NonNull
    @Override
    protected String getDefaultMessage() {
        return "Unknown error occurred";
    }
}
