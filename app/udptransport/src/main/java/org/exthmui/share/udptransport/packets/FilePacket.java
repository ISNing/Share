package org.exthmui.share.udptransport.packets;

import org.apache.commons.lang3.ArrayUtils;
import org.exthmui.share.udptransport.ByteUtils;
import org.exthmui.share.udptransport.Constants;

import java.net.DatagramPacket;
import java.util.Arrays;

public final class FilePacket extends AbstractCommandPacket<FilePacket> {
    public static final int MIN_DATA_LENGTH = 3;

    public static final int[] GROUP_ID_TIP = {0, 0};
    public static final int[] PACKET_ID_TIP = {1, 2};
    public static final int[] FLAG_TIP = {3};
    public static final int[] DATA_TIP = {4};

    private FilePacket(DatagramPacket packet) {
        super(packet);
        if (getCommand() != Constants.COMMAND_FILE_PACKET ||
                packet.getLength() < HEADER_LENGTH + MIN_DATA_LENGTH)
            throw new IllegalArgumentException();
    }

    public FilePacket() {
        this(new DatagramPacket(new byte[]{Constants.COMMAND_FILE_PACKET, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0},
                HEADER_LENGTH + MIN_DATA_LENGTH));
    }

    public static FilePacket fromDatagramPacket(DatagramPacket packet) {
        return new FilePacket(packet);
    }

    private byte getFlags() {
        return cutDataByTip(FLAG_TIP)[0];
    }

    private FilePacket setFlags(byte flags) {
        setData(getPacketIdBytes(), getGroupId(), flags, getData());
        return this;
    }

    public byte getGroupId() {
        return cutDataByTip(GROUP_ID_TIP)[0];
    }

    public FilePacket setGroupId(byte groupId) {
        setData(getPacketIdBytes(), groupId, getFlags(), getData());
        return this;
    }

    public short getPacketId() {
        return ByteUtils.bytesToShort(getPacketIdBytes());
    }

    private byte[] getPacketIdBytes() {
        return cutDataByTip(PACKET_ID_TIP);
    }

    public FilePacket setPacketId(byte[] packetId) {
        setData(packetId, getGroupId(), getFlags(), getData());
        return this;
    }

    public FilePacket setPacketId(short packetId) {
        return setPacketId(ByteUtils.shortToBytes(packetId));
    }

    public FilePacket setDataLength(int length) {
        return setData(Arrays.copyOfRange(getData(), 0, length));
    }

    @Override
    public byte[] getData() {
        return cutDataByTip(DATA_TIP);
    }

    @Override
    public FilePacket setData(byte[] data) {
        setData(data, data.length);
        return this;
    }

    public FilePacket setData(byte[] data, int length) {
        setData(data, length, 0);
        return this;
    }

    public FilePacket setData(byte[] data, int length, int offset) {
        setData(getPacketIdBytes(), getGroupId(), getFlags(), data, length, offset);
        return this;
    }

    public FilePacket setData(byte[] packetIdBytes, byte groupId, byte flags, byte[] data) {
        setData(packetIdBytes, groupId, flags, data, data.length, 0);
        return this;
    }

    public FilePacket setData(byte[] packetIdBytes, byte groupId, byte flags, byte[] data, int length) {
        setData(packetIdBytes, groupId, flags, data, length, 0);
        return this;
    }

    public FilePacket setData(byte[] packetIdBytes, byte groupId, byte flags, byte[] data, int length, int offset) {
        super.setData(ArrayUtils.addAll(ArrayUtils.add(ArrayUtils.addFirst(packetIdBytes, groupId), flags), Arrays.copyOfRange(data, offset, offset + length)));
        return this;
    }
}
