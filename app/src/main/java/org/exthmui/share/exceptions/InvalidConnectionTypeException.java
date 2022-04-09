package org.exthmui.share.exceptions;

public class InvalidConnectionTypeException extends FailedStartSendingException {

  public InvalidConnectionTypeException() {
  }

  public InvalidConnectionTypeException(String message) {
    super(message);
  }

  public InvalidConnectionTypeException(String message, Throwable cause) {
    super(message, cause);
  }

  public InvalidConnectionTypeException(Throwable cause) {
    super(cause);
  }

  public InvalidConnectionTypeException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
