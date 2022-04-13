package org.exthmui.share.shared.listeners;

import java.lang.reflect.Method;
import java.util.EventListener;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

public interface BaseEventListener extends EventListener {
    /**
     * Like this:
     * <code>
     * HashMap<Class<? extends EventObject>, Method[]> EVENT_TYPES_ALLOWED =  new HashMap<Class<? extends EventObject>, Method[]>()
     * {{
     *      put(EventObject.class, new Method[]{,});
     * }};
     * default Map<Class<? extends EventObject>, Method[]> getEventTMethodMap(){
            return this.EVENT_TYPES_ALLOWED == null ? new HashMap<>() : EVENT_TYPES_ALLOWED;
     * };</code>
     */
    Map<Class<? extends EventObject>, Method[]> _getEventTMethodMap();
    default Map<Class<? extends EventObject>, Method[]> getEventTMethodMap(){
        return this._getEventTMethodMap() == null ? new HashMap<>() : this._getEventTMethodMap();
    }
}
