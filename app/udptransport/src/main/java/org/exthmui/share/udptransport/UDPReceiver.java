package org.exthmui.share.udptransport;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.google.gson.Gson;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.exthmui.share.shared.base.FileInfo;
import org.exthmui.share.shared.base.receive.SenderInfo;
import org.exthmui.share.shared.base.results.SuccessTransmissionResult;
import org.exthmui.share.shared.base.results.TransmissionResult;
import org.exthmui.share.shared.exceptions.trans.ReceiverCancelledException;
import org.exthmui.share.shared.exceptions.trans.RejectedException;
import org.exthmui.share.shared.exceptions.trans.SenderCancelledException;
import org.exthmui.share.shared.exceptions.trans.TimedOutException;
import org.exthmui.share.shared.exceptions.trans.TransmissionException;
import org.exthmui.share.shared.exceptions.trans.UnknownErrorException;
import org.exthmui.share.udptransport.packets.AbstractCommandPacket;
import org.exthmui.share.udptransport.packets.CommandPacket;
import org.exthmui.share.udptransport.packets.FilePacket;
import org.exthmui.share.udptransport.packets.IdentifierPacket;
import org.exthmui.share.udptransport.packets.ResendRequestPacket;
import org.exthmui.utils.FileUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class UDPReceiver {
    public static final String TAG = "UDPReceiver";
    private static final Gson GSON = new Gson();

    private final Context context;

    private static final Map<Byte, ConnectionHandler> CONN_ID_HANDLER_MAP = new HashMap<>();

    @NonNull
    private final OutputStreamFactory outputStreamFactory;
    @NonNull
    private final InputStreamFactory inputStreamFactory;
    @NonNull
    private final ConnectionListener listener;

    private int serverPortTcp;
    private int serverPortUdp;
    private final boolean lazyInit;
    private final boolean validateMd5;

    @Nullable
    private ServerSocket serverSocket;
    @Nullable
    private DatagramSocket datagramSocket;

    private boolean commandWatcherStopFlag;
    private boolean isRunning;

    private boolean tcpReady;
    private boolean udpReady;

    private final ThreadPoolExecutor coreThreadPool = new ThreadPoolExecutor(1,
            4, 5L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(2),
            r -> new Thread(r, r.toString()));
    private final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(0,
            3, 1L, TimeUnit.SECONDS, new SynchronousQueue<>(),
            r -> new Thread(r, r.toString()));
    private final Runnable connectionWatcher;
    private final Runnable packetInterceptor = () -> {
        while (udpReady) {
            try {
                CommandPacket packet = receivePacket(new CommandPacket(new DatagramPacket(new byte[Constants.BUF_LEN_MAX_HI], Constants.BUF_LEN_MAX_HI)));
                ConnectionHandler handler = CONN_ID_HANDLER_MAP.get(packet.getConnId());
                if (handler != null) {
                    if (!handler.packetReceived.isDone())
                        handler.packetReceived.complete(packet);
                    else handler.packetsBlocked.add(packet);
                } else Log.e(TAG, String.format("Packet with connection id %d received, but no " +
                        "correspond handler found", packet.getConnId()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    public UDPReceiver(@NonNull Context context, @NonNull OutputStreamFactory outputStreamFactory, @NonNull InputStreamFactory inputStreamFactory, @NonNull ConnectionListener listener, int serverPortTcp,
                       int serverPortUdp, boolean lazyInit, boolean validateMd5) throws IOException {
        this.context = context;
        this.outputStreamFactory = outputStreamFactory;
        this.inputStreamFactory = inputStreamFactory;
        this.listener = listener;
        this.serverPortTcp = serverPortTcp;
        this.serverPortUdp = serverPortUdp;
        this.lazyInit = lazyInit;
        this.validateMd5 = validateMd5;
        if (!lazyInit) initialize();

        connectionWatcher = () -> {
            while (isRunning) {
                try {
                    for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++)
                        if (i != 0 /* 0 is a invalid connection id*/ && !CONN_ID_HANDLER_MAP.containsKey((byte) i)) {
                            assert serverSocket != null;
                            ConnectionHandler handler = new ConnectionHandler((byte) i,
                                    serverSocket.accept());
                            CONN_ID_HANDLER_MAP.put((byte) i, handler);
                            handler.receiveAsync();
                            this.listener.onConnectionEstablished((byte) i);
                            break;
                        }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    public void initialize() throws IOException {
        if (!lazyInit && !tcpReady) tcpReady();
        if (!lazyInit && !udpReady) udpReady();
    }

    private void tcpReady() throws IOException {
        serverSocket = new ServerSocket(serverPortTcp);
        serverSocket.setReuseAddress(true);
        serverPortTcp = serverSocket.getLocalPort();
        tcpReady = true;
    }

    private void tcpStop() {
        if (serverSocket != null) {
            Utils.silentClose(serverSocket);
            serverSocket = null;
        }
        tcpReady = false;
    }

    private void udpReady() throws SocketException {
        datagramSocket = new DatagramSocket(serverPortUdp);
        datagramSocket.setReuseAddress(true);
        serverPortUdp = datagramSocket.getLocalPort();
        udpReady = true;
        threadPool.execute(packetInterceptor);
    }

    private void udpStop() {
        if (datagramSocket != null) {
            Utils.silentClose(datagramSocket);
            datagramSocket = null;
        }
        udpReady = false;
    }

    public void startReceive() throws IOException {
        isRunning = true;
        tcpReady();
        if (!lazyInit && !udpReady) udpReady();
        threadPool.execute(connectionWatcher);
    }

    public void stopReceive() {
        tcpStop();
        udpStop();
    }

    private <T extends AbstractCommandPacket<T>> T receivePacket(T packet) throws IOException {
        assert datagramSocket != null;
        DatagramPacket p = packet.toDatagramPacket();
        datagramSocket.receive(p);
        p.setData(Arrays.copyOfRange(p.getData(), p.getOffset(), p.getOffset() + p.getLength()));
        return packet;
    }

    private <T extends AbstractCommandPacket<T>> void sendPacket(T packet, byte connId) throws IOException {
        assert datagramSocket != null;
        packet.setConnId(connId);
        DatagramPacket p = packet.toDatagramPacket();
        ConnectionHandler handler = getHandler(connId);
        if (handler == null) {
            Log.e(TAG, String.format("Packet sending request with connection id %d received, but " +
                    "no correspond handler found, ignoring", packet.getConnId()));
            return;
        }
        p.setAddress(handler.address);
        p.setPort(handler.remoteUdpPort);
        datagramSocket.send(p);
    }

    @Nullable
    public ConnectionHandler getHandler(byte connId) {
        return CONN_ID_HANDLER_MAP.get(connId);
    }

    public class ConnectionHandler implements PacketListener {
        private final byte connId;
        private final InetAddress address;
        private final int remoteTcpPort;
        private int remoteUdpPort;

        private boolean canceled;
        private boolean remoteCanceled;

        long totalBytesToSend = 0;
        long bytesReceived = 0;
        SenderInfo senderInfo = null;
        FileInfo[] fileInfos = null;

        @NonNull
        private final Socket socket;
        @NonNull
        private final DataInputStream in;
        @NonNull
        private final DataOutputStream out;

        public final List<AbstractCommandPacket<?>> packetsBlocked = new ArrayList<>();
        @NonNull
        public CompletableFuture<AbstractCommandPacket<?>> packetReceived = new CompletableFuture<>();

        private final Runnable commandWatcher;
        @Nullable
        private ReceivingListener listener;

        public void cancel() {
            canceled = true;
        }

        public ConnectionHandler setListener(@Nullable ReceivingListener listener) {
            this.listener = listener;
            return this;
        }

        public byte getConnId() {
            return connId;
        }

        private ConnectionHandler(byte connId, @NonNull Socket socket)
                throws IOException {
            this.connId = connId;
            this.address = socket.getInetAddress();
            this.socket = socket;
            this.remoteTcpPort = socket.getPort();
            in = Utils.getDataInput(socket);
            out = Utils.getDataOutput(socket);
            commandWatcher = () -> {
                String cmd;
                while (!commandWatcherStopFlag) {
                    try {
                        cmd = in.readUTF();
                        dealWithCommand(cmd);
                    } catch (EOFException ignored) {
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
        }

        private void writeJson(Object object) throws IOException {
            String jsonStr = GSON.toJson(object);
            Log.d(TAG, "Trying to send \"" + jsonStr + "\" -> " + socket.getInetAddress());
            out.writeUTF(jsonStr);
        }

        private void writeCommand(String command) throws IOException {
            out.writeUTF(command);
        }

        private <T> T readJson(Class<T> classOfT) throws IOException {
            String s = in.readUTF();
            Log.d(TAG, "String \"" + s + "\" <- " + socket.getInetAddress());
            return GSON.fromJson(s, classOfT);
        }

        private <T> T readJson(Class<T> classOfT, Type typeOfT) throws IOException {
            String s = in.readUTF();
            Log.d(TAG, "String \"" + s + "\" <- " + socket.getInetAddress());
            return GSON.fromJson(s, typeOfT);
        }

        private void dealWithCommand(String cmd) {
            if (cmd.equals(Constants.COMMAND_CANCEL)) {
                remoteCanceled = true;
            }
        }

        public Future<Pair<TransmissionResult, Map<String, TransmissionResult>>> receiveAsync() {
            return coreThreadPool.submit(() -> {
                try {
                    return receive();
                } catch (Throwable e) {
                    String message = e.getMessage();
                    if (listener != null)
                        listener.onComplete(new UnknownErrorException(e.getMessage(), e.getLocalizedMessage(), e),
                                null);
                    e.printStackTrace();
//                    Log.e(String.format(TAG + "/ConnectionHandler(ConnId: %d)", getConnId()), e.getMessage());
//                    Log.e(String.format(TAG + "/ConnectionHandler(ConnId: %d)", getConnId()),
//                            StackTraceUtils.getStackTraceString(e.getStackTrace()));
                    return new Pair<>(new UnknownErrorException(e.getMessage(), e.getLocalizedMessage(), e), null);
                }
            });
        }

        public Pair<TransmissionResult, Map<String, TransmissionResult>> receive() throws IOException, ExecutionException, InterruptedException {
            Map<String, TransmissionResult> resultMap = new HashMap<>();
            SenderInfo senderInfo = readJson(SenderInfo.class);// (2)
            this.senderInfo = senderInfo;
            FileInfo[] fileInfos = readJson(FileInfo[].class);// (3)
            CompletableFuture<Set<String>> idsAccepted = new CompletableFuture<>();

            while (true) {
                if (listener != null) break;
            }// Stuck while worker is not started
            listener.requestAcceptationAsync(senderInfo, fileInfos, idsAccepted);
            listener.onProgressUpdate(
                    org.exthmui.share.shared.misc.Constants.TransmissionStatus.WAITING_FOR_ACCEPTATION.getNumVal(),
                    0, 0, senderInfo, fileInfos, null, 0, 0);
            Set<String> acceptedIdsAsSet = idsAccepted.get();
            String[] accepted = acceptedIdsAsSet.toArray(new String[0]);
            writeJson(accepted);// (4)
            if (accepted.length == 0) {
                Log.d(TAG, "No file were accepted");
                listener.onComplete(
                        new RejectedException(context),
                        null);
                return new Pair<>(new RejectedException(context),
                        null);
            }

            List<FileInfo> fileInfosToReceive = new ArrayList<>();
            for (FileInfo fileInfo : fileInfos) {
                if (acceptedIdsAsSet.contains(fileInfo.getId())) {
                    fileInfosToReceive.add(fileInfo);
                    totalBytesToSend += fileInfo.getFileSize();
                }
            }
            this.fileInfos = fileInfosToReceive.toArray(new FileInfo[0]);
            threadPool.execute(commandWatcher);// (4-6)

            if (!udpReady) udpReady();
            writeCommand(String.format(Locale.ROOT, "%s%d:%d", Constants.COMMAND_UDP_SOCKET_READY, serverPortUdp, getConnId()));// (6)

            for (FileInfo fileInfo : fileInfosToReceive) {
                resultMap.put(fileInfo.getId(), receiveFile(fileInfo));

                Pair<Boolean, Pair<TransmissionResult, Map<String, TransmissionResult>>> p =
                        checkCanceled(resultMap, accepted);
                if (p.first) return p.second;
            }
            commandWatcherStopFlag = true;
            // (16)

            for (TransmissionResult result : resultMap.values()) {
                if (result.getStatus() != org.exthmui.share.shared.misc.Constants.TransmissionStatus.COMPLETED)
                    return new Pair<>(new UnknownErrorException(context), resultMap);
            }
            releaseResources();
            listener.onComplete(new SuccessTransmissionResult(context),
                    resultMap);
            return new Pair<>(new SuccessTransmissionResult(context), resultMap);
        }

        private TransmissionResult receiveFile(FileInfo fileInfo) throws IOException {
            try {
                Log.d(TAG, String.format("Start receiving file: %s(%s)", fileInfo.getFileName(), fileInfo.getId()));
                OutputStream rawOutputStream = outputStreamFactory.produce(fileInfo);
                BufferedOutputStream outputStream = rawOutputStream instanceof BufferedOutputStream ?
                        (BufferedOutputStream) rawOutputStream : new BufferedOutputStream(rawOutputStream);
                assert listener != null;
                listener.onProgressUpdate(
                        org.exthmui.share.shared.misc.Constants.TransmissionStatus.IN_PROGRESS.getNumVal(),
                        totalBytesToSend, bytesReceived, senderInfo, fileInfos, fileInfo.getId(), fileInfo.getFileSize(), 0);

                long curFileBytesReceived = 0;

                assert datagramSocket != null;
                byte[][] bufTemp = new byte[Short.MAX_VALUE - Short.MIN_VALUE + 1][];
                Byte curGroup = null;
                short startPacketId = Short.MIN_VALUE;
                short endPacketId = Short.MAX_VALUE;
                boolean groupIdExceeded = false;
                boolean fileEnded = false;

                boolean waitingForResending = false;
                ResendRequestPacket resendPacket = null;

                try {
                    IdentifierPacket packet = receiveIdentifier(Constants.START_IDENTIFIER, -1, null);//Constants.IDENTIFIER_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);// (7)
                    curGroup = ByteUtils.cutBytesByTip(Constants.START_ID_GROUP_ID_TIP, packet.getExtra())[0];
                } catch (TimeoutException e) {
                    return new TimedOutException(context);
                }
                sendIdentifier(Constants.START_ACK_IDENTIFIER, null);// (8)

                AbstractCommandPacket<?> packet;
                do {
                    try {
                        if (waitingForResending && resendPacket != null)
                            sendPacket(resendPacket);// (11)
                        packet = receivePacket(-1, null);
                    } catch (TimeoutException ignored) {
                        continue;
                    }
                    if (packet == null) continue;
                    if (groupIdExceeded) {
                        if (packet.getCommand() != Constants.COMMAND_IDENTIFIER) continue;
                        IdentifierPacket identifierPacket =
                                IdentifierPacket.fromDatagramPacket(packet.toDatagramPacket());
                        if (identifierPacket.getIdentifier() != Constants.GROUP_ID_RESET_IDENTIFIER ||
                                ByteUtils.cutBytesByTip(Constants.GROUP_ID_RESET_ID_GROUP_ID_BEF_TIP, identifierPacket.getExtra())[0] != curGroup)
                            continue;
                        curGroup = ByteUtils.cutBytesByTip(Constants.GROUP_ID_RESET_ID_GROUP_ID_AFT_TIP, identifierPacket.getExtra())[0];
                        groupIdExceeded = false;
                        sendIdentifier(Constants.GROUP_ID_RESET_ACK_IDENTIFIER, null);
                    }
                    switch (packet.getCommand()) {
                        case Constants.COMMAND_FILE_PACKET:// (7), (13)
                            FilePacket filePacket = FilePacket.fromDatagramPacket(packet.toDatagramPacket());
                            if (filePacket.getGroupId() != curGroup) continue;
                            byte[] data = bufTemp[filePacket.getPacketId() - Short.MIN_VALUE] = filePacket.getData();
                            curFileBytesReceived += data.length;
                            bytesReceived += data.length;
                            listener.onProgressUpdate(
                                    org.exthmui.share.shared.misc.Constants.TransmissionStatus.IN_PROGRESS.getNumVal(),
                                    totalBytesToSend, bytesReceived, senderInfo, fileInfos, fileInfo.getId(), fileInfo.getFileSize(), curFileBytesReceived);
                            break;
                        case Constants.COMMAND_IDENTIFIER:
                            IdentifierPacket identifierPacket = IdentifierPacket.fromDatagramPacket(packet.toDatagramPacket());
                            Log.d(String.format(TAG + "/ConnectionHandler(ConnId: %d)", getConnId()),
                                    String.format("Identifier \"%d\" received", identifierPacket.getIdentifier()));

                            if (identifierPacket.getIdentifier() == Constants.START_IDENTIFIER) {// (7)
                                if (ByteUtils.cutBytesByTip(Constants.START_ID_GROUP_ID_TIP, identifierPacket.getExtra())[0] != curGroup)
                                    continue;
                                sendIdentifier(Constants.START_ACK_IDENTIFIER, null);// (8)
                                waitingForResending = false;
                                startPacketId = ByteUtils.bytesToShort(ByteUtils.cutBytesByTip(Constants.START_ID_START_PACKET_ID_TIP, identifierPacket.getExtra()));
                            } else if (identifierPacket.getIdentifier() == Constants.END_IDENTIFIER) {// (10)
                                if (ByteUtils.cutBytesByTip(Constants.END_ID_GROUP_ID_TIP, identifierPacket.getExtra())[0] != curGroup)
                                    continue;
                                sendIdentifier(Constants.END_ACK_IDENTIFIER, null);// (11)
                                endPacketId = ByteUtils.bytesToShort(ByteUtils.cutBytesByTip(Constants.END_ID_END_PACKET_ID_TIP, identifierPacket.getExtra()));

                                Set<Short> idsToResendAsSet = new HashSet<>();
                                for (int i = startPacketId; i <= endPacketId; i++) {
                                    if (bufTemp[i - Short.MIN_VALUE] == null)
                                        idsToResendAsSet.add((short) (i));
                                }
                                short[] idsToResend =
                                        ArrayUtils.toPrimitive(idsToResendAsSet.toArray(new Short[0]));
                                resendPacket = new ResendRequestPacket();
                                resendPacket.setPacketIds(idsToResend);
                                sendPacket(resendPacket);// (11)

                                if (idsToResendAsSet.isEmpty()) {
                                    for (int i = startPacketId; i <= endPacketId; i++) {
                                        byte[] buf = bufTemp[i - Short.MIN_VALUE];
                                        if (buf == null) break;
                                        outputStream.write(buf, 0, buf.length);
                                    }
                                    outputStream.flush();
                                    if (curGroup == Byte.MAX_VALUE) groupIdExceeded = true;
                                    else curGroup++;
                                } else waitingForResending = true;
                            } else if (identifierPacket.getIdentifier() == Constants.GROUP_ID_RESET_IDENTIFIER) {
                                if (ByteUtils.cutBytesByTip(Constants.GROUP_ID_RESET_ID_GROUP_ID_BEF_TIP, identifierPacket.getExtra())[0] != curGroup)
                                    continue;
                                curGroup = ByteUtils.cutBytesByTip(Constants.GROUP_ID_RESET_ID_GROUP_ID_AFT_TIP, identifierPacket.getExtra())[0];
                                sendIdentifier(Constants.GROUP_ID_RESET_ACK_IDENTIFIER, null);
                            } else if (identifierPacket.getIdentifier() == Constants.FILE_END_IDENTIFIER) {// (14)
                                fileEnded = true;
                                sendIdentifier(Constants.FILE_END_ACK_IDENTIFIER, null);// (15)
                            }
                            break;
                    }
                    if (isCanceled()) return isRemoteCanceled() ?
                            new SenderCancelledException(context) :
                            new ReceiverCancelledException(context);
                } while (!fileEnded);
                outputStream.close();

                // Validate md5
                String md5Expected = fileInfo.getExtra(Constants.FILE_INFO_EXTRA_KEY_MD5);
                if (validateMd5 && md5Expected != null) {
                    InputStream rawInputStream = inputStreamFactory.produce(fileInfo);
                    BufferedInputStream inputStream = rawInputStream instanceof BufferedInputStream ?
                            (BufferedInputStream) rawInputStream : new BufferedInputStream(rawInputStream);
                    String md5 = FileUtils.getMD5(inputStream);
                    inputStream.close();
                    if (!StringUtils.equals(md5Expected, md5)) {
                        Log.e(String.format(TAG + "/ConnectionHandler(ConnId: %d)", getConnId()),
                                String.format("Md5 validation failed: %s(%s)",
                                        fileInfo.getFileName(), fileInfo.getId()));
                        return new UnknownErrorException("File validation failed");
                    } else
                        Log.d(String.format(TAG + "/ConnectionHandler(ConnId: %d)", getConnId()),
                                String.format("Md5 validation passed: %s(%s)",
                                        fileInfo.getFileName(), fileInfo.getId()));
                }
                //TODO: Watch file end packet
                return new SuccessTransmissionResult(context);
            } catch (TransmissionException e) {
                return e;
            }
        }

        private <T extends AbstractCommandPacket<T>> T receivePacket(T packet, int timeout, TimeUnit unit) throws TimeoutException {
            assert datagramSocket != null;
            boolean tryAgain = false;
            do {
                try {
                    DatagramPacket datagramPacket = receivePacket(timeout, unit).toDatagramPacket();
                    packet.toDatagramPacket().setData(datagramPacket.getData(), datagramPacket.getOffset(), datagramPacket.getLength());
                } catch (IllegalArgumentException ignored) {
                    tryAgain = true;
                }
            } while (tryAgain);
            return packet;
        }

        private AbstractCommandPacket<?> receivePacket(int timeout, TimeUnit unit) throws TimeoutException {
            AbstractCommandPacket<?> packet = null;
            try {
                if (timeout < 0) {
                    packet = this.packetReceived.get();
                } else packet = this.packetReceived.get(timeout, unit);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                if (!this.packetReceived.isDone()) this.packetReceived.cancel(true);
                this.packetReceived = new CompletableFuture<>();
            }
            if (!packetsBlocked.isEmpty()) {
                this.packetReceived.complete(packetsBlocked.get(0));
                packetsBlocked.remove(0);
            }
            if (packet != null) this.remoteUdpPort = packet.getPort();
            return packet;
        }

        private <T extends AbstractCommandPacket<T>> void sendPacket(T packet) throws IOException {
            UDPReceiver.this.sendPacket(packet, connId);
        }

        /**
         * Send Identifier under udp socket
         *
         * @param identifier    Identifier
         * @param ackIdentifier Ack Identifier (Value {@link null} means not to wait for ack)
         * @return Ack packet
         */
        public IdentifierPacket sendIdentifier(byte identifier, Byte ackIdentifier) throws IOException, TimedOutException {
            return sendIdentifier(identifier, ackIdentifier, null);
        }

        public IdentifierPacket sendIdentifier(byte identifier, Byte ackIdentifier, byte[] extra) throws IOException, TimedOutException {
            assert datagramSocket != null;
            IdentifierPacket sendPacket = new IdentifierPacket().setIdentifier(identifier).setExtra(extra);
            if (ackIdentifier != null) {
                IdentifierPacket recvPacket = new IdentifierPacket();
                int tryouts = 0;
                boolean timedout;
                do {
                    try {
                        tryouts++;
                        timedout = false;
                        sendPacket(sendPacket);

                        if (isCanceled()) return null;

                        receivePacket(recvPacket, Constants.ACK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

                        Log.d(TAG,
                                String.format("Ack Identifier \"%d\" received", recvPacket.getIdentifier()));
                        if (recvPacket.getIdentifier() == ackIdentifier &&
                                recvPacket.getConnId() == connId) {
                            return recvPacket;
                        }
                    } catch (TimeoutException e) {
                        Log.w(TAG, String.format("Ack identifier packet receiving timed out: %s", e), e);
                        if (tryouts >= Constants.MAX_ACK_TRYOUTS)
                            throw new TimedOutException(context, e);
                        timedout = true;
                    }
                } while (timedout);
            } else {
                sendPacket(sendPacket);
            }
            return null;
        }

        private IdentifierPacket receiveIdentifier(byte identifier, int timeout, TimeUnit unit) throws TimeoutException {
            while (true) {
                IdentifierPacket recvPacket = IdentifierPacket.fromDatagramPacket(
                        receivePacket(timeout, unit).toDatagramPacket());
                if (recvPacket.getIdentifier() == identifier &&
                        recvPacket.getConnId() == connId) {
                    return recvPacket;
                }
            }
        }

        public void releaseResources() {
            commandWatcherStopFlag = true;
            Utils.silentClose(in);
            Utils.silentClose(out);
            Utils.silentClose(socket);
            isRunning = false;
            CONN_ID_HANDLER_MAP.remove(connId);
        }

        public boolean isCanceled() {
            return canceled || remoteCanceled;
        }

        public boolean isRemoteCanceled() {
            return remoteCanceled;
        }

        // TransmissionResults with id already existed in map will not be replaced.
        public Pair<Boolean, Pair<TransmissionResult, Map<String, TransmissionResult>>> checkCanceled(Map<String, TransmissionResult> resultMap, String[] idsCanceled) {
            if (canceled) {
                Log.d(TAG, "Sending canceled by sender, releasing resources and notifying listener");
                genCancelResultMap(resultMap, idsCanceled, false);
                if (listener != null) listener.onComplete(new SenderCancelledException(context),
                        resultMap);
                return new Pair<>(true, new Pair<>(new SenderCancelledException(context), resultMap));
            } else if (remoteCanceled) {
                Log.d(TAG, "Sending canceled by receiver, releasing resources and notifying listener");
                genCancelResultMap(resultMap, idsCanceled, true);
                if (listener != null) listener.onComplete(new ReceiverCancelledException(context),
                        resultMap);
                return new Pair<>(true, new Pair<>(new ReceiverCancelledException(context), resultMap));
            } else return new Pair<>(false, null);
        }

        private Map<String, TransmissionResult> genCancelResultMap(Map<String, TransmissionResult> resultMap, String[] idsCanceled, boolean remoteCanceled) {
            for (String id : idsCanceled)
                resultMap.put(id, remoteCanceled ? new SenderCancelledException(context) : new ReceiverCancelledException(context));
            return resultMap;
        }

        public SenderInfo getSenderInfo() {
            return senderInfo;
        }

        public FileInfo[] getFileInfos() {
            return fileInfos;
        }

        @Override
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public void onReceivePacket(AbstractCommandPacket<?> packet) {
            if (!this.packetReceived.isDone()) this.packetReceived.complete(packet);
            else packetsBlocked.add(packet);
        }
    }

    public int getServerPortTcp() {
        return serverPortTcp;
    }

    public interface ConnectionListener {
        void onConnectionEstablished(byte connId);
    }

    public interface ReceivingListener {
        void requestAcceptationAsync(SenderInfo senderInfo, FileInfo[] fileInfos, CompletableFuture<Set<String>> idsAccepted);

        void onProgressUpdate(int status, long totalBytesToSend, long bytesReceived,
                              @NonNull SenderInfo senderInfo, @NonNull FileInfo[] fileInfos,
                              String curFileId, long curFileBytesToSend, long curFileBytesReceived);

        void onComplete(TransmissionResult result, Map<String, TransmissionResult> resultMap);
    }

    public interface PacketListener {
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        void onReceivePacket(AbstractCommandPacket<?> packet);
    }

    public interface OutputStreamFactory {
        OutputStream produce(FileInfo fileInfo);
    }

    public interface InputStreamFactory {
        InputStream produce(FileInfo fileInfo);
    }
}
