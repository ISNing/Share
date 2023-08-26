package org.exthmui.share.udptransport

import android.util.Log
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableList
import androidx.databinding.ObservableList.OnListChangedCallback
import com.google.gson.Gson
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.lang.reflect.Type
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class TCPUtil(private val socket: Socket) {
    private val threadPool = ThreadPoolExecutor(
        0,
        2, 1L, TimeUnit.SECONDS, SynchronousQueue()
    ) { r: Runnable -> Thread(r, r.toString()) }
    private val ioScope: CoroutineScope by lazy { CoroutineScope(Dispatchers.IO) }

    val cmdBlocked: ObservableArrayList<String> = ObservableArrayList()
    val jsonBlocked: MutableList<String> = ArrayList()
    val bareBlocked: MutableList<String> = ArrayList()
    var tag = SUB_TAG
    private lateinit var inputStream: DataInputStream
    private lateinit var outputStream: DataOutputStream
    var cmdReceived = CompletableFuture<String>()
    var jsonReceived = CompletableFuture<String>()
    var bareReceived = CompletableFuture<String>()
    private val commandConsumerMap: MutableMap<Regex, MutableList<(String) -> Boolean>> = HashMap()
    private var stringWatcherStopFlag = true
    private val stringWatcher = Runnable {
        var str: String
        loop@ while (!stringWatcherStopFlag) {
            try {
                str = inputStream.readUTF()
                Log.d(tag, "UTF received \"$str\" <- ${socket.inetAddress}")
                if (str.startsWith(PREFIX_COMMAND)) {
                    val cmd = str.replaceFirst(PREFIX_COMMAND.toRegex(), "")
                    var consumed = false
                    for (re in commandConsumerMap.keys)
                        if (re.matches(cmd))
                            commandConsumerMap[re]?.let {
                                for (consumer in it)
                                    if (consumer(cmd)) consumed = true//TODO: continue@loop
                            }
                    if (consumed) continue@loop
                    if (!cmdReceived.isDone) cmdReceived.complete(cmd)
                    else cmdBlocked.add(cmd)
                }
                if (str.startsWith(PREFIX_JSON)) {
                    val json = str.replaceFirst(PREFIX_JSON.toRegex(), "")
                    if (!jsonReceived.isDone) jsonReceived.complete(json)
                    else jsonBlocked.add(json)
                }
                if (str.startsWith(PREFIX_BARE)) {
                    val bare = str.replaceFirst(PREFIX_BARE.toRegex(), "")
                    if (!bareReceived.isDone) bareReceived.complete(bare)
                    else bareBlocked.add(bare)
                }
            } catch (ignored: EOFException) {
            } catch (e: SocketException) {
                // Ignore socket closing
                if (e.message != "Socket closed") e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    constructor() : this(Socket()) {
        socket.tcpNoDelay = true
        socket.reuseAddress = true
    }

    @Throws(IOException::class)
    fun connect(tcpAddress: InetSocketAddress) {
        Log.d(tag, "Trying to connect to receiver under tcp socket: $tcpAddress")
        socket.connect(tcpAddress)
        initialize()
    }

    @Throws(IOException::class)
    fun initialize() {
        inputStream = Utils.getDataInput(socket)
        outputStream = Utils.getDataOutput(socket)
        stringWatcherStopFlag = false
        threadPool.execute(stringWatcher)
    }

    @Throws(IOException::class)
    fun writeCommand(command: String) {
        Log.d(tag, "Trying to send COMMAND \"" + command + "\" -> " + socket.inetAddress)
        outputStream.writeUTF(PREFIX_COMMAND + command)
    }

    @Throws(IOException::class)
    fun writeJson(`object`: Any?) {
        val jsonStr = GSON.toJson(`object`)
        Log.d(tag, "Trying to send JSON \"" + jsonStr + "\" -> " + socket.inetAddress)
        outputStream.writeUTF(PREFIX_JSON + jsonStr)
    }

    @Throws(IOException::class)
    fun writeBare(s: String) {
        Log.d(tag, "Trying to send BARE \"" + s + "\" -> " + socket.inetAddress)
        outputStream.writeUTF(PREFIX_BARE + s)
    }

    fun readCommand(consumed: Boolean = false): String {
        var str: String? = null
        var caughtException = false
        do {
            try {
                str = cmdReceived.get()
            } catch (e: ExecutionException) {
                e.printStackTrace()
                caughtException = true
            } catch (e: InterruptedException) {
                e.printStackTrace()
                caughtException = true
            }
            if (consumed || caughtException) {
                if (!cmdReceived.isDone) cmdReceived.cancel(true)
                cmdReceived = CompletableFuture()
                if (cmdBlocked.isNotEmpty()) {
                    cmdReceived.complete(cmdBlocked[0])
                    cmdBlocked.removeAt(0)
                }
            }
        } while (str == null)
        return str
    }

    suspend fun readCommand(consumed: Deferred<Boolean>): String {
        var str: String? = null
        var caughtException = false
        do {
            try {
                str = cmdReceived.get()
            } catch (e: ExecutionException) {
                e.printStackTrace()
                caughtException = true
            } catch (e: InterruptedException) {
                e.printStackTrace()
                caughtException = true
            }
            ioScope.launch {
                if (consumed.await() || caughtException) {
                    if (!cmdReceived.isDone) cmdReceived.cancel(true)
                    cmdReceived = CompletableFuture()
                    if (cmdBlocked.isNotEmpty()) {
                        cmdReceived.complete(cmdBlocked[0])
                        cmdBlocked.removeAt(0)
                    }
                }
            }
        } while (str == null)
        return str
    }

    fun readCommand(regex: Regex, consumed: Boolean = false): Deferred<String> =
        readCommand(regex, CompletableDeferred(consumed))

    fun readCommand(regex: Regex, consumed: Deferred<Boolean>): Deferred<String> =
        ioScope.async {
            val consumeIt: CompletableDeferred<Boolean> = CompletableDeferred()
            val cmd = readCommand(consumeIt)
            regex.matches(cmd).let { matches ->
                if (matches) {
                    ioScope.launch {
                        consumeIt.complete(consumed.await())
                    }
                    cmd
                } else {
                    consumeIt.complete(false)
                    cmdBlocked.firstOrNull { regex.matches(it) }.let {
                        if (it != null) it
                        else {
                            val cmdDeferred = CompletableDeferred<String>()
                            cmdBlocked.addOnListChangedCallback(object :
                                OnListChangedCallback<ObservableList<String>>() {
                                override fun onChanged(sender: ObservableList<String>) {}
                                override fun onItemRangeChanged(
                                    sender: ObservableList<String>,
                                    positionStart: Int,
                                    itemCount: Int
                                ) {
                                }

                                override fun onItemRangeInserted(
                                    sender: ObservableList<String>?,
                                    positionStart: Int,
                                    itemCount: Int
                                ) {
                                    val cb = this
                                    for (pos in positionStart..(positionStart + itemCount)) {
                                        sender?.get(pos)?.let {
                                            if (regex.matches(it)) {
                                                sender.removeOnListChangedCallback(cb)
                                                cmdDeferred.complete(it)
                                                ioScope.launch {
                                                    if (consumed.await()) sender.removeAt(pos)
                                                }
                                            }
                                        }
                                        if (cmdDeferred.isCompleted) break
                                    }
                                }

                                override fun onItemRangeMoved(
                                    sender: ObservableList<String>?,
                                    fromPosition: Int,
                                    toPosition: Int,
                                    itemCount: Int
                                ) {
                                }

                                override fun onItemRangeRemoved(
                                    sender: ObservableList<String>?,
                                    positionStart: Int,
                                    itemCount: Int
                                ) {
                                }
                            })
                            cmdDeferred.await()
                        }
                    }
                }
            }
        }

    fun <T> readJson(classOfT: Class<T>): T {
        var json: String? = null
        do {
            try {
                json = jsonReceived.get()
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } finally {
                if (!jsonReceived.isDone) jsonReceived.cancel(true)
                jsonReceived = CompletableFuture()
            }
            if (!jsonBlocked.isEmpty()) {
                jsonReceived.complete(jsonBlocked[0])
                jsonBlocked.removeAt(0)
            }
        } while (json == null)
        return GSON.fromJson(json, classOfT)
    }

    @Throws(IOException::class)
    fun <T> readJson(typeOfT: Type): T {
        var json: String? = null
        do {
            try {
                json = jsonReceived.get()
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } finally {
                if (!jsonReceived.isDone) jsonReceived.cancel(true)
                jsonReceived = CompletableFuture()
            }
            if (!jsonBlocked.isEmpty()) {
                jsonReceived.complete(jsonBlocked[0])
                jsonBlocked.removeAt(0)
            }
        } while (json == null)
        return GSON.fromJson(json, typeOfT)
    }

    fun readBare(): String {
        var str: String? = null
        do {
            try {
                str = bareReceived.get()
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } finally {
                if (!bareReceived.isDone) bareReceived.cancel(true)
                bareReceived = CompletableFuture()
            }
            if (bareBlocked.isNotEmpty()) {
                bareReceived.complete(bareBlocked[0])
                bareBlocked.removeAt(0)
            }
        } while (str == null)
        return str
    }

    fun releaseResources() {
        stringWatcherStopFlag = true
        Utils.silentClose(inputStream)
        Utils.silentClose(outputStream)
        Utils.silentClose(socket)
    }

    val inetAddress: InetAddress
        get() = socket.inetAddress

    fun registerCommandConsumer(re: Regex, consumer: (String) -> Boolean) {
        if (commandConsumerMap[re] == null)
            commandConsumerMap[re] = ArrayList()
        commandConsumerMap[re]?.add(consumer)
    }

    fun unregisterCommandConsumer(re: Regex, consumer: (String) -> Boolean) =
        commandConsumerMap[re]?.apply {
            if (size == 1) commandConsumerMap.remove(re)
            else remove(consumer)
        }

    fun unregisterCommandConsumer(consumer: (String) -> Boolean) {
        for (entry in commandConsumerMap)
            for (cons in entry.value)
                if (cons == consumer)
                    if (entry.value.size == 1) commandConsumerMap.remove(entry.key, entry.value)
                    else entry.value.remove(consumer)
    }

    fun setTAG(tag: String?) {
        this.tag = String.format("%s/%s", tag, SUB_TAG)
    }

    interface CommandListener {
        fun onReceiveCommand(cmd: String)
    }

    companion object {
        const val SUB_TAG = "TCPUtil"
        private val GSON = Gson()
        const val PREFIX_COMMAND = "CMD_"
        const val PREFIX_JSON = "JSON_"
        const val PREFIX_BARE = "BARE_"
    }
}
