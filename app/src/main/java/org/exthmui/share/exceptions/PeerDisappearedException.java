package org.exthmui.share.exceptions;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exthmui.share.R;

public class PeerDisappearedException extends InvalidPeerException {

    public PeerDisappearedException(@NonNull Context context) {
        super(context);
    }

    public PeerDisappearedException(@NonNull Context context, Throwable cause) {
        super(context, cause);
    }

    public PeerDisappearedException(@NonNull String message) {
        super(message);
    }

    public PeerDisappearedException(@NonNull String message, @Nullable String localizedMessage) {
        super(message, localizedMessage);
    }

    public PeerDisappearedException(@NonNull String message, Throwable cause) {
        super(message, cause);
    }

    public PeerDisappearedException(@NonNull String message, @Nullable String localizedMessage, Throwable cause) {
        super(message, localizedMessage, cause);
    }

    public PeerDisappearedException(@NonNull String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public PeerDisappearedException(@NonNull String message, @Nullable String localizedMessage, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, localizedMessage, cause, enableSuppression, writableStackTrace);
    }

    @Override
    String getDefaultMessage() {
        return "Peer disappeared";
    }

    @Override
    int getLocalizedMessageStrRes() {
        return R.string.error_peer_disappeared;
    }
}
