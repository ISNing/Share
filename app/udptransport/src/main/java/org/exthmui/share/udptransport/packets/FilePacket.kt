package org.exthmui.share.udptransport.packets

import android.util.Log
import org.apache.commons.lang3.ArrayUtils
import org.exthmui.share.udptransport.ByteUtils
import org.exthmui.share.udptransport.ByteUtils.Tip
import org.exthmui.share.udptransport.Constants
import java.net.DatagramPacket
import java.util.Arrays
import java.util.Locale

class FilePacket : AbstractCommandPacket<FilePacket?> {
    constructor(packet: DatagramPacket?) : super(packet!!)
    constructor() : super(Constants.UdpCommand.FILE_PACKET, MIN_DATA_LENGTH)

    override fun legalCheck() {
        super.legalCheck()
        if (command != Constants.UdpCommand.FILE_PACKET ||
            super.data!!.size < MIN_DATA_LENGTH
        ) {
            Log.e(
                super.toString(),
                String.format("Illegal data: %s", Arrays.toString(toDatagramPacket().data))
            )
            throw IllegalArgumentException()
        }
    }

    private val flag: Byte
        private get() = cutDataByTip(FLAG_TIP)[0]

    private fun setFlags(flags: Byte): FilePacket {
        setData(packetIdBytes, groupId, flags, data)
        return this
    }

    val groupId: Byte
        get() = cutDataByTip(GROUP_ID_TIP)[0]

    fun setGroupId(groupId: Byte): FilePacket {
        setData(packetIdBytes, groupId, flag, data)
        return this
    }

    val packetId: Short
        get() = ByteUtils.bytesToShort(packetIdBytes)
    private val packetIdBytes: ByteArray
        private get() = cutDataByTip(PACKET_ID_TIP)

    fun setPacketId(packetId: ByteArray?): FilePacket {
        setData(packetId, groupId, flag, data)
        return this
    }

    fun setPacketId(packetId: Short): FilePacket {
        return setPacketId(ByteUtils.shortToBytes(packetId))
    }

    fun setDataLength(length: Int): FilePacket? {
        return setData(Arrays.copyOfRange(data, 0, length))
    }

    override val data: ByteArray
        get() = cutDataByTip(DATA_TIP)

    override fun setData(data: ByteArray): FilePacket {
        setData(data, data.size)
        return this
    }

    public override fun clone(): FilePacket {
        return FilePacket(toDatagramPacket())
    }

    fun setData(data: ByteArray?, length: Int): FilePacket {
        setData(data, length, 0)
        return this
    }

    fun setData(data: ByteArray?, length: Int, offset: Int): FilePacket {
        setData(packetIdBytes, groupId, flag, data, length, offset)
        return this
    }

    fun setData(
        packetIdBytes: ByteArray?,
        groupId: Byte,
        flags: Byte,
        data: ByteArray
    ): FilePacket {
        setData(packetIdBytes, groupId, flags, data, data.size, 0)
        return this
    }

    fun setData(
        packetIdBytes: ByteArray?,
        groupId: Byte,
        flags: Byte,
        data: ByteArray?,
        length: Int
    ): FilePacket {
        setData(packetIdBytes, groupId, flags, data, length, 0)
        return this
    }

    fun setData(
        packetIdBytes: ByteArray?,
        groupId: Byte,
        flags: Byte,
        data: ByteArray?,
        length: Int,
        offset: Int
    ): FilePacket {
        super.setData(
            ArrayUtils.addAll(
                ArrayUtils.add(
                    ArrayUtils.addFirst(packetIdBytes, groupId),
                    flags
                ), *Arrays.copyOfRange(data, offset, offset + length)
            )
        )
        return this
    }

    override fun toString(): String {
        return String.format(
            Locale.ROOT,
            "FilePacket{GroupId: %d, PacketId: %d, Flag: %s}",
            groupId,
            packetId,
            flag
        )
    }

    companion object {
        const val MIN_DATA_LENGTH = 4
        val GROUP_ID_TIP = Tip(0, 0)
        val PACKET_ID_TIP = Tip(1, 2)
        val FLAG_TIP = Tip(3, 3)
        val DATA_TIP = Tip(4)
        fun of(packet: DatagramPacket?): FilePacket {
            return FilePacket(packet)
        }
    }
}
