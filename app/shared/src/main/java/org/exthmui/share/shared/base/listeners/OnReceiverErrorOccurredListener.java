package org.exthmui.share.shared.base.listeners;

import org.exthmui.share.shared.base.events.ReceiverErrorOccurredEvent;

import java.lang.reflect.Method;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

public interface OnReceiverErrorOccurredListener extends BaseEventListener {
    HashMap<Class<? extends EventObject>, Method[]> EVENT_TYPES_ALLOWED = new HashMap<>() {{
        try {
            put(ReceiverErrorOccurredEvent.class, new Method[]{OnReceiverErrorOccurredListener.class.getDeclaredMethod("onReceiverErrorOccurred", ReceiverErrorOccurredEvent.class),});
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }};


    @Override
    default Map<Class<? extends EventObject>, Method[]> _getEventTMethodMap() {
        return EVENT_TYPES_ALLOWED;
    }

    void onReceiverErrorOccurred(ReceiverErrorOccurredEvent event);
}
