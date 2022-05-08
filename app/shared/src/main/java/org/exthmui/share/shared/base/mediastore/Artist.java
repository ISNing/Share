package org.exthmui.share.shared.base.mediastore;

import android.database.Cursor;
import android.provider.MediaStore;

import androidx.annotation.NonNull;

public class Artist extends Base {
    String artist;
    /**
     * @deprecated
     */
    @Deprecated
    String artistKey;
    int numberOfAlbums;
    int numberOfTracks;

    public Artist() {
    }

    public Artist(@NonNull Cursor c) {
        super(c);
        try {
            this.artist = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.ArtistColumns.ARTIST));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.artistKey = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.ArtistColumns.ARTIST_KEY));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.numberOfAlbums = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.ArtistColumns.NUMBER_OF_ALBUMS));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.numberOfTracks = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.ArtistColumns.NUMBER_OF_TRACKS));
        } catch (IllegalArgumentException ignored) {
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "Artist{" +
                "artist='" + artist + '\'' +
                ", artistKey='" + artistKey + '\'' +
                ", numberOfAlbums=" + numberOfAlbums +
                ", numberOfTracks=" + numberOfTracks +
                ", _id='" + _id + '\'' +
                ", _count='" + _count + '\'' +
                '}';
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    @Deprecated
    public String getArtistKey() {
        return artistKey;
    }

    @Deprecated
    public void setArtistKey(String artistKey) {
        this.artistKey = artistKey;
    }

    public int getNumberOfAlbums() {
        return numberOfAlbums;
    }

    public void setNumberOfAlbums(int numberOfAlbums) {
        this.numberOfAlbums = numberOfAlbums;
    }

    public int getNumberOfTracks() {
        return numberOfTracks;
    }

    public void setNumberOfTracks(int numberOfTracks) {
        this.numberOfTracks = numberOfTracks;
    }
}
