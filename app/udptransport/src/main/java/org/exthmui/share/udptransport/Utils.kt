package org.exthmui.share.udptransport

import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.Socket

object Utils {
    fun silentClose(closeable: Closeable?) {
        try {
            closeable?.close()
        } catch (ignored: IOException) {
        }
    }

    @Throws(IOException::class)
    fun getDataInput(socket: Socket): DataInputStream {
        return DataInputStream(socket.getInputStream())
    }

    @Throws(IOException::class)
    fun getDataOutput(socket: Socket): DataOutputStream {
        return DataOutputStream(socket.getOutputStream())
    }

    fun <K, V> fillMapNulls(map: MutableMap<K, V>, keys: List<K>, defaultValue: V) {
        keys.forEach { key ->
            if (!map.containsKey(key)) {
                map[key] = defaultValue
            } else {
                val value = map[key]
                if (value == null) {
                    map[key] = defaultValue
                }
            }
        }
        map.keys.filterNot { keys.contains(it) }.forEach { map.remove(it) }
    }

    fun <K, V> fillMapNulls(map: MutableMap<K, V?>, defaultValue: V) {
        replaceMapValueNullable(map, null, defaultValue)
    }

    fun <K, V> replaceMapValue(map: MutableMap<K, V>, fromValue: V, toValue: V) {
        map.forEach {
            if (it.value == fromValue) {
                map[it.key] = toValue
            }
        }
    }

    fun <K, V> replaceMapValueNullable(map: MutableMap<K, V?>, fromValue: V?, toValue: V?) {
        map.forEach {
            if (it.value == fromValue) {
                map[it.key] = toValue
            }
        }
    }

    fun getCommonBaseClass(list: Collection<Any>): Class<*> {
        var baseClass: Class<*> = list.first().javaClass
        list.forEach {
            var cls = it.javaClass

            while (!baseClass.isAssignableFrom(cls)) {
                baseClass = baseClass.superclass
            }
        }
        return baseClass
    }
}
