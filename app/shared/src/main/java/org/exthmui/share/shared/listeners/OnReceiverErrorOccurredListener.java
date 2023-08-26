package org.exthmui.share.shared.listeners;

import androidx.annotation.NonNull;

import org.exthmui.share.shared.events.ReceiverErrorOccurredEvent;
import org.exthmui.utils.listeners.BaseEventListener;

import java.lang.reflect.Method;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

public interface OnReceiverErrorOccurredListener extends BaseEventListener {
    HashMap<Class<? extends EventObject>, Method[]> EVENT_TYPES_ALLOWED = new HashMap<>() {{
        try {
            put(ReceiverErrorOccurredEvent.class, new Method[]{OnReceiverErrorOccurredListener.class.getDeclaredMethod("onReceiverErrorOccurred", ReceiverErrorOccurredEvent.class),});
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }};


    @NonNull
    @Override
    default Map<Class<? extends EventObject>, Method[]> getEventToMethodMap() {
        return EVENT_TYPES_ALLOWED;
    }

    void onReceiverErrorOccurred(ReceiverErrorOccurredEvent event);
}
