package org.exthmui.share.lannsd

import android.content.Context
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import org.exthmui.share.shared.base.Entity
import org.exthmui.share.shared.base.FileInfo
import org.exthmui.share.shared.base.IConnectionType
import org.exthmui.share.shared.base.receive.ReceivingTask
import org.exthmui.share.shared.base.receive.SenderInfo
import org.exthmui.share.shared.base.results.TransmissionResult
import org.exthmui.share.shared.events.ReceiveActionAcceptEvent
import org.exthmui.share.shared.events.ReceiveActionRejectEvent
import org.exthmui.share.shared.exceptions.trans.InvalidInputDataException
import org.exthmui.share.shared.exceptions.trans.TransmissionException
import org.exthmui.share.shared.listeners.OnReceiveActionAcceptListener
import org.exthmui.share.shared.listeners.OnReceiveActionRejectListener
import org.exthmui.share.shared.misc.ReceiverUtils
import org.exthmui.share.taskMgr.Result
import org.exthmui.share.taskMgr.entities.TaskEntity
import org.exthmui.share.udptransport.UDPReceiver.ReceivingListener
import java.net.ServerSocket
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference

class NsdReceivingTask : ReceivingTask {

    constructor(context: Context?, inputData: Bundle?) : super(context!!, inputData)
    constructor(taskEntity: TaskEntity) : super(taskEntity)

    override val connectionType: IConnectionType
        get() = Metadata()

    private val serverSocketToServer: ServerSocket? = null

    override suspend fun doWork(): Result = coroutineScope {
        applicationContext?.let {
            val timeout = NsdUtils.getTimeout(it)
            val cancelledBySender = AtomicReference(false)
            val completed = CompletableDeferred<Boolean>()
            val generalResult = AtomicReference<TransmissionResult?>(null)
            val resultMap = AtomicReference<Map<String, TransmissionResult>?>(null)
            val input = inputData
            val connId = input.getByte(Constants.WORKER_INPUT_KEY_CONN_ID, 0.toByte())
            if (connId.toInt() == 0) return@coroutineScope genFailureResult(
                InvalidInputDataException(
                    it
                ), null
            )
            val receiver = NsdReceiver.getInstance(it)
            val udpReceiver = receiver.udpReceiver ?: return@coroutineScope genSilentResult()
            val handler = udpReceiver.getHandler(connId)
                ?: return@coroutineScope genFailureResult(InvalidInputDataException(it), null)
            handler.setListener(object : ReceivingListener {
                override fun requestAcceptationAsync(
                    senderInfo: SenderInfo,
                    fileInfos: Array<FileInfo>,
                    idsAccepted: CompletableFuture<Set<String>>?
                ) {
                    // Wait for acceptation from user
                    NsdReceiver.getInstance(it).registerListener(
                        OnReceiveActionAcceptListener { _: ReceiveActionAcceptEvent? ->
                            Log.d(TAG, "User accepted file")
                            val ids: MutableSet<String> = HashSet()
                            for (fileInfo in fileInfos) {
                                ids.add(fileInfo.id)
                            }
                            idsAccepted!!.complete(ids)
                        }
                    )
                    NsdReceiver.getInstance(it).registerListener(
                        OnReceiveActionRejectListener { event: ReceiveActionRejectEvent? ->
                            Log.d(TAG, "User rejected file")
                            idsAccepted!!.complete(emptySet())
                        }
                    )
                    ReceiverUtils.requestAcceptation(
                        it,
                        org.exthmui.share.shared.misc.Constants.CONNECTION_CODE_LANNSD,
                        taskId,
                        senderInfo,
                        fileInfos,
                        taskId.hashCode()
                    )
                }

                override fun onProgressUpdate(
                    status: Int, totalBytesToSend: Long, bytesReceived: Long,
                    senderInfo: SenderInfo,
                    fileInfos: Array<FileInfo>, curFileId: String?,
                    curFileBytesToSend: Long, curFileBytesReceived: Long
                ) {
                    updateProgress(
                        status,
                        totalBytesToSend,
                        bytesReceived,
                        fileInfos,
                        senderInfo,
                        curFileId,
                        curFileBytesToSend,
                        curFileBytesReceived,
                        totalBytesToSend == 0L
                    )
                }

                override fun onComplete(
                    result: TransmissionResult?,
                    r: Map<String, TransmissionResult>
                ) {
                    generalResult.set(result)
                    resultMap.set(r)
                    completed.complete(true)
                }
            })
            while (!completed.await()) {
                // Check if user cancelled
                if (isCancelled) {
                    Log.d(TAG, "User cancelled receiving file")
                    handler.cancel()
                }
            }
            if (generalResult.get() == null || generalResult.get()!!.status != org.exthmui.share.shared.misc.Constants.TransmissionStatus.COMPLETED) {
                return@coroutineScope genFailureResult(
                    (generalResult.get() as TransmissionException?)!!,
                    handler.senderInfo,
                    handler.fileInfos,
                    resultMap.get()
                )
            }
            val nsdReceiver = NsdReceiver.getInstance(it)
            val entities: MutableList<Entity?> = ArrayList(handler.fileInfos.size)
            for (fileInfo in handler.fileInfos) {
                entities.add(nsdReceiver.getEntity(fileInfo.id))
            }
            return@coroutineScope genSuccessResult(handler.senderInfo, entities)
        } ?: Result.failure()
    }

    companion object {
        const val TAG = "NsdReceivingTask"
    }
}
