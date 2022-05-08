package org.exthmui.share.shared.base.file;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exthmui.share.shared.base.mediastore.Audio;

public class AudioFile extends File {

    @Nullable
    private String album;
    @Nullable
    private String artist;
    @Nullable
    private String author;
    @Nullable
    private String composer;
    private int bitrate;
    @Nullable
    private String displayName;
    private int duration;

    public AudioFile() {
    }

    public AudioFile(@NonNull Audio audio) {
        super(audio);
        this.displayName = audio.getDisplayName();
        this.duration = audio.getDuration();
        this.album = audio.getAlbum();
        this.artist = audio.getArtist();
        this.composer = audio.getComposer();
        this.author = audio.getAuthor();
        this.bitrate = audio.getBitrate();
    }

    @Nullable
    public String getAlbum() {
        return album;
    }

    public void setAlbum(@Nullable String album) {
        this.album = album;
    }

    @Nullable
    public String getArtist() {
        return artist;
    }

    public void setArtist(@Nullable String artist) {
        this.artist = artist;
    }

    @Nullable
    public String getAuthor() {
        return author;
    }

    public void setAuthor(@Nullable String author) {
        this.author = author;
    }

    public int getBitrate() {
        return bitrate;
    }

    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }

    @Nullable
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(@Nullable String displayName) {
        this.displayName = displayName;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    @Nullable
    public String getComposer() {
        return composer;
    }

    public void setComposer(@Nullable String composer) {
        this.composer = composer;
    }
}
