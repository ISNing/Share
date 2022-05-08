package org.exthmui.share.shared.base.mediastore;

import android.database.Cursor;
import android.os.Build;
import android.provider.MediaStore;

import androidx.annotation.NonNull;

public class Album extends Base {
    String album;
    /**
     * @deprecated
     */
    @Deprecated
    String albumArt;
    int albumId;
    /**
     * @deprecated
     */
    @Deprecated
    String albumKey;
    String artist;
    int artistId;
    /**
     * @deprecated
     */
    @Deprecated
    String artistKey;
    int firstYear;
    int lastYear;
    int numberOfSongs;
    int numberOfSongsForArtist;

    public Album() {
    }

    public Album(@NonNull Cursor c) {
        super(c);
        try {
            this.album = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AlbumColumns.ALBUM));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.albumArt = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AlbumColumns.ALBUM_ART));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.albumId = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.AlbumColumns.ALBUM_ID));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.albumKey = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AlbumColumns.ALBUM_KEY));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.artist = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AlbumColumns.ARTIST));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.firstYear = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.AlbumColumns.FIRST_YEAR));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.lastYear = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.AlbumColumns.LAST_YEAR));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.numberOfSongs = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.AlbumColumns.NUMBER_OF_SONGS));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.numberOfSongsForArtist = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.AlbumColumns.NUMBER_OF_SONGS_FOR_ARTIST));
        } catch (IllegalArgumentException ignored) {
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                this.artistId = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.AlbumColumns.ARTIST_ID));
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                this.artistKey = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AlbumColumns.ARTIST_KEY));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "Album{" +
                "album='" + album + '\'' +
                ", albumArt='" + albumArt + '\'' +
                ", albumId=" + albumId +
                ", albumKey='" + albumKey + '\'' +
                ", artist='" + artist + '\'' +
                ", artistId=" + artistId +
                ", artistKey='" + artistKey + '\'' +
                ", firstYear=" + firstYear +
                ", lastYear=" + lastYear +
                ", numberOfSongs=" + numberOfSongs +
                ", numberOfSongsForArtist=" + numberOfSongsForArtist +
                ", _id='" + _id + '\'' +
                ", _count='" + _count + '\'' +
                '}';
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    @Deprecated
    public String getAlbumArt() {
        return albumArt;
    }

    @Deprecated
    public void setAlbumArt(String albumArt) {
        this.albumArt = albumArt;
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

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public int getArtistId() {
        return artistId;
    }

    public void setArtistId(int artistId) {
        this.artistId = artistId;
    }

    public String getArtistKey() {
        return artistKey;
    }

    public void setArtistKey(String artistKey) {
        this.artistKey = artistKey;
    }

    public int getFirstYear() {
        return firstYear;
    }

    public void setFirstYear(int firstYear) {
        this.firstYear = firstYear;
    }

    public int getLastYear() {
        return lastYear;
    }

    public void setLastYear(int lastYear) {
        this.lastYear = lastYear;
    }

    public int getNumberOfSongs() {
        return numberOfSongs;
    }

    public void setNumberOfSongs(int numberOfSongs) {
        this.numberOfSongs = numberOfSongs;
    }

    public int getNumberOfSongsForArtist() {
        return numberOfSongsForArtist;
    }

    public void setNumberOfSongsForArtist(int numberOfSongsForArtist) {
        this.numberOfSongsForArtist = numberOfSongsForArtist;
    }
}
