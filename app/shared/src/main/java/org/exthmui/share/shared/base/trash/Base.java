package org.exthmui.share.shared.base.trash;

import android.database.Cursor;
import android.provider.BaseColumns;

public class Base {
    String _id;
    String _count;

    public Base() {}

    public Base(Cursor c) {
        this._id = c.getString(c.getColumnIndexOrThrow(BaseColumns._ID));
        this._count = c.getString(c.getColumnIndexOrThrow(BaseColumns._COUNT));
    }

    public Base(String _id, String _count) {
        this._id = _id;
        this._count = _count;
    }

    @Override
    public String toString() {
        return "Base{" +
                "_id='" + _id + '\'' +
                ", _count='" + _count + '\'' +
                '}';
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String get_count() {
        return _count;
    }

    public void set_count(String _count) {
        this._count = _count;
    }
}
