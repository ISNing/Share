package org.exthmui.share.wifidirect;

import android.content.Context;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import androidx.annotation.NonNull;

import org.exthmui.share.shared.misc.Utils;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

public abstract class DirectUtils {
    public static final String TAG = "DirectUtils";

    public static int generatePort(int except) {
        int port;
        do {
            port = generatePort();
        } while (!isPortValid(port) || port == except);
        return port;
    }

    public static int generatePort() {
        Random random = new Random();
        return random.nextInt(60534) + 5001;
    }
    public static int getTimeout(@NonNull Context context) {
        return Utils.getDefaultSharedPreferences(context).getInt(context.getString(R.string.prefs_key_wifidirect_timeout), context.getResources().getInteger(R.integer.prefs_default_wifidirect_timeout));
    }
    public static boolean isTimeoutValid(int timeout) {
        return timeout > 0;
    }
    public static int getServerPort(@NonNull Context context) {
        int port = Utils.getDefaultSharedPreferences(context).getInt(context.getString(R.string.prefs_key_wifidirect_server_port), context.getResources().getInteger(R.integer.prefs_default_wifidirect_server_port));
        if (!isServerPortValid(context, port) || port == -1) {
            Log.d(TAG, "Got a illegal port or requesting dynamically generation, regenerating port in range of 5001-65565");
            return generatePort();
        }
        return port;
    }
    public static int getClientPort(@NonNull Context context) {
        int port = Utils.getDefaultSharedPreferences(context).getInt(context.getString(R.string.prefs_key_wifidirect_client_port), context.getResources().getInteger(R.integer.prefs_default_wifidirect_client_port));
        if (!isClientPortValid(context, port) || port == -1) {
            Log.d(TAG, "Got a illegal port or requesting dynamically generation, regenerating port in range of 5001-65565");
            return generatePort(getServerPort(context));
        }
        return port;
    }
    public static boolean isPortValid(int port) {
        return !(port < 5001 || port > 65535);
    }
    public static boolean isServerPortValid(Context context, int serverPort) {
        return serverPort == -1 || (isPortValid(serverPort));
    }
    public static boolean isClientPortValid(@NonNull Context context, int clientPort) {
        return clientPort == -1 || (isPortValid(clientPort) && clientPort != getServerPort(context));
    }

    public static int getBufferSize(@NonNull Context context) {
        int defaultSize = context.getResources().getInteger(R.integer.prefs_default_wifidirect_buffer_size);
        int bufferSize = Utils.getDefaultSharedPreferences(context).getInt(context.getString(R.string.prefs_key_wifidirect_buffer_size), defaultSize);
        if (bufferSize <= 0) {
            Log.d(TAG, "Got a illegal buffer size, returning default size");
            return defaultSize;
        }
        return bufferSize;
    }

    @NonNull
    public static String genDirectId(String peerId) {
        return peerId;
    }

    public static void connectPeer(@NonNull Context context, @NonNull DirectPeer peer, @NonNull CountDownLatch latch) {
        WifiP2pDevice targetDevice = peer.getWifiP2pDevice();
        DirectManager manager = DirectManager.getInstance(context);
        WifiP2pManager wifiP2pManager = manager.getWifiP2pManager();
        WifiP2pManager.Channel channel = manager.getChannel();
        WifiP2pConfig config = new WifiP2pConfig();
        if (config.deviceAddress != null) {
            config.deviceAddress = targetDevice.deviceAddress;
            config.wps.setup = WpsInfo.PBC;
            try {
                wifiP2pManager.connect(channel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        peer.notifyPeerUpdated();
                        latch.countDown();
                    }

                    @Override
                    public void onFailure(int reason) {

                    }
                });
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }
}
