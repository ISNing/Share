package org.exthmui.share.udptransport;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.Arrays;

public abstract class ByteUtils {
    //TIP: OFFSET, ENDIAN or OFFSET only, then endian is the end of byte array
    public static byte[] cutBytesByTip(@NonNull int[] tip, @NonNull byte[] bytes,
                                       @IntRange(from = 0) int initOffset) {
        return Arrays.copyOfRange(bytes, initOffset + tip[0],
                initOffset + tip.length == 1 ? bytes.length - 1 : tip[1]);
    }

    public static byte[] cutBytesByTip(int[] tip, byte[] bytes) {
        return cutBytesByTip(tip, bytes, 0);
    }

    public static int bytesToInt(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(Constants.DEFAULT_ORDER).getInt();
    }

    public static byte[] intToBytes(int value) {
        return ByteBuffer.allocate(Integer.BYTES).order(Constants.DEFAULT_ORDER).putInt(value).array();
    }

    public static short bytesToShort(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(Constants.DEFAULT_ORDER).getShort();
    }

    public static byte[] shortToBytes(short value) {
        return ByteBuffer.allocate(Short.BYTES).order(Constants.DEFAULT_ORDER).putShort(value).array();
    }

    public static double bytesToDouble(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(Constants.DEFAULT_ORDER).getDouble();
    }

    public static byte[] doubleToBytes(double value) {
        return ByteBuffer.allocate(Double.BYTES).order(Constants.DEFAULT_ORDER).putDouble(value).array();
    }

    public static long bytesToLong(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(Constants.DEFAULT_ORDER).getLong();
    }

    public static byte[] longToBytes(long value) {
        return ByteBuffer.allocate(Long.BYTES).order(Constants.DEFAULT_ORDER).putLong(value).array();
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
        return bytesToString(b, Constants.STRING_CHARSET);
    }

    public static byte[] stringToBytes(String s) {
        return stringToBytes(s, Constants.STRING_CHARSET);
    }

    public static byte[] stringToBytes(String s, String charset) {
        byte[] bytes = null;
        try {
            bytes = s.getBytes(charset);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bytes;
    }
}
