package org.exthmui.share.udptransport.packets

import java.net.DatagramPacket

class CommandPacket : AbstractCommandPacket<CommandPacket?> {
    constructor(packet: DatagramPacket?) : super(packet!!)
    constructor() : super()

    override fun legalCheck() {
        super.legalCheck()
    }

    public override fun clone(): CommandPacket {
        return CommandPacket(toDatagramPacket())
    }

    companion object {
        fun of(packet: DatagramPacket?): CommandPacket {
            return CommandPacket(packet)
        }
    }
}
