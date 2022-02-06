package org.exthmui.share.shared.base.exceptions;

public class FileNotExistsException extends FailedResolvingUriException {
    public FileNotExistsException() {
        super();
    }
    public FileNotExistsException(String s) {
        super(s);
    }
}
