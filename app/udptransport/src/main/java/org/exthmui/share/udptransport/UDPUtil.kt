package org.exthmui.share.udptransport

import android.content.Context
import android.util.Log
import org.exthmui.share.shared.exceptions.trans.TimedOutException
import org.exthmui.share.udptransport.packets.AbstractCommandPacket
import org.exthmui.share.udptransport.packets.AckablePacket
import org.exthmui.share.udptransport.packets.CommandPacket
import org.exthmui.share.udptransport.packets.IdentifierPacket
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketException
import java.util.Arrays
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.function.Consumer

class UDPUtil(private val datagramSocket: DatagramSocket?) {
    private val CONN_ID_HANDLER_MAP: MutableMap<Byte, Handler> = HashMap()
    private val threadPool = ThreadPoolExecutor(
        0,
        2, 1L, TimeUnit.SECONDS, SynchronousQueue()
    ) { r: Runnable -> Thread(r, r.toString()) }
    var tag = SUB_TAG
    private lateinit var stateChecker: () -> Boolean
    private var stopInterceptorFlag = false
    private val packetInterceptor = Runnable {
        while (!stopInterceptorFlag) {
            try {
                val packet = receivePacket()
                val commandPacket = CommandPacket(packet)
                val handler = CONN_ID_HANDLER_MAP[commandPacket.connId]
                if (handler != null) {
                    if (!handler.packetReceived.isDone) handler.packetReceived.complete(packet) else {
                        if (handler.packetsBlocked.size == Constants.MAX_UDP_PACKETS_RETENTION) {
                            handler.packetReceived = CompletableFuture()
                            handler.packetReceived.complete(handler.packetsBlocked[0])
                            handler.packetsBlocked.removeAt(0)
                        } else handler.packetsBlocked.add(packet)
                    }
                } else Log.e(
                    tag, String.format(
                        "Packet with connection id %d received, but no " +
                                "correspond handler found", commandPacket.connId
                    )
                )
            } catch (e: SocketException) {
                // Ignore socket closing
                if (e.message != "Socket closed") e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    constructor() : this(DatagramSocket()) {
        datagramSocket!!.reuseAddress = true
    }

    constructor(port: Int) : this(DatagramSocket(port)) {
        datagramSocket!!.reuseAddress = true
    }

    fun startListening() {
        stopInterceptorFlag = false
        threadPool.execute(packetInterceptor)
    }

    fun stopListening() {
        stopInterceptorFlag = true
    }

    @Throws(IOException::class)
    fun connect(udpAddress: InetSocketAddress?) {
        assert(datagramSocket != null)
        datagramSocket!!.connect(udpAddress)
    }

    fun addHandler(handler: Handler): Handler {
        CONN_ID_HANDLER_MAP[handler.connId] = handler
        return handler
    }

    fun addHandler(context: Context, connId: Byte): Handler {
        val handler: Handler = Handler(context, connId)
        addHandler(handler)
        return handler
    }

    fun removeHandler(handler: Handler) {
        CONN_ID_HANDLER_MAP.remove(handler.connId, handler)
    }

    fun removeHandler(connId: Byte) {
        CONN_ID_HANDLER_MAP.remove(connId)
    }

    fun getHandler(connId: Byte): Handler? {
        return CONN_ID_HANDLER_MAP[connId]
    }

    fun releaseResources() {
        stopListening()
        Utils.silentClose(datagramSocket)
        threadPool.shutdown()
    }

    @Throws(IOException::class)
    private fun receivePacket(): DatagramPacket {
        assert(datagramSocket != null)
        val p = DatagramPacket(ByteArray(Constants.BUF_LEN_MAX_HI), Constants.BUF_LEN_MAX_HI)
        datagramSocket!!.receive(p)
        p.data = Arrays.copyOfRange(p.data, p.offset, p.offset + p.length)
        return p
    }

    @Throws(IOException::class)
    private fun <T : AbstractCommandPacket<T>?> receivePacket(factory: PacketFactory<T>): T {
        assert(datagramSocket != null)
        val p = DatagramPacket(ByteArray(Constants.BUF_LEN_MAX_HI), Constants.BUF_LEN_MAX_HI)
        datagramSocket!!.receive(p)
        p.data = Arrays.copyOfRange(p.data, p.offset, p.offset + p.length)
        return factory.produce(p)
    }

    private fun checkCanceled(): Boolean {
        return stateChecker()
    }

    val localPort: Int
        get() = datagramSocket!!.localPort

    fun setStateChecker(stateChecker: () -> Boolean) {
        this.stateChecker = stateChecker
    }

    fun setTAG(tag: String?) {
        this.tag = String.format("%s/%s", tag, SUB_TAG)
    }

    inner class Handler(private val context: Context, val connId: Byte) {
        val packetsBlocked: MutableList<DatagramPacket> =
            ArrayList(Constants.MAX_UDP_PACKETS_RETENTION)
        var packetReceived = CompletableFuture<DatagramPacket>()

        @Throws(TimeoutException::class)
        private fun receiveBarePacket(timeout: Int, unit: TimeUnit?): DatagramPacket {
            var packet: DatagramPacket? = null
            do {
                try {
                    packet = if (timeout < 0) {
                        packetReceived.get()
                    } else packetReceived[timeout.toLong(), unit]
                } catch (e: ExecutionException) {
                    e.printStackTrace()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                } finally {
                    if (!packetReceived.isDone) packetReceived.cancel(true)
                    packetReceived = CompletableFuture()
                }
                if (!packetsBlocked.isEmpty()) {
                    packetReceived.complete(packetsBlocked[0])
                    packetsBlocked.removeAt(0)
                }
            } while (packet == null)
            return packet
        }

        @Throws(TimedOutException::class)
        fun <T : AbstractCommandPacket<*>?> receivePacket(
            factory: PacketFactory<T>,
            timeout: Int,
            unit: TimeUnit?,
            maxTryouts: Int
        ): T {
            var tryouts = 0
            while (true) {
                tryouts++
                try {
                    return receivePacket(factory, timeout, unit)
                } catch (e: TimedOutException) {
                    if (tryouts >= maxTryouts) throw TimedOutException(context, e)
                }
            }
        }

        @Throws(TimedOutException::class)
        fun receivePacket(timeout: Int, unit: TimeUnit?): AbstractCommandPacket<*> {
            return receivePacket({ datagramPacket: DatagramPacket ->
                Constants.UdpCommand.parse(
                    datagramPacket
                )
            }, timeout, unit)
        }

        @Throws(TimedOutException::class)
        fun <T : AbstractCommandPacket<*>?> receivePacket(
            factory: PacketFactory<T>,
            timeout: Int,
            unit: TimeUnit?
        ): T {
            assert(datagramSocket != null)
            var targetPacket: T? = null
            val packetsRejected: MutableList<DatagramPacket> = ArrayList()
            var datagramPacket: DatagramPacket
            do {
                datagramPacket = try {
                    receiveBarePacket(timeout, unit)
                } catch (e: TimeoutException) {
                    throw TimedOutException(context, e)
                }
                try {
                    targetPacket = factory.produce(datagramPacket)
                } catch (ignored: IllegalArgumentException) {
                    Log.w(
                        tag,
                        String.format("Packet rejected: %s", Arrays.toString(datagramPacket.data))
                    )
                    packetsRejected.add(datagramPacket)
                }
            } while (targetPacket == null)
            if (packetsRejected.isNotEmpty()) reinsertPackets(packetsRejected)
            Log.d(
                tag,
                String.format("Packet received: %s <- %s", targetPacket, targetPacket.socketAddress)
            )
            return targetPacket
        }

        private fun reinsertPackets(packets: MutableList<DatagramPacket>) {
            if (packetsBlocked.isNotEmpty()) {
                packetReceived.complete(packets[0])
                packets.removeAt(0)
            }
            if (!packets.isEmpty()) {
                packetsBlocked.addAll(0, packets)
            }
        }

        val isPacketsClear: Boolean
            get() = packetsBlocked.isEmpty() && !packetReceived.isDone

        @Throws(IOException::class)
        fun <T : AbstractCommandPacket<*>?> sendPacket(packet: T) {
            assert(datagramSocket != null)
            packet!!.setConnId(connId)
            val p = packet.toDatagramPacket()
            Log.d(
                tag,
                String.format(
                    "Sending packet: %s -> %s",
                    packet,
                    datagramSocket!!.remoteSocketAddress
                )
            )
            datagramSocket.send(p)
        }

        @Throws(IOException::class, TimedOutException::class)
        fun sendAckableReply(packet: AckablePacket<*>) {
            sendPacket(packet.clone().reverseAck())
        }

        @Throws(IOException::class, TimedOutException::class)
        fun sendIdentifier(
            identifier: Constants.Identifier,
            extra: ByteArray? = null
        ): IdentifierPacket? {
            return sendIdentifier(
                identifier,
                if (identifier.isAck) null else identifier.correspondIdentifier,
                extra
            )
        }

        /**
         * Send Identifier under udp socket
         *
         * @param identifier    Identifier
         * @param ackIdentifier Ack Identifier (Value [null] means not to wait for ack)
         * @return Ack packet
         */
        @Throws(IOException::class, TimedOutException::class)
        fun sendIdentifier(
            identifier: Constants.Identifier,
            ackIdentifier: Constants.Identifier?,
            extra: ByteArray? = null
        ): IdentifierPacket? {
            assert(datagramSocket != null)
            val sendPacket = IdentifierPacket().setIdentifier(identifier).setExtra(extra)
            if (ackIdentifier != null) {
                var tryouts = 0
                var recvPacket: IdentifierPacket? = null
                while (recvPacket == null) {
                    try {
                        tryouts++
                        sendPacket(sendPacket)
                        if (checkCanceled()) return null
                        do {
                            recvPacket = receiveIdentifier(
                                ackIdentifier,
                                Constants.ACK_TIMEOUT_MILLIS,
                                TimeUnit.MILLISECONDS
                            )
                        } while (!Arrays.equals(
                                sendPacket.extra,
                                recvPacket!!.extra
                            )
                        ) // Different extra, rejected and dropped
                    } catch (e: TimedOutException) {
                        if (tryouts >= Constants.MAX_ACK_TRYOUTS) {
                            Log.w(
                                tag,
                                String.format(
                                    "Ack identifier packet receiving timed out: %s, reached maximum retries",
                                    e
                                ),
                                e
                            )
                            throw TimedOutException(context, e)
                        } else Log.w(
                            tag,
                            String.format(
                                "Ack identifier packet receiving timed out: %s, retrying: %d",
                                e,
                                tryouts + 1
                            ),
                            e
                        )
                    }
                }
                return recvPacket
            } else {
                sendPacket(sendPacket)
            }
            return null
        }

        @Throws(TimedOutException::class)
        fun receiveIdentifier(
            identifier: Constants.Identifier,
            timeout: Int,
            unit: TimeUnit?,
            customCheck: Consumer<IdentifierPacket?>? = null
        ): IdentifierPacket {
            return receivePacket({ datagramPacket: DatagramPacket? ->
                val p = IdentifierPacket(datagramPacket)
                if (p.identifier != identifier) {
                    Log.w(tag, String.format("Identifier received: %s, but illegal, rejected", p))
                    throw IllegalArgumentException()
                }
                customCheck?.accept(p)
                p
            }, timeout, unit)
        }
    }

    fun interface PacketFactory<T : AbstractCommandPacket<*>?> {
        fun produce(datagramPacket: DatagramPacket): T
    }

    companion object {
        const val SUB_TAG = "UDPUtil"
    }
}
