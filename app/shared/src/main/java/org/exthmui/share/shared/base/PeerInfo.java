package org.exthmui.share.shared.base;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import org.exthmui.share.shared.listeners.OnPeerUpdatedListener;
import org.exthmui.share.shared.misc.Constants;

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
     * @return [Optional] The protocol version of this peer. (Only used in plugin internal)
     */
    @Nullable String getProtocolVersion();

    /**
     * @return [Optional] The user identifier of this peer.
     * (Used to judge whether the peer is trustable, use with {@link #getAccountServerSign()})
     */
    int getUid();

    /**
     * @return [Optional] The sign of the account server where user of this peer logged in.
     * (Used to judge whether the peer is trustable, use with {@link #getUid()})
     */
    @Nullable String getAccountServerSign();

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
     * @see Constants.ConnectionStatus
     * @return The status of connection with this peer.
     */
    int getConnectionStatus();

    /**
     * @see Constants.TransmissionStatus
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
