package org.exthmui.share.taskMgr

import java.util.concurrent.Executor
import java.util.concurrent.Executors

class RoomExecutor {
    val diskIO: Executor

    init {
        diskIO = Executors.newSingleThreadExecutor()
    }
}