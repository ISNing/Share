package org.exthmui.share.exceptions;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exthmui.share.R;

public class FailedInvokingSendingMethodException extends FailedStartSendingException {

    public FailedInvokingSendingMethodException(@NonNull Context context) {
        super(context);
    }

    public FailedInvokingSendingMethodException(@NonNull Context context, Throwable cause) {
        super(context, cause);
    }

    public FailedInvokingSendingMethodException(@NonNull String message) {
        super(message);
    }

    public FailedInvokingSendingMethodException(@NonNull String message, @Nullable String localizedMessage) {
        super(message, localizedMessage);
    }

    public FailedInvokingSendingMethodException(@NonNull String message, Throwable cause) {
        super(message, cause);
    }

    public FailedInvokingSendingMethodException(@NonNull String message, @Nullable String localizedMessage, Throwable cause) {
        super(message, localizedMessage, cause);
    }

    public FailedInvokingSendingMethodException(@NonNull String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public FailedInvokingSendingMethodException(@NonNull String message, @Nullable String localizedMessage, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, localizedMessage, cause, enableSuppression, writableStackTrace);
    }

    @Override
    String getDefaultMessage() {
        return "Failed invoking sending method";
    }

    @Override
    int getLocalizedMessageStrRes() {
        return R.string.error_failed_invoking_sending_method;
    }
}
