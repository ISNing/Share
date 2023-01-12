package org.exthmui.share.shared.exceptions;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exthmui.share.shared.R;

public class InvalidPeerException extends FailedStartSendingException {

    public InvalidPeerException(@NonNull Context context) {
        super(context);
    }

    public InvalidPeerException(@NonNull Context context, Throwable cause) {
        super(context, cause);
    }

    public InvalidPeerException(@Nullable String message) {
        super(message);
    }

    public InvalidPeerException(@Nullable String message, @Nullable String localizedMessage) {
        super(message, localizedMessage);
    }

    public InvalidPeerException(@Nullable String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidPeerException(@Nullable String message, @Nullable String localizedMessage, Throwable cause) {
        super(message, localizedMessage, cause);
    }

    public InvalidPeerException(@Nullable String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public InvalidPeerException(@Nullable String message, @Nullable String localizedMessage, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, localizedMessage, cause, enableSuppression, writableStackTrace);
    }

    @NonNull
    @Override
    protected String getDefaultMessage() {
        return "Invalid peer";
    }

    @Override
    protected int getLocalizedMessageStrRes() {
        return R.string.error_invalid_peer;
    }
}
