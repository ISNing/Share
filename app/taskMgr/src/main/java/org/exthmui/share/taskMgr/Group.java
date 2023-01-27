package org.exthmui.share.taskMgr;

import org.exthmui.share.taskMgr.entities.GroupEntity;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

public class Group {
    private String groupId;
    private int maxConcurrentTasks;
    private AtomicInteger mCurrentRunningTasks;
    private List<String> mTaskIds;
    private Queue<Task> mTaskQueue;

    public Group(String groupId, int maxConcurrentTasks) {
        this.groupId = groupId;
        this.maxConcurrentTasks = maxConcurrentTasks;
        this.mTaskIds = new ArrayList<>();
        mCurrentRunningTasks = new AtomicInteger(0);
        mTaskQueue = new LinkedList<>();
    }

    public Group(GroupEntity groupEntity) {
        this.groupId = groupEntity.groupId;
        this.maxConcurrentTasks = groupEntity.maxConcurrentTasks;
        this.mTaskIds = new ArrayList<>(groupEntity.taskIds);
        mCurrentRunningTasks = new AtomicInteger(0);
        mTaskQueue = new LinkedList<>();
    }

    public String getGroupId() {
        return groupId;
    }

    public int getMaxConcurrentTasks() {
        return maxConcurrentTasks;
    }

    public AtomicInteger getCurrentRunningTasks() {
        return mCurrentRunningTasks;
    }

    public Queue<Task> getTaskQueue() {
        return mTaskQueue;
    }

    public List<String> getTaskIds() {
        return new ArrayList<>(mTaskIds);
    }

    public void addTask(Task task) {
        addTask(task, null);
    }

    /**
     * Just add task to the list, but not to execute it
     * @param task Task object to be added
     * @param callback callback for task execution
     */
    public void addTask(Task task, Task.Callback callback) {
        if (mTaskIds.contains(task.getTaskId())) return;
        mTaskIds.add(task.getTaskId());
        task.setCallback(callback);
    }

    /**
     * Add task to the list, and also to enqueue it to execute it
     * @param task Task object to be enqueued
     *             ATTENTION: NEVER enqueue the task that had been added or enqueued before, it will
     *             be ignored, you should reconstruct a new Task object with different id
     * @param callback callback for task execution
     */
    public void enqueueTask(Task task, Task.Callback callback) {
        addTask(task, callback);
        if (mCurrentRunningTasks.get() > maxConcurrentTasks) {
            // add task to queue and return
            mTaskQueue.offer(task);
            task.setStatus(TaskStatus.QUEUED);
        } else {
            // execute the task
            mCurrentRunningTasks.getAndIncrement();
            TaskManager.getInstance().executeTaskAsync(task.getTaskId());
        }
    }

    public void removeTask(Task task) {
        if (!mTaskIds.contains(task.getTaskId())) return;
        mTaskIds.remove(task.getTaskId());
    }

    public void taskFinished() {
        mCurrentRunningTasks.getAndDecrement();
        if (mCurrentRunningTasks.get() >= maxConcurrentTasks && mTaskQueue.size() > 0) {
            Task task = mTaskQueue.poll();
            TaskManager.getInstance().executeTaskAsync(task.getTaskId());
        }
    }

    public static Group load(TaskDatabase database, String id) {
        GroupEntity groupEntity = database.groupDao().getGroupEntityById(id);
        Group group = new Group(groupEntity);
        for (String taskId : groupEntity.taskIds) {
            group.addTask(Task.load(database, taskId));
        }
        return group;
    }
}
