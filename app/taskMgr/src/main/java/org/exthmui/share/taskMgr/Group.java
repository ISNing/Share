package org.exthmui.share.taskMgr;

import android.util.Log;

import org.exthmui.share.taskMgr.entities.GroupEntity;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

public class Group {

    public static final String TAG = "Group";

    private final String groupId;
    private final int maxConcurrentTasks;
    private final AtomicInteger currentRunningTasks;
    private final List<String> taskIds;
    private final Queue<Task> taskQueue;

    public Group(String groupId, int maxConcurrentTasks) {
        this.groupId = groupId;
        this.maxConcurrentTasks = maxConcurrentTasks;
        this.taskIds = new ArrayList<>();
        this.currentRunningTasks = new AtomicInteger(0);
        this.taskQueue = new LinkedList<>();
    }

    public Group(GroupEntity groupEntity) {
        this.groupId = groupEntity.groupId;
        this.maxConcurrentTasks = groupEntity.maxConcurrentTasks;
        this.taskIds = new ArrayList<>(groupEntity.taskIds);
        this.currentRunningTasks = new AtomicInteger(0);
        this.taskQueue = new LinkedList<>();
    }

    public String getGroupId() {
        return groupId;
    }

    public int getMaxConcurrentTasks() {
        return maxConcurrentTasks;
    }

    public AtomicInteger getCurrentRunningTasks() {
        return currentRunningTasks;
    }

    public Queue<Task> getTaskQueue() {
        return taskQueue;
    }

    public List<String> getTaskIds() {
        return new ArrayList<>(taskIds);
    }

    /**
     * Just add task to the list, but not to execute it
     * @param task Task object to be added
     */
    public void addTask(Task task) {
        if (taskIds.contains(task.getTaskId())) return;
        taskIds.add(task.getTaskId());
    }

    /**
     * Add task to the list, and also to enqueue it to execute it
     * @param task Task object to be enqueued
     *             ATTENTION: NEVER enqueue the task that had been added or enqueued before, it will
     *             be ignored, you should reconstruct a new Task object with different id
     */
    public void enqueueTask(Task task) {
        addTask(task);
        if (currentRunningTasks.get() > maxConcurrentTasks) {
            // add task to queue and return
            taskQueue.offer(task);
            task.setStatus(TaskStatus.QUEUED);
            Log.d(TAG, String.format("The number of tasks running now in group %s had reach the limit, " +
                            "the new task %s(Id:%s) has been added to the queue",
                    getGroupId(), task.getClass().getName(), task.getTaskId()));
        } else {
            // execute the task
            currentRunningTasks.getAndIncrement();
            executeTask(task.getTaskId());
            Log.d(TAG, String.format("The number of tasks running now in group %s hasn't reach the limit, " +
                            "trying to execute the new task %s(Id:%s)",
                    getGroupId(), task.getClass().getName(), task.getTaskId()));
        }
    }

    public void removeTask(Task task) {
        if (!taskIds.contains(task.getTaskId())) return;
        taskIds.remove(task.getTaskId());
    }

    private void executeTask(String taskId) {
        TaskManager.getInstance().executeTaskAsync(taskId);
    }

    public void taskFinished() {
        currentRunningTasks.getAndDecrement();
        if (currentRunningTasks.get() >= maxConcurrentTasks) {
            Task task = taskQueue.poll();
            if (task != null) {
                executeTask(task.getTaskId());
                Log.d(TAG, String.format("A task in group %s has completed, requested to execut new task in queue: %s(Id:%s)",
                        getGroupId(), task.getClass().getName(), task.getTaskId()));
            }
        }
    }

    public static Group load(TaskDatabase database, GroupEntity groupEntity) {
        Group group = new Group(groupEntity);
        for (String taskId : groupEntity.taskIds) {
            Task task = Task.load(database, taskId);
            group.addTask(task);
            Log.d(TAG, String.format("Task %s(Id:%s) has been added to group %s",
                    task.getClass().getName(), task.getTaskId(), group.getGroupId()));
        }
        Log.d(TAG, String.format("Group %s loaded", group.getGroupId()));
        return group;
    }

    public static Group load(TaskDatabase database, String id) {
        GroupEntity groupEntity = database.groupDao().getGroupEntityById(id);
        return load(database, groupEntity);
    }
}
