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

    public CancelledException(@NonNull String message) {
        super(message);
    }

    public CancelledException(@NonNull String message, @Nullable String localizedMessage) {
        super(message, localizedMessage);
    }

    public CancelledException(@NonNull String message, Throwable cause) {
        super(message, cause);
    }

    public CancelledException(@NonNull String message, @Nullable String localizedMessage, Throwable cause) {
        super(message, localizedMessage, cause);
    }

    public CancelledException(@NonNull String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public CancelledException(@NonNull String message, @Nullable String localizedMessage, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
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
