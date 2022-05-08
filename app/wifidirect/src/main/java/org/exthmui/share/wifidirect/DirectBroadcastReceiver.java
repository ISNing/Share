package org.exthmui.share.wifidirect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class DirectBroadcastReceiver extends BroadcastReceiver {

    public static final String TAG = "DirectBroadcastReceiver";

    private final WifiP2pManager mWifiP2pManager;

    private final WifiP2pManager.Channel mChannel;

    private final DirectActionListener mDirectActionListener;

    public DirectBroadcastReceiver(WifiP2pManager wifiP2pManager, WifiP2pManager.Channel channel, DirectActionListener directActionListener) {
        mWifiP2pManager = wifiP2pManager;
        mChannel = channel;
        mDirectActionListener = directActionListener;
    }

    @NonNull
    public static IntentFilter getIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        return intentFilter;
    }

    @Override
    public void onReceive(Context context, @NonNull Intent intent) {
        if (!TextUtils.isEmpty(intent.getAction())) {
            switch (intent.getAction()) {
                // Whether WiFi P2P is available
                case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION: {
                    int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                    if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                        mDirectActionListener.onWifiP2pStateChanged(true);
                    } else {
                        mDirectActionListener.onWifiP2pStateChanged(false);
                        List<WifiP2pDevice> wifiP2pDeviceList = new ArrayList<>();
                        mDirectActionListener.onPeersListChanged(wifiP2pDeviceList);
                    }
                    break;
                }
                case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION: {
                    try {
                        mWifiP2pManager.requestPeers(mChannel, peers -> mDirectActionListener.onPeersListChanged(peers.getDeviceList()));
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                    // Here shouldn't cause any exception, we should only register BroadcastReceiver after permissions are granted.
                    break;
                }
                // The connection status of WiFi P2P has changed
                case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION: {
                    @SuppressWarnings("deprecated") NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                    if (networkInfo.isConnected()) {
                        mWifiP2pManager.requestConnectionInfo(mChannel, mDirectActionListener::onConnectionInfoChanged);
                    } else {
                        mDirectActionListener.onDisconnected();
                    }
                    break;
                }
                // The information of current device has changed
                case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION: {
                    mDirectActionListener.onSelfDeviceChanged(intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));
                    break;
                }
            }
        }
    }
}

