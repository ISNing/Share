package org.exthmui.share.beans;

import android.database.Cursor;
import android.os.Build;
import android.provider.MediaStore;

public class VideoFile extends FileBean implements FileInfo {

    private float capture_framerate;
    private int height;
    private int width;
    private int duration;

    public VideoFile(Cursor c) {
        super(c);

        this.height = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.HEIGHT));
        this.width = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.WIDTH));
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            this.duration = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            this.capture_framerate = c.getFloat(c.getColumnIndexOrThrow(MediaStore.Audio.Media.CAPTURE_FRAMERATE));
        }
    }

    public float getCapture_framerate() {
        return capture_framerate;
    }

    public void setCapture_framerate(float capture_framerate) {
        this.capture_framerate = capture_framerate;
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
}