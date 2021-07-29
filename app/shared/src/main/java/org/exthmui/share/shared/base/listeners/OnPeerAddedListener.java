package org.exthmui.share.shared.base.listeners;

import org.exthmui.share.shared.base.events.PeerAddedEvent;

import java.lang.reflect.Method;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

public interface OnPeerAddedListener extends BaseEventListener{
    HashMap<Class<? extends EventObject>, Method[]> EVENT_TYPES_ALLOWED = new HashMap<>() {{
        try {
            put(PeerAddedEvent.class, new Method[]{OnPeerAddedListener.class.getDeclaredMethod("onPeerAdded", PeerAddedEvent.class),});
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }};


    @Override
    default Map<Class<? extends EventObject>, Method[]> _getEventTMethodMap(){
        return EVENT_TYPES_ALLOWED;
    }

    void onPeerAdded(PeerAddedEvent event);
}
