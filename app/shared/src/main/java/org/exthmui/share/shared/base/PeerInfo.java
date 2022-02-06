package org.exthmui.share.shared.base;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import org.exthmui.share.shared.Constants;
import org.exthmui.share.shared.base.listeners.OnPeerUpdatedListener;

import java.util.List;

public interface PeerInfo {
    void registerOnPeerUpdatedListener(OnPeerUpdatedListener listener);
    void unregisterOnPeerUpdatedListener(OnPeerUpdatedListener listener);

    void notifyPeerUpdated();

    /**
     * @return The identifier of this peer. MUST conform with the form of "{@link #getConnectionType()}:[CUSTOMIZED_INTERNAL_ID]"
     */
    @NonNull String getId();

    /**
     * @return The display name of this peer which will be shown on user interface.
     */
    @NonNull String getDisplayName();

    /**
     * @return The type of this peer. MUST be contained in {@link Constants.DeviceType}
     */
    int getDeviceType();

    /**
     * @return Constant, the type of the connection e.g."wlandirect, "msnearshare"...
     */
    @NonNull String getConnectionType();

    /**
     * @return Whether this peer is paired/trusted
     */
    boolean isTrusted();

    /**
     * @see org.exthmui.share.shared.Constants.ConnectionStatus
     * @return The status of connection with this peer.
     */
    int getConnectionStatus();

    /**
     * @see org.exthmui.share.shared.Constants.TransmissionStatus
     * @return The status of transmission.
     */
    int getTransmissionStatus();

    /**
     * @return The detail message will be shown on user interface. Allowed to be customized by each type of Peer.
     */
    @Nullable String getDetailMessage();

    /**
     * @return The detail message will be shown on user interface. Allowed to be customized by each type of Peer.
     */
    default LiveData<List<WorkInfo>> getAllWorkInfosLiveData(Context ctx) {
        String workName = Constants.WORK_NAME_PREFIX_SEND + getId();
        return WorkManager.getInstance(ctx).getWorkInfosForUniqueWorkLiveData(workName);
    }
}
