package org.exthmui.share.shared.exceptions.trans;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exthmui.share.shared.base.results.TransmissionResult;
import org.exthmui.share.shared.exceptions.LocalizedException;
import org.exthmui.share.shared.misc.Constants;

public abstract class TransmissionException extends LocalizedException implements TransmissionResult {


    public TransmissionException(@NonNull Context context) {
        super(context);
    }

    public TransmissionException(@NonNull Context context, Throwable cause) {
        super(context, cause);
    }

    public TransmissionException(@Nullable String message) {
        super(message);
    }

    public TransmissionException(@Nullable String message, @Nullable String localizedMessage) {
        super(message, localizedMessage);
    }

    public TransmissionException(@Nullable String message, Throwable cause) {
        super(message, cause);
    }

    public TransmissionException(@Nullable String message, @Nullable String localizedMessage, Throwable cause) {
        super(message, localizedMessage, cause);
    }

    public TransmissionException(@Nullable String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public TransmissionException(@Nullable String message, @Nullable String localizedMessage, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, localizedMessage, cause, enableSuppression, writableStackTrace);
    }

    @NonNull
    @Override
    public abstract Constants.TransmissionStatus getStatus();

    @Override
    protected int getLocalizedMessageStrRes() {
        return getStatus().getStrResDetailed();
    }

    @Override
    public int getStatusCode() {
        return getStatus().getNumVal();
    }
}
