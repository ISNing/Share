package org.exthmui.share.exceptions;

public class FailedInvokingSendingMethod extends FailedStartSendingException {

  public FailedInvokingSendingMethod() {
  }

  public FailedInvokingSendingMethod(String message) {
    super(message);
  }

  public FailedInvokingSendingMethod(String message, Throwable cause) {
    super(message, cause);
  }

  public FailedInvokingSendingMethod(Throwable cause) {
    super(cause);
  }

  public FailedInvokingSendingMethod(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
