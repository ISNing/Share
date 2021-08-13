package org.exthmui.share.base;

import android.graphics.Bitmap;

public interface FileInfo {
    public Bitmap getFilePic();

    public void setFilePic(Bitmap filePic);

    public String getFileName();

    public void setFileName(String fileName);

    public String getFilePath();

    public void setFilePath(String filePath);

    public int getFileModifiedTime();

    public void setFileModifiedTime(int fileModifiedTime);

    public int getFileType();

    public void setFileType(int fileType);
}