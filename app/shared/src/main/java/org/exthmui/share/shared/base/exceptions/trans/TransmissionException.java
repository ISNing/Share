package org.exthmui.share.shared.base.exceptions.trans;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exthmui.share.shared.Constants;
import org.exthmui.share.shared.exceptions.LocalizedException;

public abstract class TransmissionException extends LocalizedException {


    public TransmissionException(@NonNull Context context) {
        super(context);
    }

    public TransmissionException(@NonNull Context context, Throwable cause) {
        super(context, cause);
    }

    public TransmissionException(@NonNull String message) {
        super(message);
    }

    public TransmissionException(@NonNull String message, @Nullable String localizedMessage) {
        super(message, localizedMessage);
    }

    public TransmissionException(@NonNull String message, Throwable cause) {
        super(message, cause);
    }

    public TransmissionException(@NonNull String message, @Nullable String localizedMessage, Throwable cause) {
        super(message, localizedMessage, cause);
    }

    public TransmissionException(@NonNull String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public TransmissionException(@NonNull String message, @Nullable String localizedMessage, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, localizedMessage, cause, enableSuppression, writableStackTrace);
    }

    @NonNull
    public abstract Constants.TransmissionStatus getStatus();

    @Override
    protected int getLocalizedMessageStrRes() {
        return getStatus().getStrResDetailed();
    }

    public int getStatusCode() {
        return getStatus().getNumVal();
    }
}
