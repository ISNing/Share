package org.exthmui.share.msnearshare;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.connecteddevices.remotesystems.RemoteSystem;
import com.microsoft.connecteddevices.remotesystems.RemoteSystemKinds;
import com.microsoft.connecteddevices.remotesystems.RemoteSystemStatus;

import org.exthmui.share.shared.base.IConnectionType;
import org.exthmui.share.shared.base.Peer;
import org.exthmui.share.shared.listeners.OnPeerUpdatedListener;
import org.exthmui.share.shared.misc.Constants;

import java.util.Objects;

public class NearSharePeer extends Peer {

    final RemoteSystem remoteSystem;

    public NearSharePeer(RemoteSystem remoteSystem) {
        this.remoteSystem = remoteSystem;
    }

    @Override
    public void registerOnPeerUpdatedListener(@NonNull OnPeerUpdatedListener listener) {

    }

    @Override
    public void notifyPeerUpdated() {

    }

    @NonNull
    @Override
    public String getId() {
        return String.format(Constants.PEER_ID_STRING, getConnectionType().getCode(), remoteSystem.getId());
    }

    @NonNull
    @Override
    public String getDisplayName() {
        return getDisplayName(remoteSystem.getDisplayName());
    }

    @Nullable
    @Override
    public String getProtocolVersion() {
        return null;
    }

    @Override
    public int getUid() {
        return 0;
    }

    @Nullable
    @Override
    public String getAccountServerSign() {
        return null;
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
    public IConnectionType getConnectionType() {
        return new Metadata();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NearSharePeer)) return false;
        NearSharePeer that = (NearSharePeer) o;
        return Objects.equals(remoteSystem, that.remoteSystem);
    }

    @Override
    public int hashCode() {
        return Objects.hash(remoteSystem);
    }
}
