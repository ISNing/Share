package org.exthmui.share.shared.base.mediastore;

import android.database.Cursor;
import android.provider.MediaStore;

import androidx.annotation.NonNull;

public class Genres extends Base {
    String name;

    public Genres() {
    }

    public Genres(@NonNull Cursor c) {
        super(c);
        try {
            this.name = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.GenresColumns.NAME));
        } catch (IllegalArgumentException ignored) {
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "Genres{" +
                "_id='" + _id + '\'' +
                ", _count='" + _count + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
