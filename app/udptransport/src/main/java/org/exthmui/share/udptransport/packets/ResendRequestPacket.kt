package org.exthmui.share.udptransport.packets

import android.util.Log
import org.apache.commons.lang3.ArrayUtils
import org.exthmui.share.udptransport.ByteUtils.Tip
import org.exthmui.share.udptransport.Constants
import java.net.DatagramPacket
import java.nio.ByteBuffer
import java.util.Arrays

class ResendRequestPacket : AckablePacket<ResendRequestPacket?> {
    constructor(packet: DatagramPacket?) : super(packet!!)
    constructor() : super(Constants.UdpCommand.PACKET_RESEND_REQ, MIN_DATA_LENGTH)

    override fun legalCheck() {
        super.legalCheck()
        if (command != Constants.UdpCommand.PACKET_RESEND_REQ ||
            super.data!!.size < MIN_DATA_LENGTH
        ) {
            Log.e(
                super.toString(),
                String.format("Illegal data: %s", Arrays.toString(toDatagramPacket().data))
            )
            throw IllegalArgumentException()
        }
    }

    public override fun clone(): ResendRequestPacket {
        return ResendRequestPacket(toDatagramPacket())
    }

    override fun isAck(): Boolean {
        TODO("Not yet implemented")
    }

    override fun reverseAck(): AckablePacket<ResendRequestPacket?> {
        TODO("Not yet implemented")
    }

    fun setData(groupId: Byte, idsBytes: ByteArray?): ResendRequestPacket {
        setData(ArrayUtils.addFirst(idsBytes, groupId))
        return this
    }

    fun setData(groupId: Byte, ids: ShortArray): ResendRequestPacket {
        val byteBuffer = ByteBuffer.allocate(ids.size * 2)
        for (id in ids) byteBuffer.putShort(id)
        setData(groupId, byteBuffer.array())
        return this
    }

    fun setGroupId(groupId: Byte): ResendRequestPacket {
        return setData(groupId, packetIds)
    }

    val groupId: Byte
        get() = cutDataByTip(GROUP_ID_TIP)[0]

    fun setPacketIds(ids: ShortArray): ResendRequestPacket {
        return setData(groupId, ids)
    }

    val packetIds: ShortArray
        get() {
            val shortBuffer = ByteBuffer.wrap(packetIdsBytes).asShortBuffer()
            val packetIds = ShortArray(shortBuffer.remaining())
            shortBuffer[packetIds]
            return packetIds
        }
    private val packetIdsBytes: ByteArray
        private get() = cutDataByTip(PACKET_IDS_TIP)

    override fun toString(): String {
        return "ResendRequestPacket{Ids:" + Arrays.toString(packetIds) + "}"
    }

    companion object {
        const val MIN_DATA_LENGTH = 1
        val GROUP_ID_TIP = Tip(0, 0)
        val PACKET_IDS_TIP = Tip(1)
    }
}
