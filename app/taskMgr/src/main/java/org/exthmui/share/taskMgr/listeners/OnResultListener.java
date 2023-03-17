
package org.exthmui.share.taskMgr.listeners;

import androidx.annotation.NonNull;

import org.exthmui.share.taskMgr.events.ResultEvent;
import org.exthmui.utils.listeners.BaseEventListener;

import java.lang.reflect.Method;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

public interface OnResultListener extends BaseEventListener {
    HashMap<Class<? extends EventObject>, Method[]> EVENT_TYPES_ALLOWED = new HashMap<>() {{
        try {
            put(ResultEvent.class, new Method[]{OnResultListener.class.getDeclaredMethod("onResult", ResultEvent.class),});
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }};


    @NonNull
    @Override
    default Map<Class<? extends EventObject>, Method[]> _getEventToMethodMap() {
        return EVENT_TYPES_ALLOWED;
    }

    void onResult(ResultEvent event);
}
