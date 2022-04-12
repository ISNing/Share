package org.exthmui.share.shared.base.exceptions.trans;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exthmui.share.shared.Constants;

public class FileIOErrorException extends TransmissionException {
    public FileIOErrorException(@NonNull Context context) {
        super(context);
    }

    public FileIOErrorException(@NonNull Context context, Throwable cause) {
        super(context, cause);
    }

    public FileIOErrorException(@NonNull String message) {
        super(message);
    }

    public FileIOErrorException(@NonNull String message, @Nullable String localizedMessage) {
        super(message, localizedMessage);
    }

    public FileIOErrorException(@NonNull String message, Throwable cause) {
        super(message, cause);
    }

    public FileIOErrorException(@NonNull String message, @Nullable String localizedMessage, Throwable cause) {
        super(message, localizedMessage, cause);
    }

    public FileIOErrorException(@NonNull String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public FileIOErrorException(@NonNull String message, @Nullable String localizedMessage, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, localizedMessage, cause, enableSuppression, writableStackTrace);
    }

    @NonNull
    @Override
    public Constants.TransmissionStatus getStatus() {
        return Constants.TransmissionStatus.FILE_IO_ERROR;
    }

    @NonNull
    @Override
    protected String getDefaultMessage() {
        return "File IO error occurred";
    }
}
