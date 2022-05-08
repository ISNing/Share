package org.exthmui.share.shared.base.file;

import android.graphics.Bitmap;
import android.net.Uri;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exthmui.share.shared.base.mediastore.Audio;
import org.exthmui.share.shared.base.mediastore.Image;
import org.exthmui.share.shared.base.mediastore.Media;
import org.exthmui.share.shared.base.mediastore.Video;
import org.exthmui.share.shared.misc.Constants;

public class File {
    @Nullable
    private Bitmap thumbnail = null;
    @Nullable
    private String title;
    @Nullable
    private String name;
    @Nullable
    private Uri uri;
    @Nullable
    private String path;
    @IntRange(from = -1)
    private long fileSize = -1;
    @IntRange(from = 0)
    private int dateAdded;
    @IntRange(from = 0)
    private int dateModified;
    private int type = Constants.FileType.UNKNOWN.getNumVal();

    @SuppressWarnings("deprecated")
    public File(@NonNull Media media) {
        if (media instanceof Audio) {
            this.uri = ((Audio) media).getUri();
        } else if (media instanceof Image) {
            this.uri = ((Image) media).getUri();
        } else if (media instanceof Video) {
            this.uri = ((Video) media).getUri();
        }

        this.title = media.getTitle();
        this.path = media.getData();
        this.name = path.substring(path.lastIndexOf('/') + 1);
        this.fileSize = media.getSize();
        this.dateAdded = media.getDateAdded();
        this.dateModified = media.getDateModified();
    }

    public File() {
    }

    @Nullable
    public Bitmap getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(@Nullable Bitmap thumbnail) {
        this.thumbnail = thumbnail;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    @Nullable
    public String getPath() {
        return path;
    }

    public void setPath(@Nullable String path) {
        this.path = path;
    }

    @IntRange(from = -1)
    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(@IntRange(from = -1) long fileSize) {
        this.fileSize = fileSize;
    }

    @IntRange(from = 0)
    public int getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(@IntRange(from = 0) int dateAdded) {
        this.dateAdded = dateAdded;
    }

    @IntRange(from = 0)
    public int getDateModified() {
        return dateModified;
    }

    public void setDateModified(@IntRange(from = 0) int dateModified) {
        this.dateModified = dateModified;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    public void setTitle(@Nullable String title) {
        this.title = title;
    }

    @Nullable
    public Uri getUri() {
        return uri;
    }

    public void setUri(@Nullable Uri uri) {
        this.uri = uri;
    }
}