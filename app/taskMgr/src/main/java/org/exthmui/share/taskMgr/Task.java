package org.exthmui.share.taskMgr;

import android.os.Bundle;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import org.exthmui.share.taskMgr.entities.TaskEntity;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public abstract class Task implements Runnable {

    public static final String TAG = "TaskManager/Task";

    private final String taskId;
    private TaskStatus mStatus;
    private final Bundle mInputData;
    private final InternalCancelCompletableFuture<Result> mResultFuture = new InternalCancelCompletableFuture();
    private Callback mCallback;
    private volatile boolean isCancelled = false;
    private volatile Bundle mProgressData;

    private final MutableLiveData<Bundle> mProgressDataLive = new MutableLiveData<>(mProgressData);

    public Task(String taskId, Bundle inputData) {
        this.taskId = taskId;
        this.mInputData = inputData;
        mStatus = TaskStatus.CREATED;
    }

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
    public final void setCallback(Callback callback) {
        mCallback = callback;
    }

    public Bundle getProgressData() {
        return mProgressData;
    }

    public MutableLiveData<Bundle> getProgressDataLiveData() {
        return mProgressDataLive;
    }

    public Bundle getInputData() {
        return mInputData;
    }

    public Future<Result> getResultFuture() {
        return mResultFuture;
    }

    public final boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public final void run() {
        if (!isCancelled) {
            try {
                setResult(doWork(mInputData));
            } catch (Throwable error) {
                setResult(new Result.Error(error));
            }
        } else {
            setResult(new Result.Cancelled());
        }
    }

    public abstract Result doWork(Bundle input);

    public void cancel() {
        isCancelled = true;
        mResultFuture.internalCancel(true);
    }

    public final void updateProgress(Bundle progressData) {
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