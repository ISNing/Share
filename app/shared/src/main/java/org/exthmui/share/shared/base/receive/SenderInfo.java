package org.exthmui.share.shared.base.receive;

import androidx.annotation.NonNull;

import org.exthmui.share.shared.base.IPeer;
import org.exthmui.share.shared.base.PeerInfoTransfer;

public class SenderInfo extends PeerInfoTransfer {
    public SenderInfo() {
    }

    public SenderInfo(@NonNull IPeer peer) {
        super(peer);
    }
}
