package org.exthmui.share.taskMgr

import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import org.exthmui.share.taskMgr.converters.BundleConverter
import org.exthmui.utils.StackTraceUtils
import java.util.UUID

@Entity(tableName = "result")
data class Result @RestrictTo(RestrictTo.Scope.LIBRARY) constructor(
    @ColumnInfo(name = "result_id") @PrimaryKey val resultId: String,
    @ColumnInfo(
        name = "status"
    ) val status: Status,
    @field:TypeConverters(BundleConverter::class) @ColumnInfo(name = "data") val data: Bundle
) {

    enum class Status {
        SUCCESS, ERROR, CANCELLED
    }

    private constructor(status: Status, data: Bundle) : this(
        UUID.randomUUID().toString(), status, data
    )

    private constructor(status: Status, data: Bundle, error: Throwable?) : this(
        UUID.randomUUID().toString(), status, data, error
    )

    private constructor(resultId: String, status: Status, data: Bundle, error: Throwable?) : this(
        resultId, status, data
    ) {
        if (error != null) {
            data.putString(DATA_KEY_THROWABLE_NAME_STR, error.javaClass.name)
            data.putString(DATA_KEY_THROWABLE_MESSAGE_STR, error.message)
            data.putString(DATA_KEY_THROWABLE_MESSAGE_LOCAL_STR, error.localizedMessage)
            data.putString(
                DATA_KEY_THROWABLE_STACKTRACE_STR,
                StackTraceUtils.getStackTraceString(error.stackTrace)
            )
        }
    }

    override fun toString(): String {
        return "Result{" + "resultId='" + resultId + '\'' + ", status=" + status + ", data=" + data + '}'
    }

    companion object {
        @Ignore
        val DATA_KEY_THROWABLE_NAME_STR = "TASK_MGR_THROWABLE_NAME"

        @Ignore
        val DATA_KEY_THROWABLE_MESSAGE_STR = "TASK_MGR_THROWABLE_MESSAGE"

        @Ignore
        val DATA_KEY_THROWABLE_MESSAGE_LOCAL_STR = "TASK_MGR_THROWABLE_MESSAGE_LOCAL"

        @Ignore
        val DATA_KEY_THROWABLE_STACKTRACE_STR = "TASK_MGR_THROWABLE_STACKTRACE"

        fun success(data: Bundle): Result {
            return Result(Status.SUCCESS, data)
        }

        fun failure(): Result {
            return Result(Status.ERROR, Bundle())
        }

        fun failure(error: Throwable?): Result {
            return Result(Status.ERROR, Bundle(), error)
        }

        fun failure(data: Bundle): Result {
            return Result(Status.ERROR, data)
        }

        fun failure(data: Bundle?, error: Throwable?): Result {
            return Result(Status.ERROR, data ?: Bundle(), error)
        }

        fun cancelled(): Result {
            return Result(Status.CANCELLED, Bundle())
        }

        fun cancelled(data: Bundle): Result {
            return Result(Status.CANCELLED, data)
        }

        fun load(database: TaskDatabase, id: String?): Result? {
            return database.resultDao().getResultByTaskId(id)
        }
    }
}
