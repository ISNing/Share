package org.exthmui.share.msnearshare;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Data;
import androidx.work.WorkerParameters;
import com.microsoft.connecteddevices.AsyncOperationWithProgress;
import com.microsoft.connecteddevices.remotesystems.commanding.RemoteSystemConnectionRequest;
import com.microsoft.connecteddevices.remotesystems.commanding.nearshare.NearShareFileProvider;
import com.microsoft.connecteddevices.remotesystems.commanding.nearshare.NearShareHelper;
import com.microsoft.connecteddevices.remotesystems.commanding.nearshare.NearShareProgress;
import com.microsoft.connecteddevices.remotesystems.commanding.nearshare.NearShareStatus;
import org.exthmui.share.base.SendingWorker;
import org.exthmui.share.base.events.AcceptedOrRejectedEvent;
import org.exthmui.share.base.events.SendFailedEvent;
import org.exthmui.share.base.events.SentEvent;
import org.exthmui.share.base.listeners.BaseEventListener;
import org.exthmui.share.base.listeners.OnAcceptedOrRejectedListener;
import org.exthmui.share.base.listeners.OnProgressUpdatedListener;
import org.exthmui.share.misc.Constants;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class NearShareSendingWorker extends SendingWorker {

    private static final String PROGRESS = "PROGRESS";

    private long totalBytesToSend;
    private long bytesSent;
    private int totalFilesToSend;
    private int filesSent;
    private Collection<BaseEventListener> mListeners;

    private static final Class<? extends BaseEventListener>[] LISTENER_TYPES_ALLOWED;

    static {
        LISTENER_TYPES_ALLOWED = (Class<? extends BaseEventListener>[]) new Class<?>[]
                {
                        OnAcceptedOrRejectedListener.class,
                        OnProgressUpdatedListener.class
                };
    }

    public void addListener(BaseEventListener listener) {
        if (mListeners == null) mListeners = new HashSet<>();
        for (Class<? extends BaseEventListener> t : LISTENER_TYPES_ALLOWED) {
            if (listener.getClass().isAssignableFrom(t)) {
                mListeners.add(listener);
                break;
            }
        }
    }

    private void notifyListeners(EventObject event) {
        List<Method> methodsToPerform = new ArrayList<>();
        for (BaseEventListener listener : mListeners) {
            for (Class<? extends EventObject> t: listener.getEventTMethodMap().keySet()) {
                if (t.isAssignableFrom(event.getClass()))
                    Collections.addAll(methodsToPerform, Objects.requireNonNull(listener.getEventTMethodMap().get(t)));
            }
        }
        for(Method m:methodsToPerform) {
            try {
                m.invoke(event);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    public NearShareSendingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        // Set initial progress to 0
        setProgressAsync(new Data.Builder().putInt(PROGRESS, 0).build());
    }

    private void updateProgress(long totalBytesToSend, long bytesSent, int totalFilesToSend, int filesSent) {
        this.totalBytesToSend = totalBytesToSend;
        this.bytesSent = bytesSent;
        this.totalFilesToSend = totalFilesToSend;
        this.filesSent = filesSent;
        setProgressAsync(new Data.Builder().putInt(PROGRESS, (int)(bytesSent/totalBytesToSend)*100).build());
    }

    @NonNull
    @Override
    public Result doWork() {

        final RemoteSystemConnectionRequest connectionRequest = new RemoteSystemConnectionRequest(peer.remoteSystem);

        final AsyncOperationWithProgress<NearShareStatus, NearShareProgress> operation;
        if (entities.size() == 1) {
            final NearShareFileProvider fileProvider = NearShareHelper.createNearShareFileFromContentUri(
                    entities.get(0).uri, mContext);

            operation = mNearShareSender.sendFileAsync(connectionRequest, fileProvider);
        } else {
            final NearShareFileProvider[] fileProviders = new NearShareFileProvider[entities.size()];

            for (int i = 0; i < entities.size(); i++) {
                fileProviders[i] = NearShareHelper.createNearShareFileFromContentUri(
                        entities.get(i).uri, mContext);
            }

            operation = mNearShareSender.sendFilesAsync(connectionRequest, fileProviders);
        }

        final AtomicBoolean accepted = new AtomicBoolean(false);

        operation.progress().subscribe((op, progress) -> {
            if (progress.filesSent != 0 || progress.totalFilesToSend != 0) {
                if (accepted.compareAndSet(false, true)) {
                        notifyListeners(new AcceptedOrRejectedEvent(this, true));
                }
                updateProgress(progress.totalBytesToSend, progress.bytesSent, progress.totalFilesToSend, progress.filesSent);
            }
        });

        operation.whenComplete((status, tr) -> {
            if (status == NearShareStatus.COMPLETED) notifyListeners(new SentEvent(this));
            HashMap<NearShareStatus, Constants.TransmissionStatus> m = new HashMap<NearShareStatus, Constants.TransmissionStatus> (){{
                put(NearShareStatus.UNKNOWN, Constants.TransmissionStatus.UNKNOWN_ERROR);
                put(NearShareStatus.COMPLETED, Constants.TransmissionStatus.COMPLETED);
                put(NearShareStatus.IN_PROGRESS, Constants.TransmissionStatus.IN_PROGRESS);
                put(NearShareStatus.TIMED_OUT, Constants.TransmissionStatus.TIMED_OUT);
                put(NearShareStatus.CANCELLED, Constants.TransmissionStatus.CANCELLED);
                put(NearShareStatus.DENIED_BY_REMOTE_SYSTEM, Constants.TransmissionStatus.DENIED_BY_REMOTE_SYSTEM);
            }

                @Nullable
                @Override
                public Constants.TransmissionStatus getOrDefault(@Nullable Object key, @Nullable Constants.TransmissionStatus defaultValue) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        return super.getOrDefault(key, defaultValue);
                    }else {
                        Constants.TransmissionStatus ret = get(key);
                        return ret != null ? ret : defaultValue;
                    }
                }
            };
            if (tr != null) {
                notifyListeners(new SendFailedEvent(this, m.getOrDefault(status, Constants.TransmissionStatus.UNKNOWN_ERROR), tr.getLocalizedMessage()));
            } else {
                Log.e(TAG, "Failed sending files to " + peer.name + ": " + status);
                notifyListeners(new SendFailedEvent(this, m.getOrDefault(status, Constants.TransmissionStatus.UNKNOWN_ERROR), null));
            }
        });

        return
    }
}