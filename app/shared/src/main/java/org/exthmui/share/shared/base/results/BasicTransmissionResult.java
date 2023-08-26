package org.exthmui.share.shared.base.results;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;

import org.exthmui.share.shared.misc.Constants;

public final class BasicTransmissionResult extends AbstractTransmissionResult {
    public static final BasicTransmissionResult UNKNOWN_RESULT =
            new BasicTransmissionResult(Constants.TransmissionStatus.UNKNOWN, null, null);

    private static final Gson GSON = new Gson();

    private final Constants.TransmissionStatus status;

    public BasicTransmissionResult(@NonNull TransmissionResult result) {
        this(result.getStatus(), result.getMessage(), result.getLocalizedMessage());
    }

    public BasicTransmissionResult(@NonNull Constants.TransmissionStatus status,
                                   @Nullable String message) {
        super(message);
        this.status = status;
    }

    public BasicTransmissionResult(@NonNull Constants.TransmissionStatus status,
                                   @Nullable String message, @Nullable String localizedMessage) {
        super(message, localizedMessage);
        this.status = status;
    }

    @NonNull
    @Override
    public Constants.TransmissionStatus getStatus() {
        return status;
    }

    @NonNull
    @Override
    protected String getDefaultMessage() {
        return "Unknown Status";
    }
}
