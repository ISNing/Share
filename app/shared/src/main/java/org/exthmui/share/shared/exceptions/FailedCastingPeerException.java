package org.exthmui.share.shared.exceptions;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exthmui.share.shared.R;

public class FailedCastingPeerException extends InvalidPeerException {
    public FailedCastingPeerException(@NonNull Context context) {
        super(context);
    }

    public FailedCastingPeerException(@NonNull Context context, Throwable cause) {
        super(context, cause);
    }

    public FailedCastingPeerException(@NonNull String message) {
        super(message);
    }

    public FailedCastingPeerException(@NonNull String message, @Nullable String localizedMessage) {
        super(message, localizedMessage);
    }

    public FailedCastingPeerException(@NonNull String message, Throwable cause) {
        super(message, cause);
    }

    public FailedCastingPeerException(@NonNull String message, @Nullable String localizedMessage, Throwable cause) {
        super(message, localizedMessage, cause);
    }

    public FailedCastingPeerException(@NonNull String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public FailedCastingPeerException(@NonNull String message, @Nullable String localizedMessage, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, localizedMessage, cause, enableSuppression, writableStackTrace);
    }

    @Override
    String getDefaultMessage() {
        return "Failed casting peer";
    }

    @Override
    int getLocalizedMessageStrRes() {
        return R.string.error_failed_casting_peer;
    }
}
