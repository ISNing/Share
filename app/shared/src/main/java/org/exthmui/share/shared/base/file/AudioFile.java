package org.exthmui.share.shared.base.file;

import android.database.Cursor;
import android.os.Build;
import android.provider.MediaStore;

public class AudioFile extends File implements FileInfo {

    private String album;
    private String album_artist;
    private String artist;
    private String author;
    private int bitrate;
    private String display_name;
    private int duration;
    private String title;

    public AudioFile(Cursor c) {
        super(c);
        this.display_name = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME));
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            this.duration = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            this.album = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
            this.album_artist = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ARTIST));
            this.artist = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
            this.author = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.AUTHOR));
            this.bitrate = c.getInt(c.getColumnIndexOrThrow(MediaStore.MediaColumns.BITRATE));
        }
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getAlbum_artist() {
        return album_artist;
    }

    public void setAlbum_artist(String album_artist) {
        this.album_artist = album_artist;
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

    public String getDisplay_name() {
        return display_name;
    }

    public void setDisplay_name(String display_name) {
        this.display_name = display_name;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
