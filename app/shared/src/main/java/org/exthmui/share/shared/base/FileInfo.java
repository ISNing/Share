package org.exthmui.share.shared.base;

import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.HashMap;

public class FileInfo implements Serializable {
    @Nullable
    private String fileName;
    private long fileSize;
    private final HashMap<String, String> extras = new HashMap<>();

    public FileInfo() {

    }

    public FileInfo(Entity entity) {
        this.fileName = entity.getFileName();
        this.fileSize = entity.getFileSize();
    }

    @Nullable
    public String getFileName() {
        return fileName;
    }

    public void setFileName(@Nullable String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public void putExtra(String key, String value) {
        extras.put(key, value);
    }

    public String getExtra(String key) {
        return extras.get(key);
    }

    public HashMap<String, String> getExtras() {
        return extras;
    }
}
