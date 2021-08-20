package org.exthmui.share.base.listeners;

import org.exthmui.share.base.events.AcceptedOrRejectedEvent;
import org.exthmui.share.base.events.ProgressUpdatedEvent;

import java.lang.reflect.Method;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

public interface OnAcceptedOrRejectedListener extends BaseEventListener {
    HashMap<Class<? extends EventObject>, Method[]> EVENT_TYPES_ALLOWED =  new HashMap<Class<? extends EventObject>, Method[]>()
    {{
        try {
            put(ProgressUpdatedEvent.class, new Method[]{this.getClass().getMethod("onAcceptedOrRejected"),});
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }};


    @Override
    default Map<Class<? extends EventObject>, Method[]> _getEventTMethodMap(){
        return EVENT_TYPES_ALLOWED;
    };

    void onAcceptedOrRejected(AcceptedOrRejectedEvent event);
}
