//package org.exthmui.share.web.exceptions;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//
//import com.yanzhenjie.andserver.http.StatusCode;
//
//public final class ApiException extends AbstractApiException {
//    private String code;
//
//    public ApiException() {
//    }
//
//    public ApiException(String code) {
//        this.code = code;
//    }
//
//    public ApiException(int statusCode) {
//        super(statusCode);
//    }
//
//    public ApiException(@Nullable String description, int statusCode) {
//        super(description, statusCode);
//    }
//
//    public ApiException(int statusCode, Throwable cause) {
//        super(statusCode, cause);
//    }
//
//    public ApiException(@Nullable String description, int statusCode, Throwable cause) {
//        super(description, statusCode, cause);
//    }
//
//    public ApiException(@Nullable String description, String code) {
//        super(description);
//        this.code = code;
//    }
//
//    public ApiException(int statusCode, String code) {
//        super(statusCode);
//        this.code = code;
//    }
//
//    public ApiException(@Nullable String description, int statusCode, String code) {
//        super(description, statusCode);
//        this.code = code;
//    }
//
//    public ApiException(int statusCode, Throwable cause, String code) {
//        super(statusCode, cause);
//        this.code = code;
//    }
//
//    public ApiException(@Nullable String description, int statusCode, Throwable cause, String code) {
//        super(description, statusCode, cause);
//        this.code = code;
//    }
//
//    public ApiException setCode(String code) {
//        this.code = code;
//        return this;
//    }
//
//    @Nullable
//    @Override
//    public String getMessage() {
//        return this.getDescription();
//    }
//
//    @Override
//    @NonNull
//    public String getCode() {
//        return code != null ? code : "internal_server_error";
//    }
//
//    @Override
//    protected int getDefaultStatusCode() {
//        return StatusCode.SC_INTERNAL_SERVER_ERROR;
//    }
//
//    @Override
//    @NonNull
//    protected String getDefaultDescription() {
//        return "Unknown internal server error";
//    }
//}
