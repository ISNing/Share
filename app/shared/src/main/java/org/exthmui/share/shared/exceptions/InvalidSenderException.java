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

    public InvalidSenderException(@NonNull String message) {
        super(message);
    }

    public InvalidSenderException(@NonNull String message, @Nullable String localizedMessage) {
        super(message, localizedMessage);
    }

    public InvalidSenderException(@NonNull String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidSenderException(@NonNull String message, @Nullable String localizedMessage, Throwable cause) {
        super(message, localizedMessage, cause);
    }

    public InvalidSenderException(@NonNull String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public InvalidSenderException(@NonNull String message, @Nullable String localizedMessage, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, localizedMessage, cause, enableSuppression, writableStackTrace);
    }

    @Override
    String getDefaultMessage() {
        return "Invalid sender";
    }

    @Override
    int getLocalizedMessageStrRes() {
        return R.string.error_invalid_sender;
    }
}
