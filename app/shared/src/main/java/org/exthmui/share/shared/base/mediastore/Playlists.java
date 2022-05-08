package org.exthmui.share.shared.base.mediastore;

import android.database.Cursor;
import android.provider.MediaStore;

import androidx.annotation.NonNull;

@Deprecated
public class Playlists extends Base {
    /**
     * @deprecated
     */
    @Deprecated
    private String data;
    /**
     * @deprecated
     */
    @Deprecated
    private String dateAdded;
    /**
     * @deprecated
     */
    @Deprecated
    private String dateModified;
    /**
     * @deprecated
     */
    @Deprecated
    private String name;

    public Playlists() {
    }

    public Playlists(@NonNull Cursor c) {
        super(c);
        try {
            this.data = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.PlaylistsColumns.DATA));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.dateAdded = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.PlaylistsColumns.DATE_ADDED));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.dateModified = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.PlaylistsColumns.DATE_MODIFIED));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.name = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.PlaylistsColumns.NAME));
        } catch (IllegalArgumentException ignored) {
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "Playlists{" +
                "_id='" + _id + '\'' +
                ", _count='" + _count + '\'' +
                ", data='" + data + '\'' +
                ", dateAdded='" + dateAdded + '\'' +
                ", dateModified='" + dateModified + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    @Deprecated
    public String getData() {
        return data;
    }

    @Deprecated
    public void setData(String data) {
        this.data = data;
    }

    @Deprecated
    public String getDateAdded() {
        return dateAdded;
    }

    @Deprecated
    public void setDateAdded(String dateAdded) {
        this.dateAdded = dateAdded;
    }

    @Deprecated
    public String getDateModified() {
        return dateModified;
    }

    @Deprecated
    public void setDateModified(String dateModified) {
        this.dateModified = dateModified;
    }

    @Deprecated
    public String getName() {
        return name;
    }

    @Deprecated
    public void setName(String name) {
        this.name = name;
    }
}
