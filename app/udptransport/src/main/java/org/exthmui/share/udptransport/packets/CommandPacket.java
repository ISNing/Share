package org.exthmui.share.udptransport.packets;

import androidx.annotation.NonNull;

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

    @NonNull
    @Override
    public CommandPacket clone() {
        return new CommandPacket(this.toDatagramPacket());
    }

    public static CommandPacket of(DatagramPacket packet) {
        return new CommandPacket(packet);
    }


}
