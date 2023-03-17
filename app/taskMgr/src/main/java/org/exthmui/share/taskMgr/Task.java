package org.exthmui.share.taskMgr;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;

import org.exthmui.share.taskMgr.entities.TaskEntity;
import org.exthmui.share.taskMgr.events.ProgressUpdatedEvent;
import org.exthmui.share.taskMgr.events.ResultEvent;
import org.exthmui.share.taskMgr.listeners.OnProgressUpdatedListener;
import org.exthmui.share.taskMgr.listeners.OnResultListener;
import org.exthmui.utils.BaseEventListenersUtils;
import org.exthmui.utils.listeners.BaseEventListener;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.EventObject;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public abstract class Task implements Runnable {

    public static final String TAG = "Task";

    @SuppressWarnings("unchecked")
    private static final Class<? extends BaseEventListener>[] LISTENER_TYPES_ALLOWED = (Class<? extends BaseEventListener>[]) new Class<?>[]
            {
                    OnProgressUpdatedListener.class,
                    OnResultListener.class
            };

    @NonNull
    private final String taskId;
    private TaskStatus mStatus;
    @NonNull
    private final Bundle mInputData;
    private final InternalCancelCompletableFuture<Result> mResultFuture = new InternalCancelCompletableFuture<>();
    @Nullable
    private volatile Bundle mProgressData;

    private final MutableLiveData<Bundle> mProgressDataLive = new MutableLiveData<>(mProgressData);

    private final Collection<BaseEventListener> mListeners = new HashSet<>();

    private void notifyListeners(@NonNull EventObject event) {
        BaseEventListenersUtils.notifyListeners(event, mListeners);
    }

    public void registerListener(@NonNull BaseEventListener listener) {
        if (BaseEventListenersUtils.isThisListenerSuitable(listener, LISTENER_TYPES_ALLOWED))
            mListeners.add(listener);
    }

    public void unregisterListener(BaseEventListener listener) {
        mListeners.remove(listener);
    }


    public Task(@NonNull String taskId, @Nullable Bundle inputData) {
        this.taskId = taskId;
        this.mInputData = inputData == null ? Bundle.EMPTY : inputData;
        mStatus = TaskStatus.CREATED;
    }

    public Task(@NonNull TaskEntity taskEntity) {
        this.taskId = taskEntity.getTaskId();
        Bundle input = taskEntity.getInputData();
        this.mInputData = input == null ? Bundle.EMPTY : input;
        this.mStatus = taskEntity.getStatus();
        // Result will be loaded by TaskManager
    }

    @NonNull
    public String getTaskId() {
        return taskId;
    }
    public TaskStatus getStatus() {
        return mStatus;
    }
    void setStatus(TaskStatus status) {
        mStatus = status;
    }
    private void setResult(Result result) {
        mResultFuture.complete(result);
        switch (result.getStatus()) {
            case SUCCESS:
                setStatus(TaskStatus.SUCCESS);
                break;
            case ERROR:
                setStatus(TaskStatus.ERROR);
                break;
            case CANCELLED:
                setStatus(TaskStatus.CANCELLED);
                break;
        }
        notifyListeners(new ResultEvent(this, result));
    }

    @Nullable
    public Bundle getProgressData() {
        return mProgressData;
    }

    @NonNull
    public MutableLiveData<Bundle> getProgressDataLiveData() {
        return mProgressDataLive;
    }

    @NonNull
    public Bundle getInputData() {
        return mInputData;
    }

    @NonNull
    public Future<Result> getResultFuture() {
        return mResultFuture;
    }

    public final boolean isCancelled() {
        return mStatus == TaskStatus.CANCELLED;
    }

    @Override
    public final void run() {
        if (!isCancelled()) {
            try {
                setResult(doWork());
            } catch (Throwable error) {
                setResult(Result.failure(error));
            }
        } else {
            setResult(Result.cancelled());
        }
    }

    public abstract Result doWork();

    public void cancel() {
        mStatus = TaskStatus.CANCELLED;
        mResultFuture.internalCancel(true);
    }

    protected final void updateProgress(Bundle progressData) {
        mProgressData = progressData;
        mProgressDataLive.postValue(progressData);
        notifyListeners(new ProgressUpdatedEvent(this, progressData));
    }

    public static Task load(TaskDatabase database, String id) {
        TaskEntity taskEntity = database.taskDao().getTaskEntityById(id);
        Task task = null;
        try {
            task = (Task) Class.forName(taskEntity.getTaskType()).getConstructor(TaskEntity.class).newInstance(taskEntity);
            Result result = Result.load(database, taskEntity.getResultId());
            if (result != null) task.mResultFuture.complete(result);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException |
                 NoSuchMethodException | ClassNotFoundException e) {
            Log.e(TAG, String.format("Failed instancing task %s from database: %s", id, e));
            e.printStackTrace();
        }
        Log.d(TAG, String.format("Task %s(Id:%s) loaded", task.getClass().getName(), task.getTaskId()));
        return task;
    }

    private static class InternalCancelCompletableFuture<T> extends CompletableFuture<T> {
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            // Refuse to cancel the future outside the class
            return false;
        }

        private void internalCancel(boolean mayInterruptIfRunning) {
            super.cancel(mayInterruptIfRunning);
        }
    }
}