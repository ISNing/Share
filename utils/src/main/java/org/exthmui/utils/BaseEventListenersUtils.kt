package org.exthmui.utils

import org.exthmui.utils.listeners.BaseEventListener
import java.lang.reflect.InvocationTargetException
import java.util.EventObject

object BaseEventListenersUtils {
    fun isThisListenerSuitable(
        listener: BaseEventListener,
        listenerTypesAllowed: Array<Class<out BaseEventListener>>
    ): Boolean {
        for (t in listenerTypesAllowed) {
            if (t.isAssignableFrom(listener.javaClass)) {
                return true
            }
        }
        return false
    }

    fun notifyListeners(event: EventObject, listeners: Collection<BaseEventListener>) {
        for (listener in listeners) {
            for (t in listener.eventToMethodMap.keys) {
                if (t.isAssignableFrom(event.javaClass)) {
                    val methods = listener.eventToMethodMap[t] ?: break
                    for (method in methods) {
                        try {
                            val actualMethod = listener.javaClass.getDeclaredMethod(
                                method.name,
                                *method.parameterTypes
                            )
                            actualMethod.invoke(listener, event)
                        } catch (e: NoSuchMethodException) {
                            e.printStackTrace()
                        } catch (e: IllegalAccessException) {
                            e.printStackTrace()
                        } catch (e: InvocationTargetException) {
                            val targetException = e.targetException
                            targetException.printStackTrace()
                        }
                    }
                }
            }
        }
    }
}
