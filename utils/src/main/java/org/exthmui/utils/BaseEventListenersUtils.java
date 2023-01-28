package org.exthmui.utils;

import androidx.annotation.NonNull;

import org.exthmui.utils.listeners.BaseEventListener;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.EventObject;

public abstract class BaseEventListenersUtils {
    public static boolean isThisListenerSuitable(@NonNull BaseEventListener listener, @NonNull Class<? extends BaseEventListener>[] listenerTypesAllowed) {
        for (Class<? extends BaseEventListener> t : listenerTypesAllowed) {
            if (t.isAssignableFrom(listener.getClass())) {
                return true;
            }
        }
        return false;
    }

    public static void notifyListeners(@NonNull EventObject event, @NonNull Collection<BaseEventListener> listeners) {
        for (BaseEventListener listener : listeners) {
            for (Class<? extends EventObject> t: listener.getEventToMethodMap().keySet()) {
                if (t.isAssignableFrom(event.getClass())){
                    Method[] methods = listener.getEventToMethodMap().get(t);
                    if (methods == null) break;
                    for (Method method: methods) {
                        try {
                            Method actualMethod = listener.getClass().getDeclaredMethod(method.getName(), method.getParameterTypes());

                            actualMethod.invoke(listener, event);
                        } catch (@NonNull NoSuchMethodException | IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            Throwable targetException = e.getTargetException();
                            targetException.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}
