package org.exthmui.share.shared.base.send;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;

import org.exthmui.share.shared.base.Entity;
import org.exthmui.share.shared.base.IConnectionType;
import org.exthmui.share.shared.base.IPeer;
import org.exthmui.share.shared.exceptions.FailedCastingPeerException;
import org.exthmui.utils.listeners.BaseEventListener;

import java.util.List;
import java.util.UUID;

/**
 * IMPORTANT: Should have a static method "getInstance({@link android.content.Context} context)"
 */
public interface Sender<T extends IPeer> {
    String TAG = "Sender";
    String TARGET_PEER_ID = "TARGET_PEER_ID";
    String TARGET_PEER_NAME = "TARGET_PEER_NAME";

    @NonNull IConnectionType getConnectionType();
    /**
     * Send an entity
     *
     * @param peer   Target peer file will be sent to
     * @param entities A list of {@link org.exthmui.share.shared.base.Entity} object of files will be sent
     * @return Identifier of work. Can be used to get {@link androidx.work.WorkInfo} form
     * {@link androidx.work.WorkManager}
     */
    @NonNull
    UUID send(T peer, List<Entity> entities);

    @NonNull
    @SuppressWarnings("unchecked")
    default UUID sendToPeerInfo(@NonNull Context context, IPeer peer, List<Entity> entities) throws FailedCastingPeerException {
        try {
            return send((T) peer, entities);
        } catch (ClassCastException e) {
            throw new FailedCastingPeerException(context, e);
        }
    }

    void registerListener(BaseEventListener listener);
    void unregisterListener(BaseEventListener listener);
    void initialize();
    boolean isInitialized();

    boolean isFeatureAvailable();

    @NonNull
    default Data genSendingInputData(@NonNull IPeer peer, @NonNull List<Entity> entities) {
        return new Data.Builder()
                .putStringArray(Entity.ENTITIES, Entity.entitiesToStrings(entities.toArray(new Entity[0])))
                .putString(TARGET_PEER_ID, peer.getId())
                .putString(TARGET_PEER_NAME, peer.getDisplayName())
                .build();
    }
}
