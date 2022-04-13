package org.exthmui.share.shared.exceptions.trans;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exthmui.share.shared.misc.Constants;

public class ReceiverCancelledException extends CancelledException {

    public ReceiverCancelledException(@NonNull Context context) {
        super(context);
    }

    public ReceiverCancelledException(@NonNull Context context, Throwable cause) {
        super(context, cause);
    }

    public ReceiverCancelledException(@NonNull String message) {
        super(message);
    }

    public ReceiverCancelledException(@NonNull String message, @Nullable String localizedMessage) {
        super(message, localizedMessage);
    }

    public ReceiverCancelledException(@NonNull String message, Throwable cause) {
        super(message, cause);
    }

    public ReceiverCancelledException(@NonNull String message, @Nullable String localizedMessage, Throwable cause) {
        super(message, localizedMessage, cause);
    }

    public ReceiverCancelledException(@NonNull String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ReceiverCancelledException(@NonNull String message, @Nullable String localizedMessage, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, localizedMessage, cause, enableSuppression, writableStackTrace);
    }

    @NonNull
    @Override
    public Constants.TransmissionStatus getStatus() {
        return Constants.TransmissionStatus.RECEIVER_CANCELLED;
    }

    @NonNull
    @Override
    protected String getDefaultMessage() {
        return "Receiver cancelled receiving file(s)";
    }
}
