package org.exthmui.share.shared.base.exceptions;

public class UnknownUriSchemeException extends FailedResolvingUriException{
    public UnknownUriSchemeException() {
        super();
    }
    public UnknownUriSchemeException(String s) {
        super(s);
    }
}
