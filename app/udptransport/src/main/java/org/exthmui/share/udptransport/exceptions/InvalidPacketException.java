package org.exthmui.share.udptransport.exceptions;

public class InvalidPacketException extends IllegalArgumentException {
    public InvalidPacketException() {
    }

    public InvalidPacketException(String s) {
        super(s);
    }

    public InvalidPacketException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidPacketException(Throwable cause) {
        super(cause);
    }
}
