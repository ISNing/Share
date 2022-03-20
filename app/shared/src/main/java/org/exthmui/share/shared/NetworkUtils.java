package org.exthmui.share.shared;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import androidx.annotation.NonNull;

public class NetworkUtils {
    public static boolean networkConnected(@NonNull Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network network = cm.getActiveNetwork();
        if (network != null) {
            NetworkCapabilities nc = cm.getNetworkCapabilities(network);
            if (nc != null) {
                //移动数据
                if (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {//WIFI
                    return true;
                } else return nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
            }
        }

        return false;
    }
}
