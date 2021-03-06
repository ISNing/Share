package org.exthmui.share.udptransport.packets;

import java.net.DatagramPacket;

public final class CommandPacket extends AbstractCommandPacket<CommandPacket> {
    public CommandPacket(DatagramPacket packet) {
        super(packet);
    }

    public CommandPacket() {
        super();
    }

    public static CommandPacket fromDatagramPacket(DatagramPacket packet) {
        return new CommandPacket(packet);
    }
}
