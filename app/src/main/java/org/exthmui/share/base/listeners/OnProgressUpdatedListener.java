package org.exthmui.share.base.listeners;

import org.exthmui.share.base.events.AcceptedOrRejectedEvent;

import java.lang.reflect.Method;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

public interface OnProgressUpdatedListener extends BaseEventListener {
    HashMap<Class<? extends EventObject>, Method[]> EVENT_TYPES_ALLOWED =  new HashMap<Class<? extends EventObject>, Method[]>()
    {{
        try {
            put(AcceptedOrRejectedEvent.class, new Method[]{this.getClass().getMethod("onProgressUpdated"),});
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }};

    @Override
    default Map<Class<? extends EventObject>, Method[]> _getEventTMethodMap(){
        return EVENT_TYPES_ALLOWED;
    };

    void onProgressUpdated(AcceptedOrRejectedEvent event);

}
