package org.exthmui.share.shared.base.listeners;

import org.exthmui.share.shared.base.events.ProgressUpdatedEvent;

import java.lang.reflect.Method;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

public interface OnProgressUpdatedListener extends BaseEventListener {
    HashMap<Class<? extends EventObject>, Method[]> EVENT_TYPES_ALLOWED = new HashMap<>() {{
        try {
            put(ProgressUpdatedEvent.class, new Method[]{OnProgressUpdatedListener.class.getDeclaredMethod("onProgressUpdated", ProgressUpdatedEvent.class),});
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }};

    @Override
    default Map<Class<? extends EventObject>, Method[]> _getEventTMethodMap(){
        return EVENT_TYPES_ALLOWED;
    }

    void onProgressUpdated(ProgressUpdatedEvent event);

}
