package org.exthmui.share.shared.base.file;

import android.graphics.Bitmap;

public interface FileInfo {
    Bitmap getThumbnail();

    void setThumbnail(Bitmap thumbnail);

    String getFileName();

    void setFileName(String fileName);

    String getFilePath();

    void setFilePath(String filePath);

    int getFileModifiedTime();

    void setFileModifiedTime(int fileModifiedTime);

    int getFileType();

    void setFileType(int fileType);
}