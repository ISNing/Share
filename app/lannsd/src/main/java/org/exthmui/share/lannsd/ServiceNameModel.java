package org.exthmui.share.lannsd;

import com.google.gson.annotations.SerializedName;

public class ServiceNameModel {
    @SerializedName("a")
    private int deviceType;
    @SerializedName("b")
    private String displayName;
    @SerializedName("c")
    private String peerId;

    public int getDeviceType() {
        return deviceType;
    }

    public ServiceNameModel setDeviceType(int deviceType) {
        this.deviceType = deviceType;
        return this;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ServiceNameModel setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public String getPeerId() {
        return peerId;
    }

    public ServiceNameModel setPeerId(String peerId) {
        this.peerId = peerId;
        return this;
    }
}
