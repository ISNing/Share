package org.exthmui.share.base.events;

import java.util.EventObject;

public class AcceptedOrRejectedEvent extends EventObject {

    private final boolean accepted;

    public AcceptedOrRejectedEvent(Object source, boolean accepted) {
        super(source);
        this.accepted = accepted;
    }

    public boolean isAccepted() {
        return accepted;
    }
}
