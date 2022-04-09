package org.exthmui.share.exceptions;

public class FailedStartSendingException extends Exception {

    public FailedStartSendingException() {
    }

    public FailedStartSendingException(String message) {
        super(message);
    }

    public FailedStartSendingException(String message, Throwable cause) {
        super(message, cause);
    }

    public FailedStartSendingException(Throwable cause) {
        super(cause);
    }

    public FailedStartSendingException(String message, Throwable cause, boolean enableSuppression,
        boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
