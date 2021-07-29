package org.exthmui.share.shared.base.trash;

import android.database.Cursor;
import android.os.Build;
import android.provider.MediaStore;

/**
 * Bean for AudioColumns
 * @see android.provider.MediaStore.Audio.AudioColumns
 * @see <a href="https://developer.android.google.cn/reference/android/provider/MediaStore.Audio.AudioColumns">MediaStore.Audio.AudioColumns</a>
 * */
public class Audio extends Media {

    private int album_id;
    /** @deprecated */
    @Deprecated
    private String album_key;
    private int artist_id;
    /** @deprecated */
    @Deprecated
    private String artist_key;
    private int bookmark;
    private String genre;
    private int genre_id;
    /** @deprecated */
    @Deprecated
    private String genre_key;
    private int is_alarm;
    private int is_audiobook;
    private int is_music;
    private int is_notification;
    private int is_podcast;
    private int is_ringtone;
    /** @deprecated */
    @Deprecated
    private String title_key;
    private String title_resource_uri;
    private int track;
    private int year;

    public Audio() {
        super();
    }

    public Audio(Cursor c){
        super(c);

        this.album_id = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
        this.album_key = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_KEY));
        this.artist_id = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID));
        this.artist_key = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_KEY));
        this.bookmark = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.BOOKMARK));
        this.is_alarm = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_ALARM));
        this.is_music = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_MUSIC));
        this.is_notification = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_NOTIFICATION));
        this.is_podcast = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_PODCAST));
        this.is_ringtone = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_RINGTONE));
        this.title_key = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE_KEY));
        this.track = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK));
        this.year = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR));
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            this.genre = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.GENRE));
            this.genre_id = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.GENRE_ID));
            this.genre_key = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.GENRE_KEY));
            this.is_audiobook = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_AUDIOBOOK));
            this.title_resource_uri = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE_RESOURCE_URI));
        }
    }

    public Audio(String album, String album_artist, String artist, String author, int bitrate, float capture_framerate, String cd_track_number, String compilation, String composer, String data, int date_added, int date_expires, int date_modified, int date_taken, String disc_number, String display_name, String document_id, int duration, int generation_added, int generation_modified, String genre, int height, String instance_id, int is_download, int is_drm, int is_favorite, int is_pending, int is_trashed, String mime_type, int num_tracks, int orientation, String original_document_id, String owner_package_name, String relative_path, String resolution, int size, String title, String volume_name, int width, String writer, byte[] xmp, int year, int album_id, String album_key, int artist_id, String artist_key, int bookmark, String genre1, int genre_id, String genre_key, int is_alarm, int is_audiobook, int is_music, int is_notification, int is_podcast, int is_ringtone, String title_key, String title_resource_uri, int track) {
        super(album, album_artist, artist, author, bitrate, capture_framerate, cd_track_number, compilation, composer, data, date_added, date_expires, date_modified, date_taken, disc_number, display_name, document_id, duration, generation_added, generation_modified, genre, height, instance_id, is_download, is_drm, is_favorite, is_pending, is_trashed, mime_type, num_tracks, orientation, original_document_id, owner_package_name, relative_path, resolution, size, title, volume_name, width, writer, xmp, year);
        this.album_id = album_id;
        this.album_key = album_key;
        this.artist_id = artist_id;
        this.artist_key = artist_key;
        this.bookmark = bookmark;
        this.genre = genre1;
        this.genre_id = genre_id;
        this.genre_key = genre_key;
        this.is_alarm = is_alarm;
        this.is_audiobook = is_audiobook;
        this.is_music = is_music;
        this.is_notification = is_notification;
        this.is_podcast = is_podcast;
        this.is_ringtone = is_ringtone;
        this.title_key = title_key;
        this.title_resource_uri = title_resource_uri;
        this.track = track;
        this.year = year;
    }

    public int getAlbum_id() {
        return album_id;
    }

    public void setAlbum_id(int album_id) {
        this.album_id = album_id;
    }

    @Deprecated
    public String getAlbum_key() {
        return album_key;
    }

    @Deprecated
    public void setAlbum_key(String album_key) {
        this.album_key = album_key;
    }

    public int getArtist_id() {
        return artist_id;
    }

    public void setArtist_id(int artist_id) {
        this.artist_id = artist_id;
    }

    @Deprecated
    public String getArtist_key() {
        return artist_key;
    }

    @Deprecated
    public void setArtist_key(String artist_key) {
        this.artist_key = artist_key;
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

    public int getGenre_id() {
        return genre_id;
    }

    public void setGenre_id(int genre_id) {
        this.genre_id = genre_id;
    }

    @Deprecated
    public String getGenre_key() {
        return genre_key;
    }

    @Deprecated
    public void setGenre_key(String genre_key) {
        this.genre_key = genre_key;
    }

    public int getIs_alarm() {
        return is_alarm;
    }

    public void setIs_alarm(int is_alarm) {
        this.is_alarm = is_alarm;
    }

    public int getIs_audiobook() {
        return is_audiobook;
    }

    public void setIs_audiobook(int is_audiobook) {
        this.is_audiobook = is_audiobook;
    }

    public int getIs_music() {
        return is_music;
    }

    public void setIs_music(int is_music) {
        this.is_music = is_music;
    }

    public int getIs_notification() {
        return is_notification;
    }

    public void setIs_notification(int is_notification) {
        this.is_notification = is_notification;
    }

    public int getIs_podcast() {
        return is_podcast;
    }

    public void setIs_podcast(int is_podcast) {
        this.is_podcast = is_podcast;
    }

    public int getIs_ringtone() {
        return is_ringtone;
    }

    public void setIs_ringtone(int is_ringtone) {
        this.is_ringtone = is_ringtone;
    }

    public String getTitle_key() {
        return title_key;
    }

    public void setTitle_key(String title_key) {
        this.title_key = title_key;
    }

    public String getTitle_resource_uri() {
        return title_resource_uri;
    }

    public void setTitle_resource_uri(String title_resource_uri) {
        this.title_resource_uri = title_resource_uri;
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
}