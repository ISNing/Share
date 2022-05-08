package org.exthmui.share.shared.base.receive;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import org.exthmui.share.shared.base.IPeer;
import org.exthmui.share.shared.base.PeerInfoTransfer;

public class SenderInfo extends PeerInfoTransfer {
    @IntRange(from = 5001, to = 65535) private int clientPort;

    public SenderInfo() {
    }

    public SenderInfo(@NonNull IPeer peer, @IntRange(from = 5001, to = 65535) int clientPort) {
        super(peer);
        this.clientPort = clientPort;
    }

    public int getClientPort() {
        return clientPort;
    }

    public void setClientPort(@IntRange(from = 5001, to = 65535) int clientPort) {
        this.clientPort = clientPort;
    }
}
