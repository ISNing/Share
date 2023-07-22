package org.exthmui.share.udptransport.packets;

import static org.exthmui.share.udptransport.Constants.PACKET_END_FLAG;

import android.util.Log;

import androidx.annotation.IntRange;

import org.apache.commons.lang3.ArrayUtils;
import org.exthmui.share.udptransport.ByteUtils;
import org.exthmui.share.udptransport.Constants;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.Arrays;

public abstract class AbstractCommandPacket<T extends AbstractCommandPacket<T>> {
    public static final int HEADER_LENGTH = 2;

    public static final ByteUtils.Tip COMMAND_TIP = new ByteUtils.Tip(0, 0);
    public static final ByteUtils.Tip CONN_ID_TIP = new ByteUtils.Tip(1, 1);
    public static final ByteUtils.Tip DATA_TIP = new ByteUtils.Tip(2);

    private final DatagramPacket packet;

    protected AbstractCommandPacket(DatagramPacket packet) {
        this.packet = packet;
        legalCheck();
    }

    protected AbstractCommandPacket(Constants.UdpCommand command, int dataLength) {
        byte[] rawData = new byte[HEADER_LENGTH + dataLength + 1];
        rawData[rawData.length - 1] = PACKET_END_FLAG;
        this.packet = new DatagramPacket(rawData, 0, rawData.length);
        setCommand(command);
        legalCheck();
    }

    public AbstractCommandPacket() {
        this(Constants.UdpCommand.UNKNOWN, 0);
    }

    public DatagramPacket toDatagramPacket() {
        return packet;
    }

    public void legalCheck() {
        if (toDatagramPacket().getLength() < HEADER_LENGTH ||
                packet.getData()[packet.getData().length - 1] != PACKET_END_FLAG) {
            Log.e(super.toString(), String.format("Illegal data: %s", Arrays.toString(toDatagramPacket().getData())));
            throw new IllegalArgumentException();
        }
    }

    public InetAddress getAddress() {
        return packet.getAddress();
    }

    public int getPort() {
        return packet.getPort();
    }

    public byte[] getData() {
        return getDataAbstract();
    }

    private byte[] getDataAbstract() {
        return ByteUtils.removeLastElement(ByteUtils.cutBytesByTip(DATA_TIP, packet.getData(), packet.getOffset()));
    }

    @SuppressWarnings("unchecked")
    public T setData(byte[] buf) {
        byte[] bytes = ArrayUtils.add(ArrayUtils.addAll(new byte[]{getCommandByte(), getConnId()}, buf), PACKET_END_FLAG);
        packet.setData(bytes);
        packet.setLength(bytes.length);
        return (T) this;
    }

    public Constants.UdpCommand getCommand() {
        return Constants.UdpCommand.parse(getCommandByte());
    }

    private byte getCommandByte() {
        return ByteUtils.cutBytesByTip(COMMAND_TIP, packet.getData(), packet.getOffset())[0];
    }

    @SuppressWarnings("unchecked")
    public T setCommand(Constants.UdpCommand command) {
        packet.getData()[0] = command.getCmd();
        return (T) this;
    }

    public byte getConnId() {
        return ByteUtils.cutBytesByTip(CONN_ID_TIP, packet.getData(), packet.getOffset())[0];
    }

    @SuppressWarnings("unchecked")
    public T setConnId(byte connId) {
        packet.getData()[1] = connId;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setAddress(InetAddress iAddr) {
        packet.setAddress(iAddr);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setPort(int iport) {
        packet.setPort(iport);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setSocketAddress(SocketAddress address) {
        packet.setSocketAddress(address);
        return (T) this;
    }

    public SocketAddress getSocketAddress() {
        return packet.getSocketAddress();
    }

    final byte[] cutDataByTip(ByteUtils.Tip tip, @IntRange(from = 0) int initOffset) {
        return ByteUtils.cutBytesByTip(tip, AbstractCommandPacket.this.getDataAbstract(), initOffset);
    }

    final byte[] cutDataByTip(ByteUtils.Tip tip) {
        return cutDataByTip(tip, 0);
    }
}
