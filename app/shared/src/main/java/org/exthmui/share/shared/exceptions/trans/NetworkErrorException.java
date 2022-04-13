package org.exthmui.share.shared.exceptions.trans;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exthmui.share.shared.misc.Constants;

public class NetworkErrorException extends TransmissionException {

    public NetworkErrorException(@NonNull Context context) {
        super(context);
    }

    public NetworkErrorException(@NonNull Context context, Throwable cause) {
        super(context, cause);
    }

    public NetworkErrorException(@NonNull String message) {
        super(message);
    }

    public NetworkErrorException(@NonNull String message, @Nullable String localizedMessage) {
        super(message, localizedMessage);
    }

    public NetworkErrorException(@NonNull String message, Throwable cause) {
        super(message, cause);
    }

    public NetworkErrorException(@NonNull String message, @Nullable String localizedMessage, Throwable cause) {
        super(message, localizedMessage, cause);
    }

    public NetworkErrorException(@NonNull String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public NetworkErrorException(@NonNull String message, @Nullable String localizedMessage, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, localizedMessage, cause, enableSuppression, writableStackTrace);
    }

    @NonNull
    @Override
    public Constants.TransmissionStatus getStatus() {
        return Constants.TransmissionStatus.NETWORK_ERROR;
    }

    @NonNull
    @Override
    protected String getDefaultMessage() {
        return "Network error occurred";
    }
}
