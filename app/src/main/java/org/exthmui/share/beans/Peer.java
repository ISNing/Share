package org.exthmui.share.beans;

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

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public int getDeviceType() {
        return deviceType;
    }

    @Override
    public void setDeviceType(int deviceType) {
        this.deviceType = deviceType;
    }

    @Override
    public String getConnectionType() {
        return connectionType;
    }

    @Override
    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }

    @Override
    public boolean isTrusted() {
        return trusted;
    }

    @Override
    public void setTrusted(boolean trusted) {
        this.trusted = trusted;
    }

    @Override
    public int getConnectionStatus() {
        return connectionStatus;
    }

    @Override
    public void setConnectionStatus(int connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    @Override
    public int getTransmissionStatus() {
        return transmissionStatus;
    }

    @Override
    public void setTransmissionStatus(int transmissionStatus) {
        this.transmissionStatus = transmissionStatus;
    }

    @Override
    public String getDetailMessage() {
        return detailMessage;
    }

    @Override
    public void setDetailMessage(String detailMessage) {
        this.detailMessage = detailMessage;
    }
}
