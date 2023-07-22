package org.exthmui.share.udptransport.packets;

import android.util.Log;

import org.apache.commons.lang3.ArrayUtils;
import org.exthmui.share.udptransport.ByteUtils;
import org.exthmui.share.udptransport.Constants;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

public final class ResendRequestPacket extends AbstractCommandPacket<ResendRequestPacket> {
    public static final int MIN_DATA_LENGTH = 1;

    public static final ByteUtils.Tip GROUP_ID_TIP = new ByteUtils.Tip(0, 0);

    public static final ByteUtils.Tip PACKET_IDS_TIP = new ByteUtils.Tip(1);

    public ResendRequestPacket(DatagramPacket packet) {
        super(packet);
    }

    public ResendRequestPacket() {
        super(Constants.UdpCommand.PACKET_RESEND_REQ, MIN_DATA_LENGTH);
    }

    @Override
    public void legalCheck() {
        super.legalCheck();
        if (getCommand() != Constants.UdpCommand.PACKET_RESEND_REQ ||
                super.getData().length < MIN_DATA_LENGTH) {
            Log.e(super.toString(), String.format("Illegal data: %s", Arrays.toString(toDatagramPacket().getData())));
            throw new IllegalArgumentException();
        }
    }

    public ResendRequestPacket setData(byte groupId, byte[] idsBytes) {
        setData(ArrayUtils.addFirst(idsBytes, groupId));
        return this;
    }

    public ResendRequestPacket setData(byte groupId, short[] ids) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(ids.length * 2);

        for (short id : ids) byteBuffer.putShort(id);
        setData(groupId, byteBuffer.array());
        return this;
    }

    public ResendRequestPacket setGroupId(byte groupId) {
        return setData(groupId, getPacketIds());
    }

    public byte getGroupId() {
        return cutDataByTip(GROUP_ID_TIP)[0];
    }

    public ResendRequestPacket setPacketIds(short[] ids) {
        return setData(getGroupId(), ids);
    }

    public short[] getPacketIds() {
        ShortBuffer shortBuffer = ByteBuffer.wrap(getPacketIdsBytes()).asShortBuffer();
        short[] packetIds = new short[shortBuffer.remaining()];
        shortBuffer.get(packetIds);
        return packetIds;
    }

    private byte[] getPacketIdsBytes() {
        return cutDataByTip(PACKET_IDS_TIP);
    }

    @Override
    public String toString() {
        return "ResendRequestPacket{Ids:" + Arrays.toString(getPacketIds()) + "}";
    }
}
