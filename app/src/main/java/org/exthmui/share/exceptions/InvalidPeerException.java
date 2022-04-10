package org.exthmui.share.exceptions;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exthmui.share.R;

public class InvalidPeerException extends FailedStartSendingException {

    public InvalidPeerException(@NonNull Context context) {
        super(context);
    }

    public InvalidPeerException(@NonNull Context context, Throwable cause) {
        super(context, cause);
    }

    public InvalidPeerException(@NonNull String message) {
        super(message);
    }

    public InvalidPeerException(@NonNull String message, @Nullable String localizedMessage) {
        super(message, localizedMessage);
    }

    public InvalidPeerException(@NonNull String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidPeerException(@NonNull String message, @Nullable String localizedMessage, Throwable cause) {
        super(message, localizedMessage, cause);
    }

    public InvalidPeerException(@NonNull String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public InvalidPeerException(@NonNull String message, @Nullable String localizedMessage, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, localizedMessage, cause, enableSuppression, writableStackTrace);
    }

    @Override
    String getDefaultMessage() {
        return "Invalid peer";
    }

    @Override
    int getLocalizedMessageStrRes() {
        return R.string.error_invalid_peer;
    }
}
