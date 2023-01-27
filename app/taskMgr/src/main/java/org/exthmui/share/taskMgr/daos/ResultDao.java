package org.exthmui.share.taskMgr.daos;


import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import org.exthmui.share.taskMgr.Result;

import java.util.List;

@Dao
public interface ResultDao {
    @Insert
    void insert(Result result);

    @Update
    void update(Result result);

    @Delete
    void delete(Result result);

    @Query("SELECT * FROM result WHERE result_id = :resultId")
    Result getResultByTaskId(String resultId);

    @Query("SELECT * FROM result")
    List<Result> getAllResults();
}
