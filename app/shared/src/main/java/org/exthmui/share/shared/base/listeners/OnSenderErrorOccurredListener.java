package org.exthmui.share.shared.base.listeners;

import org.exthmui.share.shared.base.events.SenderErrorOccurredEvent;

import java.lang.reflect.Method;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

public interface OnSenderErrorOccurredListener extends BaseEventListener {
    HashMap<Class<? extends EventObject>, Method[]> EVENT_TYPES_ALLOWED = new HashMap<>() {{
        try {
            put(SenderErrorOccurredEvent.class, new Method[]{OnSenderErrorOccurredListener.class.getDeclaredMethod("onSenderErrorOccurred", SenderErrorOccurredEvent.class),});
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }};


    @Override
    default Map<Class<? extends EventObject>, Method[]> _getEventTMethodMap() {
        return EVENT_TYPES_ALLOWED;
    }

    void onSenderErrorOccurred(SenderErrorOccurredEvent event);
}
