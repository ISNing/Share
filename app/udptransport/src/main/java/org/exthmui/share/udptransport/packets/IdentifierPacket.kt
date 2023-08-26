package org.exthmui.share.udptransport.packets

import android.util.Log
import org.apache.commons.lang3.ArrayUtils
import org.exthmui.share.udptransport.ByteUtils.Tip
import org.exthmui.share.udptransport.Constants
import java.net.DatagramPacket
import java.util.Arrays

class IdentifierPacket : AckablePacket<IdentifierPacket?> {
    constructor(packet: DatagramPacket?) : super(packet!!)
    constructor() : super(Constants.UdpCommand.IDENTIFIER, DATA_LENGTH)

    override fun legalCheck() {
        super.legalCheck()
        if (command != Constants.UdpCommand.IDENTIFIER || super.data!!.size != DATA_LENGTH) {
            Log.e(
                super.toString(),
                String.format("Illegal data: %s", Arrays.toString(toDatagramPacket().data))
            )
            throw IllegalArgumentException()
        }
    }

    public override fun clone(): IdentifierPacket {
        return IdentifierPacket(
            DatagramPacket(
                toDatagramPacket().data.clone(),
                toDatagramPacket().offset, toDatagramPacket().length
            )
        )
    }

    val identifier: Constants.Identifier?
        get() = Constants.Identifier.parse(identifierByte)
    private val identifierByte: Byte
        private get() = cutDataByTip(IDENTIFIER_TIP)[0]

    fun setIdentifier(identifier: Constants.Identifier): IdentifierPacket {
        return setIdentifierByte(identifier.identifier)
    }

    fun setIdentifierByte(identifier: Byte): IdentifierPacket {
        val extra = extra
        val data = ArrayUtils.addFirst(extra, identifier)
        setData(data)
        return this
    }

    val extra: ByteArray
        get() = cutDataByTip(EXTRA_TIP)

    fun setExtra(extra: ByteArray?): IdentifierPacket {
        val e = ByteArray(EXTRA_TIP.end - EXTRA_TIP.offset + 1)
        if (extra != null) System.arraycopy(extra, 0, e, 0, Math.min(extra.size, e.size))
        setData(ArrayUtils.addFirst(e, identifierByte))
        return this
    }

    override fun isAck() = identifier?.isAck ?: false

    override fun reverseAck() = turnIdentifier()

    /**
     * Change non-ack identifier to ack identifier or vice versa
     */
    fun turnIdentifier(): IdentifierPacket {
        setIdentifier(identifier!!.correspondIdentifier)
        return this
    }

    override fun toString(): String {
        return String.format(
            "IdentifierPacket{Identifier: %s, extra: %s}", identifier, Arrays.toString(
                extra
            )
        )
    }

    companion object {
        const val DATA_LENGTH = 13
        val IDENTIFIER_TIP = Tip(0, 0)
        val EXTRA_TIP = Tip(1, 12)
    }
}
