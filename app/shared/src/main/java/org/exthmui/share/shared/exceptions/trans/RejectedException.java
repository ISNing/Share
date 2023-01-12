package org.exthmui.share.shared.exceptions.trans;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exthmui.share.shared.misc.Constants;

public class RejectedException extends TransmissionException {

    public RejectedException(@NonNull Context context) {
        super(context);
    }

    public RejectedException(@NonNull Context context, Throwable cause) {
        super(context, cause);
    }

    public RejectedException(@Nullable String message) {
        super(message);
    }

    public RejectedException(@Nullable String message, @Nullable String localizedMessage) {
        super(message, localizedMessage);
    }

    public RejectedException(@Nullable String message, Throwable cause) {
        super(message, cause);
    }

    public RejectedException(@Nullable String message, @Nullable String localizedMessage, Throwable cause) {
        super(message, localizedMessage, cause);
    }

    public RejectedException(@Nullable String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public RejectedException(@Nullable String message, @Nullable String localizedMessage, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, localizedMessage, cause, enableSuppression, writableStackTrace);
    }

    @NonNull
    @Override
    public Constants.TransmissionStatus getStatus() {
        return Constants.TransmissionStatus.REJECTED;
    }

    @NonNull
    @Override
    protected String getDefaultMessage() {
        return "Transmission has been rejected by receiver";
    }
}
