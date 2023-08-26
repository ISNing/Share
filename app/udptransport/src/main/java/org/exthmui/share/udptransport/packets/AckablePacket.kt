package org.exthmui.share.udptransport.packets

import org.exthmui.share.udptransport.Constants
import java.net.DatagramPacket

abstract class AckablePacket<T : AckablePacket<T>?> : AbstractCommandPacket<T> {
    constructor(packet: DatagramPacket) : super(packet)
    constructor(command: Constants.UdpCommand, dataLength: Int) : super(command, dataLength)
    constructor() : super()

    public abstract override fun clone(): AckablePacket<T>
    abstract fun isAck(): Boolean
    abstract fun reverseAck(): AckablePacket<T>
}