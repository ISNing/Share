package org.exthmui.share.udptransport

import android.content.Context
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.apache.commons.lang3.ArrayUtils
import org.apache.commons.lang3.StringUtils
import org.exthmui.share.shared.base.FileInfo
import org.exthmui.share.shared.base.receive.SenderInfo
import org.exthmui.share.shared.base.results.BasicTransmissionResult
import org.exthmui.share.shared.base.results.SuccessTransmissionResult
import org.exthmui.share.shared.base.results.TransmissionResult
import org.exthmui.share.shared.exceptions.trans.FileIOErrorException
import org.exthmui.share.shared.exceptions.trans.ReceiverCancelledException
import org.exthmui.share.shared.exceptions.trans.RejectedException
import org.exthmui.share.shared.exceptions.trans.SenderCancelledException
import org.exthmui.share.shared.exceptions.trans.TimedOutException
import org.exthmui.share.shared.exceptions.trans.TransmissionException
import org.exthmui.share.shared.exceptions.trans.UnknownErrorException
import org.exthmui.share.udptransport.Constants.UdpCommand
import org.exthmui.share.udptransport.exceptions.FailedCreatingStreamException
import org.exthmui.share.udptransport.packets.AbstractCommandPacket
import org.exthmui.share.udptransport.packets.FilePacket
import org.exthmui.share.udptransport.packets.IdentifierPacket
import org.exthmui.share.udptransport.packets.ResendRequestPacket
import org.exthmui.utils.FileUtils
import org.exthmui.utils.StackTraceUtils
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.Arrays
import java.util.Locale
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class UDPReceiver(
    private val context: Context,
    private val outputStreamFactory: OutputStreamFactory,
    private val inputStreamFactory: InputStreamFactory,
    private val listener: ConnectionListener,
    var serverTcpPort: Int,
    var serverUdpPort: Int,
    private val lazyInit: Boolean,
    private val validateMd5: Boolean
) {
    private var serverSocket: ServerSocket? = null
    private var udpUtil: UDPUtil? = null
    private var commandWatcherStopFlag = false
    private var isRunning = false
    private var tcpReady = false
    private val udpReady = false
    private val coreThreadPool = ThreadPoolExecutor(
        1, 4, 5L, TimeUnit.SECONDS, ArrayBlockingQueue(2)
    ) { r: Runnable -> Thread(r, r.toString()) }
    private val threadPool = ThreadPoolExecutor(
        0, 3, 1L, TimeUnit.SECONDS, SynchronousQueue()
    ) { r: Runnable -> Thread(r, r.toString()) }
    private val connectionWatcher: Runnable

    init {
        if (!lazyInit) initialize()
        connectionWatcher = Runnable {
            while (isRunning) {
                try {
                    for (i in Byte.MIN_VALUE..Byte.MAX_VALUE) if (i != 0 /* 0 is a invalid connection id*/ && !CONN_ID_HANDLER_MAP.containsKey(
                            i.toByte()
                        )
                    ) {
                        assert(serverSocket != null)
                        val socket = serverSocket!!.accept()
                        assert(udpUtil != null)
                        udpUtil!!.addHandler(context, i.toByte())
                        val handler: ConnectionHandler = ConnectionHandler(i.toByte(), socket)
                        CONN_ID_HANDLER_MAP[i.toByte()] = handler
                        handler.receiveAsync()
                        listener.onConnectionEstablished(i.toByte())
                        break
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    @Throws(IOException::class)
    fun initialize() {
        if (!tcpReady) tcpReady()
        if (!udpReady) udpReady()
    }

    @Throws(IOException::class)
    private fun tcpReady() {
        serverSocket = ServerSocket(serverTcpPort)
        serverSocket!!.reuseAddress = true
        serverTcpPort = serverSocket!!.localPort
        tcpReady = true
    }

    private fun tcpStop() {
        if (serverSocket != null) {
            Utils.silentClose(serverSocket)
            serverSocket = null
        }
        tcpReady = false
    }

    @Throws(SocketException::class)
    private fun udpReady() {
        udpUtil = UDPUtil(serverUdpPort)
        udpUtil!!.setTAG(TAG)
        serverUdpPort = udpUtil!!.localPort
        udpUtil!!.startListening()
    }

    private fun udpStop() {
        udpUtil?.releaseResources()
    }

    @Throws(IOException::class)
    fun startReceive() {
        isRunning = true
        if (lazyInit) initialize()
        threadPool.execute(connectionWatcher)
    }

    fun stopReceive() {
        if (isRunning) {
            tcpStop()
            udpStop()
        }
    }

    fun getHandler(connId: Byte): ConnectionHandler? {
        return CONN_ID_HANDLER_MAP[connId]
    }

    inner class ConnectionHandler internal constructor(val connId: Byte, socket: Socket) :
        ResultMapCompletable(context) {
        private val handlerLock = Any()
        private val address: InetAddress
        private val remoteTcpPort: Int
        private var remoteUdpPort = CompletableDeferred<Int>()
        override var isSenderCanceled = false
        override var isReceiverCanceled = false

        var totalBytesToSend: Long = 0
        var bytesReceived: Long = 0

        lateinit var senderInfo: SenderInfo

        lateinit var fileInfos: Array<FileInfo>
        private val tcpUtil: TCPUtil
        private val handler: UDPUtil.Handler
        private var listener: ReceivingListener? = null

        private lateinit var job: Deferred<Pair<TransmissionResult, Map<String, TransmissionResult>>>

        fun cancel() {
            isSenderCanceled = true
            job.cancel()
        }

        private fun updateProgress(
            status: Int,
            totalBytesToSend: Long,
            bytesReceived: Long,
            senderInfo: SenderInfo,
            fileInfos: Array<FileInfo>,
            curFileId: String?,
            curFileBytesToSend: Long,
            curFileBytesReceived: Long
        ) {
            listener?.onProgressUpdate(
                status,
                totalBytesToSend,
                bytesReceived,
                senderInfo,
                fileInfos,
                curFileId,
                curFileBytesToSend,
                curFileBytesReceived
            )
        }

        fun setListener(listener: ReceivingListener?): ConnectionHandler {
            this.listener = listener
            return this
        }

        init {
            address = socket.inetAddress
            remoteTcpPort = socket.port
            assert(udpUtil != null)
            handler = udpUtil!!.getHandler(connId)!!
            tcpUtil = TCPUtil(socket)
            tcpUtil.setTAG(TAG)
            tcpUtil.initialize()
            tcpUtil.registerCommandConsumer(Regex(Constants.COMMAND_CANCEL)) {
                isReceiverCanceled = true
                true
            }
        }

        fun receiveAsync(): Deferred<Pair<TransmissionResult, Map<String, TransmissionResult>>> {
            job = ProcessLifecycleOwner.get().lifecycleScope.async(Dispatchers.Default) {
                val result = try {
                    receive()
                } catch (tr: Throwable) {
                    Log.e(
                        String.format(UDPSender.TAG, connId),
                        "Error occurred while sending: $tr(message: ${tr.message})\n" +
                                StackTraceUtils.getStackTraceString(tr.stackTrace)
                    )
                    setRestAs(ReceiverCancelledException(context))
                    complete()
                }
                tcpUtil.writeBare(TransmissionResult.mapToString(result.second))//TODO
                releaseResources()

                listener?.onComplete(result.first, result.second);
                result
            }
            return job
        }

        @Throws(IOException::class, ExecutionException::class, InterruptedException::class)
        suspend fun receive(): Pair<TransmissionResult, Map<String, TransmissionResult>> =
            coroutineScope {
                val senderInfo = tcpUtil.readJson(SenderInfo::class.java) // (2)
                this@ConnectionHandler.senderInfo = senderInfo
                val fileInfos = tcpUtil.readJson(
                    Array<FileInfo>::class.java
                ) // (3)
                fileInfos.forEach {
                    mutableResultMap[it.id] = BasicTransmissionResult.UNKNOWN_RESULT
                }
                val idsAccepted = CompletableFuture<Set<String>>()
                while (true) {
                    if (listener != null) break// TODO
                } // Loop while worker is not started
                listener?.requestAcceptationAsync(senderInfo, fileInfos, idsAccepted)
                updateProgress(
                    org.exthmui.share.shared.misc.Constants.TransmissionStatus.WAITING_FOR_ACCEPTATION.numVal,
                    0,
                    0,
                    senderInfo,
                    fileInfos,
                    null,
                    0,
                    0
                )
                val acceptedIdsAsSet = idsAccepted.get()
                val accepted = acceptedIdsAsSet.toTypedArray<String>()

                val fileInfosToReceive: MutableList<FileInfo> =
                    fileInfos.filter { accepted.contains(it.id) }.toMutableList()

                fileInfos.forEach {
                    if (fileInfosToReceive.contains(it)) totalBytesToSend += it.fileSize
                    else mutableResultMap[it.id] = RejectedException(context)
                }

                tcpUtil.writeJson(accepted) // (4)
                if (accepted.isEmpty()) {
                    Log.d(TAG, "No file were accepted")
                    return@coroutineScope complete(RejectedException(context))
                }

                this@ConnectionHandler.fileInfos = fileInfosToReceive.toTypedArray<FileInfo>()
                assert(udpUtil != null)
                tcpUtil.writeCommand(
                    String.format(
                        Locale.ROOT,
                        "%s%d:%d",
                        Constants.COMMAND_UDP_SOCKET_READY,
                        serverUdpPort,
                        connId
                    )
                ) // (6)

                // TODO:CancellationException Handle
                tcpUtil.readCommand(Regex(Constants.COMMAND_UDP_SOCKET_READY + ".+"), true).await()
                    .let {
                        // e.g. UDP_READY5000:-128
                        remoteUdpPort.complete(
                            it.replace(Constants.COMMAND_UDP_SOCKET_READY, "")
                                .split(":".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray()[0].toInt()
                        )
                        true
                    }// (4-6)
                remoteUdpPort.await()

                checkCanceledAndComplete()?.let { return@coroutineScope it }

                udpUtil!!.connect(InetSocketAddress(address, remoteUdpPort.await()))
                for (fileInfo in fileInfosToReceive) {
                    mutableResultMap[fileInfo.id] = receiveFile(fileInfo)
                    checkCanceledAndComplete()?.let { return@coroutineScope it }
                }
                commandWatcherStopFlag = true
                // (16)
                return@coroutineScope complete()
            }

        @Throws(IOException::class)
        private suspend fun receiveFile(fileInfo: FileInfo): TransmissionResult = coroutineScope {
            try {
                Log.d(
                    TAG,
                    String.format("Start receiving file: %s(%s)", fileInfo.fileName, fileInfo.id)
                )
                val rawOutputStream = outputStreamFactory.produce(fileInfo)
                val outputStream =
                    if (rawOutputStream is BufferedOutputStream) rawOutputStream else BufferedOutputStream(
                        rawOutputStream
                    )// Use BufferedOutputStream to enhance performance
                updateProgress(
                    org.exthmui.share.shared.misc.Constants.TransmissionStatus.IN_PROGRESS.numVal,
                    totalBytesToSend,
                    bytesReceived,
                    senderInfo,
                    fileInfos,
                    fileInfo.id,
                    fileInfo.fileSize,
                    0
                )
                var curFileBytesReceived: Long = 0
                val produceNewBufTemp =
                    { arrayOfNulls<ByteArray>(Short.MAX_VALUE - Short.MIN_VALUE + 1) }

                var bufTemp = produceNewBufTemp()

                var curGroup: Byte

                var startPacketId =
                    Constants.START_PACKET_ID // Expcected to be replaced when receiving START
                var endPacketId: Short // Expcected to be replaced when receiving END
                var fileEnded = false
                var groupEnded = false
                var waitingForResending = false
                var resendPacket: ResendRequestPacket?
                run {
                    val packet = handler.receiveIdentifier(
                        Constants.Identifier.START,
                        Constants.IDENTIFIER_TIMEOUT_MILLIS,
                        TimeUnit.MILLISECONDS
                    ) // (7)
                    curGroup =
                        ByteUtils.cutBytesByTip(Constants.START_ID_GROUP_ID_TIP, packet.extra)[0]
                    handler.sendAckableReply(packet) // (8)
                }
                var packet: AbstractCommandPacket<*>
                do {
                    packet = try {
                        handler.receivePacket(-1, null)
                    } catch (ignored: TimedOutException) {
                        continue
                    }

                    when (packet.command) {
                        UdpCommand.FILE_PACKET -> {
                            val filePacket = packet as FilePacket
                            if (filePacket.groupId != curGroup) {
                                Log.i(
                                    TAG,
                                    "Packet with wrong group, $curGroup expected, packet: $filePacket, ignored"
                                )
                                continue
                            }
                            if (bufTemp[filePacket.packetId - Short.MIN_VALUE] != null) {
                                Log.i(
                                    TAG, "Duplicate packet $filePacket, ignored"
                                )
                                continue
                            }
                            val data = filePacket.data.also {
                                bufTemp[filePacket.packetId - Short.MIN_VALUE] = it
                            }
                            curFileBytesReceived += data.size.toLong()
                            bytesReceived += data.size.toLong()
                            updateProgress(
                                org.exthmui.share.shared.misc.Constants.TransmissionStatus.IN_PROGRESS.numVal,
                                totalBytesToSend,
                                bytesReceived,
                                senderInfo,
                                fileInfos,
                                fileInfo.id,
                                fileInfo.fileSize,
                                curFileBytesReceived
                            )
                        }

                        UdpCommand.IDENTIFIER -> {
                            val identifierPacket = packet as IdentifierPacket
                            if (identifierPacket.identifier == null) continue
                            if (!identifierPacket.isAck()) handler.sendAckableReply(
                                identifierPacket
                            )
                            when (identifierPacket.identifier) {
                                Constants.Identifier.START -> {
                                    if (ByteUtils.cutBytesByTip(
                                            Constants.START_ID_GROUP_ID_TIP, identifierPacket.extra
                                        )[0] != curGroup
                                    ) continue
                                    waitingForResending = false
                                    startPacketId = ByteUtils.bytesToShort(
                                        ByteUtils.cutBytesByTip(
                                            Constants.START_ID_START_PACKET_ID_TIP,
                                            identifierPacket.extra
                                        )
                                    )
                                }

                                Constants.Identifier.END -> {
                                    if (waitingForResending && identifierPacket.extra[3].toInt() == 0) {
                                        Log.d(
                                            String.format(
                                                "$TAG/ConnectionHandler(%d)", connId
                                            ), "Waiting for resending, but received END, ignoring"
                                        )
                                        continue
                                    }
                                    Log.i(
                                        String.format(
                                            "$TAG/ConnectionHandler(%d)", connId
                                        ), String.format(
                                            "Group end received: %d, current group: %d",
                                            ByteUtils.cutBytesByTip(
                                                Constants.END_ID_GROUP_ID_TIP,
                                                identifierPacket.extra
                                            )[0],
                                            curGroup
                                        )
                                    )
                                    if (ByteUtils.cutBytesByTip(
                                            Constants.END_ID_GROUP_ID_TIP, identifierPacket.extra
                                        )[0] != curGroup
                                    ) continue
                                    endPacketId = ByteUtils.bytesToShort(
                                        ByteUtils.cutBytesByTip(
                                            Constants.END_ID_END_PACKET_ID_TIP,
                                            identifierPacket.extra
                                        )
                                    )
                                    val idsToResendAsSet: MutableSet<Short> = HashSet()
                                    var i = startPacketId.toInt()
                                    while (i <= endPacketId) {
                                        if (bufTemp[i - Short.MIN_VALUE] == null) idsToResendAsSet.add(
                                            i.toShort()
                                        )
                                        i++
                                    }
                                    val idsToResend =
                                        ArrayUtils.toPrimitive(idsToResendAsSet.toTypedArray<Short>())
                                    resendPacket = ResendRequestPacket()
                                    resendPacket.setPacketIds(idsToResend)
                                    handler.sendPacket(resendPacket) // (11) TODO: ack it
                                    if (idsToResendAsSet.isEmpty()) {
                                        val b = bufTemp
                                        withContext(Dispatchers.IO)
                                        {
                                            var i = startPacketId.toInt()
                                            while (i <= endPacketId) {
                                                val buf = b[i - Short.MIN_VALUE] ?: break
                                                outputStream.write(buf, 0, buf.size)
                                                i++
                                            }
                                            outputStream.flush()
                                        }
                                        curGroup++
                                        bufTemp = produceNewBufTemp()
                                    } else {
                                        Log.d(
                                            String.format(
                                                "$TAG/ConnectionHandler(%d)", connId
                                            ), String.format(
                                                "Resend required: %s", Arrays.toString(idsToResend)
                                            )
                                        )
                                        waitingForResending = true
                                    }
                                }

                                Constants.Identifier.GROUP_ID_RESET -> {
                                    if (!groupEnded) {
                                        Log.w(
                                            TAG,
                                            "Received GROUP_ID_RESET, but group $curGroup hasn't ended, ignoring"
                                        )
                                        continue
                                    }
                                    val fromGroup = ByteUtils.cutBytesByTip(
                                        Constants.GROUP_ID_RESET_ID_GROUP_ID_BEF_TIP,
                                        identifierPacket.extra
                                    )[0]
                                    if (fromGroup != curGroup
                                    ) {
                                        Log.w(
                                            TAG,
                                            "Received GROUP_ID_RESET, but formGroup=$fromGroup, curGroup=$curGroup"
                                        )
                                        continue
                                    }
                                    val toGroup = ByteUtils.cutBytesByTip(
                                        Constants.GROUP_ID_RESET_ID_GROUP_ID_AFT_TIP,
                                        identifierPacket.extra
                                    )[0]
                                    Log.d(
                                        TAG,
                                        "Received GROUP_ID_RESET, formGroup=$fromGroup, toGroup=$toGroup"
                                    )
                                    curGroup = toGroup
                                }

                                Constants.Identifier.FILE_END -> fileEnded = true
                                else -> {/* Ignored */
                                }
                            }
                        }

                        else -> {/* Ignored */
                        }
                    }
                    if (isCanceled()) return@coroutineScope if (isReceiverCanceled) SenderCancelledException(
                        context
                    ) else ReceiverCancelledException(context)
                } while (!fileEnded)
                outputStream.close()

                // Validate md5
                val md5Expected = fileInfo.getExtra(Constants.FILE_INFO_EXTRA_KEY_MD5)
                if (validateMd5 && md5Expected != null) {
                    val rawInputStream = inputStreamFactory.produce(fileInfo)
                    val inputStream =
                        if (rawInputStream is BufferedInputStream) rawInputStream else BufferedInputStream(
                            rawInputStream
                        )
                    val md5 = FileUtils.getMd5(inputStream)
                    inputStream.close()
                    if (!StringUtils.equals(md5Expected, md5)) {
                        Log.e(
                            String.format("$TAG/ConnectionHandler(%d)", connId), String.format(
                                "Md5 validation failed: %s(%s)", fileInfo.fileName, fileInfo.id
                            )
                        )
                        return@coroutineScope UnknownErrorException("File validation failed")
                    } else Log.d(
                        String.format("$TAG/ConnectionHandler(%d)", connId), String.format(
                            "Md5 validation passed: %s(%s)", fileInfo.fileName, fileInfo.id
                        )
                    )
                }
                //TODO: Watch file end packet
                SuccessTransmissionResult(context)
            } catch (e: FailedCreatingStreamException) {
                FileIOErrorException(context, e)
            } catch (e: TransmissionException) {
                e
            }
        }

        fun releaseResources() {
            commandWatcherStopFlag = true
            tcpUtil.releaseResources()
            isRunning = false
            CONN_ID_HANDLER_MAP.remove(connId)
        }

    }

    interface ConnectionListener {
        fun onConnectionEstablished(connId: Byte)
    }

    interface ReceivingListener {
        fun requestAcceptationAsync(
            senderInfo: SenderInfo,
            fileInfos: Array<FileInfo>,
            idsAccepted: CompletableFuture<Set<String>>?
        )

        fun onProgressUpdate(
            status: Int,
            totalBytesToSend: Long,
            bytesReceived: Long,
            senderInfo: SenderInfo,
            fileInfos: Array<FileInfo>,
            curFileId: String?,
            curFileBytesToSend: Long,
            curFileBytesReceived: Long
        )

        fun onComplete(result: TransmissionResult?, resultMap: Map<String, TransmissionResult>)
    }

    interface OutputStreamFactory {
        @Throws(FailedCreatingStreamException::class)
        fun produce(fileInfo: FileInfo): OutputStream
    }

    interface InputStreamFactory {
        @Throws(FailedCreatingStreamException::class)
        fun produce(fileInfo: FileInfo): InputStream
    }

    companion object {
        const val TAG = "UDPReceiver"
        private val GSON = Gson()
        private val CONN_ID_HANDLER_MAP: MutableMap<Byte, ConnectionHandler> = HashMap()
    }
}
