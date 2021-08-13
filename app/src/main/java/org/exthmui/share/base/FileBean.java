package org.exthmui.share.base;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.provider.MediaStore;

public class FileBean implements FileInfo {
    private Bitmap filePic;
    private String fileName;
    private String filePath;
    private int fileSize;
    private int fileAddedTime;
    private int fileModifiedTime;
    private int fileType;

    public FileBean(Cursor c) {
        this.filePath = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA));
        this.fileName = filePath.substring(filePath.lastIndexOf('/')+1);
        this.fileSize = c.getInt(c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE));
        this.fileAddedTime = c.getInt(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED));
        this.fileModifiedTime = c.getInt(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED));
    }

    public FileBean(Bitmap filePic, String fileName, String filePath, int fileAddedTime, int fileModifiedTime, int fileType) {
        this.filePic = filePic;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileAddedTime = fileAddedTime;
        this.fileModifiedTime = fileModifiedTime;
        this.fileType = fileType;
    }

    @Override
    public Bitmap getFilePic() {
        return filePic;
    }

    @Override
    public void setFilePic(Bitmap filePic) {
        this.filePic = filePic;
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

    public int getFileSize() {
        return fileSize;
    }

    public void setFileSize(int fileSize) {
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