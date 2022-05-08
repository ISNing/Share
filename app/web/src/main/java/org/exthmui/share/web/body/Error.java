package org.exthmui.share.web.body;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.yanzhenjie.andserver.framework.body.JsonBody;
import com.yanzhenjie.andserver.http.HttpRequest;

import org.exthmui.share.shared.misc.StackTraceUtils;
import org.exthmui.share.web.exceptions.AbstractApiException;

import java.util.Locale;

public class Error {
    private int status;
    private final ErrorDetails error;

    public Error(AbstractApiException e, @Nullable HttpRequest request) {
        this.status = e.getStatusCode();
        this.error = new ErrorDetails(e, request);
    }

    public static class ErrorDetails {
        private static final String REQUEST_INFO_TEMPLATE = "[%1$s] >> %2$s";

        private String code;
        private String name;
        private String description;
        private String request;
        @SerializedName("stack_trace")
        private String stackTrace;

        private ErrorDetails(AbstractApiException e, @Nullable HttpRequest request) {
            this.code = e.getCode();
            this.name = e.getClass().getSimpleName();
            this.description = e.getDescription();
            this.stackTrace = StackTraceUtils.getStackTraceString(e.getStackTrace());
            if (request != null)
                setRequest(request);
        }

        public String getCode() {
            return code;
        }

        public ErrorDetails setCode(String code) {
            this.code = code;
            return this;
        }

        public String getName() {
            return name;
        }

        public ErrorDetails setName(String name) {
            this.name = name;
            return this;
        }

        public String getDescription() {
            return description;
        }

        public ErrorDetails setDescription(String description) {
            this.description = description;
            return this;
        }

        public String getRequest() {
            return request;
        }

        public ErrorDetails setRequest(@NonNull HttpRequest request) {
            this.request = String.format(REQUEST_INFO_TEMPLATE,
                    request.getMethod().toString().toUpperCase(Locale.ROOT),
                    request.getURI());
            return this;
        }

        public ErrorDetails setRequest(String request) {
            this.request = request;
            return this;
        }

        public String getStackTrace() {
            return stackTrace;
        }

        public ErrorDetails setStackTrace(String stackTrace) {
            this.stackTrace = stackTrace;
            return this;
        }
    }

    public int getStatus() {
        return status;
    }

    public Error setStatus(int status) {
        this.status = status;
        return this;
    }

    public ErrorDetails getError() {
        return error;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

    public JsonBody toBody() {
        return new JsonBody(this.toJson());
    }
}
