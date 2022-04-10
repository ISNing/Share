package org.exthmui.share.shared.base;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;

import org.exthmui.share.shared.base.listeners.BaseEventListener;
import org.exthmui.share.shared.exceptions.FailedCastingPeerException;

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
     *
     * @param peer   Target peer file will be sent to
     * @param entity The {@link org.exthmui.share.shared.base.Entity} object of the file will be sent
     * @return Identifier of work. Can be used to get {@link androidx.work.WorkInfo} form
     * {@link androidx.work.WorkManager}
     */
    UUID send(T peer, Entity entity);

    UUID send(T peer, List<Entity> entities);

    @SuppressWarnings("unchecked")
    default UUID sendToPeerInfo(Context context, PeerInfo peer, Entity entity) throws FailedCastingPeerException {
        try {
            return send((T) peer, entity);
        } catch (ClassCastException e) {
            throw new FailedCastingPeerException(context, e);
        }
    }

    @SuppressWarnings("unchecked")
    default UUID sendToPeerInfo(Context context, PeerInfo peer, List<Entity> entities) throws FailedCastingPeerException {
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

    default Data genSendingInputData(PeerInfo peer, @NonNull Entity entity) {
        return new Data.Builder()
                .putString(Entity.FILE_URI, entity.getUri().toString())
                .putString(Entity.FILE_NAME, entity.getFileName())
                .putString(Entity.FILE_PATH, entity.getFilePath())
                .putLong(Entity.FILE_SIZE, entity.getFileSize())
                .putInt(Entity.FILE_TYPE, entity.getFileType())
                .putString(TARGET_PEER_ID, peer.getId())
                .build();
    }

    default Data genSendingInputData(PeerInfo peer, @NonNull List<Entity> entities) {
        String[] uriStrings = new String[entities.size()];
        String[] fileNames = new String[entities.size()];
        String[] filePaths = new String[entities.size()];
        long[] fileSizes = new long[entities.size()];
        int[] fileTypes = new int[entities.size()];
        for (int i = 0; i < entities.size(); i++) {
            uriStrings[i] = entities.get(i).getUri().toString();
            fileNames[i] = entities.get(i).getFileName();
            filePaths[i] = entities.get(i).getFilePath();
            fileSizes[i] = entities.get(i).getFileSize();
            fileTypes[i] = entities.get(i).getFileType();
        }
        return new Data.Builder()
                .putStringArray(Entity.FILE_URIS, uriStrings)
                .putStringArray(Entity.FILE_NAMES, fileNames)
                .putStringArray(Entity.FILE_PATHS, filePaths)
                .putLongArray(Entity.FILE_SIZES, fileSizes)
                .putIntArray(Entity.FILE_TYPES, fileTypes)
                .putString(TARGET_PEER_ID, peer.getId())
                .build();

    }
}
