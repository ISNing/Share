package org.exthmui.share.wifidirect;

import android.net.wifi.p2p.WifiP2pDevice;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exthmui.share.shared.base.IConnectionType;
import org.exthmui.share.shared.base.Peer;
import org.exthmui.share.shared.misc.Constants;

import java.util.Objects;

public class DirectPeer extends Peer {

    @NonNull
    private WifiP2pDevice wifiP2pDevice;
    @IntRange(from = 5001, to = 65535)
    private int serverPort;
    @NonNull
    private String protocolVersion;
    @NonNull
    private String peerId;
    @IntRange(from = 0)
    private int uid;
    @Nullable
    private String accountServerSign;

    public DirectPeer(@NonNull WifiP2pDevice wifiP2pDevice, @NonNull String protocolVersion,
                      @IntRange(from = 5001, to = 65535) int serverPort,
                      @NonNull String peerId,
                      @IntRange(from = 0) int uid, @Nullable String accountServerSign) {
        this.wifiP2pDevice = wifiP2pDevice;
        this.serverPort = serverPort;
        this.protocolVersion = protocolVersion;
        this.peerId = peerId;
        this.uid = uid;
        this.accountServerSign = accountServerSign;
    }

    @NonNull
    @Override
    public String getId() {
        return DirectUtils.genDirectId(peerId);
    }

    public void setId(@NonNull String peerId) {
        this.peerId = peerId;
    }

    @NonNull
    @Override
    public String getDisplayName() {
        return getDisplayName(wifiP2pDevice.deviceName);
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
    public IConnectionType getConnectionType() {
        return new Metadata();
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
                stat = Constants.TransmissionStatus.ERROR.getNumVal();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DirectPeer)) return false;
        DirectPeer that = (DirectPeer) o;
        return getServerPort() == that.getServerPort() && getUid() == that.getUid() && getWifiP2pDevice().equals(that.getWifiP2pDevice()) && getProtocolVersion().equals(that.getProtocolVersion()) && peerId.equals(that.peerId) && getAccountServerSign().equals(that.getAccountServerSign());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getWifiP2pDevice(), getServerPort(), getProtocolVersion(), peerId, getUid(), getAccountServerSign());
    }
}
