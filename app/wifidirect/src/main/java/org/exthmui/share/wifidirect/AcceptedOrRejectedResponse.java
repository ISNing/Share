package org.exthmui.share.wifidirect;

import java.io.Serializable;

public class AcceptedOrRejectedResponse implements Serializable {
    private final boolean accepted;

    public AcceptedOrRejectedResponse(boolean accepted) {
        this.accepted = accepted;
    }

    public boolean isAccepted() {
        return accepted;
    }
}
