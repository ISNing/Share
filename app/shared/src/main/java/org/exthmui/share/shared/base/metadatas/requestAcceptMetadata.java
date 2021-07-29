package org.exthmui.share.shared.base.metadatas;

import androidx.annotation.Nullable;

public class requestAcceptMetadata {
    @Nullable private final String fileName;
    private final long fileSize;
    private final String sourcePeerName;

    public requestAcceptMetadata(@Nullable String fileName, long fileSize, String sourcePeerName) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.sourcePeerName = sourcePeerName;
    }

    public boolean isFileNameKnown() {
        return fileName == null;
    }

    @Nullable
    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getSourcePeerName() {
        return sourcePeerName;
    }
}
