package org.exthmui.share.shared.base.mediastore;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import androidx.annotation.NonNull;

import java.util.Arrays;

/**
 * Bean for VideoColumns
 *
 * @see android.provider.MediaStore.Video.VideoColumns
 * @see <a href="https://developer.android.google.cn/reference/android/provider/MediaStore.Video.VideoColumns">MediaStore.Video.VideoColumns</a>
 */
public class Video extends Media {
    int bookmark;
    String category;
    int colorRange;
    int colorStandard;
    int colorTransfer;
    String description;
    int isPrivate;
    String language;
    /**
     * @deprecated
     */
    @Deprecated
    float latitude;
    /**
     * @deprecated
     */
    @Deprecated
    float longitude;
    /**
     * @deprecated
     */
    @Deprecated
    int miniThumbMagic;
    String tags;

    public Video() {
        super();
    }

    public Video(@NonNull Cursor c) {
        super(c);

        try {
            this.bookmark = c.getInt(c.getColumnIndexOrThrow(MediaStore.Video.Media.BOOKMARK));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.category = c.getString(c.getColumnIndexOrThrow(MediaStore.Video.Media.CATEGORY));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.description = c.getString(c.getColumnIndexOrThrow(MediaStore.Video.Media.DESCRIPTION));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.isPrivate = c.getInt(c.getColumnIndexOrThrow(MediaStore.Video.Media.IS_PRIVATE));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.language = c.getString(c.getColumnIndexOrThrow(MediaStore.Video.Media.LANGUAGE));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.latitude = c.getFloat(c.getColumnIndexOrThrow(MediaStore.Video.Media.LATITUDE));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.longitude = c.getFloat(c.getColumnIndexOrThrow(MediaStore.Video.Media.LONGITUDE));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.miniThumbMagic = c.getInt(c.getColumnIndexOrThrow(MediaStore.Video.Media.MINI_THUMB_MAGIC));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.tags = c.getString(c.getColumnIndexOrThrow(MediaStore.Video.Media.TAGS));
        } catch (IllegalArgumentException ignored) {
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                this.colorRange = c.getInt(c.getColumnIndexOrThrow(MediaStore.Video.Media.COLOR_RANGE));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.colorStandard = c.getInt(c.getColumnIndexOrThrow(MediaStore.Video.Media.COLOR_STANDARD));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.colorTransfer = c.getInt(c.getColumnIndexOrThrow(MediaStore.Video.Media.COLOR_TRANSFER));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "Video{" +
                "album='" + album + '\'' +
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
                ", bookmark=" + bookmark +
                ", category='" + category + '\'' +
                ", colorRange=" + colorRange +
                ", colorStandard=" + colorStandard +
                ", colorTransfer=" + colorTransfer +
                ", description='" + description + '\'' +
                ", isPrivate=" + isPrivate +
                ", language='" + language + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", miniThumbMagic=" + miniThumbMagic +
                ", tags='" + tags + '\'' +
                '}';
    }

    public int getBookmark() {
        return bookmark;
    }

    public void setBookmark(int bookmark) {
        this.bookmark = bookmark;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getColorRange() {
        return colorRange;
    }

    public void setColorRange(int colorRange) {
        this.colorRange = colorRange;
    }

    public int getColorStandard() {
        return colorStandard;
    }

    public void setColorStandard(int colorStandard) {
        this.colorStandard = colorStandard;
    }

    public int getColorTransfer() {
        return colorTransfer;
    }

    public void setColorTransfer(int colorTransfer) {
        this.colorTransfer = colorTransfer;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int isPrivate() {
        return isPrivate;
    }

    public void setPrivate(int isPrivate) {
        this.isPrivate = isPrivate;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @Deprecated
    public float getLatitude() {
        return latitude;
    }

    @Deprecated
    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    @Deprecated
    public float getLongitude() {
        return longitude;
    }

    @Deprecated
    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    @Deprecated
    public int getMiniThumbMagic() {
        return miniThumbMagic;
    }

    @Deprecated
    public void setMiniThumbMagic(int miniThumbMagic) {
        this.miniThumbMagic = miniThumbMagic;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Uri getUri() {
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            uri = MediaStore.Video.Media.getContentUri(getVolumeName(), getId());
        else
            uri = ContentUris.withAppendedId(MediaStore.Video.Media.getContentUri(getVolumeName()), getId());
        return uri;
    }
}