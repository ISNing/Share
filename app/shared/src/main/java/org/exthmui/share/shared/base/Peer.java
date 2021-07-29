package org.exthmui.share.shared.base;

import org.exthmui.share.shared.base.events.PeerUpdatedEvent;
import org.exthmui.share.shared.base.listeners.OnPeerUpdatedListener;

import java.util.HashSet;
import java.util.Set;

public abstract class Peer implements PeerInfo{

    private final Set<OnPeerUpdatedListener> mOnPeerUpdatedListeners = new HashSet<>();


    @Override
    public void registerOnPeerUpdatedListener(OnPeerUpdatedListener listener) {
        mOnPeerUpdatedListeners.add(listener);
    }

    @Override
    public void unregisterOnPeerUpdatedListener(OnPeerUpdatedListener listener) {
        mOnPeerUpdatedListeners.remove(listener);
    }

    @Override
    public void notifyPeerUpdated() {
        for (OnPeerUpdatedListener listener: mOnPeerUpdatedListeners) listener.onPeerUpdated(new PeerUpdatedEvent(this, this));
    }
}
