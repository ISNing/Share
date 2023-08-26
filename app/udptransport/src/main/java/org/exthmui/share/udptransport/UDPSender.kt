package org.exthmui.share.udptransport

import android.content.Context
import android.util.Log
import androidx.annotation.IntRange
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.ArrayUtils
import org.exthmui.share.shared.base.Entity
import org.exthmui.share.shared.base.FileInfo
import org.exthmui.share.shared.base.receive.SenderInfo
import org.exthmui.share.shared.base.results.BasicTransmissionResult
import org.exthmui.share.shared.base.results.SuccessTransmissionResult
import org.exthmui.share.shared.base.results.TransmissionResult
import org.exthmui.share.shared.base.send.ReceiverInfo
import org.exthmui.share.shared.exceptions.trans.CancelledException
import org.exthmui.share.shared.exceptions.trans.RejectedException
import org.exthmui.share.shared.exceptions.trans.RemoteErrorException
import org.exthmui.share.shared.exceptions.trans.SenderCancelledException
import org.exthmui.share.shared.exceptions.trans.TransmissionException
import org.exthmui.share.udptransport.packets.FilePacket
import org.exthmui.share.udptransport.packets.ResendRequestPacket
import org.exthmui.utils.StackTraceUtils
import java.io.BufferedInputStream
import java.io.IOException
import java.net.DatagramPacket
import java.net.InetSocketAddress
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

