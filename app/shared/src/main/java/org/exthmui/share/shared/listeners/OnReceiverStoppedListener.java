package org.exthmui.share.shared.listeners;

import org.exthmui.share.shared.events.ReceiverStoppedEvent;

import java.lang.reflect.Method;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

public interface OnReceiverStoppedListener extends BaseEventListener {
    HashMap<Class<? extends EventObject>, Method[]> EVENT_TYPES_ALLOWED = new HashMap<>() {{
        try {
            put(ReceiverStoppedEvent.class, new Method[]{OnReceiverStoppedListener.class.getDeclaredMethod("onReceiverStopped", ReceiverStoppedEvent.class),});
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }};


    @Override
    default Map<Class<? extends EventObject>, Method[]> _getEventTMethodMap() {
        return EVENT_TYPES_ALLOWED;
    }

    void onReceiverStopped(ReceiverStoppedEvent event);
}
