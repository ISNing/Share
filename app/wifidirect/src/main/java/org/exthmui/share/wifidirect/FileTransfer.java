package org.exthmui.share.wifidirect;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;

public class FileTransfer implements Serializable {
    @Nullable private String fileName;
    private long fileSize;
    private String peerName;
    private String md5;
    @IntRange(from = 5000, to = 65535) private int clientPort;

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

    @NonNull
    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public int getClientPort() {
        return clientPort;
    }

    public void setClientPort(@IntRange(from = 5000, to=65535) int clientPort) {
        this.clientPort = clientPort;
    }

    public String getPeerName() {
        return peerName;
    }

    public void setPeerName(String peerName) {
        this.peerName = peerName;
    }
}
