package org.exthmui.share.udptransport.packets;

import androidx.annotation.IntRange;

import org.apache.commons.lang3.ArrayUtils;
import org.exthmui.share.udptransport.ByteUtils;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketAddress;

public abstract class AbstractCommandPacket <T extends AbstractCommandPacket<T>> {
    public static final int HEADER_LENGTH = 2;

    public static final int[] COMMAND_TIP = {0, 0};
    public static final int[] CONN_ID_TIP = {1, 1};
    public static final int[] DATA_TIP = {2};

    private final DatagramPacket packet;

    protected AbstractCommandPacket(DatagramPacket packet) {
        this.packet = packet;
        legalCheck();
    }

    public AbstractCommandPacket() {
        packet = new DatagramPacket(new byte[]{0x0, 0x0}, 0, 2);
    }

    public DatagramPacket toDatagramPacket() {
        return packet;
    }

    public abstract void legalCheck();

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
        return ByteUtils.cutBytesByTip(DATA_TIP, packet.getData(), packet.getOffset());
    }

    @SuppressWarnings("unchecked")
    public T setData(byte[] buf) {
        byte[] bytes = ArrayUtils.addAll(new byte[]{getCommand(), getConnId()}, buf);
        packet.setData(bytes);
        packet.setLength(bytes.length);
        return (T) this;
    }

    public byte getCommand() {
        return ByteUtils.cutBytesByTip(COMMAND_TIP, packet.getData(), packet.getOffset())[0];
    }

    @SuppressWarnings("unchecked")
    public T setCommand(byte command) {
        packet.setData(ArrayUtils.addAll(new byte[]{command, getConnId()}, getDataAbstract()));
        return (T) this;
    }

    public byte getConnId() {
        return ByteUtils.cutBytesByTip(CONN_ID_TIP, packet.getData(), packet.getOffset())[0];
    }

    @SuppressWarnings("unchecked")
    public T setConnId(byte connId) {
        packet.setData(ArrayUtils.addAll(new byte[]{getCommand(), connId}, getDataAbstract()));
        return (T) this;
    }

    private void updateData() {
        packet.setData(ArrayUtils.addAll(new byte[]{getCommand(), getConnId()}, getDataAbstract()));
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

    final byte[] cutDataByTip(int[] tip, @IntRange(from = 0) int initOffset) {
        return ByteUtils.cutBytesByTip(tip, AbstractCommandPacket.this.getDataAbstract(), initOffset);
    }

    final byte[] cutDataByTip(int[] tip) {
        return cutDataByTip(tip, 0);
    }
}
