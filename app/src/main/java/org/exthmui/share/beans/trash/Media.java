package org.exthmui.share.beans.trash;

import android.database.Cursor;
import android.os.Build;
import android.provider.MediaStore;
import androidx.annotation.NonNull;

import java.util.Arrays;

/**
 * Bean for MediaColumns
 * @see android.provider.MediaStore.MediaColumns
 * @see <a href="https://developer.android.google.cn/reference/android/provider/MediaStore.MediaColumns">MediaStore.MediaColumns</a>
 * */
public class Media {
    private String album;
    private String album_artist;
    private String artist;
    private String author;
    private int bitrate;
    private float capture_framerate;
    private String cd_track_number;
    private String compilation;
    private String composer;
    /** @deprecated */
    @Deprecated
    private String data;
    private int date_added;
    private int date_expires;
    private int date_modified;
    private int date_taken;
    private String disc_number;
    private String display_name;
    private String document_id;
    private int duration;
    private int generation_added;
    private int generation_modified;
    private String genre;
    private int height;
    private String instance_id;
    private int is_download;
    private int is_drm;
    private int is_favorite;
    private int is_pending;
    private int is_trashed;
    private String mime_type;
    private int num_tracks;
    private int orientation;
    private String original_document_id;
    private String owner_package_name;
    private String relative_path;
    private String resolution;
    private int size;
    private String title;
    private String volume_name;
    private int width;
    private String writer;
    private byte[] xmp;
    private int year;

    public Media() {}

