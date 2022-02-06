package org.exthmui.share.wifidirect;

import android.content.Context;
import android.util.Log;

import org.exthmui.share.shared.Utils;

import java.util.Random;

public class DirectUtils {
    private static final String TAG = "DirectUtils";
    public static int generatePort(){
        Random random = new Random();
        return random.nextInt(60534) + 5001;
    }
    public static int getTimeout(Context context) {
        return Utils.getDefaultSharedPreferences(context).getInt(context.getString(R.string.prefs_key_wifidirect_timeout), context.getResources().getInteger(R.integer.prefs_default_wifidirect_timeout));
    }
    public static int getServerPort(Context context) {
        return Utils.getDefaultSharedPreferences(context).getInt(context.getString(R.string.prefs_key_wifidirect_server_port), context.getResources().getInteger(R.integer.prefs_default_wifidirect_server_port));
    }
    public static int getClientPort(Context context) {
        int port = Utils.getDefaultSharedPreferences(context).getInt(context.getString(R.string.prefs_key_wifidirect_client_port), context.getResources().getInteger(R.integer.prefs_default_wifidirect_client_port));
        if (port == getServerPort(context) | port == -1 | port < 5001 | port > 65535) {
            Log.d(TAG, "Got a illegal port, regenerating port in range of 5001-65565");
            return generatePort();
        }
        return port;
    }
    public static int getBufferSize(Context context) {
        int defaultSize = context.getResources().getInteger(R.integer.prefs_default_wifidirect_buffer_size);
        int bufferSize = Utils.getDefaultSharedPreferences(context).getInt(context.getString(R.string.prefs_key_wifidirect_buffer_size), defaultSize);
        if (bufferSize < 1) {
            Log.d(TAG, "Got a illegal port returning default size");
            return defaultSize;
        }
        return bufferSize;
    }
}
