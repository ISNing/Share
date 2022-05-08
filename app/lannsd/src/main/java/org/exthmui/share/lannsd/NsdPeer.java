package org.exthmui.share.lannsd;

import static org.exthmui.share.lannsd.Constants.RECORD_KEY_ACCOUNT_SERVER_SIGN;
import static org.exthmui.share.lannsd.Constants.RECORD_KEY_SERVER_PORT;
import static org.exthmui.share.lannsd.Constants.RECORD_KEY_SHARE_PROTOCOL_VERSION;
import static org.exthmui.share.lannsd.Constants.RECORD_KEY_UID;
import static org.exthmui.share.lannsd.Constants.SHARE_PROTOCOL_VERSION_1;

import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;
import org.exthmui.share.shared.base.Peer;
import org.exthmui.share.shared.misc.IConnectionType;

import java.net.InetAddress;
import java.util.Map;
import java.util.Objects;

public class NsdPeer extends Peer {

    public static final String TAG = "NsdPeer";

    @NonNull
    private NsdServiceInfo nsdServiceInfo;
    private int connectionStatus;
    private int transmissionStatus;
    private final int deviceType;
    @IntRange(from = 5001, to = 65535)
    private int serverPort;
    private String protocolVersion;
    @NonNull
    private String peerId;
    @NonNull
    private String displayName;
    @IntRange(from = 0)
    private int uid;
    private String accountServerSign;

    private boolean attributesLoaded;

    public NsdPeer(@NonNull NsdServiceInfo nsdServiceInfo, int deviceType,
                   @NonNull String peerId,
                   @NonNull String displayName) {
        this.nsdServiceInfo = nsdServiceInfo;
        this.deviceType = deviceType;
        this.peerId = peerId;
        this.displayName = displayName;
    }

    @NonNull
    @Override
    public String getId() {
        return NsdUtils.genNsdId(peerId);
    }

    public void setId(@NonNull String peerId) {
        this.peerId = peerId;
    }

    @NonNull
    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public int getDeviceType() {
        return deviceType;
    }

    @NonNull
    @Override
    public IConnectionType getConnectionType() {
        return new Metadata();
    }

    @Override
    public boolean isTrusted() {
        return false;
    }

    @Override
    public int getConnectionStatus() {
        return connectionStatus;
    }

    @Override
    public int getTransmissionStatus() {
        return transmissionStatus;
    }

    @Override
    public String getDetailMessage() {
        return String.format("IP: %1$s\n", nsdServiceInfo.getHost());
    }

    @NonNull
    public NsdServiceInfo getNsdServiceInfo() {
        return nsdServiceInfo;
    }

    public void setNsdServiceInfo(@NonNull NsdServiceInfo nsdServiceInfo) {
        this.nsdServiceInfo = nsdServiceInfo;
    }

    @IntRange(from = 5001, to = 65535)
    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(@IntRange(from = 5001, to = 65535) int serverPort) {
        this.serverPort = serverPort;
    }

    @NonNull
    @Override
    public String getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(@NonNull String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    @Override
    @IntRange(from = 0)
    public int getUid() {
        return uid;
    }

    public void setUid(@IntRange(from = 5001, to = 65535) int uid) {
        this.uid = uid;
    }

    @NonNull
    @Override
    public String getAccountServerSign() {
        return accountServerSign;
    }

    public void setAccountServerSign(@NonNull String accountServerSign) {
        this.accountServerSign = accountServerSign;
    }

    public InetAddress getAddress() {
        return nsdServiceInfo.getHost();
    }

    public void setDisplayName(@NonNull String displayName) {
        this.displayName = displayName;
    }

    public boolean isAttributesLoaded() {
        return attributesLoaded;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NsdPeer)) return false;
        NsdPeer nsdPeer = (NsdPeer) o;
        return getConnectionStatus() == nsdPeer.getConnectionStatus() && getTransmissionStatus() == nsdPeer.getTransmissionStatus() && getDeviceType() == nsdPeer.getDeviceType() && getServerPort() == nsdPeer.getServerPort() && getUid() == nsdPeer.getUid() && getNsdServiceInfo().equals(nsdPeer.getNsdServiceInfo()) && getProtocolVersion().equals(nsdPeer.getProtocolVersion()) && peerId.equals(nsdPeer.peerId) && getAccountServerSign().equals(nsdPeer.getAccountServerSign());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getNsdServiceInfo(), getConnectionStatus(), getTransmissionStatus(), getDeviceType(), getServerPort(), getProtocolVersion(), peerId, getUid(), getAccountServerSign());
    }

    /**
     * Load attributes
     * @param attributes Attributes
     * @return Is succeeded
     */
    public boolean loadAttributes(Map<String, byte[]> attributes) {

        byte[] shareProtocolVersionBytes = attributes.get(RECORD_KEY_SHARE_PROTOCOL_VERSION);
        String shareProtocolVersion = shareProtocolVersionBytes == null ? null : NsdUtils.bytesToString(shareProtocolVersionBytes);
        if (shareProtocolVersion == null) {
            Log.d(TAG, String.format("The key %s for protocol version not found in attributes, ignoring...", RECORD_KEY_SHARE_PROTOCOL_VERSION));
            return false;
        } else if (shareProtocolVersion.equals(SHARE_PROTOCOL_VERSION_1)) {
            byte[] serverPortStrBytes = attributes.get(RECORD_KEY_SERVER_PORT);
            byte[] uidStrBytes = attributes.get(RECORD_KEY_UID);
            byte[] serverSignBytes = attributes.get(RECORD_KEY_ACCOUNT_SERVER_SIGN);

            if (serverPortStrBytes == null || uidStrBytes == null ||
                    serverSignBytes == null) {
                Log.d(TAG, "Invalid attributes, ignoring...");
                Log.d(TAG, "Share protocol version: " + shareProtocolVersion);
                Log.d(TAG, "Attributes: " + attributes);
                return false;
            }
            String serverPortStr = NsdUtils.bytesToString(serverPortStrBytes);
            String uidStr = NsdUtils.bytesToString(uidStrBytes);
            String serverSign = NsdUtils.bytesToString(serverSignBytes);

            if (serverSign == null || !StringUtils.isNumeric(serverPortStr) ||
                    !StringUtils.isNumeric(uidStr)) {
                Log.d(TAG, "Invalid attributes, ignoring...");
                Log.d(TAG, "Share protocol version: " + shareProtocolVersion);
                Log.d(TAG, "Attributes: " + attributes);
                return false;
            }

            Log.d(TAG, "Share protocol version: " + shareProtocolVersion);
            Log.d(TAG, "Valid attributes, loading...");
            this.protocolVersion = shareProtocolVersion;
            this.serverPort = Integer.parseInt(serverPortStr);
            this.uid = Integer.parseInt(uidStr);
            this.accountServerSign = serverSign;

            attributesLoaded = true;
            return true;
        } else {
            // Unsupported
            Log.w(TAG, "Unsupported protocol version: %s, ignoring...");
            return false;
        }
    }
}