    public Media(@NonNull Cursor c){
        this.data = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
        this.date_added = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED));
        this.date_modified = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED));
        this.display_name = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME));
        this.height = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.HEIGHT));
        this.mime_type = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE));
        this.size = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE));
        this.title = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
        this.width = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.WIDTH));
        this.year = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR));
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            this.instance_id = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.INSTANCE_ID));
            this.is_pending = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_PENDING));
            this.document_id = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DOCUMENT_ID));
            this.duration = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
            this.date_taken = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_TAKEN));
            this.date_expires = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_EXPIRES));
            this.orientation = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ORIENTATION));
            this.original_document_id = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ORIGINAL_DOCUMENT_ID));
            this.owner_package_name = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.OWNER_PACKAGE_NAME));
            this.relative_path = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH));
            this.volume_name = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.VOLUME_NAME));
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            this.disc_number = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISC_NUMBER));
            this.generation_added = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.GENERATION_ADDED));
            this.generation_modified = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.GENERATION_MODIFIED));
            this.genre = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.GENRE));
            this.is_download = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_DOWNLOAD));
            this.is_drm = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_DRM));
            this.is_favorite = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_FAVORITE));
            this.is_trashed = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_TRASHED));
            this.album = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
            this.album_artist = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ARTIST));
            this.artist = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
            this.author = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.AUTHOR));
            this.bitrate = c.getInt(c.getColumnIndexOrThrow(MediaStore.MediaColumns.BITRATE));
            this.capture_framerate = c.getFloat(c.getColumnIndexOrThrow(MediaStore.Audio.Media.CAPTURE_FRAMERATE));
            this.cd_track_number = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.CD_TRACK_NUMBER));
            this.compilation = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.COMPILATION));
            this.composer = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.COMPOSER));
            this.num_tracks = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.NUM_TRACKS));
            this.resolution = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.RESOLUTION));
            this.writer = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.WRITER));
            this.xmp = c.getBlob(c.getColumnIndexOrThrow(MediaStore.Audio.Media.XMP));
        }
    }

    public Media(String album, String album_artist, String artist, String author, int bitrate, float capture_framerate, String cd_track_number, String compilation, String composer, String data, int date_added, int date_expires, int date_modified, int date_taken, String disc_number, String display_name, String document_id, int duration, int generation_added, int generation_modified, String genre, int height, String instance_id, int is_download, int is_drm, int is_favorite, int is_pending, int is_trashed, String mime_type, int num_tracks, int orientation, String original_document_id, String owner_package_name, String relative_path, String resolution, int size, String title, String volume_name, int width, String writer, byte[] xmp, int year) {
        this.album = album;
        this.album_artist = album_artist;
        this.artist = artist;
        this.author = author;
        this.bitrate = bitrate;
        this.capture_framerate = capture_framerate;
        this.cd_track_number = cd_track_number;
        this.compilation = compilation;
        this.composer = composer;
        this.data = data;
        this.date_added = date_added;
        this.date_expires = date_expires;
        this.date_modified = date_modified;
        this.date_taken = date_taken;
        this.disc_number = disc_number;
        this.display_name = display_name;
        this.document_id = document_id;
        this.duration = duration;
        this.generation_added = generation_added;
        this.generation_modified = generation_modified;
        this.genre = genre;
        this.height = height;
        this.instance_id = instance_id;
        this.is_download = is_download;
        this.is_drm = is_drm;
        this.is_favorite = is_favorite;
        this.is_pending = is_pending;
        this.is_trashed = is_trashed;
        this.mime_type = mime_type;
        this.num_tracks = num_tracks;
        this.orientation = orientation;
        this.original_document_id = original_document_id;
        this.owner_package_name = owner_package_name;
        this.relative_path = relative_path;
        this.resolution = resolution;
        this.size = size;
        this.title = title;
        this.volume_name = volume_name;
        this.width = width;
        this.writer = writer;
        this.xmp = xmp;
        this.year = year;
    }

    @Override
    public String toString() {
        return "Media{" +
                "album='" + album + '\'' +
                ", album_artist='" + album_artist + '\'' +
                ", artist='" + artist + '\'' +
                ", author='" + author + '\'' +
                ", bitrate=" + bitrate +
                ", capture_framerate=" + capture_framerate +
                ", cd_track_number='" + cd_track_number + '\'' +
                ", compilation='" + compilation + '\'' +
                ", composer='" + composer + '\'' +
                ", data='" + data + '\'' +
                ", date_added=" + date_added +
                ", date_expires=" + date_expires +
                ", date_modified=" + date_modified +
                ", date_taken=" + date_taken +
                ", disc_number='" + disc_number + '\'' +
                ", display_name='" + display_name + '\'' +
                ", document_id='" + document_id + '\'' +
                ", duration=" + duration +
                ", generation_added=" + generation_added +
                ", generation_modified=" + generation_modified +
                ", genre='" + genre + '\'' +
                ", height=" + height +
                ", instance_id='" + instance_id + '\'' +
                ", is_download=" + is_download +
                ", is_drm=" + is_drm +
                ", is_favorite=" + is_favorite +
                ", is_pending=" + is_pending +
                ", is_trashed=" + is_trashed +
                ", mime_type='" + mime_type + '\'' +
                ", num_tracks=" + num_tracks +
                ", orientation=" + orientation +
                ", original_document_id='" + original_document_id + '\'' +
                ", owner_package_name='" + owner_package_name + '\'' +
                ", relative_path='" + relative_path + '\'' +
                ", resolution='" + resolution + '\'' +
                ", size=" + size +
                ", title='" + title + '\'' +
                ", volume_name='" + volume_name + '\'' +
                ", width=" + width +
                ", writer='" + writer + '\'' +
                ", xmp=" + Arrays.toString(xmp) +
                ", year='" + year + '\'' +
                '}';
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

    public float getCapture_framerate() {
        return capture_framerate;
    }

    public void setCapture_framerate(float capture_framerate) {
        this.capture_framerate = capture_framerate;
    }

    public String getCd_track_number() {
        return cd_track_number;
    }

    public void setCd_track_number(String cd_track_number) {
        this.cd_track_number = cd_track_number;
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

    public int getDate_added() {
        return date_added;
    }

    public void setDate_added(int date_added) {
        this.date_added = date_added;
    }

    public int getDate_expires() {
        return date_expires;
    }

    public void setDate_expires(int date_expires) {
        this.date_expires = date_expires;
    }

    public int getDate_modified() {
        return date_modified;
    }

    public void setDate_modified(int date_modified) {
        this.date_modified = date_modified;
    }

    public int getDate_taken() {
        return date_taken;
    }

    public void setDate_taken(int date_taken) {
        this.date_taken = date_taken;
    }

    public String getDisc_number() {
        return disc_number;
    }

    public void setDisc_number(String disc_number) {
        this.disc_number = disc_number;
    }

    public String getDisplay_name() {
        return display_name;
    }

    public void setDisplay_name(String display_name) {
        this.display_name = display_name;
    }

    public String getDocument_id() {
        return document_id;
    }

    public void setDocument_id(String document_id) {
        this.document_id = document_id;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getGeneration_added() {
        return generation_added;
    }

    public void setGeneration_added(int generation_added) {
        this.generation_added = generation_added;
    }

    public int getGeneration_modified() {
        return generation_modified;
    }

    public void setGeneration_modified(int generation_modified) {
        this.generation_modified = generation_modified;
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

    public String getInstance_id() {
        return instance_id;
    }

    public void setInstance_id(String instance_id) {
        this.instance_id = instance_id;
    }

    public int getIs_download() {
        return is_download;
    }

    public void setIs_download(int is_download) {
        this.is_download = is_download;
    }

    public int getIs_drm() {
        return is_drm;
    }

    public void setIs_drm(int is_drm) {
        this.is_drm = is_drm;
    }

    public int getIs_favorite() {
        return is_favorite;
    }

    public void setIs_favorite(int is_favorite) {
        this.is_favorite = is_favorite;
    }

    public int getIs_pending() {
        return is_pending;
    }

    public void setIs_pending(int is_pending) {
        this.is_pending = is_pending;
    }

    public int getIs_trashed() {
        return is_trashed;
    }

    public void setIs_trashed(int is_trashed) {
        this.is_trashed = is_trashed;
    }

    public String getMime_type() {
        return mime_type;
    }

    public void setMime_type(String mime_type) {
        this.mime_type = mime_type;
    }

    public int getNum_tracks() {
        return num_tracks;
    }

    public void setNum_tracks(int num_tracks) {
        this.num_tracks = num_tracks;
    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(int orientation) {
        this.orientation = orientation;
    }

    public String getOriginal_document_id() {
        return original_document_id;
    }

    public void setOriginal_document_id(String original_document_id) {
        this.original_document_id = original_document_id;
    }

    public String getOwner_package_name() {
        return owner_package_name;
    }

    public void setOwner_package_name(String owner_package_name) {
        this.owner_package_name = owner_package_name;
    }

    public String getRelative_path() {
        return relative_path;
    }

    public void setRelative_path(String relative_path) {
        this.relative_path = relative_path;
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

    public String getVolume_name() {
        return volume_name;
    }

    public void setVolume_name(String volume_name) {
        this.volume_name = volume_name;
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
