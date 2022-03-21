package org.exthmui.share.shared.base.listeners;

import org.exthmui.share.shared.base.events.DiscovererStoppedEvent;

import java.lang.reflect.Method;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

public interface OnDiscovererStoppedListener extends BaseEventListener {
    HashMap<Class<? extends EventObject>, Method[]> EVENT_TYPES_ALLOWED = new HashMap<>() {{
        try {
            put(DiscovererStoppedEvent.class, new Method[]{OnDiscovererStoppedListener.class.getDeclaredMethod("onDiscovererStopped", DiscovererStoppedEvent.class),});
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }};


    @Override
    default Map<Class<? extends EventObject>, Method[]> _getEventTMethodMap() {
        return EVENT_TYPES_ALLOWED;
    }

    void onDiscovererStopped(DiscovererStoppedEvent event);
}
