package org.exthmui.share.udptransport.packets;

import android.util.Log;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.ArrayUtils;
import org.exthmui.share.udptransport.ByteUtils;
import org.exthmui.share.udptransport.Constants;

import java.net.DatagramPacket;
import java.util.Arrays;

public final class IdentifierPacket extends AbstractCommandPacket<IdentifierPacket> {
    public static final int DATA_LENGTH = 13;

    public static final ByteUtils.Tip IDENTIFIER_TIP = new ByteUtils.Tip(0, 0);
    public static final ByteUtils.Tip EXTRA_TIP = new ByteUtils.Tip(1, 12);

    public IdentifierPacket(DatagramPacket packet) {
        super(packet);
    }

    public IdentifierPacket() {
        super(Constants.UdpCommand.IDENTIFIER, DATA_LENGTH);
    }

    @Override
    public void legalCheck() {
        super.legalCheck();
        if (getCommand() != Constants.UdpCommand.IDENTIFIER || super.getData().length != DATA_LENGTH) {
            Log.e(super.toString(), String.format("Illegal data: %s", Arrays.toString(toDatagramPacket().getData())));
            throw new IllegalArgumentException();
        }
    }

    @NonNull
    @Override
    public IdentifierPacket clone() {
        return new IdentifierPacket(this.toDatagramPacket());
    }

    public Constants.Identifier getIdentifier() {
        return Constants.Identifier.parse(getIdentifierByte());
    }

    private byte getIdentifierByte() {
        return cutDataByTip(IDENTIFIER_TIP)[0];
    }

    public IdentifierPacket setIdentifier(Constants.Identifier identifier) {
        return setIdentifierByte(identifier.getIdentifier());
    }

    public IdentifierPacket setIdentifierByte(byte identifier) {
        byte[] extra = getExtra();
        byte[] data = ArrayUtils.addFirst(extra, identifier);
        setData(data);
        return this;
    }

    public byte[] getExtra() {
        return cutDataByTip(EXTRA_TIP);
    }

    public IdentifierPacket setExtra(byte[] extra) {
        byte[] e = new byte[(EXTRA_TIP.getEnd() - EXTRA_TIP.getOffset() + 1)];
        if (extra != null)
            System.arraycopy(extra, 0, e, 0, Math.min(extra.length, e.length));
        setData(ArrayUtils.addFirst(e, getIdentifierByte()));
        return this;
    }

    /**
     * Change non-ack identifier to ack identifier or vice versa
     */
    public IdentifierPacket turnIdentifier() {
        setIdentifier(getIdentifier().getCorrespondIdentifier());
        return this;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("IdentifierPacket{Identifier: %s, extra: %s}", getIdentifier(), Arrays.toString(getExtra()));
    }
}
