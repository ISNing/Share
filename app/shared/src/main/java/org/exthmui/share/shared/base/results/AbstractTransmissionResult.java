package org.exthmui.share.shared.base.results;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.Expose;

import org.exthmui.share.shared.misc.Constants;

public abstract class AbstractTransmissionResult implements TransmissionResult {

    @Nullable
    private String detailMessage;
    @Nullable
    private String localizedMessage;
    @Expose(serialize = false, deserialize = false)
    private String defaultLocalizedMessage;
    @Expose(serialize = false, deserialize = false)
    private boolean initialized;

    // Default localized message
    public AbstractTransmissionResult(@NonNull Context context) {
        initialize(context);
    }

    public AbstractTransmissionResult(@Nullable String message) {
        this(message, null);
    }

    public AbstractTransmissionResult(@Nullable String message, @Nullable String localizedMessage) {
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
    public abstract Constants.TransmissionStatus getStatus();


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

    protected int getLocalizedMessageStrRes() {
        return getStatus().getStrResDetailed();
    }

    @Override
    public int getStatusCode() {
        return getStatus().getNumVal();
    }
}
