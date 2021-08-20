package org.exthmui.share.base;

import androidx.work.WorkManager;

public class Peer implements PeerInfo{
    private String id;
    private String displayName;
    private int deviceType;
    private String connectionType;
    private boolean trusted;
    private int connectionStatus;
    private int transmissionStatus;
    private String detailMessage;


    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public int getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(int deviceType) {
        this.deviceType = deviceType;
    }

    @Override
    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }

    @Override
    public boolean isTrusted() {
        return trusted;
    }

    public void setTrusted(boolean trusted) {
        this.trusted = trusted;
    }

    @Override
    public int getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(int connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    @Override
    public int getTransmissionStatus() {
        return transmissionStatus;
    }

    public void setTransmissionStatus(int transmissionStatus) {
        this.transmissionStatus = transmissionStatus;
    }

    @Override
    public String getDetailMessage() {
        return detailMessage;
    }

    public void setDetailMessage(String detailMessage) {
        this.detailMessage = detailMessage;
    }
}
