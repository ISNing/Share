package org.exthmui.share.shared.base.file;

import androidx.annotation.NonNull;

import org.exthmui.share.shared.base.mediastore.Image;

public class ImageFile extends File {
    private String description;
    private String exposureTime;
    private String fNumber;
    private int iso;

    public ImageFile() {
    }

    public ImageFile(@NonNull Image image) {
        super(image);

        this.description = image.getDescription();
        this.exposureTime = image.getExposureTime();
        this.fNumber = image.getfNumber();
        this.iso = image.getIso();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExposureTime() {
        return exposureTime;
    }

    public void setExposureTime(String exposureTime) {
        this.exposureTime = exposureTime;
    }

    public String getfNumber() {
        return fNumber;
    }

    public void setfNumber(String fNumber) {
        this.fNumber = fNumber;
    }

    public int getIso() {
        return iso;
    }

    public void setIso(int iso) {
        this.iso = iso;
    }
}
