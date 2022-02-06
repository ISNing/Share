package org.exthmui.share.shared.base.events;

import org.exthmui.share.shared.base.PeerInfo;

import java.util.EventObject;

public class PeerAddedEvent extends EventObject {

    private final PeerInfo peer;

    public PeerAddedEvent(Object source, PeerInfo peer) {
        super(source);
        this.peer = peer;
    }

    public PeerInfo getPeer() {
        return peer;
    }
}
