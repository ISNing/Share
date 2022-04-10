package org.exthmui.share.exceptions;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exthmui.share.R;

public class FailedStartSendingException extends LocalizedException {

    public FailedStartSendingException(@NonNull Context context) {
        super(context);
    }

    public FailedStartSendingException(@NonNull Context context, Throwable cause) {
        super(context, cause);
    }

    public FailedStartSendingException(@NonNull String message) {
        super(message);
    }

    public FailedStartSendingException(@NonNull String message, @Nullable String localizedMessage) {
        super(message, localizedMessage);
    }

    public FailedStartSendingException(@NonNull String message, Throwable cause) {
        super(message, cause);
    }

    public FailedStartSendingException(@NonNull String message, @Nullable String localizedMessage, Throwable cause) {
        super(message, localizedMessage, cause);
    }

    public FailedStartSendingException(@NonNull String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public FailedStartSendingException(@NonNull String message, @Nullable String localizedMessage, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, localizedMessage, cause, enableSuppression, writableStackTrace);
    }

    @Override
    String getDefaultMessage() {
        return "Failed start sending";
    }

    @Override
    int getLocalizedMessageStrRes() {
        return R.string.error_failed_start_sending;
    }
}
