package org.exthmui.share.shared.base.results;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exthmui.share.shared.misc.Constants;

public interface TransmissionResult {
    @NonNull
    Constants.TransmissionStatus getStatus();

    default int getStatusCode() {
        return getStatus().getNumVal();
    }

    @NonNull
    String getMessage();

    @Nullable
    String getLocalizedMessage();
}
