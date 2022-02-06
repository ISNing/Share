package org.exthmui.share.wifidirect;

import android.net.wifi.p2p.WifiP2pDevice;

import org.exthmui.share.shared.Constants;
import org.exthmui.share.shared.base.Peer;

public class DirectPeer extends Peer {
    public static final String CONNECTION_CODE = Constants.CONNECTION_CODE_WIFIDIRECT;
    private WifiP2pDevice wifiP2pDevice;

    public DirectPeer(WifiP2pDevice wifiP2pDevice) {
        this.wifiP2pDevice = wifiP2pDevice;
    }

    @Override
    public String getId() {
        return String.format(Constants.PEER_ID_STRING, getConnectionType(), wifiP2pDevice.deviceAddress);
    }

    @Override
    public String getDisplayName() {
        return wifiP2pDevice.deviceName;
    }

    @Override
    public int getDeviceType() {
        /*
         * The predefined values for Category ID and Sub Category ID are provided in Table
         * 41 (Primary Device Type) in Section 12 (Data Element Definitions) of the 
         * <a href="https://www.wi-fi.org/download.php?file=/sites/default/files/private/Wi-Fi_Protected_Setup_Specification_v2.0.8.pdf">Wi-Fi Protected Setup Specification [2]</a>.
         * <a href="https://www.wi-fi.org/download.php?file=/sites/default/files/private/Wi-Fi_Direct_Specification_v1.9.pdf"></a>From Wi-Fi Direct® Specification(Version 1.9).</a>
         */
        String primaryDeviceType = wifiP2pDevice.primaryDeviceType;
        String[] sliced = primaryDeviceType.split("-");
        if (sliced.length != 3) return Constants.DeviceType.UNKNOWN.getNumVal();
        int id1 = Integer.parseInt(sliced[0]);
        int id2 = Integer.parseInt(sliced[2]);
        if (id1 == 1) {
            if (id2 == 1 | id2 == 4 | id2 == 6)
                return Constants.DeviceType.DESKTOP.getNumVal();
            if (id2 ==5|id2==8|id2==10)
                return Constants.DeviceType.LAPTOP.getNumVal();
            if (id2==9)
                return Constants.DeviceType.TABLET.getNumVal();
        } else if (id1==10)
            return Constants.DeviceType.PHONE.getNumVal();
        return Constants.DeviceType.UNKNOWN.getNumVal();
    }

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
        return null;
    }

    public WifiP2pDevice getWifiP2pDevice() {
        return wifiP2pDevice;
    }

    public void setWifiP2pDevice(WifiP2pDevice wifiP2pDevice) {
        this.wifiP2pDevice = wifiP2pDevice;
    }
}
