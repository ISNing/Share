package org.exthmui.share.exceptions;

public class FailedInvokingSendingMethodException extends FailedStartSendingException {

  public FailedInvokingSendingMethodException() {
  }

  public FailedInvokingSendingMethodException(String message) {
    super(message);
  }

  public FailedInvokingSendingMethodException(String message, Throwable cause) {
    super(message, cause);
  }

  public FailedInvokingSendingMethodException(Throwable cause) {
    super(cause);
  }

  public FailedInvokingSendingMethodException(String message, Throwable cause,
      boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
