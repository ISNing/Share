package org.exthmui.share.shared.base.mediastore;

import android.database.Cursor;
import android.provider.MediaStore;

import androidx.annotation.NonNull;

import java.util.Arrays;

public class File extends Media {
    public static final int MEDIA_TYPE_AUDIO = 2;
    public static final int MEDIA_TYPE_DOCUMENT = 6;
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_NONE = 0;
    /**
     * @deprecated
     */
    @Deprecated
    public static final int MEDIA_TYPE_PLAYLIST = 4;
    public static final int MEDIA_TYPE_SUBTITLE = 5;
    public static final int MEDIA_TYPE_VIDEO = 3;

    int mediaType;
    String mimeType;
    int parent;

    public File() {
    }

    public File(@NonNull Cursor c) {
        super(c);
        try {
            this.mediaType = c.getInt(c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.mimeType = c.getString(c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.parent = c.getInt(c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.PARENT));
        } catch (IllegalArgumentException ignored) {
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "File{" +
                "_id='" + _id + '\'' +
                ", _count='" + _count + '\'' +
                ", mediaType=" + mediaType +
                ", mimeType='" + mimeType + '\'' +
                ", parent=" + parent +
                ", album='" + album + '\'' +
                ", albumArtist='" + albumArtist + '\'' +
                ", artist='" + artist + '\'' +
                ", author='" + author + '\'' +
                ", bitrate=" + bitrate +
                ", captureFrameRate=" + captureFrameRate +
                ", cdTrackNumber='" + cdTrackNumber + '\'' +
                ", compilation='" + compilation + '\'' +
                ", composer='" + composer + '\'' +
                ", data='" + data + '\'' +
                ", dateAdded=" + dateAdded +
                ", dateExpires=" + dateExpires +
                ", dateModified=" + dateModified +
                ", dateTaken=" + dateTaken +
                ", discNumber='" + discNumber + '\'' +
                ", displayName='" + displayName + '\'' +
                ", documentId='" + documentId + '\'' +
                ", duration=" + duration +
                ", generationAdded=" + generationAdded +
                ", generationModified=" + generationModified +
                ", genre='" + genre + '\'' +
                ", height=" + height +
                ", instanceId='" + instanceId + '\'' +
                ", isDownload=" + isDownload +
                ", isDrm=" + isDrm +
                ", isFavorite=" + isFavorite +
                ", isPending=" + isPending +
                ", isTrashed=" + isTrashed +
                ", mimeType='" + mimeType + '\'' +
                ", numTracks=" + numTracks +
                ", orientation=" + orientation +
                ", originalDocumentId='" + originalDocumentId + '\'' +
                ", ownerPackageName='" + ownerPackageName + '\'' +
                ", relativePath='" + relativePath + '\'' +
                ", resolution='" + resolution + '\'' +
                ", size=" + size +
                ", title='" + title + '\'' +
                ", volumeName='" + volumeName + '\'' +
                ", width=" + width +
                ", writer='" + writer + '\'' +
                ", xmp=" + Arrays.toString(xmp) +
                ", year=" + year +
                '}';
    }

    public int getMediaType() {
        return mediaType;
    }

    public void setMediaType(int mediaType) {
        this.mediaType = mediaType;
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

    @Override
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public int getParent() {
        return parent;
    }

    public void setParent(int parent) {
        this.parent = parent;
    }
}
