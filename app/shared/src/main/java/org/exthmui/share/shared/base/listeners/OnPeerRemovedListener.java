package org.exthmui.share.shared.base.listeners;

import org.exthmui.share.shared.base.events.PeerRemovedEvent;
import org.exthmui.share.shared.base.events.ProgressUpdatedEvent;

import java.lang.reflect.Method;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

public interface OnPeerRemovedListener extends BaseEventListener{
    HashMap<Class<? extends EventObject>, Method[]> EVENT_TYPES_ALLOWED = new HashMap<>() {{
        try {
            put(PeerRemovedEvent.class, new Method[]{OnPeerRemovedListener.class.getDeclaredMethod("onPeerRemoved", PeerRemovedEvent.class),});
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }};


    @Override
    default Map<Class<? extends EventObject>, Method[]> _getEventTMethodMap(){
        return EVENT_TYPES_ALLOWED;
    }

    void onPeerRemoved(PeerRemovedEvent event);
}