class UDPSender(context: Context, private val listener: SendingListener?) :
    ResultMapCompletable(context) {
    private val tcpUtil: TCPUtil by lazy {
        TCPUtil().apply {
            setTAG(TAG)
            registerCommandConsumer(Regex(Constants.COMMAND_CANCEL)) {
                isReceiverCanceled = true
                cancel()
                true
            }
        }
    }
    private val udpUtil: UDPUtil by lazy {
        UDPUtil().apply {
            setTAG(TAG)
            setStateChecker { runBlocking { isCanceled() } }
        }
    }
    private lateinit var handler: UDPUtil.Handler
    var totalBytesToSend: Long = 0
    var bytesSent: Long = 0
    var receiverInfo: ReceiverInfo? = null
    var fileInfos: Array<FileInfo>? = null
    override var isSenderCanceled: Boolean = false
    override var isReceiverCanceled: Boolean = false
    private var remoteUdpPort = CompletableDeferred<Int>()
    private var connId = CompletableDeferred<Byte>()

    private val udpReadyCommandConsumer: (String) -> Boolean = {
        // e.g. UDP_READY5000:-128
        remoteUdpPort.complete(
            it.replace(Constants.COMMAND_UDP_SOCKET_READY, "").split(":".toRegex())
                .dropLastWhile { it.isEmpty() }.toTypedArray()[0].toInt()
        )
        connId.complete(
            it.replace(Constants.COMMAND_UDP_SOCKET_READY, "").split(":".toRegex())
                .dropLastWhile { it.isEmpty() }.toTypedArray()[1].toByte()
        )
        true
    }

    private lateinit var job: Deferred<Pair<TransmissionResult, Map<String, TransmissionResult>>>

    fun cancel() {
        isSenderCanceled = true
        job.cancel()
    }

    private fun updateProgress(
        status: Int,
        totalBytesToSend: Long,
        bytesSent: Long,
        curFileId: String?,
        curFileBytesToSend: Long,
        curFileBytesSent: Long
    ) {
        listener?.onProgressUpdate(
            status, totalBytesToSend, bytesSent, curFileId, curFileBytesToSend, curFileBytesSent
        )
    }

    private fun accept(accepted: Array<String>) {
        listener?.onAccepted(accepted)
    }

    fun sendAsync(
        entities: Array<Entity>,
        fileInfos: Array<FileInfo>,
        sender: SenderInfo,
        tcpAddress: InetSocketAddress
    ): Deferred<Pair<TransmissionResult, Map<String, TransmissionResult>>> {
        require(entities.size == fileInfos.size)
        job = ProcessLifecycleOwner.get().lifecycleScope.async(Dispatchers.Default) {
            val result = try {
                send(entities, fileInfos, sender, tcpAddress)
            } catch (tr: Throwable) {
                Log.e(
                    String.format(UDPSender.TAG, connId.await()),
                    "Error occurred while sending: $tr(message: ${tr.message})\n" + StackTraceUtils.getStackTraceString(
                        tr.stackTrace
                    )
                )
                setRestAs(SenderCancelledException(context))
                complete()
            } finally {
                releaseResources()
            }

            listener?.onComplete(result.first, result.second)
            result
        }
        return job
    }

    @Throws(IOException::class)
    private suspend fun send(
        entities: Array<Entity>,
        fileInfos: Array<FileInfo>,
        sender: SenderInfo,
        tcpAddress: InetSocketAddress
    ): Pair<TransmissionResult, Map<String, TransmissionResult>> = coroutineScope {
        require(entities.size == fileInfos.size)
        tcpUtil.connect(tcpAddress) // (1)
        tcpUtil.writeJson(sender) // (2)
        tcpUtil.writeJson(fileInfos) // (3)
        updateProgress(
            org.exthmui.share.shared.misc.Constants.TransmissionStatus.WAITING_FOR_ACCEPTATION.numVal,
            0,
            0,
            null,
            0,
            0
        )
        val accepted: Array<String> = tcpUtil.readJson(Array<String>::class.java) // (4)
        Log.d(TAG, "Remote accepted: ${ArrayUtils.toString(accepted)}")
        for (key in accepted) mutableResultMap[key] = BasicTransmissionResult.UNKNOWN_RESULT
        if (accepted.isEmpty()) {
            Log.d(TAG, "No file were accepted")
            return@coroutineScope completeRestWith(RejectedException(context))
        } else accept(accepted)
        val entitiesToSend: MutableList<Entity> = ArrayList()
        val fileInfosToSend: MutableList<FileInfo> = ArrayList()
        val acceptedIdsAsList = listOf(*accepted)
        for (fileInfo in fileInfos) {
            if (acceptedIdsAsList.contains(fileInfo.id)) {
                totalBytesToSend += fileInfo.fileSize
                fileInfosToSend.add(fileInfo)
                entitiesToSend.add(entities[listOf(*fileInfos).indexOf(fileInfo)])
            }
        }

        tcpUtil.readCommand(Regex(Constants.COMMAND_UDP_SOCKET_READY + ".+"), true).await().let {
            // e.g. UDP_READY5000:-128
            val params = it.replace(Constants.COMMAND_UDP_SOCKET_READY, "").split(":".toRegex())
                .dropLastWhile { it.isEmpty() }.toTypedArray()
            remoteUdpPort.complete(params[0].toInt())
            connId.complete(params[1].toByte())
            true
        }// (4-6)

        // TODO:CancellationException Handle
        remoteUdpPort.await()// (6)
        checkCanceledAndComplete()?.let { return@coroutineScope it }

        handler = udpUtil.addHandler(context, connId.await())
        udpUtil.connect(InetSocketAddress(tcpUtil.inetAddress, remoteUdpPort.await()))
        udpUtil.startListening()
        tcpUtil.writeCommand(
            String.format(
                Locale.ROOT,
                "%s%d:%d",
                Constants.COMMAND_UDP_SOCKET_READY,
                udpUtil.localPort,
                connId.await()
            )
        ) // (6)
        updateProgress(
            org.exthmui.share.shared.misc.Constants.TransmissionStatus.CONNECTION_ESTABLISHED.numVal,
            totalBytesToSend,
            0,
            null,
            0,
            0
        )
        for (entity in entitiesToSend) {
            Log.d(TAG, "Start sending file: " + entity.fileName)
            sendFile(entity, fileInfosToSend[entitiesToSend.indexOf(entity)])
            checkCanceledAndComplete()?.let { return@coroutineScope it }
        }
        val resultMap: Map<String, TransmissionResult> =
            TransmissionResult.stringToMap(context, tcpUtil.readBare()) // (16)

        resultMap.forEach {
            if (mutableResultMap.containsKey(it.key)) if (it.value is TransmissionException && it.value !is CancelledException && it.value !is RemoteErrorException) mutableResultMap[it.key] =
                RemoteErrorException(context, it.value as TransmissionException)
        }

        for (id in resultMap.keys) {
            if (org.exthmui.share.shared.misc.Constants.TransmissionStatus.ERROR.numVal != resultMap[id]?.statusCode) {
                complete(
                    RemoteErrorException(context)
                )
            }
        }

        complete()
    }

    enum class GroupStatus {
        Waiting,// Before START
        Transferring, Ending,// Packets all sent out
        DoResendCheck// After END, before resending check
    }

    enum class FileStatus {
        Waiting, Transferring, GroupIdExceeded, Ended
    }

    @Throws(IOException::class)
    suspend fun sendFile(entity: Entity, fileInfo: FileInfo?): TransmissionResult = coroutineScope {
        try {
            updateProgress(
                org.exthmui.share.shared.misc.Constants.TransmissionStatus.CONNECTION_ESTABLISHED.numVal,
                totalBytesToSend,
                0,
                null,
                0,
                0
            )
            val stream = entity.getInputStream(context)
            val inputStream =
                if (stream is BufferedInputStream) stream else BufferedInputStream(stream)
            val sendPacket = FilePacket()

            val startGroup = Constants.START_GROUP_ID
            val endGroup = Constants.END_GROUP_ID
            val startPacket = Constants.START_PACKET_ID
            val endPacket = Constants.END_PACKET_ID

            var isResending = false
            var groupStatus = GroupStatus.Waiting
            var fileStatus = FileStatus.Waiting

            @IntRange(
                from = Byte.MIN_VALUE.toLong(),
                to = Byte.MAX_VALUE.toLong()
            ) var curGroup by Delegates.vetoable(startGroup.toInt()) { _, _, newValue ->
                if (newValue > endGroup) {
                    fileStatus = FileStatus.GroupIdExceeded
                    true
                } else true
            }

            @IntRange(
                from = Byte.MIN_VALUE.toLong(),
                to = Byte.MAX_VALUE.toLong()
            ) var curPacket by Delegates.vetoable(startPacket.toInt()) { _, _, newValue ->
                if (newValue > endPacket) {
                    groupStatus = GroupStatus.Ending
                    false
                } else true
            }

            var packetIdIterator: Iterator<Number> by Delegates.observable((startPacket..endPacket).iterator()) { _, _, _ ->
                groupStatus = GroupStatus.Waiting
            }

            fun resetIdIterator() {
                packetIdIterator = (startPacket..endPacket).iterator()
            }

            val bufTemp = arrayOfNulls<ByteArray>(endPacket - startPacket + 1)
            val bufLen = arrayOfNulls<Int>(bufTemp.size)
            val buf = ByteArray(Constants.DATA_LEN_MAX_HI)
            var len: Int
            while (true) {
                checkCanceledSingleResult()?.let { return@coroutineScope it }
                when (fileStatus) {
                    FileStatus.Waiting -> {
                        fileStatus = FileStatus.Transferring
                    }

                    FileStatus.GroupIdExceeded -> {
                        handler.sendIdentifier(
                            Constants.Identifier.GROUP_ID_RESET,
                            byteArrayOf(curGroup.toByte(), startGroup)
                        )
                        curGroup = startGroup.toInt()
                    }

                    FileStatus.Transferring -> do {
                        when (groupStatus) {
                            GroupStatus.Waiting -> {
                                handler.sendIdentifier(
                                    Constants.Identifier.START, ArrayUtils.addFirst(
                                        ByteUtils.shortToBytes(startPacket), curGroup.toByte()
                                    )
                                ) // (7), (8)
                                groupStatus = GroupStatus.Transferring
                            }

                            GroupStatus.Transferring -> {
                                if (packetIdIterator.hasNext())
                                    curPacket = packetIdIterator.next().toInt()
                                else {
                                    groupStatus = GroupStatus.Ending
                                    continue
                                }
                                if (!isResending) {
                                    if (inputStream.read(buf).also { len = it } < 0) {
                                        groupStatus = GroupStatus.Ending
                                        fileStatus = FileStatus.Ended
                                        continue
                                    }
                                    bufTemp[curPacket - startPacket] =
                                        buf //TODO:.clone() needed?
                                    bufLen[curPacket - startPacket] =
                                        len
                                }
                                handler.sendPacket(
                                    sendPacket.setPacketId(curPacket.toShort())
                                        .setGroupId(curGroup.toByte())
                                        .setData(
                                            bufTemp[curPacket - startPacket],
                                            bufLen[curPacket - startPacket]!!
                                        )
                                ) // (9)
                            }

                            GroupStatus.Ending -> {
                                handler.sendIdentifier(
                                    Constants.Identifier.END, ArrayUtils.add(
                                        ArrayUtils.addFirst(
                                            ByteUtils.shortToBytes(curPacket.toShort()),
                                            curGroup.toByte()
                                        ),
                                        (if (isResending) 0x1 else 0x0).toByte()
                                    )
                                ) // (10), (11)
                                groupStatus = GroupStatus.DoResendCheck
                            }

                            GroupStatus.DoResendCheck -> {
                                val resendReq = handler.receivePacket<ResendRequestPacket>(
                                    { packet: DatagramPacket? ->
                                        ResendRequestPacket(packet)
                                    },
                                    Constants.ACK_TIMEOUT_MILLIS,
                                    TimeUnit.MILLISECONDS,
                                    Constants.MAX_ACK_TRYOUTS
                                ) // (12)
                                val packetIds = resendReq.packetIds
                                if (packetIds.isNotEmpty()) {
                                    Log.d(TAG, "Resend needed for group $curGroup: $packetIds")
                                    isResending = true
                                    packetIdIterator = packetIds.iterator()
                                } else {
                                    Log.d(TAG, "No resend needed for group $curGroup")
                                    isResending = false
                                    curGroup++
                                    resetIdIterator()
                                }
                            }
                        }
                    } while (groupStatus != GroupStatus.Waiting)

                    FileStatus.Ended -> {
                        handler.sendIdentifier(Constants.Identifier.FILE_END) // (7), (8)
                        break
                    }
                }
            }
            SuccessTransmissionResult(context)
        } catch (e: TransmissionException) {
            e
        }
    }

    fun releaseResources() {
        tcpUtil.releaseResources()
        udpUtil.releaseResources()
    }

    interface SendingListener {
        fun onAccepted(fileIdsAccepted: Array<String>?)
        fun onProgressUpdate(
            status: Int,
            totalBytesToSend: Long,
            bytesSent: Long,
            curFileId: String?,
            curFileBytesToSend: Long,
            curFileBytesSent: Long
        )

        fun onComplete(status: TransmissionResult, resultMap: Map<String, TransmissionResult>?)
    }

    companion object {
        const val TAG = "UDPSender"
    }
}
