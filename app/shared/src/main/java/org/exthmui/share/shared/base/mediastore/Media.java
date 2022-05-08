package org.exthmui.share.shared.base.mediastore;

import android.database.Cursor;
import android.os.Build;
import android.provider.MediaStore;

import androidx.annotation.NonNull;

import java.util.Arrays;

/**
 * Bean for MediaColumns
 *
 * @see android.provider.MediaStore.MediaColumns
 * @see <a href="https://developer.android.google.cn/reference/android/provider/MediaStore.MediaColumns">MediaStore.MediaColumns</a>
 */
public class Media extends Base {
    String album;
    String albumArtist;
    String artist;
    String author;
    int bitrate;
    float captureFrameRate;
    String cdTrackNumber;
    String compilation;
    String composer;
    /**
     * @deprecated
     */
    @Deprecated
    String data;
    int dateAdded;
    int dateExpires;
    int dateModified;
    int dateTaken;
    String discNumber;
    String displayName;
    String documentId;
    int duration;
    int generationAdded;
    int generationModified;
    String genre;
    int height;
    String instanceId;
    int isDownload;
    int isDrm;
    int isFavorite;
    int isPending;
    int isTrashed;
    String mimeType;
    int numTracks;
    int orientation;
    String originalDocumentId;
    String ownerPackageName;
    String relativePath;
    String resolution;
    int size;
    String title;
    String volumeName;
    int width;
    String writer;
    byte[] xmp;
    int year;

    public Media() {
    }

