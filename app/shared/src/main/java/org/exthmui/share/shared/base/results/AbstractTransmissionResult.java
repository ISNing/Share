package org.exthmui.share.shared.base.results;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exthmui.share.shared.misc.Constants;

public abstract class AbstractTransmissionResult implements TransmissionResult {

    @Nullable
    private String detailMessage;
    @Nullable
    private final String localizedMessage;

    // Default localized message
    public AbstractTransmissionResult(@NonNull Context context) {
        localizedMessage = context.getString(getLocalizedMessageStrRes());
    }

    public AbstractTransmissionResult(@Nullable String message) {
        this(message, (String) null);
    }

    public AbstractTransmissionResult(@Nullable String message, @Nullable String localizedMessage) {
        this.detailMessage = message;
        this.localizedMessage = localizedMessage;
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
        return this.localizedMessage != null ? this.localizedMessage : this.getMessage();
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
