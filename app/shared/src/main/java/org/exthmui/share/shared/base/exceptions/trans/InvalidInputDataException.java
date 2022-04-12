package org.exthmui.share.shared.base.exceptions.trans;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class InvalidInputDataException extends UnknownErrorException {
    public InvalidInputDataException(@NonNull Context context) {
        super(context);
    }

    public InvalidInputDataException(@NonNull Context context, Throwable cause) {
        super(context, cause);
    }

    public InvalidInputDataException(@NonNull String message) {
        super(message);
    }

    public InvalidInputDataException(@NonNull String message, @Nullable String localizedMessage) {
        super(message, localizedMessage);
    }

    public InvalidInputDataException(@NonNull String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidInputDataException(@NonNull String message, @Nullable String localizedMessage, Throwable cause) {
        super(message, localizedMessage, cause);
    }

    public InvalidInputDataException(@NonNull String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public InvalidInputDataException(@NonNull String message, @Nullable String localizedMessage, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, localizedMessage, cause, enableSuppression, writableStackTrace);
    }
}
