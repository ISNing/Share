package org.exthmui.share.exceptions;

public class InvalidPeerException extends FailedStartSendingException {

  public InvalidPeerException(String message) {
    super(message);
  }

  public InvalidPeerException(String message, Throwable cause) {
    super(message, cause);
  }

  public InvalidPeerException(Throwable cause) {
    super(cause);
  }

  public InvalidPeerException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
