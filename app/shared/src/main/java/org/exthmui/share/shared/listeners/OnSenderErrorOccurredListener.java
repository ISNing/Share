package org.exthmui.share.shared.listeners;

import androidx.annotation.NonNull;

import org.exthmui.share.shared.events.SenderErrorOccurredEvent;
import org.exthmui.utils.listeners.BaseEventListener;

import java.lang.reflect.Method;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

public interface OnSenderErrorOccurredListener extends BaseEventListener {
    HashMap<Class<? extends EventObject>, Method[]> EVENT_TYPES_ALLOWED = new HashMap<>() {{
        try {
            put(SenderErrorOccurredEvent.class, new Method[]{OnSenderErrorOccurredListener.class.getDeclaredMethod("onSenderErrorOccurred", SenderErrorOccurredEvent.class),});
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }};


    @NonNull
    @Override
    default Map<Class<? extends EventObject>, Method[]> _getEventToMethodMap() {
        return EVENT_TYPES_ALLOWED;
    }

    void onSenderErrorOccurred(SenderErrorOccurredEvent event);
}
