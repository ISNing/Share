package org.exthmui.share.udptransport.packets;

import android.util.Log;

import org.apache.commons.lang3.ArrayUtils;
import org.exthmui.share.udptransport.ByteUtils;
import org.exthmui.share.udptransport.Constants;

import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ResendRequestPacket extends AbstractCommandPacket<ResendRequestPacket> {
    public static final int MIN_DATA_LENGTH = 1;

    public static final int[] PACKET_IDS_TIP = {0};

    public ResendRequestPacket(DatagramPacket packet) {
        super(packet);
    }

    public ResendRequestPacket() {
        this(new DatagramPacket(new byte[]{Constants.COMMAND_PACKET_RESEND_REQ, 0x0, 0x0},
                HEADER_LENGTH + MIN_DATA_LENGTH));
        byte[] bytes = new byte[Constants.BUF_LEN_MAX_HI];
        bytes[0] = Constants.COMMAND_PACKET_RESEND_REQ;
        toDatagramPacket().setData(bytes);
        toDatagramPacket().setLength(HEADER_LENGTH + MIN_DATA_LENGTH);
    }

    @Override
    public void legalCheck() {
        if (getCommand() != Constants.COMMAND_PACKET_RESEND_REQ ||
                toDatagramPacket().getLength() < HEADER_LENGTH + MIN_DATA_LENGTH) {
            Log.e(this.toString(), String.format("Illegal data: %s", Arrays.toString(toDatagramPacket().getData())));
            throw new IllegalArgumentException();
        }
    }

    public ResendRequestPacket setPacketIds(short[] ids) {
        List<Byte> idsBytes = new ArrayList<>(2 * ids.length);
        for (short id: ids) {
            byte[] bytes = ByteUtils.shortToBytes(id);
            idsBytes.add(bytes[0]);
            idsBytes.add(bytes[1]);
        }
        setData(ArrayUtils.toPrimitive(idsBytes.toArray(new Byte[0])));
        return this;
    }

    public short[] getPacketIds() {
        byte[] idsBytes = cutDataByTip(PACKET_IDS_TIP);
        List<Short> ids = new ArrayList<>();
        byte[] bytes = new byte[2];
        for (int i = 0; i + 1 < idsBytes.length; i += 2) {
            bytes[0] = idsBytes[i];
            bytes[1] = idsBytes[i + 1];
            ids.add(ByteUtils.bytesToShort(bytes));
        }
        return ArrayUtils.toPrimitive(ids.toArray(new Short[0]));
    }
}
