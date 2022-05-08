package org.exthmui.share.shared.listeners;

import androidx.annotation.NonNull;

import org.exthmui.share.shared.events.PeerAddedEvent;

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


    @NonNull
    @Override
    default Map<Class<? extends EventObject>, Method[]> _getEventToMethodMap(){
        return EVENT_TYPES_ALLOWED;
    }

    void onPeerAdded(PeerAddedEvent event);
}
