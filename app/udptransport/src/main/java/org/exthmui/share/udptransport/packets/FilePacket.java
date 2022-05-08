package org.exthmui.share.udptransport.packets;

import org.apache.commons.lang3.ArrayUtils;
import org.exthmui.share.udptransport.ByteUtils;
import org.exthmui.share.udptransport.Constants;

import java.net.DatagramPacket;

public final class FilePacket extends AbstractCommandPacket<FilePacket> {
    public static final int MIN_DATA_LENGTH = 3;

    public static final int[] GROUP_ID_TIP = {0, 0};
    public static final int[] PACKET_ID_TIP = {1, 2};
    public static final int[] DATA_TIP = {0};

    private FilePacket(DatagramPacket packet) {
        super(packet);
        if (getCommand() != Constants.COMMAND_FILE_PACKET ||
                packet.getLength() < HEADER_LENGTH + MIN_DATA_LENGTH)
            throw new IllegalArgumentException();
    }

    public FilePacket() {
        this(new DatagramPacket(new byte[]{Constants.COMMAND_FILE_PACKET, 0x0, 0x0, 0x0, 0x0, 0x0},
                HEADER_LENGTH + MIN_DATA_LENGTH));
    }

    public static FilePacket fromDatagramPacket(DatagramPacket packet) {
        return new FilePacket(packet);
    }

    public byte getGroupId() {
        return cutDataByTip(GROUP_ID_TIP)[0];
    }

    public FilePacket setGroupId(byte groupId) {
        super.setData(ArrayUtils.addAll(ArrayUtils.addFirst(getPacketIdBytes(), groupId), getData()));
        return this;
    }

    public short getPacketId() {
        return ByteUtils.bytesToShort(getPacketIdBytes());
    }

    private byte[] getPacketIdBytes() {
        return cutDataByTip(PACKET_ID_TIP);
    }

    public FilePacket setPacketId(byte[] packetId) {
        super.setData(ArrayUtils.addAll(ArrayUtils.addFirst(packetId, getGroupId()), getData()));
        return this;
    }

    public FilePacket setPacketId(short packetId) {
        return setPacketId(ByteUtils.shortToBytes(packetId));
    }

    @Override
    public byte[] getData() {
        return cutDataByTip(DATA_TIP);
    }

    @Override
    public FilePacket setData(byte[] data) {
        super.setData(ArrayUtils.addAll(ArrayUtils.addFirst(getPacketIdBytes(), getGroupId()), data));
        return this;
    }
}
