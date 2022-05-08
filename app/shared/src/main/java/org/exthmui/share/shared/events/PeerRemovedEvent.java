package org.exthmui.share.shared.events;

import org.exthmui.share.shared.base.IPeer;

import java.util.EventObject;

public class PeerRemovedEvent extends EventObject {

    private final IPeer peer;

    public PeerRemovedEvent(Object source, IPeer peer) {
        super(source);
        this.peer = peer;
    }

    public IPeer getPeer() {
        return peer;
    }
}
