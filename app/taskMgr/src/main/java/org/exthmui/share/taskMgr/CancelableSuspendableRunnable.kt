package org.exthmui.share.taskMgr

interface CancelableSuspendableRunnable {
    suspend fun runSuspendable()
    fun cancel()
}