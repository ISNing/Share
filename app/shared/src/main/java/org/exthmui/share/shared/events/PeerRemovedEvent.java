package org.exthmui.share.shared.events;

import org.exthmui.share.shared.base.PeerInfo;

import java.util.EventObject;

public class PeerRemovedEvent extends EventObject {

    private final PeerInfo peer;

    public PeerRemovedEvent(Object source, PeerInfo peer) {
        super(source);
        this.peer = peer;
    }

    public PeerInfo getPeer() {
        return peer;
    }
}
