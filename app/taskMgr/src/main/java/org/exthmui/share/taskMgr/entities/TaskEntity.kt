package org.exthmui.share.taskMgr.entities

import android.os.Bundle
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import org.exthmui.share.taskMgr.Task
import org.exthmui.share.taskMgr.TaskStatus
import org.exthmui.share.taskMgr.converters.BundleConverter
import java.util.concurrent.ExecutionException

@Entity(tableName = "task")
class TaskEntity {
    @PrimaryKey
    @ColumnInfo(name = "task_id")
    val taskId: String

    @ColumnInfo(name = "group_id")
    val groupId: String

    @ColumnInfo(name = "task_type")
    val taskType: String

    @ColumnInfo(name = "status")
    var status: TaskStatus = TaskStatus.CREATED

    @ColumnInfo(name = "input_data")
    @TypeConverters(BundleConverter::class)
    val inputData: Bundle

    @ColumnInfo(name = "result_id")
    var resultId: String? = null
        private set

    @ColumnInfo(name = "progress_data")
    @TypeConverters(BundleConverter::class)
    @Volatile
    var progressData: Bundle? = null

    constructor(
        taskId: String,
        groupId: String,
        taskType: String,
        status: TaskStatus,
        inputData: Bundle,
        resultId: String?,
        progressData: Bundle?
    ) {
        this.taskId = taskId
        this.groupId = groupId
        this.taskType = taskType
        this.status = status
        this.inputData = inputData
        this.resultId = resultId
        this.progressData = progressData
    }

    constructor(task: Task, groupId: String) {
        taskId = task.taskId
        this.groupId = groupId
        taskType = task.javaClass.canonicalName ?: ""
        inputData = task.inputData
        updateTask(task)
    }

    fun updateTask(task: Task) {
        status = task.status
        try {
            resultId = if (task.resultFuture.isDone) task.resultFuture.get()!!.resultId else null
        } catch (e: ExecutionException) {
            throw RuntimeException(e)
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }
        progressData = task.progressData
    }

    fun setResult(resultId: String?) {
        this.resultId = resultId
    }
}