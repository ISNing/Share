package org.exthmui.share.lannsd;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import androidx.annotation.NonNull;

import org.exthmui.share.shared.misc.Utils;

import java.util.Map;
import java.util.Random;

public abstract class NsdUtils {
    public static final String TAG = "NsdUtils";

    public interface ResolveListener {
        int RESOLVE_FAILURE_FAILED_GETTING_MANAGER = -2;
        int RESOLVE_FAILURE_INCAPABLE_PEER = -1;

        void onResolveFailed(NsdPeer peer, int errorCode);

        void onServiceResolved(NsdPeer peer);
    }

    public static void resolvePeer(Context context, NsdPeer peer, ResolveListener listener) {
        NsdManager manager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);

        if (manager == null) {
            listener.onResolveFailed(peer, ResolveListener.RESOLVE_FAILURE_FAILED_GETTING_MANAGER);
            return;
        }

        android.net.nsd.NsdManager.ResolveListener resolveListener =
                new android.net.nsd.NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                peer.setNsdServiceInfo(serviceInfo);
                listener.onResolveFailed(peer, errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                peer.setNsdServiceInfo(serviceInfo);

                Map<String, byte[]> rec = serviceInfo.getAttributes();

                boolean succeeded = peer.loadAttributes(rec);
                peer.setNsdServiceInfo(serviceInfo);
                if (succeeded) listener.onServiceResolved(peer);
                else listener.onResolveFailed(peer, ResolveListener.RESOLVE_FAILURE_INCAPABLE_PEER);

            }
        };

        manager.resolveService(peer.getNsdServiceInfo(), resolveListener);
    }

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
        return Utils.getDefaultSharedPreferences(context).getInt(context.getString(R.string.prefs_key_lannsd_timeout), context.getResources().getInteger(R.integer.prefs_default_lannsd_timeout));
    }

    public static boolean isTimeoutValid(int timeout) {
        return timeout > 0;
    }

    public static int getServerPortTcp(@NonNull Context context) {
        int port = Utils.getDefaultSharedPreferences(context).getInt(context.getString(R.string.prefs_key_lannsd_server_port_tcp), context.getResources().getInteger(R.integer.prefs_default_lannsd_server_port_tcp));
        if (!isServerPortValid(context, port) || port == -1) {
            Log.d(TAG, "Got a illegal port or requesting dynamically generation, regenerating port in range of 5001-65565");
            return generatePort();
        }
        return port;
    }

    public static int getServerPortUdp(@NonNull Context context) {
        int port = Utils.getDefaultSharedPreferences(context).getInt(context.getString(R.string.prefs_key_lannsd_server_port_udp), context.getResources().getInteger(R.integer.prefs_default_lannsd_server_port_udp));
        if (!isServerPortValid(context, port) || port == -1) {
            Log.d(TAG, "Got a illegal port or requesting dynamically generation, regenerating port in range of 5001-65565");
            return generatePort();
        }
        return port;
    }

    public static boolean isMd5ValidationEnabled(@NonNull Context context) {
        return Utils.getDefaultSharedPreferences(context).getBoolean(
                context.getString(R.string.prefs_key_lannsd_md5_validation),
                context.getResources().getBoolean(R.bool.prefs_default_lannsd_md5_validation));
    }

    public static boolean isPortValid(int port) {
        return !(port < 5001 || port > 65535);
    }

    public static boolean isServerPortValid(Context context, int serverPort) {
        return serverPort == -1 || (isPortValid(serverPort));
    }

    @NonNull
    public static String genNsdId(String peerId) {
        return peerId;
    }

    public static byte[] intToBytes(int number) {
        int temp = number;
        byte[] b = new byte[4];
        for (int i = 0; i < b.length; i++) {
            b[i] = Integer.valueOf(temp & 0xff).byteValue();
            temp = temp >> 8;
        }
        return b;
    }

    public static int bytesToInt(byte[] intByte) {
        int fromByte = 0;
        for (int i = 0; i < 4; i++) {
            int n = (intByte[i] < 0 ? (int) intByte[i] + 256 : (int) intByte[i]) << (8 * i);
            fromByte += n;
        }
        return fromByte;
    }

    public static byte[] shortToBytes(short number) {
        int temp = number;
        byte[] b = new byte[2];
        for (int i = 0; i < b.length; i++) {
            b[i] = Integer.valueOf(temp & 0xff).byteValue();
            temp = temp >> 8;
        }
        return b;
    }

    public static int bytesToShort(byte[] bytes) {
        int fromByte = 0;
        for (int i = 0; i < 2; i++) {
            int n = (bytes[i] < 0 ? (short) bytes[i] + 256 : (short) bytes[i]) << (8 * i);
            fromByte += n;
        }
        return fromByte;
    }

    public static byte[] doubleToByte(double d) {
        byte[] b = new byte[8];
        long l = Double.doubleToLongBits(d);
        for (int i = 0; i < b.length; i++) {
            b[i] = Long.valueOf(l).byteValue();
            l = l >> 8;
        }
        return b;
    }

    public static double bytesToDouble(byte[] b) {
        long l;
        l = b[0];
        l &= 0xff;
        l |= ((long) b[1] << 8);
        l &= 0xffff;
        l |= ((long) b[2] << 16);
        l &= 0xffffff;
        l |= ((long) b[3] << 24);
        l &= 0xffffffffL;
        l |= ((long) b[4] << 32);
        l &= 0xffffffffffL;
        l |= ((long) b[5] << 40);
        l &= 0xffffffffffffL;
        l |= ((long) b[6] << 48);
        l &= 0xffffffffffffffL;
        l |= ((long) b[7] << 56);
        return Double.longBitsToDouble(l);
    }

    public static byte[] longToByte(long num) {
        byte[] b = new byte[8];
        long l = num;
        for (int i = 0; i < b.length; i++) {
            b[i] = Long.valueOf(l).byteValue();
            l = l >> 8;
        }
        return b;
    }

    public static long byteToLong(byte[] b) {
        long l;
        l = b[0];
        l &= 0xff;
        l |= ((long) b[1] << 8);
        l &= 0xffff;
        l |= ((long) b[2] << 16);
        l &= 0xffffff;
        l |= ((long) b[3] << 24);
        l &= 0xffffffffL;
        l |= ((long) b[4] << 32);
        l &= 0xffffffffffL;
        l |= ((long) b[5] << 40);
        l &= 0xffffffffffffL;
        l |= ((long) b[6] << 48);
        l &= 0xffffffffffffffL;
        l |= ((long) b[7] << 56);
        return l;
    }

    public static String bytesToString(byte[] b, String charset) {
        String str = null;
        try {
            str = new String(b, charset);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return str;
    }

    public static String bytesToString(byte[] b) {
        return bytesToString(b, Constants.RECORD_STRING_CHARSET);
    }

    public static byte[] stringToByte(String s) {
        return stringToByte(s, Constants.RECORD_STRING_CHARSET);
    }

    public static byte[] stringToByte(String s, String charset) {
        byte[] bytes = null;
        try {
            bytes = s.getBytes(charset);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bytes;
    }
}
