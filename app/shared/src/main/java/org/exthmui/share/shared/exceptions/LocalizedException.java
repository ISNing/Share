package org.exthmui.share.shared.exceptions;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.google.gson.annotations.Expose;

public abstract class LocalizedException extends Exception {
    @Nullable
    private String detailMessage;
    @Nullable
    private String localizedMessage;
    @Expose(serialize = false, deserialize = false)
    private String defaultLocalizedMessage;
    @Expose(serialize = false, deserialize = false)
    private boolean initialized;

    // Default localized message
    public LocalizedException(@NonNull Context context) {
        super((String) null);
        initialize(context);
    }

    // Default localized message with cause set
    public LocalizedException(@NonNull Context context, Throwable cause) {
        super(cause);
        initialize(context);
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

    public LocalizedException(@Nullable String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        this(message, null, cause, enableSuppression, writableStackTrace);
    }

    public LocalizedException(@Nullable String message, @Nullable String localizedMessage, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.detailMessage = message;
        this.localizedMessage = localizedMessage;
    }

    public void initialize(Context context) {
        if (!initialized && defaultLocalizedMessage == null) {
            defaultLocalizedMessage = context.getString(getLocalizedMessageStrRes());
        }
        initialized = true;
    }

    @NonNull
    @Override
    public String getMessage() {
        return this.detailMessage != null ? this.detailMessage : this.getDefaultMessage();
    }

    @Nullable
    @Override
    public String getLocalizedMessage() {
        if (localizedMessage != null) return localizedMessage;
        if (detailMessage != null) return detailMessage;
        if (defaultLocalizedMessage != null) return defaultLocalizedMessage;
        return getDefaultMessage();
    }

    @NonNull
    protected abstract String getDefaultMessage();

    @StringRes
    protected abstract int getLocalizedMessageStrRes();
}