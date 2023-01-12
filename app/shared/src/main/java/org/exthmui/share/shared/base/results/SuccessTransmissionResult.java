package org.exthmui.share.shared.base.results;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exthmui.share.shared.misc.Constants;

public class SuccessTransmissionResult extends AbstractTransmissionResult {
    public SuccessTransmissionResult(@NonNull Context context) {
        super(context);
    }

    public SuccessTransmissionResult(@Nullable String message) {
        super(message);
    }

    public SuccessTransmissionResult(@Nullable String message, @Nullable String localizedMessage) {
        super(message, localizedMessage);
    }

    @NonNull
    @Override
    public Constants.TransmissionStatus getStatus() {
        return Constants.TransmissionStatus.COMPLETED;
    }

    @NonNull
    @Override
    protected String getDefaultMessage() {
        return "Transmission completed";
    }
}
