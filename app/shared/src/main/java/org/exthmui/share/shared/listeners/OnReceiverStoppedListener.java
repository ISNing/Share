package org.exthmui.share.shared.listeners;

import androidx.annotation.NonNull;

import org.exthmui.share.shared.events.ReceiverStoppedEvent;
import org.exthmui.utils.listeners.BaseEventListener;

import java.lang.reflect.Method;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

public interface OnReceiverStoppedListener extends BaseEventListener {
    HashMap<Class<? extends EventObject>, Method[]> EVENT_TYPES_ALLOWED = new HashMap<>() {{
        try {
            put(ReceiverStoppedEvent.class, new Method[]{OnReceiverStoppedListener.class.getDeclaredMethod("onReceiverStopped", ReceiverStoppedEvent.class),});
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }};


    @NonNull
    @Override
    default Map<Class<? extends EventObject>, Method[]> _getEventToMethodMap() {
        return EVENT_TYPES_ALLOWED;
    }

    void onReceiverStopped(ReceiverStoppedEvent event);
}
