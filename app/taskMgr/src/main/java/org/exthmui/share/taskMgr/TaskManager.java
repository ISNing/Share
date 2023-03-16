package org.exthmui.share.taskMgr;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import org.exthmui.share.taskMgr.entities.GroupEntity;
import org.exthmui.share.taskMgr.entities.TaskEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

public class TaskManager {

    public static final String TAG = "TaskManager";

    private static TaskManager instance;
    private final Map<String, Group> groups;
    private final Map<String, Task> tasks;
    private final Executor executor;
    private final TaskDatabase database;

    public Boolean groupsLoaded = false;

    private TaskManager(@NonNull Context context) {
        groups = new HashMap<>();
        tasks = new HashMap<>();
        executor = Executors.newCachedThreadPool(new ThreadFactory() {
            int id = 0;
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(String.format(Locale.ROOT, "TaskManager-ChildThread-%d", id++)) {
                    @Override
                    public void run() {
                        r.run();
                    }
                };
            }
        });
        database = TaskDatabase.getInstance(context.getApplicationContext());
        database.runInDatabaseThread(() -> {
            List<GroupEntity> groupEntities = database.groupDao().getAllGroupEntities();
            for (GroupEntity groupEntity : groupEntities)
                groups.put(groupEntity.groupId, Group.load(database, groupEntity));
            Log.w(TAG, "Groups loaded");
            synchronized (groupsLoaded) {
                groupsLoaded.notifyAll();
            }
            groupsLoaded = true;
        });
        Log.w(TAG, "TaskManager initialized");
    }

    public static TaskManager getInstance(Context context) {
        if (instance == null) {
            instance = new TaskManager(context);
        }
        return instance;
    }

    protected static TaskManager getInstance() {
        if (instance == null) {
            throw new RuntimeException("TaskManager hasn't been initialized");
        }
        return instance;
    }

    public void addGroup(String groupId) {
        addGroup(groupId, Integer.MAX_VALUE);
    }

    public void addGroup(String groupId, int maxConcurrentTasks) {
        Group group = new Group(groupId, maxConcurrentTasks);
        groups.put(groupId, group);
        GroupEntity groupEntity = new GroupEntity(group);

        database.runInDatabaseThread(() -> database.groupDao().insert(groupEntity));
    }

    public void enqueueTask(String groupId, Task task) {
        Group g;
        synchronized (groupsLoaded) {
            try {
                if (!groupsLoaded) groupsLoaded.wait();
            } catch (InterruptedException ignored) {
            }
        }
        g = groups.get(groupId);
        if (g == null) {
            Log.w(TAG, String.format("Requested to enqueue the task %s(Id:%s), with group %s not found, " +
                            "going to add a new group with default configuration.",
                    task.getClass().getName(), task.getTaskId(), groupId));
            addGroup(groupId);
            g = groups.get(groupId);
            if (g == null) {
                Log.e(TAG, String.format("Failed to add group %s", groupId));
                return;
            }
        }
        final Group group = g;
        tasks.put(task.getTaskId(), task);
        Object lock = new Object();
        database.runInDatabaseThread(() -> {
            synchronized (lock) {
                database.taskDao().insert(new TaskEntity(task, groupId));
                lock.notify();
            }
        });
        synchronized (lock) {
            try {
                lock.wait();
            } catch (InterruptedException ignored) {
            }
        }
        task.getProgressDataLiveData().observeForever(ignored -> updateTaskInDatabase(task));
        group.addTask(task, result -> {
            updateTaskInDatabase(task);
            addResult(result);
            group.taskFinished();
        });
        updateGroupInDatabase(group);
    }

    public void removeTask(String groupId, Task task) {
        Group group = groups.get(groupId);
        if (group == null) {
            throw new RuntimeException(String.format("Group %s does not exists", groupId));
        }
        Future<Result> resultFuture = task.getResultFuture();
        if (resultFuture.isDone()) {
            try {
                removeResult(resultFuture.get());
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        TaskEntity taskEntity = database.taskDao().getTaskEntityById(task.getTaskId());
        database.runInDatabaseThread(() -> database.taskDao().delete(taskEntity));
        tasks.remove(task.getTaskId());
        group.removeTask(task);
        // Don't update database for this task because it had already been deleted from database
        task.setCallback(result -> {
            group.taskFinished();
            Log.d(TAG, String.format("Task %s(Id:%s) has completed, resulted in %s(result id: %s)",
                    task.getClass().getName(), task.getTaskId(), result.getStatus(), result.getResultId()));
        });
        updateGroupInDatabase(group);
    }

    public void removeGroup(String groupId) {
        Group group = groups.get(groupId);
        if (group == null) {
            throw new RuntimeException(String.format("Group %s does not exists", groupId));
        }
        for (String taskId: group.getTaskIds()) removeTask(groupId, getTask(taskId));

        GroupEntity groupEntity = database.groupDao().getGroupEntityById(group.getGroupId());
        database.runInDatabaseThread(() -> database.groupDao().delete(groupEntity));
    }

    private void addResult(Result result) {
        database.runInDatabaseThread(() -> database.resultDao().insert(result));
    }

    private void removeResult(Result result) {
        database.runInDatabaseThread(() -> database.resultDao().delete(result));
    }

    private void updateTaskInDatabase(Task task) {
        database.runInDatabaseThread(() -> {
            TaskEntity taskEntity = database.taskDao().getTaskEntityById(task.getTaskId());
            taskEntity.updateTask(task);
            database.taskDao().update(taskEntity);
        });
    }

    private void updateGroupInDatabase(Group group) {
        database.runInDatabaseThread(() -> {
            GroupEntity groupEntity = database.groupDao().getGroupEntityById(group.getGroupId());
            groupEntity.updateGroup(group);
            database.groupDao().update(groupEntity);
        });
    }

    public List<Task> getTasksByGroupId(String groupId) {
        Group group = groups.get(groupId);
        if (group == null) {
            return null;
        }
        return new ArrayList<>(group.getTaskQueue());
    }

    public Task getTask(String taskId) {
        return tasks.get(taskId);
    }

    public Group getGroup(String groupId) {
        return groups.get(groupId);
    }

    public void executeTaskAsync(String taskId) {
        Task task = tasks.get(taskId);
        if (task == null) {
            return;
        }
        task.setStatus(TaskStatus.RUNNING);
        executor.execute(task);
        Log.d(TAG, String.format("Task %s(Id:%s) has been started", task.getClass().getName(), task.getTaskId()));
    }
}