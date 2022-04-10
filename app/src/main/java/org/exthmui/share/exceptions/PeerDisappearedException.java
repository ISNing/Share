package org.exthmui.share.exceptions;

public class PeerDisappearedException extends InvalidPeerException {
    public PeerDisappearedException() {
    }

    public PeerDisappearedException(String message) {
        super(message);
    }

    public PeerDisappearedException(String message, Throwable cause) {
        super(message, cause);
    }

    public PeerDisappearedException(Throwable cause) {
        super(cause);
    }

    public PeerDisappearedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
