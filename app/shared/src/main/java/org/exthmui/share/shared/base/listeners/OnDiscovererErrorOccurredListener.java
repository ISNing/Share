package org.exthmui.share.shared.base.listeners;

import org.exthmui.share.shared.base.events.DiscovererErrorOccurredEvent;

import java.lang.reflect.Method;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

public interface OnDiscovererErrorOccurredListener extends BaseEventListener {
    HashMap<Class<? extends EventObject>, Method[]> EVENT_TYPES_ALLOWED = new HashMap<>() {{
        try {
            put(DiscovererErrorOccurredEvent.class, new Method[]{OnDiscovererErrorOccurredListener.class.getDeclaredMethod("onDiscovererErrorOccurred", DiscovererErrorOccurredEvent.class),});
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }};


    @Override
    default Map<Class<? extends EventObject>, Method[]> _getEventTMethodMap() {
        return EVENT_TYPES_ALLOWED;
    }

    void onDiscovererErrorOccurred(DiscovererErrorOccurredEvent event);
}
