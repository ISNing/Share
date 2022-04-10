package org.exthmui.share.exceptions;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exthmui.share.R;

public class InvalidConnectionTypeException extends FailedStartSendingException {

    public InvalidConnectionTypeException(@NonNull Context context) {
        super(context);
    }

    public InvalidConnectionTypeException(@NonNull Context context, Throwable cause) {
        super(context, cause);
    }

    public InvalidConnectionTypeException(@NonNull String message) {
        super(message);
    }

    public InvalidConnectionTypeException(@NonNull String message, @Nullable String localizedMessage) {
        super(message, localizedMessage);
    }

    public InvalidConnectionTypeException(@NonNull String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidConnectionTypeException(@NonNull String message, @Nullable String localizedMessage, Throwable cause) {
        super(message, localizedMessage, cause);
    }

    public InvalidConnectionTypeException(@NonNull String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public InvalidConnectionTypeException(@NonNull String message, @Nullable String localizedMessage, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, localizedMessage, cause, enableSuppression, writableStackTrace);
    }

    @Override
    String getDefaultMessage() {
        return "Invalid connection type";
    }

    @Override
    int getLocalizedMessageStrRes() {
        return R.string.error_invalid_connection_type;
    }
}
