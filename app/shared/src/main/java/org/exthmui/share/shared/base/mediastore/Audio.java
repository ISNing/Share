package org.exthmui.share.shared.base.mediastore;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import androidx.annotation.NonNull;

import java.util.Arrays;

/**
 * Bean for AudioColumns
 *
 * @see android.provider.MediaStore.Audio.AudioColumns
 * @see <a href="https://developer.android.google.cn/reference/android/provider/MediaStore.Audio.AudioColumns">MediaStore.Audio.AudioColumns</a>
 */
public class Audio extends Media {

    int albumId;
    /**
     * @deprecated
     */
    @Deprecated
    String albumKey;
    int artistId;
    /**
     * @deprecated
     */
    @Deprecated
    String artistKey;
    int bookmark;
    String genre;
    int genreId;
    /**
     * @deprecated
     */
    @Deprecated
    String genreKey;
    int isAlarm;
    int isAudiobook;
    int isMusic;
    int isNotification;
    int isPodcast;
    int isRingtone;
    /**
     * @deprecated
     */
    @Deprecated
    String titleKey;
    String titleResourceUri;
    int track;
    int year;

    public Audio() {
        super();
    }

    public Audio(@NonNull Cursor c) {
        super(c);
        try {
            this.albumId = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.albumKey = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_KEY));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.artistId = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.artistKey = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_KEY));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.bookmark = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.BOOKMARK));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.isAlarm = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_ALARM));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.isMusic = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_MUSIC));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.isNotification = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_NOTIFICATION));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.isPodcast = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_PODCAST));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.isRingtone = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_RINGTONE));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.titleKey = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE_KEY));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.track = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.year = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR));
        } catch (IllegalArgumentException ignored) {
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                this.genre = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.GENRE));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.genreId = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.GENRE_ID));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.genreKey = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.GENRE_KEY));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.isAudiobook = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_AUDIOBOOK));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                this.titleResourceUri = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE_RESOURCE_URI));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "Audio{" +
                "albumId=" + albumId +
                ", albumKey='" + albumKey + '\'' +
                ", artistId=" + artistId +
                ", artistKey='" + artistKey + '\'' +
                ", bookmark=" + bookmark +
                ", genre='" + genre + '\'' +
                ", genreId=" + genreId +
                ", genreKey='" + genreKey + '\'' +
                ", isAlarm=" + isAlarm +
                ", isAudiobook=" + isAudiobook +
                ", isMusic=" + isMusic +
                ", isNotification=" + isNotification +
                ", isPodcast=" + isPodcast +
                ", isRingtone=" + isRingtone +
                ", titleKey='" + titleKey + '\'' +
                ", titleResourceUri='" + titleResourceUri + '\'' +
                ", track=" + track +
                ", year=" + year +
                ", _id='" + _id + '\'' +
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

    public int getAlbumId() {
        return albumId;
    }

    public void setAlbumId(int albumId) {
        this.albumId = albumId;
    }

    @Deprecated
    public String getAlbumKey() {
        return albumKey;
    }

    @Deprecated
    public void setAlbumKey(String albumKey) {
        this.albumKey = albumKey;
    }

    public int getArtistId() {
        return artistId;
    }

    public void setArtistId(int artistId) {
        this.artistId = artistId;
    }

    @Deprecated
    public String getArtistKey() {
        return artistKey;
    }

    @Deprecated
    public void setArtistKey(String artistKey) {
        this.artistKey = artistKey;
    }

    public int getBookmark() {
        return bookmark;
    }

    public void setBookmark(int bookmark) {
        this.bookmark = bookmark;
    }

    @Override
    public String getGenre() {
        return genre;
    }

    @Override
    public void setGenre(String genre) {
        this.genre = genre;
    }

    public int getGenreId() {
        return genreId;
    }

    public void setGenreId(int genreId) {
        this.genreId = genreId;
    }

    @Deprecated
    public String getGenreKey() {
        return genreKey;
    }

    @Deprecated
    public void setGenreKey(String genreKey) {
        this.genreKey = genreKey;
    }

    public int isAlarm() {
        return isAlarm;
    }

    public void setAlarm(int isAlarm) {
        this.isAlarm = isAlarm;
    }

    public int isAudiobook() {
        return isAudiobook;
    }

    public void setAudiobook(int isAudiobook) {
        this.isAudiobook = isAudiobook;
    }

    public int isMusic() {
        return isMusic;
    }

    public void setMusic(int isMusic) {
        this.isMusic = isMusic;
    }

    public int isNotification() {
        return isNotification;
    }

    public void setNotification(int isNotification) {
        this.isNotification = isNotification;
    }

    public int isPodcast() {
        return isPodcast;
    }

    public void setPodcast(int isPodcast) {
        this.isPodcast = isPodcast;
    }

    public int isRingtone() {
        return isRingtone;
    }

    public void setRingtone(int isRingtone) {
        this.isRingtone = isRingtone;
    }

    @Deprecated
    public String getTitleKey() {
        return titleKey;
    }

    @Deprecated
    public void setTitleKey(String titleKey) {
        this.titleKey = titleKey;
    }

    public String getTitleResourceUri() {
        return titleResourceUri;
    }

    public void setTitleResourceUri(String titleResourceUri) {
        this.titleResourceUri = titleResourceUri;
    }

    public int getTrack() {
        return track;
    }

    public void setTrack(int track) {
        this.track = track;
    }

    @Override
    public int getYear() {
        return year;
    }

    @Override
    public void setYear(int year) {
        this.year = year;
    }

    public Uri getUri() {
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            uri = MediaStore.Audio.Media.getContentUri(getVolumeName(), getId());
        else
            uri = ContentUris.withAppendedId(MediaStore.Audio.Media.getContentUri(getVolumeName()), getId());
        return uri;
    }

    public Uri getAlbumUri() {
        return ContentUris.withAppendedId(MediaStore.Audio.Albums.getContentUri(getVolumeName()), getAlbumId());
    }
}