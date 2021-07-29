package org.exthmui.share.beans;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;

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