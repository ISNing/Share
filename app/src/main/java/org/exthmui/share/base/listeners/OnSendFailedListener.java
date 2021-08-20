package org.exthmui.share.base.listeners;

import org.exthmui.share.base.events.SendFailedEvent;

import java.lang.reflect.Method;
import java.util.EventListener;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

public interface OnSendFailedListener extends BaseEventListener {
    HashMap<Class<? extends EventObject>, Method[]> EVENT_TYPES_ALLOWED =  new HashMap<Class<? extends EventObject>, Method[]>()
    {{
        try {
            put(SendFailedEvent.class, new Method[]{this.getClass().getMethod("onSendFailed"),});
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }};

    @Override
    default Map<Class<? extends EventObject>, Method[]> _getEventTMethodMap(){
        return EVENT_TYPES_ALLOWED;
    };
    void onSendFailed(SendFailedEvent event);
}
