package org.exthmui.share.taskMgr.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import org.exthmui.share.taskMgr.entities.TaskEntity

@Dao
interface TaskDao {
    @Insert
    fun insert(task: TaskEntity)

    @Update
    fun update(task: TaskEntity)

    @Delete
    fun delete(task: TaskEntity)

    @Query("SELECT * FROM task WHERE task_id = :taskId")
    fun getTaskEntityById(taskId: String?): TaskEntity?

    @Query("SELECT * FROM task WHERE group_id = :groupId")
    fun getTaskEntitiesByGroupId(groupId: String?): List<TaskEntity>

    @get:Query("SELECT * FROM task")
    val allTaskEntities: List<TaskEntity?>?
}
