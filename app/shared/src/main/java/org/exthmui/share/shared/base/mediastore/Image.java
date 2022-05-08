package org.exthmui.share.shared.base.mediastore;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import androidx.annotation.NonNull;

import java.util.Arrays;

/**
 * Bean for MediaColumns
 *
 * @see android.provider.MediaStore.Images.ImageColumns
 * @see <a href="https://developer.android.google.cn/reference/android/provider/MediaStore.MediaColumns">MediaStore.MediaColumns</a>
 */
public class Image extends Media {
    String description;
    String exposureTime;
    String fNumber;
    int iso;
    int isPrivate;
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
    /**
     * @deprecated
     */
    @Deprecated
    String picasaId;
    int sceneCaptureType;

    public Image() {
    }

    public Image(@NonNull Cursor c) {
        super(c);
        try {
            this.description = c.getString(c.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DESCRIPTION));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.isPrivate = c.getInt(c.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.IS_PRIVATE));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.latitude = c.getFloat(c.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.LATITUDE));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.longitude = c.getFloat(c.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.LONGITUDE));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.miniThumbMagic = c.getInt(c.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.MINI_THUMB_MAGIC));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.picasaId = c.getString(c.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.PICASA_ID));
        } catch (IllegalArgumentException ignored) {
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                this.exposureTime = c.getString(c.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.EXPOSURE_TIME));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.fNumber = c.getString(c.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.F_NUMBER));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.iso = c.getInt(c.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.ISO));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.sceneCaptureType = c.getInt(c.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.SCENE_CAPTURE_TYPE));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "Image{" +
                "_id='" + _id + '\'' +
                ", _count='" + _count + '\'' +
                ", description='" + description + '\'' +
                ", exposureTime='" + exposureTime + '\'' +
                ", fNumber='" + fNumber + '\'' +
                ", iso=" + iso +
                ", isPrivate=" + isPrivate +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", miniThumbMagic=" + miniThumbMagic +
                ", picasaId='" + picasaId + '\'' +
                ", sceneCaptureType=" + sceneCaptureType +
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExposureTime() {
        return exposureTime;
    }

    public void setExposureTime(String exposureTime) {
        this.exposureTime = exposureTime;
    }

    public String getfNumber() {
        return fNumber;
    }

    public void setfNumber(String fNumber) {
        this.fNumber = fNumber;
    }

    public int getIso() {
        return iso;
    }

    public void setIso(int iso) {
        this.iso = iso;
    }

    public int isPrivate() {
        return isPrivate;
    }

    public void setPrivate(int isPrivate) {
        this.isPrivate = isPrivate;
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

    @Deprecated
    public String getPicasaId() {
        return picasaId;
    }

    @Deprecated
    public void setPicasaId(String picasaId) {
        this.picasaId = picasaId;
    }

    public int getSceneCaptureType() {
        return sceneCaptureType;
    }

    public void setSceneCaptureType(int sceneCaptureType) {
        this.sceneCaptureType = sceneCaptureType;
    }

    public Uri getUri() {
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            uri = MediaStore.Images.Media.getContentUri(getVolumeName(), getId());
        else
            uri = ContentUris.withAppendedId(MediaStore.Images.Media.getContentUri(getVolumeName()), getId());
        return uri;
    }
}
