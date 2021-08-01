package org.exthmui.share.beans;

public interface PeerInfo {
    /**
     * @return The identity of this peer
     */
    String getId();

    /**
     * @return The display name of this peer which will be shown on user interface.
     */
    String getDisplayName();

    /**
     * @return The type of this peer. MUST be contained in {@link org.exthmui.share.misc.Constants.DeviceTypes}
     */
    int getDeviceType();

    /**
     * @return The type of the connection e.g."wilandirect, "msnearshare"...
     */
    String getConnectionType();

    /**
     * @return Whether this peer is paired/trusted
     */
    boolean isTrusted();

    /**TODO: define this in{@link org.exthmui.share.misc.Constants}
     * @return The status of connection with this peer. MUST be contained in {@link org.exthmui.share.misc.Constants.ConnectionStatus}
     */
    int getConnectionStatus();

    /**TODO: define this in{@link org.exthmui.share.misc.Constants}
     * @return The status of transmission. MUST be contained in {@link org.exthmui.share.misc.Constants.TransmissionStatus}
     */
    int getTransmissionStatus();

    /**
     * @return The detail message will be shown on user interface. Allowed to be customized by each type of Peer.
     */
    public String getDetailMessage();
}
