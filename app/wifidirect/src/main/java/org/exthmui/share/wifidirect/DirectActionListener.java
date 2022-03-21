package org.exthmui.share.wifidirect;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;

import java.util.Collection;

public interface DirectActionListener {
    void onWifiP2pStateChanged(boolean enabled);
    void onPeersListChanged(Collection<WifiP2pDevice> wifiP2pDeviceList);
    void onConnectionInfoChanged(WifiP2pInfo wifiP2pInfo);
    void onSelfDeviceChanged(WifiP2pDevice self);
    void onDisconnected();
}
