package org.exthmui.share.msnearshare;

import androidx.annotation.NonNull;

import com.microsoft.connecteddevices.remotesystems.RemoteSystem;
import com.microsoft.connecteddevices.remotesystems.RemoteSystemKinds;
import com.microsoft.connecteddevices.remotesystems.RemoteSystemStatus;

import org.exthmui.share.shared.Constants;
import org.exthmui.share.shared.base.Peer;
import org.exthmui.share.shared.base.listeners.OnPeerUpdatedListener;

import java.util.Objects;

public class NearSharePeer extends Peer {
    public static final String CONNECTION_CODE = Constants.CONNECTION_CODE_MSNEARSHARE;
    final RemoteSystem remoteSystem;

    public NearSharePeer(RemoteSystem remoteSystem) {
        this.remoteSystem = remoteSystem;
    }

    @Override
    public void registerOnPeerUpdatedListener(OnPeerUpdatedListener listener) {

    }

    @Override
    public void notifyPeerUpdated() {

    }

    @NonNull
    @Override
    public String getId() {
        return String.format(Constants.PEER_ID_STRING, getConnectionType(), remoteSystem.getId());
    }

    @NonNull
    @Override
    public String getDisplayName() {
        return Objects.requireNonNull(remoteSystem.getDisplayName());
    }

    @Override
    public int getDeviceType() {
        String kind = remoteSystem.getKind();
        int deviceType = Constants.DeviceType.UNKNOWN.getNumVal();
        if(kind.equals(RemoteSystemKinds.Desktop())) deviceType = Constants.DeviceType.DESKTOP.getNumVal();
        else if(kind.equals(RemoteSystemKinds.Laptop())) deviceType = Constants.DeviceType.LAPTOP.getNumVal();
        else if(kind.equals(RemoteSystemKinds.Phone())) deviceType = Constants.DeviceType.PHONE.getNumVal();
        else if(kind.equals(RemoteSystemKinds.Tablet())) deviceType = Constants.DeviceType.TABLET.getNumVal();
        return deviceType;
    }

    @NonNull
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
        if (remoteSystem.getStatus().equals(RemoteSystemStatus.UNAVAILABLE)) stat = Constants.ConnectionStatus.UNAVAILABLE.getNumVal();
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
