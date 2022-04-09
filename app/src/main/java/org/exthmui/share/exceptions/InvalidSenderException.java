package org.exthmui.share.exceptions;

public class InvalidSenderException extends FailedStartSendingException {

  public InvalidSenderException() {
  }

  public InvalidSenderException(String message) {
    super(message);
  }

  public InvalidSenderException(String message, Throwable cause) {
    super(message, cause);
  }

  public InvalidSenderException(Throwable cause) {
    super(cause);
  }

  public InvalidSenderException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
