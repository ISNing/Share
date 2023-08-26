package org.exthmui.share.taskMgr.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import org.exthmui.share.taskMgr.Group
import org.exthmui.share.taskMgr.converters.StringListConvertor

@Entity(tableName = "group")
class GroupEntity {
    @PrimaryKey
    @ColumnInfo(name = "group_id")
    val groupId: String

    @ColumnInfo(name = "max_concurrent_tasks")
    var maxConcurrentTasks = 0

    @ColumnInfo(name = "task_ids")
    @TypeConverters(StringListConvertor::class)
    val taskIds: MutableList<String> = ArrayList()

    constructor(groupId: String, maxConcurrentTasks: Int, taskIds: MutableList<String>) {
        this.groupId = groupId
        this.maxConcurrentTasks = maxConcurrentTasks
        this.taskIds.addAll(taskIds)
    }

    constructor(group: Group) {
        groupId = group.groupId
        updateGroup(group)
    }

    fun updateGroup(group: Group) {
        maxConcurrentTasks = group.maxConcurrentTasks
        taskIds.clear()
        taskIds.addAll(group.getTaskIds())
    }
}