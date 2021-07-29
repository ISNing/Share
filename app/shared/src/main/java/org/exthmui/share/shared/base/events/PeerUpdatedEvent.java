package org.exthmui.share.shared.base.events;

import org.exthmui.share.shared.base.PeerInfo;

import java.util.EventObject;

public class PeerUpdatedEvent extends EventObject {

    private final PeerInfo peer;

    public PeerUpdatedEvent(Object source, PeerInfo peer) {
        super(source);
        this.peer = peer;
    }

    public PeerInfo getPeer() {
        return peer;
    }
}
