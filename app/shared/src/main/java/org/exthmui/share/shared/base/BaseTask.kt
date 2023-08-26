package org.exthmui.share.shared.base

import android.content.Context
import android.os.Bundle
import org.exthmui.share.shared.base.results.SilentResult
import org.exthmui.share.shared.base.results.TransmissionResult
import org.exthmui.share.shared.exceptions.trans.TransmissionException
import org.exthmui.share.taskMgr.Result
import org.exthmui.share.taskMgr.Result.Companion.failure
import org.exthmui.share.taskMgr.Result.Companion.success
import org.exthmui.share.taskMgr.SuspendableTask
import org.exthmui.share.taskMgr.entities.TaskEntity
import java.util.UUID

abstract class BaseTask : SuspendableTask {
    protected var applicationContext: Context? = null
        private set

    constructor(context: Context, inputData: Bundle?) : super(
        UUID.randomUUID().toString(),
        inputData
    ) {
        applicationContext = context.applicationContext
    }

    constructor(taskEntity: TaskEntity) : super(taskEntity)

    abstract val connectionType: IConnectionType

    /**
     * InputData are customized by implements
     *
     * @return See below:
     * Success:
     * MUST contain:Everything in InputData & nothing else
     * @see .genSuccessData
     * @see .genSuccessResult
     * @see .genFailureData
     * @see .genFailureResult
     * @see .genFailureResult
     */
    abstract override suspend fun doWork(): Result
    protected fun updateProgress(
        statusCode: Int, totalBytesToSend: Long, bytesReceived: Long,
        fileInfos: Array<FileInfo>,
        peerInfoTransfer: PeerInfoTransfer?,
        curFileId: String?, curFileBytesToSend: Long,
        curFileBytesSent: Long, indeterminate: Boolean
    ) {
        super.updateProgress(
            genProgressData(
                statusCode, totalBytesToSend, bytesReceived,
                fileInfos, peerInfoTransfer, curFileId, curFileBytesToSend, curFileBytesSent,
                indeterminate
            )
        )
    }

    protected open fun genSuccessResult(): Result {
        return success(genSuccessData())
    }

    protected fun genFailureResult(
        e: TransmissionException,
        resultMap: Map<String, TransmissionResult>?
    ): Result {
        return genFailureResult(e as TransmissionResult, resultMap)
    }

    private fun genFailureResult(
        result: TransmissionResult,
        resultMap: Map<String, TransmissionResult>?
    ): Result {
        return failure(genFailureData(result, resultMap))
    }

    protected open fun genSilentResult(): Result {
        return failure(genFailureData(SilentResult(), null))
    }

    protected open fun genSuccessData(): Bundle {
        return Bundle(inputData)
    }

    protected fun genProgressData(
        statusCode: Int, totalBytesToSend: Long,
        bytesTransmitted: Long,
        fileInfos: Array<FileInfo>,
        peerInfoTransfer: PeerInfoTransfer?,
        curFileId: String?,
        curFileBytesToSend: Long,
        curFileBytesTransmitted: Long, indeterminate: Boolean
    ): Bundle {
        val bundle = Bundle(inputData)
        bundle.putInt(STATUS_CODE, statusCode)
        bundle.putLong(P_BYTES_TOTAL, totalBytesToSend)
        bundle.putLong(P_BYTES_TRANSMITTED, bytesTransmitted)
        bundle.putSerializable(P_FILE_INFOS, fileInfos)
        bundle.putSerializable(P_PEER_INFO_TRANSFER, peerInfoTransfer)
        bundle.putString(P_CUR_FILE_ID, curFileId)
        bundle.putLong(P_CUR_FILE_BYTES_TOTAL, curFileBytesToSend)
        bundle.putLong(P_CUR_FILE_BYTES_TRANSMITTED, curFileBytesTransmitted)
        bundle.putBoolean(P_INDETERMINATE, indeterminate)
        return bundle
    }

    /**
     * Generate Failure Data
     *
     * @param result    General result
     * @param resultMap Result map
     * @return Bundle
     */
    protected open fun genFailureData(
        result: TransmissionResult,
        resultMap: Map<String, TransmissionResult>?
    ): Bundle {
        val bundle = Bundle(inputData)
        bundle.putInt(STATUS_CODE, result.statusCode)
        bundle.putString(F_MESSAGE, result.message)
        bundle.putString(F_MESSAGE, result.message)
        bundle.putString(F_LOCALIZED_MESSAGE, result.localizedMessage)
        bundle.putString(
            F_RESULT_MAP,
            if (resultMap == null) null else TransmissionResult.mapToString(resultMap)
        )
        return bundle
    }

    companion object {
        const val STATUS_CODE = "STATUS_CODE"
        const val P_BYTES_TOTAL = "BYTES_TOTAL"
        const val P_BYTES_TRANSMITTED = "BYTES_TRANSMITTED"
        const val P_FILE_INFOS = "FILE_INFOS"
        const val P_PEER_INFO_TRANSFER = "PEER_INFO_TRANSFER"
        const val P_CUR_FILE_ID = "CUR_FILE_ID"
        const val P_CUR_FILE_BYTES_TOTAL = "CUR_FILE_BYTES_TOTAL"
        const val P_CUR_FILE_BYTES_TRANSMITTED = "CUR_FILE_BYTES_TRANSMITTED"
        const val P_INDETERMINATE = "INDETERMINATE"
        const val F_MESSAGE = "MESSAGE"
        const val F_LOCALIZED_MESSAGE = "LOCALIZED_MESSAGE"
        const val F_RESULT_MAP = "RESULT_MAP"
    }
}
