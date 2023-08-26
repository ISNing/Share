package org.exthmui.share.taskMgr.converters

import android.os.Bundle
import android.os.Parcel
import androidx.room.TypeConverter

class BundleConverter {
    @TypeConverter
    fun bytesToBundle(bytes: ByteArray): Bundle {
        val p = Parcel.obtain()
        p.unmarshall(bytes, 0, bytes.size)
        val b = p.readBundle(javaClass.classLoader)
        p.recycle()
        return b ?: Bundle()
    }

    @TypeConverter
    fun BundleToBytes(bundle: Bundle): ByteArray {
        val p = Parcel.obtain()
        p.writeBundle(bundle)
        val bytes = p.marshall()
        p.recycle()
        return bytes
    }
}
