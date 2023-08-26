package org.exthmui.share.taskMgr.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import org.exthmui.share.taskMgr.Result

@Dao
interface ResultDao {
    @Insert
    fun insert(result: Result)

    @Update
    fun update(result: Result)

    @Delete
    fun delete(result: Result)

    @Query("SELECT * FROM result WHERE result_id = :resultId")
    fun getResultByTaskId(resultId: String?): Result?

    @get:Query("SELECT * FROM result")
    val allResults: List<Result>
}
