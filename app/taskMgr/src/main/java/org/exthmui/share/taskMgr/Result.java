package org.exthmui.share.taskMgr;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import org.exthmui.share.taskMgr.converters.BundleConverter;
import org.exthmui.utils.StackTraceUtils;

import java.util.UUID;

@Entity(tableName = "result")
public class Result {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "result_id")
    private final String resultId;

    @ColumnInfo(name = "status")
    private final Status status;

    @ColumnInfo(name = "data")
    @TypeConverters(BundleConverter.class)
    private final Bundle data;

    @Ignore
    public static final String DATA_KEY_THROWABLE_NAME_STR="TASK_MGR_THROWABLE_NAME";
    @Ignore
    public static final String DATA_KEY_THROWABLE_MESSAGE_STR="TASK_MGR_THROWABLE_MESSAGE";
    @Ignore
    public static final String DATA_KEY_THROWABLE_MESSAGE_LOCAL_STR="TASK_MGR_THROWABLE_MESSAGE_LOCAL";
    @Ignore
    public static final String DATA_KEY_THROWABLE_STACKTRACE_STR="TASK_MGR_THROWABLE_STACKTRACE";

    public enum Status {
        SUCCESS, ERROR, CANCELLED
    }

    private Result(Status status, Bundle data) {
        this(UUID.randomUUID().toString(), status, data);
    }

    private Result(Status status, Bundle data, Throwable error) {
        this(UUID.randomUUID().toString(), status, data, error);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public Result(@NonNull String resultId, Status status, Bundle data) {
        this.resultId = resultId;
        this.status = status;
        this.data = data;
    }

    private Result(String resultId, Status status, Bundle data, Throwable error) {
        this(resultId, status, data);
        if (error != null) {
            data.putString(DATA_KEY_THROWABLE_NAME_STR, error.getClass().getName());
            data.putString(DATA_KEY_THROWABLE_MESSAGE_STR, error.getMessage());
            data.putString(DATA_KEY_THROWABLE_MESSAGE_LOCAL_STR, error.getLocalizedMessage());
            data.putString(DATA_KEY_THROWABLE_STACKTRACE_STR,
                    StackTraceUtils.getStackTraceString(error.getStackTrace()));
        }
    }

    public String getResultId() {
        return resultId;
    }

    public Status getStatus() {
        return status;
    }

    public Bundle getData() {
        return data;
    }

    public static Result success(Bundle data) {
        return new Result(Status.SUCCESS, data);
    }

    public static Result failure() {
        return new Result(Status.ERROR, null);
    }

    public static Result failure(Throwable error) {
        return new Result(Status.ERROR, null, error);
    }

    public static Result failure(Bundle data) {
        return new Result(Status.ERROR, data);
    }

    public static Result failure(Bundle data, Throwable error) {
        return new Result(Status.ERROR, data, error);
    }

    public static Result cancelled() {
        return new Result(Status.CANCELLED, null);
    }
    public static Result cancelled(Bundle data) {
        return new Result(Status.CANCELLED, data);
    }

    public static Result load(TaskDatabase database, String id) {
        Result result = database.resultDao().getResultByTaskId(id);
        return result;
    }
}
