package org.exthmui.share.taskMgr.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import org.exthmui.share.taskMgr.Group;

import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "group")
public class GroupEntity {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "group_id")
    public final String groupId;
    @ColumnInfo(name = "max_concurrent_tasks")
    public int maxConcurrentTasks;
    @ColumnInfo(name = "task_ids")
    public final List<String> taskIds = new ArrayList<>();

    public GroupEntity(@NonNull String groupId, int maxConcurrentTasks, List<String> taskIds) {
        this.groupId = groupId;
        this.maxConcurrentTasks = maxConcurrentTasks;
        this.taskIds.addAll(taskIds);
    }

    public GroupEntity(@NonNull Group group) {
        this.groupId = group.getGroupId();
        updateGroup(group);
    }

    public void updateGroup(Group group) {
        maxConcurrentTasks = group.getMaxConcurrentTasks();
        taskIds.clear();
        taskIds.addAll(group.getTaskIds());
    }

}