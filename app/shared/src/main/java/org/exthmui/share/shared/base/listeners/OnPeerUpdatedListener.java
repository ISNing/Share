package org.exthmui.share.shared.base.listeners;

import org.exthmui.share.shared.base.events.PeerUpdatedEvent;
import org.exthmui.share.shared.base.events.ProgressUpdatedEvent;

import java.lang.reflect.Method;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

public interface OnPeerUpdatedListener extends BaseEventListener{
    HashMap<Class<? extends EventObject>, Method[]> EVENT_TYPES_ALLOWED = new HashMap<>() {{
        try {
            put(PeerUpdatedEvent.class, new Method[]{OnPeerUpdatedListener.class.getDeclaredMethod("onPeerUpdated", PeerUpdatedEvent.class),});
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }};


    @Override
    default Map<Class<? extends EventObject>, Method[]> _getEventTMethodMap(){
        return EVENT_TYPES_ALLOWED;
    }

    void onPeerUpdated(PeerUpdatedEvent event);
}
