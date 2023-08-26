package org.exthmui.share.taskMgr

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.exthmui.share.taskMgr.entities.GroupEntity
import org.exthmui.share.taskMgr.entities.TaskEntity
import org.exthmui.share.taskMgr.events.ProgressUpdatedEvent
import org.exthmui.share.taskMgr.events.ResultEvent
import org.exthmui.share.taskMgr.listeners.OnProgressUpdatedListener
import org.exthmui.share.taskMgr.listeners.OnResultListener
import java.util.concurrent.ExecutionException

class TaskManager private constructor(context: Context) {
    private val groups: MutableMap<String, Group>
    private val tasks: MutableMap<String, Task>
    private val executor: CoroutineScope
    private val database: TaskDatabase
    var groupsLoaded = CompletableDeferred<Boolean>()

    init {
        groups = HashMap()
        tasks = HashMap()
        executor = CoroutineScope(Dispatchers.Default)
        database = TaskDatabase.getInstance(context.applicationContext)
        database.runInDatabaseThread {
            val groupEntities = database.groupDao().allGroupEntities
            for (groupEntity in groupEntities) groups[groupEntity.groupId] =
                Group.load(database, groupEntity)
            Log.w(TAG, "Groups loaded")
            groupsLoaded.complete(true)
        }
        Log.w(TAG, "TaskManager initialized")
    }

    fun addGroup(groupId: String, maxConcurrentTasks: Int = Int.MAX_VALUE) {
        val group = Group(groupId, maxConcurrentTasks)
        groups[groupId] = group
        val groupEntity = GroupEntity(group)
        Log.d(TAG, String.format("Adding group %s", groupId))
        database.runInDatabaseThread { database.groupDao().insert(groupEntity) }
    }

    fun enqueueTaskBlocking(groupId: String, task: Task) {
        runBlocking {
            enqueueTask(groupId, task).await()
        }
    }

    fun enqueueTask(groupId: String, task: Task) = CoroutineScope(Dispatchers.IO).async {
        enqueueTaskCoroutine(groupId, task)
    }

    suspend fun enqueueTaskCoroutine(groupId: String, task: Task) {
        var g: Group?
        groupsLoaded.await()
        g = groups[groupId]
        if (g == null) {
            Log.w(
                TAG, String.format(
                    "Requested to enqueue the task %s(Id:%s), with group %s not found, " +
                            "going to add a new group with default configuration.",
                    task.javaClass.name, task.taskId, groupId
                )
            )
            addGroup(groupId)
            g = groups[groupId]
            if (g == null) {
                Log.e(TAG, String.format("Failed to add group %s", groupId))
                return
            }
        }
        val group: Group = g
        tasks[task.taskId] = task
        database.runInDatabaseThread {
            database.taskDao().insert(TaskEntity(task, groupId))
        }
        task.registerListener(OnProgressUpdatedListener { event: ProgressUpdatedEvent? ->
            updateTaskInDatabase(
                task
            )
        })
        Log.d(TAG, String.format("Adding task %s to group %s", task.taskId, groupId))
        group.enqueueTask(task)
        task.registerListener(OnResultListener { event: ResultEvent ->
            Log.d(
                TAG, String.format(
                    "Task %s(Id:%s) of group %s has completed: %s",
                    task.javaClass.name, task.taskId, groupId, event.result
                )
            )
            if (tasks.containsValue(task)) {
                updateTaskInDatabase(task)
                addResult(event.result)
            } else Log.d(
                TAG, String.format(
                    "Task %s(Id:%s) of group %s has been deleted so it will not be " +
                            "updated and it's result will not be inserted into database",
                    task.javaClass.name, task.taskId, groupId
                )
            )
            group.taskFinished()
        })
        updateGroupInDatabase(group)
    }

    fun removeTask(groupId: String, task: Task) {
        val group = groups[groupId]
            ?: throw RuntimeException(String.format("Group %s does not exists", groupId))
        val resultFuture = task.resultFuture
        if (resultFuture.isDone) {
            try {
                removeResult(resultFuture.get())
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
        database.taskDao().getTaskEntityById(task.taskId)?.let {
            database.runInDatabaseThread { database.taskDao().delete(it) }
        }
        tasks.remove(task.taskId)
        group.removeTask(task)
        updateGroupInDatabase(group)
    }

    fun removeGroup(groupId: String) {
        val group = groups[groupId]
            ?: throw RuntimeException(String.format("Group %s does not exists", groupId))
        for (taskId in group.taskIds) getTask(taskId)?.let { removeTask(groupId, it) }
        database.groupDao().getGroupEntityById(group.groupId)?.let {
            database.runInDatabaseThread { database.groupDao().delete(it) }
        }
    }

    private fun addResult(result: Result) {
        database.runInDatabaseThread { database.resultDao().insert(result) }
    }

    private fun removeResult(result: Result) {
        database.runInDatabaseThread { database.resultDao().delete(result) }
    }

    private fun updateTaskInDatabase(task: Task) {
        database.runInDatabaseThread {
            val taskEntity = database.taskDao().getTaskEntityById(task.taskId)
            taskEntity?.let {
                it.updateTask(task)
                database.taskDao().update(it)
            }
        }
    }

    private fun updateGroupInDatabase(group: Group) {
        database.runInDatabaseThread {
            val groupEntity = database.groupDao().getGroupEntityById(group.groupId)
            groupEntity?.let {
                it.updateGroup(group)
                database.groupDao().update(it)
            }
        }
    }

    fun getTasksByGroupId(groupId: String): List<Task>? {
        val group = groups[groupId] ?: return null
        return ArrayList(group.taskQueue)
    }

    fun getTask(taskId: String): Task? {
        return tasks[taskId]
    }

    fun getGroup(groupId: String): Group? {
        return groups[groupId]
    }

    fun executeTaskAsync(taskId: String) {
        val task = tasks[taskId] ?: return
        task.status = TaskStatus.RUNNING
        executor.launch { task.runSuspendable() }
        Log.d(
            TAG,
            String.format("Task %s(Id:%s) has been started", task.javaClass.name, task.taskId)
        )
    }

    companion object {
        const val TAG = "TaskManager"
        private var instance: TaskManager? = null

        fun getInstance(context: Context): TaskManager {
            if (instance == null) {
                instance = TaskManager(context)
            }
            return instance as TaskManager
        }

        fun getInstance(): TaskManager {
            if (instance == null) {
                throw IllegalStateException("TaskManager hasn't been initialized")
            }
            return instance as TaskManager
        }
    }
}