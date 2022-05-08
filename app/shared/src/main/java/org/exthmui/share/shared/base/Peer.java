package org.exthmui.share.shared.base;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import org.exthmui.share.shared.events.PeerUpdatedEvent;
import org.exthmui.share.shared.listeners.OnPeerUpdatedListener;
import org.exthmui.share.shared.misc.Constants;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class Peer implements IPeer {

    private final Set<OnPeerUpdatedListener> mOnPeerUpdatedListeners = new HashSet<>();

    @Override
    public void registerOnPeerUpdatedListener(@NonNull OnPeerUpdatedListener listener) {
        mOnPeerUpdatedListeners.add(listener);
    }

    @Override
    public void unregisterOnPeerUpdatedListener(@NonNull OnPeerUpdatedListener listener) {
        mOnPeerUpdatedListeners.remove(listener);
    }

    @Override
    public void notifyPeerUpdated() {
        for (OnPeerUpdatedListener listener: mOnPeerUpdatedListeners) listener.onPeerUpdated(new PeerUpdatedEvent(this, this));
    }

    @NonNull
    @Override
    public LiveData<List<WorkInfo>> getAllWorkInfosLiveData(@NonNull Context ctx) {
        String workName = Constants.WORK_NAME_PREFIX_SEND + getId();
        return WorkManager.getInstance(ctx).getWorkInfosForUniqueWorkLiveData(workName);
    }

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();
}
