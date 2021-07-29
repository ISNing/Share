package org.exthmui.share.web.exceptions;

import com.yanzhenjie.andserver.error.HttpException;

public class InvalidRequestException extends HttpException {

    public InvalidRequestException() {
        super(400, "Invalid request");
    }
    public InvalidRequestException(int statusCode, String message) {
        super(statusCode, message);
    }

    public InvalidRequestException(int statusCode, String message, Throwable cause) {
        super(statusCode, message, cause);
    }

    public InvalidRequestException(int statusCode, Throwable cause) {
        super(statusCode, cause);
    }
}
