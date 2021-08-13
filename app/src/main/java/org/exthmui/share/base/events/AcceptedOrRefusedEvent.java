package org.exthmui.share.base.events;

import java.util.EventObject;

public class AcceptedOrRefusedEvent extends EventObject {

    private final boolean accepted;

    public AcceptedOrRefusedEvent(Object source, boolean accepted) {
        super(source);
        this.accepted = accepted;
    }

    public boolean isAccepted() {
        return accepted;
    }
}
