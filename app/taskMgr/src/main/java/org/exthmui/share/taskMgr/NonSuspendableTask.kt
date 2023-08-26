package org.exthmui.share.taskMgr

import android.os.Bundle
import org.exthmui.share.taskMgr.entities.TaskEntity

abstract class NonSuspendableTask : Task {
    constructor(taskId: String, inputData: Bundle?) : super(taskId, inputData)
    constructor(taskEntity: TaskEntity) : super(taskEntity)

    override suspend fun doWorkDelegate() = doWork()

    abstract fun doWork(): Result
}