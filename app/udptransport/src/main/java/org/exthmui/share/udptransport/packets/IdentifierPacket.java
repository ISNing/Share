package org.exthmui.share.udptransport.packets;

import org.apache.commons.lang3.ArrayUtils;
import org.exthmui.share.udptransport.Constants;

import java.net.DatagramPacket;

public final class IdentifierPacket extends AbstractCommandPacket<IdentifierPacket> {
    public static final int DATA_LENGTH = 3;

    public static final int[] IDENTIFIER_TIP = {0, 0};
    public static final int[] EXTRA_TIP = {1, 2};

    private IdentifierPacket(DatagramPacket packet) {
        super(packet);
        if (getCommand() != Constants.COMMAND_IDENTIFIER ||
                packet.getLength() != HEADER_LENGTH + DATA_LENGTH)
            throw new IllegalArgumentException();
    }

    public IdentifierPacket() {
        this(new DatagramPacket(new byte[]{Constants.COMMAND_IDENTIFIER, 0x0, 0x0, 0x0, 0x0}, HEADER_LENGTH + DATA_LENGTH));
    }

    public static IdentifierPacket fromDatagramPacket(DatagramPacket packet) {
        return new IdentifierPacket(packet);
    }

    public byte getIdentifier() {
        return cutDataByTip(IDENTIFIER_TIP)[0];
    }

    public IdentifierPacket setIdentifier(byte identifier) {
        setData(ArrayUtils.addFirst(getExtra(), identifier));
        return this;
    }

    public byte[] getExtra() {
        return cutDataByTip(EXTRA_TIP);
    }

    public IdentifierPacket setExtra(byte[] extra) {
        setData(ArrayUtils.addFirst(getExtra(), getIdentifier()));
        return this;
    }
}
