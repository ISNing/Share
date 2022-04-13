package org.exthmui.share.shared.exceptions.trans;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exthmui.share.shared.misc.Constants;

public class NoEnoughSpaceException extends FileIOErrorException {
    public NoEnoughSpaceException(@NonNull Context context) {
        super(context);
    }

    public NoEnoughSpaceException(@NonNull Context context, Throwable cause) {
        super(context, cause);
    }

    public NoEnoughSpaceException(@NonNull String message) {
        super(message);
    }

    public NoEnoughSpaceException(@NonNull String message, @Nullable String localizedMessage) {
        super(message, localizedMessage);
    }

    public NoEnoughSpaceException(@NonNull String message, Throwable cause) {
        super(message, cause);
    }

    public NoEnoughSpaceException(@NonNull String message, @Nullable String localizedMessage, Throwable cause) {
        super(message, localizedMessage, cause);
    }

    public NoEnoughSpaceException(@NonNull String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public NoEnoughSpaceException(@NonNull String message, @Nullable String localizedMessage, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, localizedMessage, cause, enableSuppression, writableStackTrace);
    }

    @NonNull
    @Override
    public Constants.TransmissionStatus getStatus() {
        return Constants.TransmissionStatus.NO_ENOUGH_SPACE;
    }

    @NonNull
    @Override
    protected String getDefaultMessage() {
        return "No enough disk space left";
    }
}
