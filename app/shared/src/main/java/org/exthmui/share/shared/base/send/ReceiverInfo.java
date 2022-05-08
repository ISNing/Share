package org.exthmui.share.shared.base.send;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import org.exthmui.share.shared.base.IPeer;
import org.exthmui.share.shared.base.PeerInfoTransfer;

public class ReceiverInfo extends PeerInfoTransfer {
    @IntRange(from = 5001, to = 65535) private int serverPort;

    public ReceiverInfo() {
    }

    public ReceiverInfo(@NonNull IPeer peer, @IntRange(from = 5001, to = 65535) int serverPort) {
        super(peer);
        this.serverPort = serverPort;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(@IntRange(from = 5001, to = 65535) int serverPort) {
        this.serverPort = serverPort;
    }
}

