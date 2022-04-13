package org.exthmui.share.shared.exceptions;

public class EmptyPathException extends FailedResolvingUriException {
    public EmptyPathException() {
        super();
    }

    public EmptyPathException(String s) {
        super(s);
    }
}