    @SuppressWarnings("deprecated")
    public Media(@NonNull Cursor c) {
        super(c);
        try {
            this.data = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.dateAdded = c.getInt(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.dateModified = c.getInt(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.displayName = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.height = c.getInt(c.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.mimeType = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.size = c.getInt(c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.title = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.TITLE));
        } catch (IllegalArgumentException ignored) {
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                this.width = c.getInt(c.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.duration = c.getInt(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DURATION));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.instanceId = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.INSTANCE_ID));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.isPending = c.getInt(c.getColumnIndexOrThrow(MediaStore.MediaColumns.IS_PENDING));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.documentId = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DOCUMENT_ID));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.dateTaken = c.getInt(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.dateExpires = c.getInt(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_EXPIRES));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.orientation = c.getInt(c.getColumnIndexOrThrow(MediaStore.MediaColumns.ORIENTATION));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.originalDocumentId = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.ORIGINAL_DOCUMENT_ID));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.ownerPackageName = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.OWNER_PACKAGE_NAME));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.relativePath = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.volumeName = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.VOLUME_NAME));
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                this.year = c.getInt(c.getColumnIndexOrThrow(MediaStore.MediaColumns.YEAR));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.album = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.ALBUM));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.artist = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.ARTIST));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.composer = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.COMPOSER));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.discNumber = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISC_NUMBER));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.generationAdded = c.getInt(c.getColumnIndexOrThrow(MediaStore.MediaColumns.GENERATION_ADDED));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.generationModified = c.getInt(c.getColumnIndexOrThrow(MediaStore.MediaColumns.GENERATION_MODIFIED));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.genre = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.GENRE));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.isDownload = c.getInt(c.getColumnIndexOrThrow(MediaStore.MediaColumns.IS_DOWNLOAD));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.isDrm = c.getInt(c.getColumnIndexOrThrow(MediaStore.MediaColumns.IS_DRM));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.isFavorite = c.getInt(c.getColumnIndexOrThrow(MediaStore.MediaColumns.IS_FAVORITE));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.isTrashed = c.getInt(c.getColumnIndexOrThrow(MediaStore.MediaColumns.IS_TRASHED));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.albumArtist = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.ALBUM_ARTIST));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.author = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.AUTHOR));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.bitrate = c.getInt(c.getColumnIndexOrThrow(MediaStore.MediaColumns.BITRATE));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.captureFrameRate = c.getFloat(c.getColumnIndexOrThrow(MediaStore.MediaColumns.CAPTURE_FRAMERATE));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.cdTrackNumber = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.CD_TRACK_NUMBER));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.compilation = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.COMPILATION));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.numTracks = c.getInt(c.getColumnIndexOrThrow(MediaStore.MediaColumns.NUM_TRACKS));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.resolution = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.RESOLUTION));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.writer = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.WRITER));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.xmp = c.getBlob(c.getColumnIndexOrThrow(MediaStore.MediaColumns.XMP));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "Media{" +
                "_id='" + _id + '\'' +
                ", _count='" + _count + '\'' +
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

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getAlbumArtist() {
        return albumArtist;
    }

    public void setAlbumArtist(String albumArtist) {
        this.albumArtist = albumArtist;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public int getBitrate() {
        return bitrate;
    }

    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }

    public float getCaptureFrameRate() {
        return captureFrameRate;
    }

    public void setCaptureFrameRate(float captureFrameRate) {
        this.captureFrameRate = captureFrameRate;
    }

    public String getCdTrackNumber() {
        return cdTrackNumber;
    }

    public void setCdTrackNumber(String cdTrackNumber) {
        this.cdTrackNumber = cdTrackNumber;
    }

    public String getCompilation() {
        return compilation;
    }

    public void setCompilation(String compilation) {
        this.compilation = compilation;
    }

    public String getComposer() {
        return composer;
    }

    public void setComposer(String composer) {
        this.composer = composer;
    }

    @Deprecated
    public String getData() {
        return data;
    }

    @Deprecated
    public void setData(String data) {
        this.data = data;
    }

    public int getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(int dateAdded) {
        this.dateAdded = dateAdded;
    }

    public int getDateExpires() {
        return dateExpires;
    }

    public void setDateExpires(int dateExpires) {
        this.dateExpires = dateExpires;
    }

    public int getDateModified() {
        return dateModified;
    }

    public void setDateModified(int dateModified) {
        this.dateModified = dateModified;
    }

    public int getDateTaken() {
        return dateTaken;
    }

    public void setDateTaken(int dateTaken) {
        this.dateTaken = dateTaken;
    }

    public String getDiscNumber() {
        return discNumber;
    }

    public void setDiscNumber(String discNumber) {
        this.discNumber = discNumber;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getGenerationAdded() {
        return generationAdded;
    }

    public void setGenerationAdded(int generationAdded) {
        this.generationAdded = generationAdded;
    }

    public int getGenerationModified() {
        return generationModified;
    }

    public void setGenerationModified(int generationModified) {
        this.generationModified = generationModified;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public int isDownload() {
        return isDownload;
    }

    public void setDownload(int isDownload) {
        this.isDownload = isDownload;
    }

    public int isDrm() {
        return isDrm;
    }

    public void setDrm(int isDrm) {
        this.isDrm = isDrm;
    }

    public int isFavorite() {
        return isFavorite;
    }

    public void setFavorite(int isFavorite) {
        this.isFavorite = isFavorite;
    }

    public int isPending() {
        return isPending;
    }

    public void setPending(int isPending) {
        this.isPending = isPending;
    }

    public int isTrashed() {
        return isTrashed;
    }

    public void setTrashed(int isTrashed) {
        this.isTrashed = isTrashed;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public int getNumTracks() {
        return numTracks;
    }

    public void setNumTracks(int numTracks) {
        this.numTracks = numTracks;
    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(int orientation) {
        this.orientation = orientation;
    }

    public String getOriginalDocumentId() {
        return originalDocumentId;
    }

    public void setOriginalDocumentId(String originalDocumentId) {
        this.originalDocumentId = originalDocumentId;
    }

    public String getOwnerPackageName() {
        return ownerPackageName;
    }

    public void setOwnerPackageName(String ownerPackageName) {
        this.ownerPackageName = ownerPackageName;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getVolumeName() {
        return volumeName;
    }

    public void setVolumeName(String volumeName) {
        this.volumeName = volumeName;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public String getWriter() {
        return writer;
    }

    public void setWriter(String writer) {
        this.writer = writer;
    }

    public byte[] getXmp() {
        return xmp;
    }

    public void setXmp(byte[] xmp) {
        this.xmp = xmp;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }
}
