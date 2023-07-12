package org.exthmui.share.udptransport.packets;

import android.util.Log;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.ArrayUtils;
import org.exthmui.share.udptransport.Constants;

import java.net.DatagramPacket;
import java.util.Arrays;

public final class IdentifierPacket extends AbstractCommandPacket<IdentifierPacket> {
    public static final int DATA_LENGTH = 4;

    public static final int[] IDENTIFIER_TIP = {0, 0};
    public static final int[] EXTRA_TIP = {1, 3};

    public IdentifierPacket(DatagramPacket packet) {
        super(packet);
    }

    public IdentifierPacket() {
        this(new DatagramPacket(new byte[]{Constants.COMMAND_IDENTIFIER, 0x0, 0x0, 0x0, 0x0, 0x0}, HEADER_LENGTH + DATA_LENGTH));
    }

    @Override
    public void legalCheck() {
        if (getCommand() != Constants.COMMAND_IDENTIFIER ||
                toDatagramPacket().getLength() != HEADER_LENGTH + DATA_LENGTH) {
            Log.e(this.toString(), String.format("Illegal data: %s", Arrays.toString(toDatagramPacket().getData())));
            throw new IllegalArgumentException();
        }
    }

    public byte getIdentifier() {
        return cutDataByTip(IDENTIFIER_TIP)[0];
    }

    public IdentifierPacket setIdentifier(byte identifier) {
        byte[] extra = getExtra();
        byte[] data = ArrayUtils.addFirst(extra, identifier);
        setData(data);
        return this;
    }

    public byte[] getExtra() {
        return cutDataByTip(EXTRA_TIP);
    }

    public IdentifierPacket setExtra(byte[] extra) {
        byte[] e = new byte[(EXTRA_TIP[1] - EXTRA_TIP[0] + 1)];
        if (extra != null)
            System.arraycopy(extra, 0, e, 0, Math.min(extra.length, e.length));
        setData(ArrayUtils.addFirst(e, getIdentifier()));
        return this;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("IdentifierPacket{Identifier: %s, extra: %s}", getIdentifier(), Arrays.toString(getExtra()));
    }
}
