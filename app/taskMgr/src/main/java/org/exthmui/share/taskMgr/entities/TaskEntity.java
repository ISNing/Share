package org.exthmui.share.taskMgr.entities;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import org.exthmui.share.taskMgr.Task;
import org.exthmui.share.taskMgr.TaskStatus;

import java.util.concurrent.ExecutionException;

@Entity(tableName = "task")
public class TaskEntity {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "task_id")
    private final String taskId;

    @ColumnInfo(name = "group_id")
    private final String groupId;

    @ColumnInfo(name = "task_type")
    private final String taskType;

    @ColumnInfo(name = "status")
    private TaskStatus status;

    @ColumnInfo(name = "input_data")
    private final Bundle inputData;

    @ColumnInfo(name = "result_id")
    private String resultId;

    @ColumnInfo(name = "progress_data")
    private volatile Bundle progressData;

    public TaskEntity(@NonNull String taskId, String groupId, String taskType, TaskStatus status, Bundle inputData, String resultId, Bundle progressData) {
        this.taskId = taskId;
        this.groupId = groupId;
        this.taskType = taskType;
        this.status = status;
        this.inputData = inputData;
        this.resultId = resultId;
        this.progressData = progressData;
    }

    public TaskEntity(@NonNull Task task, String groupId) {
        this.taskId = task.getTaskId();
        this.groupId = groupId;
        this.taskType = task.getClass().getCanonicalName();
        this.inputData = task.getInputData();
        updateTask(task);
    }

    public void updateTask(Task task) {
        this.status = task.getStatus();
        try {
            this.resultId = task.getResultFuture().isDone() ? task.getResultFuture().get().getResultId() : null;
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        this.progressData = task.getProgressData();
    }
    @NonNull
    public String getTaskId() {
        return taskId;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getTaskType() {
        return taskType;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Bundle getInputData() {
        return inputData;
    }

    public String getResultId() {
        return resultId;
    }

    public void setResult(String resultId) {
        this.resultId = resultId;
    }

    public Bundle getProgressData() {
        return progressData;
    }

    public void setProgressData(Bundle progressData) {
        this.progressData = progressData;
    }
}