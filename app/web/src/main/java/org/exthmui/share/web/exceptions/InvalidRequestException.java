package org.exthmui.share.web.exceptions;

import androidx.annotation.NonNull;

import com.yanzhenjie.andserver.http.StatusCode;

public class InvalidRequestException extends AbstractApiException {

    @NonNull
    @Override
    public String getCode() {
        return "invalid_request";
    }

    @Override
    protected int getDefaultStatusCode() {
        return StatusCode.SC_BAD_REQUEST;
    }

    @NonNull
    @Override
    protected String getDefaultDescription() {
        return "Invalid request";
    }
}
