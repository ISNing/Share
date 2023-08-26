package org.exthmui.share.taskMgr

import android.content.Context
import androidx.room.Database
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.exthmui.share.taskMgr.daos.GroupDao
import org.exthmui.share.taskMgr.daos.ResultDao
import org.exthmui.share.taskMgr.daos.TaskDao
import org.exthmui.share.taskMgr.entities.GroupEntity
import org.exthmui.share.taskMgr.entities.TaskEntity
import java.util.concurrent.Callable

@Database(entities = [TaskEntity::class, GroupEntity::class, Result::class], version = 1)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun groupDao(): GroupDao
    abstract fun resultDao(): ResultDao
    fun runInDatabaseThread(body: Runnable) {
        RoomExecutor().diskIO.execute { super.runInTransaction(body) }
    }

    fun <T> runInDatabaseThread(body: Callable<T>) {
        RoomExecutor().diskIO.execute { super.runInTransaction(body) }
    }

    fun <R> runInDatabaseThread(block: () -> R): Deferred<R> =
        CoroutineScope(Dispatchers.IO).async {
            super.runInTransaction(Callable { block() })
        }

    companion object {
        private var INSTANCE: TaskDatabase? = null
        fun getInstance(context: Context): TaskDatabase {
            if (INSTANCE == null) {
                INSTANCE = databaseBuilder(
                    context.applicationContext,
                    TaskDatabase::class.java, "task.db"
                )
                    .build()
            }
            return INSTANCE as TaskDatabase
        }
    }
}