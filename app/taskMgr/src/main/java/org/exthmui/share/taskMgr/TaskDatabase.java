package org.exthmui.share.taskMgr;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import org.exthmui.share.taskMgr.daos.*;
import org.exthmui.share.taskMgr.entities.GroupEntity;
import org.exthmui.share.taskMgr.entities.TaskEntity;

@Database(entities = {TaskEntity.class, GroupEntity.class, Result.class}, version = 1)
public abstract class TaskDatabase extends RoomDatabase {
    public abstract TaskDao taskDao();
    public abstract GroupDao groupDao();
    public abstract ResultDao resultDao();

    private static TaskDatabase INSTANCE;

    public static TaskDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            TaskDatabase.class, "task_db")
                    .build();
        }
        return INSTANCE;
    }
}