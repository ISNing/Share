package org.exthmui.share.taskMgr.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import org.exthmui.share.taskMgr.entities.GroupEntity;

import java.util.List;

@Dao
public interface GroupDao {
    @Insert
    void insert(GroupEntity group);

    @Update
    void update(GroupEntity group);

    @Delete
    void delete(GroupEntity group);

    @Query("SELECT * FROM `group` WHERE group_id = :groupId")
    GroupEntity getGroupEntityById(String groupId);

    @Query("SELECT * FROM `group`")
    List<GroupEntity> getAllGroupEntities();
}
