package org.exthmui.share.shared.exceptions;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

public abstract class LocalizedException extends Exception {
    @Nullable
    private String detailMessage;
    @Nullable
    private final String localizedMessage;

    // Default localized message
    public LocalizedException(@NonNull Context context) {
        super((String) null);
        localizedMessage = context.getString(getLocalizedMessageStrRes());
    }

    // Default localized message with cause set
    public LocalizedException(@NonNull Context context, Throwable cause) {
        super(cause);
        localizedMessage = context.getString(getLocalizedMessageStrRes());
    }

    public LocalizedException(@NonNull String message) {
        this(message, (String) null);
    }

    public LocalizedException(@NonNull String message, @Nullable String localizedMessage) {
        super(message);
        this.detailMessage = message;
        this.localizedMessage = localizedMessage;
    }

    public LocalizedException(@NonNull String message, Throwable cause) {
        this(message, null, cause);
    }

    public LocalizedException(@NonNull String message, @Nullable String localizedMessage, Throwable cause) {
        super(message, cause);
        this.detailMessage = message;
        this.localizedMessage = localizedMessage;
    }

    public LocalizedException(@NonNull String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        this(message, null, cause, enableSuppression, writableStackTrace);
    }

    public LocalizedException(@NonNull String message, @Nullable String localizedMessage, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.detailMessage = message;
        this.localizedMessage = localizedMessage;
    }

    @Nullable
    @Override
    public String getMessage() {
        return this.detailMessage != null ? this.detailMessage : this.getDefaultMessage();
    }

    @Nullable
    @Override
    public String getLocalizedMessage() {
        return this.localizedMessage != null ? this.localizedMessage : this.getMessage();
    }

    @NonNull
    protected abstract String getDefaultMessage();

    @StringRes
    protected abstract int getLocalizedMessageStrRes();
}