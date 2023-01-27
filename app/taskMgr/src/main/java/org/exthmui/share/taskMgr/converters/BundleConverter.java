package org.exthmui.share.taskMgr.converters;

import android.os.Bundle;
import android.os.Parcel;

import androidx.room.TypeConverter;

public class BundleConverter {
    @TypeConverter
    public Bundle bytesToBundle(byte[] bytes) {
        Parcel p = Parcel.obtain();
        p.unmarshall(bytes, 0, bytes.length);
        Bundle b = p.readBundle(getClass().getClassLoader());
        p.recycle();
        return b;
    }

    @TypeConverter
    public byte[] BundleToBytes(Bundle bundle) {
        Parcel p = Parcel.obtain();
        p.writeBundle(bundle);
        byte[] bytes = p.marshall();
        p.recycle();
        return bytes;
    }
}
