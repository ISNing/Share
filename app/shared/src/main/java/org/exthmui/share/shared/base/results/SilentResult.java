package org.exthmui.share.shared.base.results;

import androidx.annotation.NonNull;

import org.exthmui.share.shared.misc.Constants;

public class SilentResult extends AbstractTransmissionResult {
    public SilentResult() {
        super("Silent");
    }

    @NonNull
    @Override
    public Constants.TransmissionStatus getStatus() {
        return Constants.TransmissionStatus.SILENT;
    }

    @NonNull
    @Override
    protected String getDefaultMessage() {
        return "Silent";
    }
}
