package org.exthmui.share.base.trash;

import android.database.Cursor;
import android.os.Build;
import android.provider.MediaStore;

/**
 * Bean for VideoColumns
 * @see android.provider.MediaStore.Video.VideoColumns
 * @see <a href="https://developer.android.google.cn/reference/android/provider/MediaStore.Video.VideoColumns">MediaStore.Video.VideoColumns</a>
 * */
public class Video extends Media{
    private int bookmark;
    private String category;
    private int color_range;
    private int color_standard;
    private int color_transfer;
    private String description;
    private int isprivate;
    private String language;
    /** @deprecated */
    @Deprecated
    private float latitude;
    /** @deprecated */
    @Deprecated
    private float longitude;
    /** @deprecated */
    @Deprecated
    private int mini_thumb_magic;
    private String tags;

    public Video(){
        super();
    }
    public Video(Cursor c) {
        super(c);

        this.bookmark = c.getInt(c.getColumnIndexOrThrow(MediaStore.Video.Media.BOOKMARK));
        this.category = c.getString(c.getColumnIndexOrThrow(MediaStore.Video.Media.CATEGORY));
        this.description = c.getString(c.getColumnIndexOrThrow(MediaStore.Video.Media.DESCRIPTION));
        this.isprivate = c.getInt(c.getColumnIndexOrThrow(MediaStore.Video.Media.IS_PRIVATE));
        this.language = c.getString(c.getColumnIndexOrThrow(MediaStore.Video.Media.LANGUAGE));
        this.latitude = c.getFloat(c.getColumnIndexOrThrow(MediaStore.Video.Media.LATITUDE));
        this.longitude = c.getFloat(c.getColumnIndexOrThrow(MediaStore.Video.Media.LONGITUDE));
        this.mini_thumb_magic = c.getInt(c.getColumnIndexOrThrow(MediaStore.Video.Media.MINI_THUMB_MAGIC));
        this.tags = c.getString(c.getColumnIndexOrThrow(MediaStore.Video.Media.TAGS));

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            this.color_range = c.getInt(c.getColumnIndexOrThrow(MediaStore.Video.Media.COLOR_RANGE));
            this.color_standard = c.getInt(c.getColumnIndexOrThrow(MediaStore.Video.Media.COLOR_STANDARD));
            this.color_transfer = c.getInt(c.getColumnIndexOrThrow(MediaStore.Video.Media.COLOR_TRANSFER));
        }
    }

    public Video(String album, String album_artist, String artist, String author, int bitrate, float capture_framerate, String cd_track_number, String compilation, String composer, String data, int date_added, int date_expires, int date_modified, int date_taken, String disc_number, String display_name, String document_id, int duration, int generation_added, int generation_modified, String genre, int height, String instance_id, int is_download, int is_drm, int is_favorite, int is_pending, int is_trashed, String mime_type, int num_tracks, int orientation, String original_document_id, String owner_package_name, String relative_path, String resolution, int size, String title, String volume_name, int width, String writer, byte[] xmp, int year, int bookmark, String category, int color_range, int color_standard, int color_transfer, String description, int isprivate, String language, float latitude, float longitude, int mini_thumb_magic, String tags) {
        super(album, album_artist, artist, author, bitrate, capture_framerate, cd_track_number, compilation, composer, data, date_added, date_expires, date_modified, date_taken, disc_number, display_name, document_id, duration, generation_added, generation_modified, genre, height, instance_id, is_download, is_drm, is_favorite, is_pending, is_trashed, mime_type, num_tracks, orientation, original_document_id, owner_package_name, relative_path, resolution, size, title, volume_name, width, writer, xmp, year);
        this.bookmark = bookmark;
        this.category = category;
        this.color_range = color_range;
        this.color_standard = color_standard;
        this.color_transfer = color_transfer;
        this.description = description;
        this.isprivate = isprivate;
        this.language = language;
        this.latitude = latitude;
        this.longitude = longitude;
        this.mini_thumb_magic = mini_thumb_magic;
        this.tags = tags;
    }

    @Override
    public String toString() {
        return "Video{" +
                "bookmark=" + bookmark +
                ", category='" + category + '\'' +
                ", color_range=" + color_range +
                ", color_standard=" + color_standard +
                ", color_transfer=" + color_transfer +
                ", description='" + description + '\'' +
                ", isprivate=" + isprivate +
                ", language='" + language + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", mini_thumb_magic=" + mini_thumb_magic +
                ", tags='" + tags + '\'' +
                "} " + super.toString();
    }

    public int getBookmark() {
        return bookmark;
    }

    public void setBookmark(int bookmark) {
        this.bookmark = bookmark;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getColor_range() {
        return color_range;
    }

    public void setColor_range(int color_range) {
        this.color_range = color_range;
    }

    public int getColor_standard() {
        return color_standard;
    }

    public void setColor_standard(int color_standard) {
        this.color_standard = color_standard;
    }

    public int getColor_transfer() {
        return color_transfer;
    }

    public void setColor_transfer(int color_transfer) {
        this.color_transfer = color_transfer;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getIsprivate() {
        return isprivate;
    }

    public void setIsprivate(int isprivate) {
        this.isprivate = isprivate;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public float getLatitude() {
        return latitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public int getMini_thumb_magic() {
        return mini_thumb_magic;
    }

    public void setMini_thumb_magic(int mini_thumb_magic) {
        this.mini_thumb_magic = mini_thumb_magic;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }
}