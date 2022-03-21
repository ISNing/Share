package org.exthmui.share.wifidirect;

import static org.exthmui.share.shared.Constants.CONNECTION_CODE_WIFIDIRECT;
import static org.exthmui.share.shared.Constants.PEER_ID_STRING;

import android.content.Context;
import android.util.Log;

import org.exthmui.share.shared.Utils;

import java.util.Random;

public class DirectUtils {
    private static final String TAG = "DirectUtils";
    public static int generatePort(int except){
        int port;
        do {
            port = generatePort();
        } while (!isPortValid(port) | port == except);
        return port;
    }
    public static int generatePort(){
        Random random = new Random();
        return random.nextInt(60534) + 5001;
    }
    public static int getTimeout(Context context) {
        return Utils.getDefaultSharedPreferences(context).getInt(context.getString(R.string.prefs_key_wifidirect_timeout), context.getResources().getInteger(R.integer.prefs_default_wifidirect_timeout));
    }
    public static boolean isTimeoutValid(int timeout) {
        return timeout > 0;
    }
    public static int getServerPort(Context context) {
        int port = Utils.getDefaultSharedPreferences(context).getInt(context.getString(R.string.prefs_key_wifidirect_server_port), context.getResources().getInteger(R.integer.prefs_default_wifidirect_server_port));
        if (!isServerPortValid(context, port) | port == -1) {
            Log.e(TAG, "Got a illegal port, regenerating port in range of 5001-65565");
            return generatePort(getClientPort(context));
        }
        return port;
    }
    public static int getClientPort(Context context) {
        int port = Utils.getDefaultSharedPreferences(context).getInt(context.getString(R.string.prefs_key_wifidirect_client_port), context.getResources().getInteger(R.integer.prefs_default_wifidirect_client_port));
        if (!isClientPortValid(context, port) | port == -1) {
            Log.d(TAG, "Got a illegal port or requesting dynamically generation, regenerating port in range of 5001-65565");
            return generatePort(getServerPort(context));
        }
        return port;
    }
    public static boolean isPortValid(int port) {
        return !(port < 5001 | port > 65535);
    }
    public static boolean isServerPortValid(Context context, int serverPort) {
        return serverPort == -1 | (isPortValid(serverPort) && serverPort != getClientPort(context));
    }
    public static boolean isClientPortValid(Context context, int clientPort) {
        return clientPort == -1 | (isPortValid(clientPort) && clientPort != getServerPort(context));
    }

    public static int getBufferSize(Context context) {
        int defaultSize = context.getResources().getInteger(R.integer.prefs_default_wifidirect_buffer_size);
        int bufferSize = Utils.getDefaultSharedPreferences(context).getInt(context.getString(R.string.prefs_key_wifidirect_buffer_size), defaultSize);
        if (bufferSize <= 0) {
            Log.d(TAG, "Got a illegal port returning default size");
            return defaultSize;
        }
        return bufferSize;
    }

    public static String genDirectId(String peerId) {
        return String.format(PEER_ID_STRING, CONNECTION_CODE_WIFIDIRECT, peerId);
    }
}
