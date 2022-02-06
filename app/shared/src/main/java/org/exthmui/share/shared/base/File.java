package org.exthmui.share.shared.base;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.provider.MediaStore;

import org.exthmui.share.shared.Constants;

public class File implements FileInfo {
    private Bitmap thumbnail=null;
    private String fileName=null;
    private String filePath=null;
    private long fileSize=-1;
    private int fileAddedTime=-1;
    private int fileModifiedTime=-1;
    private int fileType= Constants.FileTypes.UNKNOWN.getNumVal();


    public File(Cursor c) {
        this.filePath = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA));
        this.fileName = filePath.substring(filePath.lastIndexOf('/')+1);
        this.fileSize = c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE));
        this.fileAddedTime = c.getInt(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED));
        this.fileModifiedTime = c.getInt(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED));
    }

    public File(Bitmap thumbnail, String fileName, String filePath, int fileAddedTime, int fileModifiedTime, int fileType) {
        this.thumbnail = thumbnail;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileAddedTime = fileAddedTime;
        this.fileModifiedTime = fileModifiedTime;
        this.fileType = fileType;
    }

    @Override
    public Bitmap getThumbnail() {
        return thumbnail;
    }

    @Override
    public void setThumbnail(Bitmap thumbnail) {
        this.thumbnail = thumbnail;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public String getFilePath() {
        return filePath;
    }

    @Override
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public int getFileAddedTime() {
        return fileAddedTime;
    }

    public void setFileAddedTime(int fileAddedTime) {
        this.fileAddedTime = fileAddedTime;
    }

    @Override
    public int getFileModifiedTime() {
        return fileModifiedTime;
    }

    @Override
    public void setFileModifiedTime(int fileModifiedTime) {
        this.fileModifiedTime = fileModifiedTime;
    }

    @Override
    public int getFileType() {
        return fileType;
    }

    @Override
    public void setFileType(int fileType) {
        this.fileType = fileType;
    }
}