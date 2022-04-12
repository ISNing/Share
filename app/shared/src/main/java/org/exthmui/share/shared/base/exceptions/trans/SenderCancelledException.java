package org.exthmui.share.shared.base.exceptions.trans;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exthmui.share.shared.Constants;

public class SenderCancelledException extends CancelledException {
    public SenderCancelledException(@NonNull Context context) {
        super(context);
    }

    public SenderCancelledException(@NonNull Context context, Throwable cause) {
        super(context, cause);
    }

    public SenderCancelledException(@NonNull String message) {
        super(message);
    }

    public SenderCancelledException(@NonNull String message, @Nullable String localizedMessage) {
        super(message, localizedMessage);
    }

    public SenderCancelledException(@NonNull String message, Throwable cause) {
        super(message, cause);
    }

    public SenderCancelledException(@NonNull String message, @Nullable String localizedMessage, Throwable cause) {
        super(message, localizedMessage, cause);
    }

    public SenderCancelledException(@NonNull String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public SenderCancelledException(@NonNull String message, @Nullable String localizedMessage, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, localizedMessage, cause, enableSuppression, writableStackTrace);
    }

    @NonNull
    @Override
    public Constants.TransmissionStatus getStatus() {
        return Constants.TransmissionStatus.SENDER_CANCELLED;
    }

    @NonNull
    @Override
    protected String getDefaultMessage() {
        return "Sender cancelled sending/receiving file(s)";
    }
}
