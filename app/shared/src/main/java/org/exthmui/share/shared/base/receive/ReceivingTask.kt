package org.exthmui.share.shared.base.receive

import android.content.Context
import android.os.Bundle
import org.exthmui.share.shared.base.BaseTask
import org.exthmui.share.shared.base.Entity
import org.exthmui.share.shared.base.FileInfo
import org.exthmui.share.shared.base.results.SilentResult
import org.exthmui.share.shared.base.results.TransmissionResult
import org.exthmui.share.shared.exceptions.trans.TransmissionException
import org.exthmui.share.taskMgr.Result
import org.exthmui.share.taskMgr.Result.Companion.failure
import org.exthmui.share.taskMgr.Result.Companion.success
import org.exthmui.share.taskMgr.entities.TaskEntity

abstract class ReceivingTask : BaseTask {
    constructor(context: Context, inputData: Bundle?) : super(context, inputData)
    constructor(taskEntity: TaskEntity) : super(taskEntity)

    @Deprecated("")
    override fun genSuccessResult(): Result {
        throw RuntimeException("Stub! Use the genSuccessResult(SenderInfo, List<Entity>) defined in ReceivingWorker instead")
    }

    protected fun genSuccessResult(peer: SenderInfo, entities: List<Entity?>): Result {
        return success(genSuccessData(peer, entities))
    }

    protected fun genFailureResult(
        e: TransmissionException, peer: SenderInfo?,
        fileInfos: Array<FileInfo>?,
        resultMap: Map<String, TransmissionResult>?
    ): Result {
        return genFailureResult(e as TransmissionResult, peer, fileInfos, resultMap)
    }

    private fun genFailureResult(
        result: TransmissionResult, peer: SenderInfo?,
        fileInfos: Array<FileInfo>?,
        resultMap: Map<String, TransmissionResult>?
    ): Result {
        return failure(
            genFailureData(
                result, peer, fileInfos,
                resultMap
            )
        )
    }

    override fun genSilentResult(): Result {
        return failure(genFailureData(SilentResult(), null, null, null))
    }

    @Deprecated("")
    override fun genSuccessData(): Bundle {
        throw RuntimeException("Stub! Use the genSuccessData(SenderInfo, List<Entity>) defined in ReceivingWorker instead")
    }

    @Deprecated("")
    override fun genFailureData(
        result: TransmissionResult,
        resultMap: Map<String, TransmissionResult>?
    ): Bundle {
        throw RuntimeException("Stub! Use the genFailureData(TransmissionResult, SenderInfo, FileInfo[], Map) defined in ReceivingWorker instead")
    }

    protected fun genSuccessData(peer: SenderInfo, entities: List<Entity?>): Bundle {
        val bundle = super.genSuccessData()
        bundle.putStringArray(
            Entity.ENTITIES,
            Entity.entitiesToStrings(entities.toTypedArray<Entity?>())
        )
        bundle.putString(Receiver.FROM_PEER_ID, peer.id)
        bundle.putString(Receiver.FROM_PEER_NAME, peer.displayName)
        return bundle
    }

    protected fun genFailureData(
        result: TransmissionResult,
        peer: SenderInfo?, fileInfos: Array<FileInfo>?,
        resultMap: Map<String, TransmissionResult>?
    ): Bundle {
        val bundle = super.genFailureData(result, resultMap)
        if (peer != null) {
            bundle.putString(Receiver.FROM_PEER_ID, peer.id)
            bundle.putString(Receiver.FROM_PEER_NAME, peer.displayName)
        }
        if (fileInfos != null) {
            val fileNames = arrayOfNulls<String>(fileInfos.size)
            for (i in fileInfos.indices) fileNames[i] = fileInfos[i].fileName
            bundle.putStringArray(Entity.FILE_NAMES, fileNames)
        }
        return bundle
    }

    /**
     * InputData:
     * MUST contain: Nothing.
     * ***** Extra values is not allowed *****
     *
     * @return Result of work.
     * @see BaseTask.doWork
     * @see .genSuccessResult
     * @see .genSuccessData
     * @see .genFailureResult
     * @see .genFailureData
     */
    abstract override suspend fun doWork(): Result
}
