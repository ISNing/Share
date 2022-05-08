package org.exthmui.share.shared.base.mediastore;

import android.database.Cursor;
import android.os.Build;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.Arrays;

@RequiresApi(Build.VERSION_CODES.Q)
public class Download extends Media {
    String downloadUri;
    String refererUri;

    public Download() {
    }

    public Download(@NonNull Cursor c) {
        super(c);
        try {
            this.downloadUri = c.getString(c.getColumnIndexOrThrow(MediaStore.DownloadColumns.DOWNLOAD_URI));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.refererUri = c.getString(c.getColumnIndexOrThrow(MediaStore.DownloadColumns.REFERER_URI));
        } catch (IllegalArgumentException ignored) {
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "Download{" +
                "_id='" + _id + '\'' +
                ", _count='" + _count + '\'' +
                ", downloadUri='" + downloadUri + '\'' +
                ", refererUri='" + refererUri + '\'' +
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

    @NonNull


    public String getDownloadUri() {
        return downloadUri;
    }

    public void setDownloadUri(String downloadUri) {
        this.downloadUri = downloadUri;
    }

    public String getRefererUri() {
        return refererUri;
    }

    public void setRefererUri(String refererUri) {
        this.refererUri = refererUri;
    }
}
