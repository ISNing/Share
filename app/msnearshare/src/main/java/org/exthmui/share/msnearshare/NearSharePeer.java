package org.exthmui.share.msnearshare;

import com.microsoft.connecteddevices.remotesystems.RemoteSystem;
import com.microsoft.connecteddevices.remotesystems.RemoteSystemKinds;
import com.microsoft.connecteddevices.remotesystems.RemoteSystemStatus;
import org.exthmui.share.base.PeerInfo;
import org.exthmui.share.misc.Constants;

public class NearSharePeer implements PeerInfo {
    public static final String CONNECTION_CODE = "msnearshare";
    RemoteSystem remoteSystem;
    public NearSharePeer(RemoteSystem remoteSystem){
        this.remoteSystem = remoteSystem;
    }

    @Override
    public String getId() {
        return remoteSystem.getId();
    }

    @Override
    public String getDisplayName() {
        return remoteSystem.getDisplayName();
    }

    @Override
    public int getDeviceType() {
        String kind = remoteSystem.getKind();
        int deviceType = Constants.DeviceTypes.UNKNOWN.getNumVal();
        if(kind.equals(RemoteSystemKinds.Desktop())) deviceType = Constants.DeviceTypes.DESKTOP.getNumVal();
        else if(kind.equals(RemoteSystemKinds.Laptop())) deviceType = Constants.DeviceTypes.LAPTOP.getNumVal();
        else if(kind.equals(RemoteSystemKinds.Phone())) deviceType = Constants.DeviceTypes.PHONE.getNumVal();
        else if(kind.equals(RemoteSystemKinds.Tablet())) deviceType = Constants.DeviceTypes.DESKTOP.getNumVal();
        return deviceType;
    }

    @Override
    public String getConnectionType() {
        return CONNECTION_CODE;
    }

    @Override
    public boolean isTrusted() {
        return false;
    }

    @Override
    public int getConnectionStatus() {
        int stat = Constants.ConnectionStatus.UNKNOWN.getNumVal();
        if (remoteSystem.getStatus().equals(RemoteSystemStatus.AVAILABLE)) stat = Constants.ConnectionStatus.AVAILABLE.getNumVal();
        if(remoteSystem.getStatus().equals(RemoteSystemStatus.UNAVAILABLE)) stat = Constants.ConnectionStatus.UNAVAILABLE.getNumVal();
        return stat;
    }

    @Override
    public int getTransmissionStatus() {
        return 0;
    }

    @Override
    public String getDetailMessage() {
        return null;
    }
}
