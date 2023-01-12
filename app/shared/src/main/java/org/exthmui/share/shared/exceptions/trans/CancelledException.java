package org.exthmui.share.shared.exceptions.trans;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exthmui.share.shared.misc.Constants;

public class CancelledException extends TransmissionException {

    public CancelledException(@NonNull Context context) {
        super(context);
    }

    public CancelledException(@NonNull Context context, Throwable cause) {
        super(context, cause);
    }

    public CancelledException(@Nullable String message) {
        super(message);
    }

    public CancelledException(@Nullable String message, @Nullable String localizedMessage) {
        super(message, localizedMessage);
    }

    public CancelledException(@Nullable String message, Throwable cause) {
        super(message, cause);
    }

    public CancelledException(@Nullable String message, @Nullable String localizedMessage, Throwable cause) {
        super(message, localizedMessage, cause);
    }

    public CancelledException(@Nullable String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public CancelledException(@Nullable String message, @Nullable String localizedMessage, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, localizedMessage, cause, enableSuppression, writableStackTrace);
    }

    @NonNull
    @Override
    public Constants.TransmissionStatus getStatus() {
        return Constants.TransmissionStatus.CANCELLED;
    }

    @NonNull
    @Override
    protected String getDefaultMessage() {
        return "Sending/Receiving has been cancelled";
    }
}
