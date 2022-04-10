package org.exthmui.share.exceptions;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

public abstract class LocalizedException extends Exception {
    @Nullable
    private String detailMessage;
    @Nullable
    private String localizedMessage;

    // Default localized message
    public LocalizedException(@NonNull Context context) {
        super((String) null);
        detailMessage = context.getString(getMessageStrRes());
        localizedMessage = context.getString(getLocalizedMessageStrRes());
    }

    // Default localized message with cause set
    public LocalizedException(Context context, Throwable cause) {
        super(cause);
        detailMessage = context.getString(getMessageStrRes());
        localizedMessage = context.getString(getLocalizedMessageStrRes());
    }

    public LocalizedException(@Nullable String message) {
        this(message, (String) null);
    }

    public LocalizedException(@Nullable String message, @Nullable String localizedMessage) {
        super(message);
        this.detailMessage = message;
        this.localizedMessage = localizedMessage;
    }

    public LocalizedException(@Nullable String message, Throwable cause) {
        this(message, null, cause);
    }

    public LocalizedException(@Nullable String message, @Nullable String localizedMessage, Throwable cause) {
        super(message, cause);
        this.detailMessage = message;
        this.localizedMessage = localizedMessage;
    }

    public LocalizedException(Throwable cause) {
        super(cause);
    }

    public LocalizedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        this(message, null, cause, enableSuppression, writableStackTrace);
    }

    public LocalizedException(@Nullable String message, @Nullable String localizedMessage, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.detailMessage = message;
        this.localizedMessage = localizedMessage;
    }

    @Nullable
    @Override
    public String getMessage() {
        return this.detailMessage;
    }

    @Nullable
    @Override
    public String getLocalizedMessage() {
        return this.localizedMessage != null ? this.localizedMessage : this.getMessage();
    }

    @StringRes
    public abstract int getMessageStrRes();

    @StringRes
    public abstract int getLocalizedMessageStrRes();
}