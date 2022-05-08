package org.exthmui.share.shared.listeners;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.EventListener;
import java.util.EventObject;
import java.util.Map;

public interface BaseEventListener extends EventListener {
    /**
     * Like this:
     * <code>
     * HashMap<Class<? extends EventObject>, Method[]> EVENT_TYPES_ALLOWED =  new HashMap<Class<? extends EventObject>, Method[]>()
     * {{
     *      put(EventObject.class, new Method[]{,});
     * }};
     * default Map<Class<? extends EventObject>, Method[]> getEventToMethodMap(){
            return this.EVENT_TYPES_ALLOWED == null ? new HashMap<>() : EVENT_TYPES_ALLOWED;
     * };</code>
     */
    @NonNull
    Map<Class<? extends EventObject>, Method[]> _getEventToMethodMap();
    @Nullable
    default Map<Class<? extends EventObject>, Method[]> getEventToMethodMap(){
        return this._getEventToMethodMap() == null ? Collections.emptyMap() : this._getEventToMethodMap();
    }
}
