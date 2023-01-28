package org.exthmui.share.taskMgr;

import org.exthmui.share.taskMgr.entities.GroupEntity;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

public class Group {
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

    public void addTask(Task task) {
        addTask(task, null);
    }

    /**
     * Just add task to the list, but not to execute it
     * @param task Task object to be added
     * @param callback callback for task execution
     */
    public void addTask(Task task, Task.Callback callback) {
        if (taskIds.contains(task.getTaskId())) return;
        taskIds.add(task.getTaskId());
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
        if (currentRunningTasks.get() > maxConcurrentTasks) {
            // add task to queue and return
            taskQueue.offer(task);
            task.setStatus(TaskStatus.QUEUED);
        } else {
            // execute the task
            currentRunningTasks.getAndIncrement();
            TaskManager.getInstance().executeTaskAsync(task.getTaskId());
        }
    }

    public void removeTask(Task task) {
        if (!taskIds.contains(task.getTaskId())) return;
        taskIds.remove(task.getTaskId());
    }

    public void taskFinished() {
        currentRunningTasks.getAndDecrement();
        if (currentRunningTasks.get() >= maxConcurrentTasks) {
            Task task = taskQueue.poll();
            if (task != null)
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
