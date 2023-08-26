package org.exthmui.utils.listeners

import java.lang.reflect.Method
import java.util.EventListener
import java.util.EventObject

interface BaseEventListener : EventListener {
    /**
     * Like this:
     * `
     * HashMap<Class></Class>, Method[]> EVENT_TYPES_ALLOWED =  new HashMap<Class></Class>, Method[]>()
     * {{
     * put(EventObject.class, new Method[]{,});
     * }};
     * default Map<Class></Class>, Method[]> getEventToMethodMap(){
     * return this.EVENT_TYPES_ALLOWED == null ? new HashMap<>() : EVENT_TYPES_ALLOWED;
     * };`
     */
    val eventToMethodMap: Map<Class<out EventObject>, Array<Method>>
}
