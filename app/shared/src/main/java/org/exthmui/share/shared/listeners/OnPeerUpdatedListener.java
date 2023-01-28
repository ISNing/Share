package org.exthmui.share.shared.listeners;

import androidx.annotation.NonNull;

import org.exthmui.share.shared.events.PeerUpdatedEvent;
import org.exthmui.utils.listeners.BaseEventListener;

import java.lang.reflect.Method;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

public interface OnPeerUpdatedListener extends BaseEventListener {
    HashMap<Class<? extends EventObject>, Method[]> EVENT_TYPES_ALLOWED = new HashMap<>() {{
        try {
            put(PeerUpdatedEvent.class, new Method[]{OnPeerUpdatedListener.class.getDeclaredMethod("onPeerUpdated", PeerUpdatedEvent.class),});
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }};


    @NonNull
    @Override
    default Map<Class<? extends EventObject>, Method[]> _getEventToMethodMap(){
        return EVENT_TYPES_ALLOWED;
    }

    void onPeerUpdated(PeerUpdatedEvent event);
}
