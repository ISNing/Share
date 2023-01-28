package org.exthmui.share.taskMgr;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;

import org.exthmui.share.taskMgr.entities.TaskEntity;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public abstract class Task implements Runnable {

    public static final String TAG = "TaskManager/Task";

    @NonNull
    private final String taskId;
    private TaskStatus mStatus;
    @NonNull
    private final Bundle mInputData;
    private final InternalCancelCompletableFuture<Result> mResultFuture = new InternalCancelCompletableFuture<>();
    @Nullable
    private Callback mCallback;
    @Nullable
    private volatile Bundle mProgressData;

    private final MutableLiveData<Bundle> mProgressDataLive = new MutableLiveData<>(mProgressData);

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
        if (mCallback != null) {
            mCallback.onResult(result);
        }
    }
    public final void setCallback(@Nullable Callback callback) {
        mCallback = callback;
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

    public interface Callback {
        void onResult(Result data);
    }
}