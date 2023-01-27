package org.exthmui.share.taskMgr.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import org.exthmui.share.taskMgr.entities.TaskEntity;

import java.util.List;

@Dao
public interface TaskDao {
    @Insert
    void insert(TaskEntity task);

    @Update
    void update(TaskEntity task);

    @Delete
    void delete(TaskEntity task);

    @Query("SELECT * FROM task WHERE task_id = :taskId")
    TaskEntity getTaskEntityById(String taskId);

    @Query("SELECT * FROM task WHERE group_id = :groupId")
    List<TaskEntity> getTaskEntitiesByGroupId(String groupId);

    @Query("SELECT * FROM task")
    List<TaskEntity> getAllTaskEntities();

}
