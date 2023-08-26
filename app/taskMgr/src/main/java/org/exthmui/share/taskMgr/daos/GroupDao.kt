package org.exthmui.share.taskMgr.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import org.exthmui.share.taskMgr.entities.GroupEntity

@Dao
interface GroupDao {
    @Insert
    fun insert(group: GroupEntity)

    @Update
    fun update(group: GroupEntity)

    @Delete
    fun delete(group: GroupEntity)

    @Query("SELECT * FROM `group` WHERE group_id = :groupId")
    fun getGroupEntityById(groupId: String?): GroupEntity?

    @get:Query("SELECT * FROM `group`")
    val allGroupEntities: List<GroupEntity>
}
