package org.exthmui.share.shared.exceptions;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exthmui.share.shared.R;

public class InvalidSenderException extends FailedStartSendingException {

    public InvalidSenderException(@NonNull Context context) {
        super(context);
    }

    public InvalidSenderException(@NonNull Context context, Throwable cause) {
        super(context, cause);
    }

    public InvalidSenderException(@Nullable String message) {
        super(message);
    }

    public InvalidSenderException(@Nullable String message, @Nullable String localizedMessage) {
        super(message, localizedMessage);
    }

    public InvalidSenderException(@Nullable String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidSenderException(@Nullable String message, @Nullable String localizedMessage, Throwable cause) {
        super(message, localizedMessage, cause);
    }

    public InvalidSenderException(@Nullable String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public InvalidSenderException(@Nullable String message, @Nullable String localizedMessage, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, localizedMessage, cause, enableSuppression, writableStackTrace);
    }

    @NonNull
    @Override
    protected String getDefaultMessage() {
        return "Invalid sender";
    }

    @Override
    protected int getLocalizedMessageStrRes() {
        return R.string.error_invalid_sender;
    }
}
