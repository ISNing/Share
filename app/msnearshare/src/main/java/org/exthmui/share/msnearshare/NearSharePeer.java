package org.exthmui.share.msnearshare;

import com.microsoft.connecteddevices.remotesystems.RemoteSystem;
import org.exthmui.share.beans.Peer;

public class NearSharePeer extends Peer {
    public NearSharePeer(RemoteSystem remoteSystem){
        this.setId(remoteSystem.getId());
        //TODO:WIP
    }
}
