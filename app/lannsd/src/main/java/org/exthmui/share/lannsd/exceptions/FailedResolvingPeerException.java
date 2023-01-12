package org.exthmui.share.lannsd.exceptions;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exthmui.share.shared.exceptions.trans.PeerDisappearedException;

public class FailedResolvingPeerException extends PeerDisappearedException {
    public FailedResolvingPeerException(@NonNull Context context) {
        super(context);
    }

    public FailedResolvingPeerException(@NonNull Context context, Throwable cause) {
        super(context, cause);
    }

    public FailedResolvingPeerException(@Nullable String message) {
        super(message);
    }

    public FailedResolvingPeerException(@Nullable String message, @Nullable String localizedMessage) {
        super(message, localizedMessage);
    }

    public FailedResolvingPeerException(@Nullable String message, Throwable cause) {
        super(message, cause);
    }

    public FailedResolvingPeerException(@Nullable String message, @Nullable String localizedMessage, Throwable cause) {
        super(message, localizedMessage, cause);
    }

    public FailedResolvingPeerException(@Nullable String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public FailedResolvingPeerException(@Nullable String message, @Nullable String localizedMessage, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, localizedMessage, cause, enableSuppression, writableStackTrace);
    }

    @NonNull
    @Override
    protected String getDefaultMessage() {
        return "Failed resolving peer";
    }
}
