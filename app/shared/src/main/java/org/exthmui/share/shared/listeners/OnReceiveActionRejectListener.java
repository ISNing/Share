package org.exthmui.share.shared.listeners;

import androidx.annotation.NonNull;

import org.exthmui.share.shared.events.ReceiveActionRejectEvent;
import org.exthmui.utils.listeners.BaseEventListener;

import java.lang.reflect.Method;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

public interface OnReceiveActionRejectListener extends BaseEventListener {
    HashMap<Class<? extends EventObject>, Method[]> EVENT_TYPES_ALLOWED = new HashMap<>() {{
        try {
            put(ReceiveActionRejectEvent.class, new Method[]{OnReceiveActionRejectListener.class.getDeclaredMethod("onReceiveActionReject", ReceiveActionRejectEvent.class),});
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }};


    @NonNull
    @Override
    default Map<Class<? extends EventObject>, Method[]> _getEventToMethodMap() {
        return EVENT_TYPES_ALLOWED;
    }

    void onReceiveActionReject(ReceiveActionRejectEvent event);
}
