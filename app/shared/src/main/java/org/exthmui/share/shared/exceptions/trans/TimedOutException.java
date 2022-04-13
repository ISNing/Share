package org.exthmui.share.shared.exceptions.trans;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exthmui.share.shared.misc.Constants;

public class TimedOutException extends NetworkErrorException {

    public TimedOutException(@NonNull Context context) {
        super(context);
    }

    public TimedOutException(@NonNull Context context, Throwable cause) {
        super(context, cause);
    }

    public TimedOutException(@NonNull String message) {
        super(message);
    }

    public TimedOutException(@NonNull String message, @Nullable String localizedMessage) {
        super(message, localizedMessage);
    }

    public TimedOutException(@NonNull String message, Throwable cause) {
        super(message, cause);
    }

    public TimedOutException(@NonNull String message, @Nullable String localizedMessage, Throwable cause) {
        super(message, localizedMessage, cause);
    }

    public TimedOutException(@NonNull String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public TimedOutException(@NonNull String message, @Nullable String localizedMessage, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, localizedMessage, cause, enableSuppression, writableStackTrace);
    }

    @NonNull
    @Override
    public Constants.TransmissionStatus getStatus() {
        return Constants.TransmissionStatus.TIMED_OUT;
    }

    @NonNull
    @Override
    protected String getDefaultMessage() {
        return "Connection timed out";
    }
}
