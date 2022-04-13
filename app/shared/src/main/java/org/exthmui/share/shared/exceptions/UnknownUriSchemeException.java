package org.exthmui.share.shared.exceptions;

public class UnknownUriSchemeException extends FailedResolvingUriException{
    public UnknownUriSchemeException() {
        super();
    }
    public UnknownUriSchemeException(String s) {
        super(s);
    }
}
