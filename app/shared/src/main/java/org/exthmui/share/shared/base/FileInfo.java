package org.exthmui.share.shared.base;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.UUID;

public class FileInfo implements Serializable {
    @Nullable
    private String fileName;
    private long fileSize;
    private String id = UUID.randomUUID().toString();
    private final HashMap<String, String> extras = new HashMap<>();

    public FileInfo() {

    }

    public FileInfo(@NonNull Entity entity) {
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void putExtra(String key, String value) {
        extras.put(key, value);
    }

    @Nullable
    public String getExtra(String key) {
        return extras.get(key);
    }

    @NonNull
    public HashMap<String, String> getExtras() {
        return extras;
    }
}
