package org.exthmui.share.shared.base.file;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exthmui.share.shared.base.mediastore.Video;

public class VideoFile extends File {

    private int height;
    private int width;
    private int duration;
    private float captureFramerate;
    private int bitrate;
    @Nullable
    private String category;
    @Nullable
    private String description;
    @Nullable
    private String language;
    @Nullable
    private String tags;

    public VideoFile() {
    }

    public VideoFile(@NonNull Video video) {
        super(video);

        this.height = video.getHeight();
        this.width = video.getWidth();
        this.duration = video.getDuration();
        this.captureFramerate = video.getCaptureFrameRate();
        this.bitrate = video.getBitrate();
        this.category = video.getCategory();
        this.description = video.getDescription();
        this.language = video.getLanguage();
        this.tags = video.getTags();
    }

    public float getCaptureFramerate() {
        return captureFramerate;
    }

    public void setCaptureFramerate(float captureFramerate) {
        this.captureFramerate = captureFramerate;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getBitrate() {
        return bitrate;
    }

    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }

    @Nullable
    public String getCategory() {
        return category;
    }

    public void setCategory(@Nullable String category) {
        this.category = category;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    @Nullable
    public String getLanguage() {
        return language;
    }

    public void setLanguage(@Nullable String language) {
        this.language = language;
    }

    @Nullable
    public String getTags() {
        return tags;
    }

    public void setTags(@Nullable String tags) {
        this.tags = tags;
    }
}