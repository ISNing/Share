//package org.exthmui.share.web.exceptions;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//
//import com.yanzhenjie.andserver.error.HttpException;
//
//public abstract class AbstractApiException extends HttpException {
//    private int statusCode;
//    @Nullable
//    private String description;
//
//    public AbstractApiException() {
//        super(0, (String) null);
//    }
//
//    public AbstractApiException(@Nullable String description) {
//        super(0, description);
//        this.description = description;
//    }
//
//    public AbstractApiException(int statusCode) {
//        super(statusCode, (String) null);
//        this.statusCode = statusCode;
//    }
//
//    public AbstractApiException(@Nullable String description, int statusCode) {
//        super(statusCode, description);
//        this.statusCode = statusCode;
//        this.description = description;
//    }
//
//    public AbstractApiException(int statusCode, Throwable cause) {
//        super(statusCode, cause);
//        this.statusCode = statusCode;
//    }
//
//    public AbstractApiException(@Nullable String description, int statusCode, Throwable cause) {
//        super(statusCode, description, cause);
//        this.statusCode = statusCode;
//        this.description = description;
//    }
//
//    public AbstractApiException setStatusCode(int statusCode) {
//        this.statusCode = statusCode;
//        return this;
//    }
//
//    public AbstractApiException setDescription(@Nullable String description) {
//        this.description = description;
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
//    public int getStatusCode() {
//        return statusCode != 0 ? statusCode : getDefaultStatusCode();
//    }
//
//    @NonNull
//    public abstract String getCode();
//
//    @Nullable
//    public String getDescription() {
//        return this.description != null ? this.description : this.getDefaultDescription();
//    }
//
//    protected abstract int getDefaultStatusCode();
//
//    @NonNull
//    protected abstract String getDefaultDescription();
//}
