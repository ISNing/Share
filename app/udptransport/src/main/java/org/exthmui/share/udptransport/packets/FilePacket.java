package org.exthmui.share.udptransport.packets;

import android.util.Log;

import org.apache.commons.lang3.ArrayUtils;
import org.exthmui.share.udptransport.ByteUtils;
import org.exthmui.share.udptransport.Constants;

import java.net.DatagramPacket;
import java.util.Arrays;
import java.util.Locale;

public final class FilePacket extends AbstractCommandPacket<FilePacket> {
    public static final int MIN_DATA_LENGTH = 4;

    public static final ByteUtils.Tip GROUP_ID_TIP = new ByteUtils.Tip(0, 0);
    public static final ByteUtils.Tip PACKET_ID_TIP = new ByteUtils.Tip(1, 2);
    public static final ByteUtils.Tip FLAG_TIP = new ByteUtils.Tip(3, 3);
    public static final ByteUtils.Tip DATA_TIP = new ByteUtils.Tip(4);

    public FilePacket(DatagramPacket packet) {
        super(packet);
    }

    public FilePacket() {
        super(Constants.UdpCommand.FILE_PACKET, MIN_DATA_LENGTH);
    }

    @Override
    public void legalCheck() {
        super.legalCheck();
        if (getCommand() != Constants.UdpCommand.FILE_PACKET ||
                super.getData().length < MIN_DATA_LENGTH) {
            Log.e(super.toString(), String.format("Illegal data: %s", Arrays.toString(toDatagramPacket().getData())));
            throw new IllegalArgumentException();
        }
    }

    public static FilePacket of(DatagramPacket packet) {
        return new FilePacket(packet);
    }

    private byte getFlag() {
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
        setData(getPacketIdBytes(), groupId, getFlag(), getData());
        return this;
    }

    public short getPacketId() {
        return ByteUtils.bytesToShort(getPacketIdBytes());
    }

    private byte[] getPacketIdBytes() {
        return cutDataByTip(PACKET_ID_TIP);
    }

    public FilePacket setPacketId(byte[] packetId) {
        setData(packetId, getGroupId(), getFlag(), getData());
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
        setData(getPacketIdBytes(), getGroupId(), getFlag(), data, length, offset);
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

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "FilePacket{GroupId: %d, PacketId: %d, Flag: %s}", getGroupId(), getPacketId(), getFlag());
    }
}
