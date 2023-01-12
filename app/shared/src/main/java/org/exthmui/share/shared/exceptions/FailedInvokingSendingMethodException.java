package org.exthmui.share.shared.exceptions;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exthmui.share.shared.R;

public class FailedInvokingSendingMethodException extends FailedStartSendingException {

    public FailedInvokingSendingMethodException(@NonNull Context context) {
        super(context);
    }

    public FailedInvokingSendingMethodException(@NonNull Context context, Throwable cause) {
        super(context, cause);
    }

    public FailedInvokingSendingMethodException(@Nullable String message) {
        super(message);
    }

    public FailedInvokingSendingMethodException(@Nullable String message, @Nullable String localizedMessage) {
        super(message, localizedMessage);
    }

    public FailedInvokingSendingMethodException(@Nullable String message, Throwable cause) {
        super(message, cause);
    }

    public FailedInvokingSendingMethodException(@Nullable String message, @Nullable String localizedMessage, Throwable cause) {
        super(message, localizedMessage, cause);
    }

    public FailedInvokingSendingMethodException(@Nullable String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public FailedInvokingSendingMethodException(@Nullable String message, @Nullable String localizedMessage, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, localizedMessage, cause, enableSuppression, writableStackTrace);
    }

    @NonNull
    @Override
    protected String getDefaultMessage() {
        return "Failed invoking sending method";
    }

    @Override
    protected int getLocalizedMessageStrRes() {
        return R.string.error_failed_invoking_sending_method;
    }
}
