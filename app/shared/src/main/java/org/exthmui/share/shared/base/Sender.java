package org.exthmui.share.shared.base;

import android.util.Log;

import androidx.work.Data;

import org.exthmui.share.shared.base.listeners.BaseEventListener;

import java.util.List;
import java.util.UUID;

/**
 * IMPORTANT: Should have a static method "getInstance({@link android.content.Context} context)"
 */
public interface Sender<T extends PeerInfo > {
    String TAG = "Sender";
    String TARGET_PEER_ID="TARGET_PEER_ID";

    /**
     * Send an entity
     * @param peer Target peer file will be sent to
     * @param entity The {@link org.exthmui.share.shared.base.Entity} object of the file will be sent
     * @return Identifier of work. Can be used to get {@link androidx.work.WorkInfo} form
     * {@link androidx.work.WorkManager}
     */
    UUID send(T peer, Entity entity);
    UUID[] send(T peer, List<Entity> entities);

    @SuppressWarnings("unchecked")
    default UUID sendToPeerInfo(PeerInfo peer, Entity entity) {
        try {
            return send((T) peer, entity);
        } catch (ClassCastException e) {
            Log.e(TAG, "Failed casting peer");
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    default UUID[] sendToPeerInfo(PeerInfo peer, List<Entity> entities) {
        try {
            return send((T) peer, entities);
        } catch (ClassCastException e) {
            Log.e(TAG, "Failed casting peer");
        }
        return null;
    }

    void registerListener(BaseEventListener listener);
    void unregisterListener(BaseEventListener listener);
    void initialize();
    boolean isInitialized();
    boolean isFeatureAvailable();
    default Data genSendingInputData(PeerInfo peer, Entity entity) {
        return new Data.Builder()
                .putString(Entity.FILE_URI, entity.getUri().toString())
                .putString(Entity.FILE_NAME, entity.getFileName())
                .putString(Entity.FILE_PATH, entity.getFilePath())
                .putLong(Entity.FILE_SIZE, entity.getFileSize())
                .putInt(Entity.FILE_TYPE, entity.getFileType())
                .putString(TARGET_PEER_ID, peer.getId())
                .build();

    }
}
