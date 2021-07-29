package org.exthmui.share.beans;

public interface PeerInfo {
    String getId();

    void setId(String id);

    String getDisplayName();

    void setDisplayName(String displayName);

    int getDeviceType();

    void setDeviceType(int deviceType);

    String getConnectionType();

    void setConnectionType(String connectionType);

    boolean isTrusted();

    void setTrusted(boolean trusted);

    int getConnectionStatus();

    void setConnectionStatus(int connectionStatus);

    int getTransmissionStatus();

    void setTransmissionStatus(int transmissionStatus);

    public String getDetailMessage();

    public void setDetailMessage(String detailMessage);
}
