package org.exthmui.share.shared.base.mediastore;

import android.database.Cursor;
import android.provider.BaseColumns;

import androidx.annotation.NonNull;

public class Base {
    long _id;
    int _count;

    public Base() {
    }

    public Base(@NonNull Cursor c) {
        try {
            this._id = c.getLong(c.getColumnIndexOrThrow(BaseColumns._ID));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this._count = c.getInt(c.getColumnIndexOrThrow(BaseColumns._COUNT));
        } catch (IllegalArgumentException ignored) {
        }
    }

    public Base(long _id, int _count) {
        this._id = _id;
        this._count = _count;
    }

    @NonNull
    @Override
    public String toString() {
        return "Base{" +
                "_id='" + _id + '\'' +
                ", _count='" + _count + '\'' +
                '}';
    }

    public long getId() {
        return _id;
    }

    public void setId(long _id) {
        this._id = _id;
    }

    public int getCount() {
        return _count;
    }

    public void setCount(int _count) {
        this._count = _count;
    }
}
