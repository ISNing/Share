package org.exthmui.share.shared.exceptions;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exthmui.share.shared.R;

public class PeerDisappearedException extends InvalidPeerException {

    public PeerDisappearedException(@NonNull Context context) {
        super(context);
    }

    public PeerDisappearedException(@NonNull Context context, Throwable cause) {
        super(context, cause);
    }

    public PeerDisappearedException(@Nullable String message) {
        super(message);
    }

    public PeerDisappearedException(@Nullable String message, @Nullable String localizedMessage) {
        super(message, localizedMessage);
    }

    public PeerDisappearedException(@Nullable String message, Throwable cause) {
        super(message, cause);
    }

    public PeerDisappearedException(@Nullable String message, @Nullable String localizedMessage, Throwable cause) {
        super(message, localizedMessage, cause);
    }

    public PeerDisappearedException(@Nullable String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public PeerDisappearedException(@Nullable String message, @Nullable String localizedMessage, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, localizedMessage, cause, enableSuppression, writableStackTrace);
    }

    @NonNull
    @Override
    protected String getDefaultMessage() {
        return "Peer disappeared";
    }

    @Override
    protected int getLocalizedMessageStrRes() {
        return R.string.error_peer_disappeared;
    }
}
