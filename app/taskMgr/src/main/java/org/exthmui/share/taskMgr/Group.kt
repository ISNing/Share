package org.exthmui.share.taskMgr

import android.util.Log
import org.exthmui.share.taskMgr.entities.GroupEntity
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.atomic.AtomicInteger

class Group {
    val groupId: String

    val maxConcurrentTasks: Int
    val currentRunningTasks: AtomicInteger
    internal val taskIds: MutableList<String>
    val taskQueue: Queue<Task>

    constructor(groupId: String, maxConcurrentTasks: Int) {
        this.groupId = groupId
        this.maxConcurrentTasks = maxConcurrentTasks
        taskIds = ArrayList()
        currentRunningTasks = AtomicInteger(0)
        taskQueue = LinkedList()
    }

    constructor(groupEntity: GroupEntity) {
        groupId = groupEntity.groupId
        maxConcurrentTasks = groupEntity.maxConcurrentTasks
        taskIds = ArrayList(groupEntity.taskIds)
        currentRunningTasks = AtomicInteger(0)
        taskQueue = LinkedList()
    }

    fun getTaskIds(): List<String> {
        return ArrayList(taskIds)
    }

    /**
     * Just add task to the list, but not to execute it
     * @param task Task object to be added
     */
    fun addTask(task: Task?) {
        if (taskIds.contains(task!!.taskId)) return
        taskIds.add(task.taskId)
    }

    /**
     * Add task to the list, and also to enqueue it to execute it
     * @param task Task object to be enqueued
     * ATTENTION: NEVER enqueue the task that had been added or enqueued before, it will
     * be ignored, you should reconstruct a new Task object with different id
     */
    fun enqueueTask(task: Task) {
        addTask(task)
        if (currentRunningTasks.get() > maxConcurrentTasks) {
            // add task to queue and return
            taskQueue.offer(task)
            task.status = TaskStatus.QUEUED
            Log.d(
                TAG, String.format(
                    "The number of tasks running now in group %s had reach the limit, " +
                            "the new task %s(Id:%s) has been added to the queue",
                    groupId, task.javaClass.name, task.taskId
                )
            )
        } else {
            // execute the task
            currentRunningTasks.getAndIncrement()
            executeTask(task.taskId)
            Log.d(
                TAG, String.format(
                    "The number of tasks running now in group %s hasn't reach the limit, " +
                            "trying to execute the new task %s(Id:%s)",
                    groupId, task.javaClass.name, task.taskId
                )
            )
        }
    }

    fun removeTask(task: Task) {
        if (!taskIds.contains(task.taskId)) return
        taskIds.remove(task.taskId)
    }

    private fun executeTask(taskId: String) {
        TaskManager.getInstance().executeTaskAsync(taskId)
    }

    fun taskFinished() {
        currentRunningTasks.getAndDecrement()
        if (currentRunningTasks.get() >= maxConcurrentTasks) {
            val task = taskQueue.poll()
            if (task != null) {
                executeTask(task.taskId)
                Log.d(
                    TAG, String.format(
                        "A task in group %s has completed, requested to execut new task in queue: %s(Id:%s)",
                        groupId, task.javaClass.name, task.taskId
                    )
                )
            }
        }
    }

    companion object {
        const val TAG = "Group"
        fun load(database: TaskDatabase?, groupEntity: GroupEntity): Group {
            val group = Group(groupEntity)
            for (taskId in groupEntity.taskIds) {
                val task = Task.load(database!!, taskId)
                group.addTask(task)
                Log.d(
                    TAG, String.format(
                        "Task %s(Id:%s) has been added to group %s",
                        task!!.javaClass.name, task.taskId, group.groupId
                    )
                )
            }
            Log.d(TAG, String.format("Group %s loaded", group.groupId))
            return group
        }

        fun load(database: TaskDatabase, id: String): Group? {
            val groupEntity = database.groupDao().getGroupEntityById(id)
            return groupEntity?.let { load(database, it) }
        }
    }
}
