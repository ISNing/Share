package org.exthmui.share.shared.listeners;

import androidx.annotation.NonNull;

import org.exthmui.share.shared.events.ReceiverStartedEvent;
import org.exthmui.utils.listeners.BaseEventListener;

import java.lang.reflect.Method;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

public interface OnReceiverStartedListener extends BaseEventListener {
    HashMap<Class<? extends EventObject>, Method[]> EVENT_TYPES_ALLOWED = new HashMap<>() {{
        try {
            put(ReceiverStartedEvent.class, new Method[]{OnReceiverStartedListener.class.getDeclaredMethod("onReceiverStarted", ReceiverStartedEvent.class),});
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }};


    @NonNull
    @Override
    default Map<Class<? extends EventObject>, Method[]> getEventToMethodMap() {
        return EVENT_TYPES_ALLOWED;
    }

    void onReceiverStarted(ReceiverStartedEvent event);
}
