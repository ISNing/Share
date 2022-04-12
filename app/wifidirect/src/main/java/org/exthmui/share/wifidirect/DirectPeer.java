package org.exthmui.share.wifidirect;

import android.net.wifi.p2p.WifiP2pDevice;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import org.exthmui.share.shared.Constants;
import org.exthmui.share.shared.base.Peer;

public class DirectPeer extends Peer {
    public static final String CONNECTION_CODE = Constants.CONNECTION_CODE_WIFIDIRECT;

    @NonNull
    private WifiP2pDevice wifiP2pDevice;
    @IntRange(from = 5001, to = 65535)
    private int serverPort;
    @NonNull
    private String shareProtocolVersion;
    @NonNull
    private String peerId;
    @IntRange(from = 0)
    private int uid;
    @NonNull
    private String serverSign;

    public DirectPeer(@NonNull WifiP2pDevice wifiP2pDevice, @NonNull String shareProtocolVersion,
                      @IntRange(from = 5001, to = 65535) int serverPort,
                      @NonNull String peerId,
                      @IntRange(from = 0) int uid, @NonNull String serverSign) {
        this.wifiP2pDevice = wifiP2pDevice;
        this.serverPort = serverPort;
        this.shareProtocolVersion = shareProtocolVersion;
        this.peerId = peerId;
        this.uid = uid;
        this.serverSign = serverSign;
    }

    @NonNull
    @Override
    public String getId() {
        return DirectUtils.genDirectId(getPeerId());
    }

    @NonNull
    @Override
    public String getDisplayName() {
        return wifiP2pDevice.deviceName;
    }

    @Override
    public int getDeviceType() {
        /*
         * The predefined values for Category ID and Sub Category ID are provided in Table
         * 41 (Primary Device Type) in Section 12 (Data Element Definitions) of the
         * <a href="https://www.wi-fi.org/download.php?file=/sites/default/files/private/Wi-Fi_Protected_Setup_Specification_v2.0.8.pdf">Wi-Fi Protected Setup Specification</a>.
         */
        String primaryDeviceType = wifiP2pDevice.primaryDeviceType;
        String[] sliced = primaryDeviceType.split("-");
        if (sliced.length != 3) return Constants.DeviceType.UNKNOWN.getNumVal();
        int id1 = Integer.parseInt(sliced[0]);
        int id2 = Integer.parseInt(sliced[2]);
        if (id1 == 1) {
            if (id2 == 1 || id2 == 4 || id2 == 6)
                return Constants.DeviceType.DESKTOP.getNumVal();
            if (id2 == 5 || id2 == 8 || id2 == 10)
                return Constants.DeviceType.LAPTOP.getNumVal();
            if (id2 == 9)
                return Constants.DeviceType.TABLET.getNumVal();
        } else if (id1 == 10)
            return Constants.DeviceType.PHONE.getNumVal();
        return Constants.DeviceType.UNKNOWN.getNumVal();
    }

    @NonNull
    @Override
    public String getConnectionType() {
        return CONNECTION_CODE;
    }

    @Override
    public boolean isTrusted() {
        return false;
    }

    @Override
    public int getConnectionStatus() {
        int stat;
        switch (wifiP2pDevice.status) {
            case WifiP2pDevice.CONNECTED:
            case WifiP2pDevice.INVITED:
            case WifiP2pDevice.FAILED:
            case WifiP2pDevice.AVAILABLE:
                stat = Constants.ConnectionStatus.AVAILABLE.getNumVal();
                break;
            case WifiP2pDevice.UNAVAILABLE:
                stat = Constants.ConnectionStatus.UNAVAILABLE.getNumVal();
                break;
            default:
                stat = Constants.ConnectionStatus.UNKNOWN.getNumVal();
        }
        return stat;
    }

    @Override
    public int getTransmissionStatus() {
        int stat;
        switch (wifiP2pDevice.status) {
            case WifiP2pDevice.CONNECTED:
            case WifiP2pDevice.INVITED:
                stat = Constants.TransmissionStatus.IN_PROGRESS.getNumVal();
                break;
            case WifiP2pDevice.FAILED:
                stat = Constants.TransmissionStatus.UNKNOWN_ERROR.getNumVal();
                break;
            case WifiP2pDevice.AVAILABLE:
            case WifiP2pDevice.UNAVAILABLE:
                stat = Constants.TransmissionStatus.UNKNOWN.getNumVal();
                break;
            default:
                stat = Constants.ConnectionStatus.UNKNOWN.getNumVal();
        }
        return stat;
    }

    @Override
    public String getDetailMessage() {
        return String.format("IP: %1$s\n", wifiP2pDevice.deviceAddress);
    }

    @NonNull
    public WifiP2pDevice getWifiP2pDevice() {
        return wifiP2pDevice;
    }

    public void setWifiP2pDevice(@NonNull WifiP2pDevice wifiP2pDevice) {
        this.wifiP2pDevice = wifiP2pDevice;
    }

    @IntRange(from = 5001, to = 65535)
    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(@IntRange(from = 5001, to = 65535) int serverPort) {
        this.serverPort = serverPort;
    }

    @NonNull
    public String getShareProtocolVersion() {
        return shareProtocolVersion;
    }

    public void setShareProtocolVersion(@NonNull String shareProtocolVersion) {
        this.shareProtocolVersion = shareProtocolVersion;
    }

    @NonNull
    public String getPeerId() {
        return peerId;
    }

    public void setPeerId(@NonNull String peerId) {
        this.peerId = peerId;
    }

    @IntRange(from = 0)
    public int getUid() {
        return uid;
    }

    public void setUid(@IntRange(from = 5001, to = 65535) int uid) {
        this.uid = uid;
    }

    @NonNull
    public String getServerSign() {
        return serverSign;
    }

    public void setServerSign(@NonNull String serverSign) {
        this.serverSign = serverSign;
    }
}
