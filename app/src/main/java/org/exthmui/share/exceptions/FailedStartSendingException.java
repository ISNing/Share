package org.exthmui.share.exceptions;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class FailedStartSendingException extends LocalizedException {

    public FailedStartSendingException(@NonNull Context context) {
        super(context);
    }

    public FailedStartSendingException(Context context, Throwable cause) {
        super(context, cause);
    }

    public FailedStartSendingException(@Nullable String message) {
        super(message);
    }

    public FailedStartSendingException(@Nullable String message, @Nullable String localizedMessage) {
        super(message, localizedMessage);
    }

    public FailedStartSendingException(@Nullable String message, Throwable cause) {
        super(message, cause);
    }

    public FailedStartSendingException(@Nullable String message, @Nullable String localizedMessage, Throwable cause) {
        super(message, localizedMessage, cause);
    }

    public FailedStartSendingException(Throwable cause) {
        super(cause);
    }

    public FailedStartSendingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public FailedStartSendingException(@Nullable String message, @Nullable String localizedMessage, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, localizedMessage, cause, enableSuppression, writableStackTrace);
    }

    @Override
    public int getMessageStrRes() {
        return 0;
    }

    @Override
    public int getLocalizedMessageStrRes() {
        return 0;
    }
}
