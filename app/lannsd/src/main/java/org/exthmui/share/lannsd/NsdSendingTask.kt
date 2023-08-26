package org.exthmui.share.lannsd

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.coroutineScope
import org.exthmui.share.lannsd.exceptions.FailedResolvingPeerException
import org.exthmui.share.shared.base.Entity
import org.exthmui.share.shared.base.FileInfo
import org.exthmui.share.shared.base.IConnectionType
import org.exthmui.share.shared.base.receive.SenderInfo
import org.exthmui.share.shared.base.results.TransmissionResult
import org.exthmui.share.shared.base.send.ReceiverInfo
import org.exthmui.share.shared.base.send.SendingTask
import org.exthmui.share.shared.exceptions.trans.FileIOErrorException
import org.exthmui.share.shared.exceptions.trans.PeerDisappearedException
import org.exthmui.share.shared.exceptions.trans.ReceiverCancelledException
import org.exthmui.share.shared.exceptions.trans.RejectedException
import org.exthmui.share.shared.exceptions.trans.SenderCancelledException
import org.exthmui.share.shared.exceptions.trans.UnknownErrorException
import org.exthmui.share.shared.misc.Utils
import org.exthmui.share.taskMgr.Result
import org.exthmui.share.taskMgr.entities.TaskEntity
import org.exthmui.share.udptransport.UDPSender
import org.exthmui.share.udptransport.UDPSender.SendingListener
import org.exthmui.utils.StackTraceUtils
import java.io.IOException
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class NsdSendingTask : SendingTask {
    constructor(context: Context, inputData: Bundle?) : super(context, inputData)
    constructor(taskEntity: TaskEntity) : super(taskEntity)

    override val connectionType: IConnectionType
        get() = Metadata()

    @SuppressLint("RestrictedApi")
    override suspend fun doWork(
        entities: Array<Entity>,
        peerId: String,
        peerName: String
    ): Result =
        coroutineScope {
            applicationContext?.let { applicationContext ->
                val fileInfos = Array<FileInfo>(entities.size) { i ->
                    FileInfo(entities[i]).also {
                        if (NsdUtils.isMd5ValidationEnabled(applicationContext)) try {
                            entities[i].calculateMd5(applicationContext)
                            it.putExtra(
                                org.exthmui.share.udptransport.Constants.FILE_INFO_EXTRA_KEY_MD5,
                                entities[i]
                                    .md5
                            )
                        } catch (e: IOException) {
                            Log.i(TAG, StackTraceUtils.getStackTraceString(e.stackTrace))
                            return@coroutineScope genFailureResult(
                                FileIOErrorException(applicationContext, e),
                                null
                            )
                        } else Log.d(
                            TAG, String.format(
                                "Md5 Validation is disabled, skipping generating md5 for %s(%s)",
                                entities[i].fileName, it.id
                            )
                        )
                    }
                }

                // Load Peer
                val manager = NsdManager.getInstance(
                    applicationContext
                )
                val peer = arrayOf(manager.peers[peerId] as NsdPeer?)
                if (peer[0] == null) return@coroutineScope genFailureResult(
                    PeerDisappearedException(
                        applicationContext
                    ), null
                )
                if (!peer[0]!!.isAttributesLoaded) {
                    val succeeded = BooleanArray(1)
                    val latch = CountDownLatch(1)
                    NsdUtils.resolvePeer(
                        applicationContext,
                        peer[0],
                        object : NsdUtils.ResolveListener {
                            override fun onResolveFailed(p: NsdPeer, errorCode: Int) {
                                succeeded[0] = false
                                peer[0] = p
                                latch.countDown()
                            }

                            override fun onServiceResolved(p: NsdPeer) {
                                succeeded[0] = true
                                peer[0] = p
                                latch.countDown()
                            }
                        })
                    try {
                        latch.await()
                        if (!succeeded[0]) return@coroutineScope genFailureResult(
                            FailedResolvingPeerException(
                                applicationContext
                            ), null
                        )
                    } catch (e: InterruptedException) {
                        return@coroutineScope genFailureResult(
                            FailedResolvingPeerException(applicationContext, e),
                            null
                        )
                    }
                }
                // End Load Peer

                // Initialize ReceiverInfo
                val receiverInfo = ReceiverInfo(
                    peer[0]!!, peer[0]!!.serverPort
                )
                val result = AtomicReference<Result>(null)
                val timeout = NsdUtils.getTimeout(applicationContext)
                val serverPort = peer[0]!!.serverPort

                // Initial SenderInfo object
                val senderInfo = SenderInfo()
                senderInfo.setDisplayName(Utils.getSelfName(applicationContext))
                senderInfo.setId(Utils.getSelfId(applicationContext))
                senderInfo.protocolVersion = Constants.SHARE_PROTOCOL_VERSION_1
                senderInfo.uid = 0 //TODO: Get from account sdk
                senderInfo.accountServerSign = "" //TODO: Get from account sdk
                suspendCoroutine<Result> { continuation ->
                    val sender = UDPSender(applicationContext, object : SendingListener {
                        override fun onAccepted(fileIdsAccepted: Array<String>?) {}
                        override fun onProgressUpdate(
                            status: Int, totalBytesToSend: Long, bytesSent: Long,
                            curFileId: String?, curFileBytesToSend: Long, curFileBytesSent: Long
                        ) {
                            updateProgress(
                                status,
                                totalBytesToSend,
                                bytesSent,
                                fileInfos,
                                receiverInfo,
                                curFileId,
                                curFileBytesToSend,
                                curFileBytesSent,
                                totalBytesToSend == 0L
                            )
                        }

                        override fun onComplete(
                            status: TransmissionResult,
                            resultMap: Map<String, TransmissionResult>?
                        ) {
                            if (status.statusCode or org.exthmui.share.shared.misc.Constants.TransmissionStatus.COMPLETED.numVal == status.statusCode) result.set(
                                genSuccessResult()
                            )
                            if (status.statusCode or org.exthmui.share.shared.misc.Constants.TransmissionStatus.REJECTED.numVal == status.statusCode) result.set(
                                genFailureResult(
                                    RejectedException(
                                        applicationContext
                                    ), null
                                )
                            ) else if (status.statusCode or org.exthmui.share.shared.misc.Constants.TransmissionStatus.SENDER_CANCELLED.numVal == status.statusCode) result.set(
                                genFailureResult(
                                    SenderCancelledException(
                                        applicationContext
                                    ), null
                                )
                            ) else if (status.statusCode or org.exthmui.share.shared.misc.Constants.TransmissionStatus.RECEIVER_CANCELLED.numVal == status.statusCode) result.set(
                                genFailureResult(
                                    ReceiverCancelledException(
                                        applicationContext
                                    ), null
                                )
                            ) else if (status.statusCode or org.exthmui.share.shared.misc.Constants.TransmissionStatus.ERROR.numVal == status.statusCode) result.set(
                                genFailureResult(
                                    UnknownErrorException(
                                        applicationContext
                                    ), null
                                )
                            )
                            continuation.resume(result.get())
                        }
                    })
                    try {
                        sender.sendAsync(
                            entities, fileInfos, senderInfo, InetSocketAddress(
                                peer[0]!!.address, serverPort
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(
                            TAG,
                            String.format(
                                "Error occurred while sending: %s(message: %s)\n%s",
                                e,
                                e.message,
                                StackTraceUtils.getStackTraceString(e.stackTrace)
                            )
                        )
                    }
                }
            } ?: Result.failure()
        }

    companion object {
        const val TAG = "NsdSendingTask"
    }
}
