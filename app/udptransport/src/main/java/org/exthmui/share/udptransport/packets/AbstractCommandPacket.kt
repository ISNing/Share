package org.exthmui.share.udptransport.packets

import android.util.Log
import androidx.annotation.IntRange
import org.apache.commons.lang3.ArrayUtils
import org.exthmui.share.udptransport.ByteUtils
import org.exthmui.share.udptransport.ByteUtils.Tip
import org.exthmui.share.udptransport.Constants
import org.exthmui.share.udptransport.Constants.UdpCommand
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.SocketAddress
import java.util.Arrays

abstract class AbstractCommandPacket<T : AbstractCommandPacket<T>?> : Cloneable {
    private val packet: DatagramPacket

    protected constructor(packet: DatagramPacket) {
        this.packet = packet
        legalCheck()
    }

    protected constructor(command: UdpCommand, dataLength: Int) {
        val rawData = ByteArray(HEADER_LENGTH + dataLength + 1)
        rawData[rawData.size - 1] = Constants.PACKET_END_FLAG
        packet = DatagramPacket(rawData, 0, rawData.size)
        setCommand(command)
        legalCheck()
    }

    constructor() : this(UdpCommand.UNKNOWN, 0)

    fun toDatagramPacket(): DatagramPacket {
        return packet
    }

    open fun legalCheck() {
        if (toDatagramPacket().length < HEADER_LENGTH ||
            packet.data[packet.data.size - 1] != Constants.PACKET_END_FLAG
        ) {
            Log.e(
                super.toString(),
                String.format("Illegal data: %s", Arrays.toString(toDatagramPacket().data))
            )
            throw IllegalArgumentException()
        }
    }

    val address: InetAddress
        get() = packet.address
    val port: Int
        get() = packet.port
    open val data: ByteArray?
        get() = dataAbstract
    private val dataAbstract: ByteArray
        private get() = ByteUtils.removeLastElement(
            ByteUtils.cutBytesByTip(
                DATA_TIP,
                packet.data,
                packet.offset
            )
        )

    open fun setData(buf: ByteArray): T {
        val bytes = ArrayUtils.add(
            ArrayUtils.addAll(
                byteArrayOf(
                    commandByte, connId
                ), *buf
            ), Constants.PACKET_END_FLAG
        )
        packet.data = bytes
        packet.length = bytes.size
        return this as T
    }

    val command: UdpCommand?
        get() = UdpCommand.parse(commandByte)
    private val commandByte: Byte
        private get() = ByteUtils.cutBytesByTip(
            COMMAND_TIP,
            packet.data,
            packet.offset
        )[0]

    fun setCommand(command: UdpCommand): T {
        packet.data[0] = command.cmd
        return this as T
    }

    val connId: Byte
        get() = ByteUtils.cutBytesByTip(
            CONN_ID_TIP,
            packet.data,
            packet.offset
        )[0]

    fun setConnId(connId: Byte): T {
        packet.data[1] = connId
        return this as T
    }

    fun setAddress(iAddr: InetAddress?): T {
        packet.address = iAddr
        return this as T
    }

    fun setPort(iport: Int): T {
        packet.port = iport
        return this as T
    }

    fun setSocketAddress(address: SocketAddress?): T {
        packet.socketAddress = address
        return this as T
    }

    val socketAddress: SocketAddress
        get() = packet.socketAddress

    @JvmOverloads
    fun cutDataByTip(tip: Tip?, @IntRange(from = 0) initOffset: Int = 0): ByteArray {
        return ByteUtils.cutBytesByTip(tip!!, dataAbstract, initOffset)
    }

    public abstract override fun clone(): AbstractCommandPacket<T>

    companion object {
        const val HEADER_LENGTH = 2
        val COMMAND_TIP = Tip(0, 0)
        val CONN_ID_TIP = Tip(1, 1)
        val DATA_TIP = Tip(2)
    }
}
