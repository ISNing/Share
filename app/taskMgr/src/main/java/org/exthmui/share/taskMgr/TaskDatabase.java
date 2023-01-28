package org.exthmui.share.taskMgr;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import org.exthmui.share.taskMgr.daos.GroupDao;
import org.exthmui.share.taskMgr.daos.ResultDao;
import org.exthmui.share.taskMgr.daos.TaskDao;
import org.exthmui.share.taskMgr.entities.GroupEntity;
import org.exthmui.share.taskMgr.entities.TaskEntity;

import java.util.concurrent.Callable;

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

    public void runInDatabaseThread(@NonNull Runnable body) {
        new RoomExecutor().getDiskIO().execute(() -> super.runInTransaction(body));
    }

    public <T> void runInDatabaseThread(@NonNull Callable<T> body) {
        new RoomExecutor().getDiskIO().execute(() -> super.runInTransaction(body));
    }
}