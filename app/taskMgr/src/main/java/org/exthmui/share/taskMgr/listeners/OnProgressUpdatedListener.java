package org.exthmui.share.taskMgr.listeners;

import androidx.annotation.NonNull;

import org.exthmui.share.taskMgr.events.ProgressUpdatedEvent;
import org.exthmui.utils.listeners.BaseEventListener;

import java.lang.reflect.Method;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

public interface OnProgressUpdatedListener extends BaseEventListener {
    HashMap<Class<? extends EventObject>, Method[]> EVENT_TYPES_ALLOWED = new HashMap<>() {{
        try {
            put(ProgressUpdatedEvent.class, new Method[]{OnProgressUpdatedListener.class.getDeclaredMethod("onProgressUpdated", ProgressUpdatedEvent.class), });
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }};


    @NonNull
    @Override
    default Map<Class<? extends EventObject>, Method[]> _getEventToMethodMap() {
        return EVENT_TYPES_ALLOWED;
    }

    void onProgressUpdated(ProgressUpdatedEvent event);
}
