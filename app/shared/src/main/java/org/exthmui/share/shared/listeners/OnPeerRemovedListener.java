package org.exthmui.share.shared.listeners;

import androidx.annotation.NonNull;

import org.exthmui.share.shared.events.PeerRemovedEvent;
import org.exthmui.utils.listeners.BaseEventListener;

import java.lang.reflect.Method;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

public interface OnPeerRemovedListener extends BaseEventListener {
    HashMap<Class<? extends EventObject>, Method[]> EVENT_TYPES_ALLOWED = new HashMap<>() {{
        try {
            put(PeerRemovedEvent.class, new Method[]{OnPeerRemovedListener.class.getDeclaredMethod("onPeerRemoved", PeerRemovedEvent.class),});
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }};


    @NonNull
    @Override
    default Map<Class<? extends EventObject>, Method[]> getEventToMethodMap() {
        return EVENT_TYPES_ALLOWED;
    }

    void onPeerRemoved(PeerRemovedEvent event);
}
