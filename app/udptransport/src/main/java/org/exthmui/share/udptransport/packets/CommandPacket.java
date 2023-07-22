package org.exthmui.share.udptransport.packets;

import java.net.DatagramPacket;

public final class CommandPacket extends AbstractCommandPacket<CommandPacket> {
    public CommandPacket(DatagramPacket packet) {
        super(packet);
    }

    public CommandPacket() {
        super();
    }

    @Override
    public void legalCheck() {
        super.legalCheck();
    }

    public static CommandPacket of(DatagramPacket packet) {
        return new CommandPacket(packet);
    }
}
