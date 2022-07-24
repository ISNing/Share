package org.exthmui.share.udptransport;

import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;

import org.apache.commons.lang3.ArrayUtils;
import org.exthmui.share.shared.base.FileInfo;
import org.exthmui.share.shared.base.receive.SenderInfo;
import org.exthmui.share.udptransport.packets.AbstractCommandPacket;
import org.exthmui.share.udptransport.packets.CommandPacket;
import org.exthmui.share.udptransport.packets.FilePacket;
import org.exthmui.share.udptransport.packets.IdentifierPacket;
import org.exthmui.share.udptransport.packets.ResendRequestPacket;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Receiver {
    public static final String TAG = "Receiver";
    private static final Gson GSON = new Gson();

    private static final Map<Byte, ConnectionHandler> CONN_ID_HANDLER_MAP = new HashMap<>();

    @NonNull
    private final OutputStreamFactory outputStreamFactory;
    @NonNull
    private final ConnectionListener listener;

    private int serverPortTcp;
    private int serverPortUdp;
    private final boolean lazyInit;

    @Nullable
    private ServerSocket serverSocket;
    @Nullable
    private DatagramSocket datagramSocket;

    private boolean commandWatcherStopFlag;
    private boolean isRunning;

    private final ThreadPoolExecutor coreThreadPool = new ThreadPoolExecutor(1,
            4, 5L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(2),
            r -> new Thread(r, r.toString()));
    private final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(0,
            1, 1L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(8),
            r -> new Thread(r, r.toString()));
    private final Runnable connectionWatcher;
    private final Runnable packetInterceptor = () -> {
        while (isRunning) {
            try {
                CommandPacket packet = receivePacket(new CommandPacket());
                ConnectionHandler handler = CONN_ID_HANDLER_MAP.get(packet.getConnId());
                if (handler != null) {
                    if (!handler.packetReceived.isDone())
                        handler.packetReceived.complete(packet);
                    else handler.packetsBlocked.add(packet);
                } else Log.e(TAG, "Packet with connection id %d received, but no correspond " +
                        "handler found");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    private boolean tcpReady;
    private boolean udpReady;

    public Receiver(@NonNull OutputStreamFactory outputStreamFactory, @NonNull ConnectionListener listener, int serverPortTcp,
                    int serverPortUdp, boolean lazyInit) throws IOException {
        this.outputStreamFactory = outputStreamFactory;
        this.listener = listener;
        this.serverPortTcp = serverPortTcp;
        this.serverPortUdp = serverPortUdp;
        this.lazyInit = lazyInit;
        if (!lazyInit) initialize();

        connectionWatcher = () -> {
            while (isRunning) {
                try {
                    for (int i = Byte.MIN_VALUE + 1; i <= Byte.MAX_VALUE; i++)
                        if (!CONN_ID_HANDLER_MAP.containsKey((byte) i)) {
                            assert serverSocket != null;
                            ConnectionHandler handler = new ConnectionHandler((byte) i,
                                    serverSocket.accept());
                            CONN_ID_HANDLER_MAP.put((byte) i, handler);
                            listener.onConnectionEstablished((byte) i);
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

    public void tcpReady() throws IOException {
        serverSocket = new ServerSocket(serverPortTcp);
        serverSocket.setReuseAddress(true);
        serverPortTcp = serverSocket.getLocalPort();
        tcpReady = true;
    }

    public void udpReady() throws SocketException {
        datagramSocket = new DatagramSocket(serverPortUdp);
        datagramSocket.setReuseAddress(true);
        serverPortUdp = datagramSocket.getLocalPort();
        threadPool.execute(packetInterceptor);
        udpReady = true;
    }

    public void startReceive() throws IOException {
        tcpReady();
        if (!lazyInit && !udpReady) udpReady();
    }

    public <T extends AbstractCommandPacket<T>> T receivePacket(T packet) throws IOException {
        assert datagramSocket != null;
        datagramSocket.receive(packet.toDatagramPacket());
        return packet;
    }

    public <T extends AbstractCommandPacket<T>> void sendPacket(T packet, byte connId) throws IOException {
        assert datagramSocket != null;
        packet.setConnId(connId);
        datagramSocket.send(packet.toDatagramPacket());
    }

    @Nullable
    public ConnectionHandler getHandler(byte connId) {
        return CONN_ID_HANDLER_MAP.get(connId);
    }

    public class ConnectionHandler implements PacketListener {
        private final byte connId;

        private boolean canceled;
        private boolean remoteCanceled;

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

        public ConnectionHandler(byte connId, @NonNull Socket socket)
                throws IOException {
            this.socket = socket;
            this.connId = connId;
            in = Utils.getDataInput(socket);
            out = Utils.getDataOutput(socket);
            commandWatcher = () -> {
                String cmd;
                while (!commandWatcherStopFlag) {
                    try {
                        cmd = in.readUTF();
                        dealWithCommand(cmd);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
        }

        public void writeJson(Object object) throws IOException {
            String jsonStr = GSON.toJson(object);
            Log.d(TAG, "Trying to send \"" + jsonStr + "\" -> " + socket.getInetAddress());
            out.writeUTF(jsonStr);
        }

        public void writeCommand(String command) throws IOException {
            out.writeUTF(command);
        }

        public <T> T readJson(Class<T> classOfT) throws IOException {
            String s = in.readUTF();
            Log.d(TAG, "String \"" + s + "\" <- " + socket.getInetAddress());
            return GSON.fromJson(s, classOfT);
        }

        public <T> T readJson(Class<T> classOfT, Type typeOfT) throws IOException {
            String s = in.readUTF();
            Log.d(TAG, "String \"" + s + "\" <- " + socket.getInetAddress());
            return GSON.fromJson(s, typeOfT);
        }

        public void dealWithCommand(String cmd) {
            switch (cmd) {
                case (Constants.COMMAND_CANCEL):
                    remoteCanceled = true;
                    break;
                default:
                    break;
            }
        }

        public Future<Pair<Integer, Map<String, Pair<Integer, String>>>> receiveAsync() {
            return coreThreadPool.submit(() -> {
                try {
                    return receive();
                } catch (Throwable e) {
                    if (listener != null) listener.onComplete(
                            org.exthmui.share.shared.misc.Constants.TransmissionStatus.REJECTED.getNumVal(),
                            null);
                    throw e;
                }
            });
        }

        public Pair<Integer, Map<String, Pair<Integer, String>>> receive() throws IOException, ExecutionException, InterruptedException {
            Map<String, Pair<Integer, String>> resultMap = new HashMap<>();
            SenderInfo senderInfo = readJson(SenderInfo.class);// (2)
            FileInfo[] fileInfos = readJson(FileInfo[].class);// (3)
            CompletableFuture<Set<String>> idsAccepted = new CompletableFuture<>();

            while (listener == null) {}
            listener.requestAcceptation(senderInfo, fileInfos, idsAccepted);
            Set<String> acceptedIdsAsSet = idsAccepted.get();
            String[] accepted = acceptedIdsAsSet.toArray(new String[0]);
            writeJson(accepted);// (4)
            if (accepted.length == 0) {
                Log.d(TAG, "No file were accepted");
                listener.onComplete(
                        org.exthmui.share.shared.misc.Constants.TransmissionStatus.REJECTED.getNumVal(),
                        null);
                return new Pair<>(org.exthmui.share.shared.misc.Constants.TransmissionStatus.REJECTED.getNumVal(),
                        null);
            }

            List<FileInfo> fileInfosToReceive = new ArrayList<>();
            for (FileInfo fileInfo : fileInfos) {
                if (acceptedIdsAsSet.contains(fileInfo.getId()))
                    fileInfosToReceive.add(fileInfo);
            }
            threadPool.execute(commandWatcher);// (4-6)

            if (!udpReady) udpReady();
            writeCommand(Constants.COMMAND_UDP_SOCKET_READY);// (6)

            for (FileInfo fileInfo : fileInfosToReceive) {
                Log.d(TAG, "Start receiving file: " + fileInfo.getFileName());
                OutputStream stream = outputStreamFactory.produce(fileInfo);
                resultMap.put(fileInfo.getId(), receiveFile(stream instanceof BufferedOutputStream ?
                        (BufferedOutputStream) stream : new BufferedOutputStream(stream)));

                Pair<Boolean, Pair<Integer, Map<String, Pair<Integer, String>>>> p = checkCanceled(resultMap, accepted);
                if (p.first) return p.second;
            }
            commandWatcherStopFlag = true;
            // (16)

            releaseResources();
            listener.onComplete(
                    org.exthmui.share.shared.misc.Constants.TransmissionStatus.COMPLETED.getNumVal(),
                    resultMap);
            return new Pair<>(org.exthmui.share.shared.misc.Constants
                    .TransmissionStatus.COMPLETED.getNumVal(), resultMap);
        }

        public Pair<Integer, String> receiveFile(BufferedOutputStream outputStream) throws IOException {
            assert datagramSocket != null;
            byte[][] bufTemp = new byte[Short.MAX_VALUE - Short.MIN_VALUE + 1][];
            byte curGroup = Byte.MIN_VALUE;
            boolean groupIdExceeded = false;
            boolean fileEnded = false;
            try {
                receiveIdentifier(Constants.START_IDENTIFIER, Constants.IDENTIFIER_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);// (7)
            } catch (TimeoutException e) {
                return new Pair<>(org.exthmui.share.shared.misc.Constants.TransmissionStatus.TIMED_OUT.getNumVal(), null);
            }
            sendIdentifier(Constants.START_ACK_IDENTIFIER, null);// (8)
            AbstractCommandPacket<?> packet;
            do {
                try {
                    packet = receivePacket(-1, null);
                } catch (TimeoutException ignored) {
                    continue;
                }
                if (groupIdExceeded) {
                    if (packet.getCommand() != Constants.COMMAND_IDENTIFIER) continue;
                    IdentifierPacket identifierPacket =
                            IdentifierPacket.fromDatagramPacket(packet.toDatagramPacket());
                    if (identifierPacket.getIdentifier() != Constants.GROUP_ID_RESET_IDENTIFIER ||
                            identifierPacket.getExtra()[0] != curGroup) continue;
                    curGroup = Byte.MIN_VALUE;
                    groupIdExceeded = false;
                    sendIdentifier(Constants.GROUP_ID_RESET_ACK_IDENTIFIER, null);
                }
                switch (packet.getCommand()) {
                    case Constants.COMMAND_FILE_PACKET:// (7), (13)
                        FilePacket filePacket = FilePacket.fromDatagramPacket(packet.toDatagramPacket());
                        if (filePacket.getGroupId() != curGroup) continue;
                        bufTemp[filePacket.getPacketId()] = filePacket.getData();
                        break;
                    case Constants.COMMAND_IDENTIFIER:
                        IdentifierPacket identifierPacket = IdentifierPacket.fromDatagramPacket(packet.toDatagramPacket());
                        if (identifierPacket.getIdentifier() == Constants.START_IDENTIFIER) {// (7)
                            if (identifierPacket.getExtra()[0] != curGroup) continue;
                            sendIdentifier(Constants.START_ACK_IDENTIFIER, null);// (8)
                        } else if (identifierPacket.getIdentifier() == Constants.END_IDENTIFIER) {// (10)
                            if (identifierPacket.getExtra()[0] != curGroup) continue;
                            sendIdentifier(Constants.END_ACK_IDENTIFIER, null);// (11)

                            Set<Short> idsToResendAsSet = new HashSet<>();
                            for (int i = 0; i < bufTemp.length; i++) {
                                idsToResendAsSet.add((short) (i + Short.MIN_VALUE));
                            }
                            short[] idsToResend =
                                    ArrayUtils.toPrimitive(idsToResendAsSet.toArray(new Short[0]));
                            sendPacket(new ResendRequestPacket().setPacketIds(idsToResend));// (11)

                            if (idsToResendAsSet.isEmpty()) {
                                for (byte[] buf : bufTemp) {
                                    outputStream.write(buf);
                                }
                                if (curGroup == Byte.MAX_VALUE) groupIdExceeded = true;
                                else curGroup++;
                            }
                        } else if (identifierPacket.getIdentifier() == Constants.GROUP_ID_RESET_IDENTIFIER) {
                            if (identifierPacket.getExtra()[0] != curGroup) continue;
                            curGroup = Byte.MIN_VALUE;
                            sendIdentifier(Constants.GROUP_ID_RESET_ACK_IDENTIFIER, null);
                        } else if (identifierPacket.getIdentifier() == Constants.FILE_END_IDENTIFIER) {// (14)
                            fileEnded = true;
                            sendIdentifier(Constants.FILE_END_ACK_IDENTIFIER, null);// (15)
                        }
                        break;
                }
                if (isCanceled()) return new Pair<>((isRemoteCanceled() ?
                        org.exthmui.share.shared.misc.Constants.TransmissionStatus.SENDER_CANCELLED :
                        org.exthmui.share.shared.misc.Constants.TransmissionStatus.RECEIVER_CANCELLED
                ).getNumVal(), null);
            } while (fileEnded);
            outputStream.flush();
            outputStream.close();
            //TODO: Watch file end packet
            return new Pair<>(
                    org.exthmui.share.shared.misc.Constants.TransmissionStatus.COMPLETED.getNumVal(),
                    null);
        }

        public AbstractCommandPacket<?> receivePacket(int timeout, TimeUnit unit) throws TimeoutException {
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
            return packet;
        }

        public <T extends AbstractCommandPacket<T>> void sendPacket(T packet) throws IOException {
            assert datagramSocket != null;
            packet.setConnId(connId);
            datagramSocket.send(packet.toDatagramPacket());
        }

        /**
         * Send Identifier under udp socket
         *
         * @param identifier    Identifier
         * @param ackIdentifier Ack Identifier (Value {@link null} means not to wait for ack)
         * @return Ack packet
         */
        public IdentifierPacket sendIdentifier(byte identifier, Byte ackIdentifier) throws IOException {
            assert datagramSocket != null;
            IdentifierPacket sendPacket = new IdentifierPacket().setIdentifier(identifier);
            if (ackIdentifier != null) {
                IdentifierPacket recvPacket;
                int initSoTimeout = datagramSocket.getSoTimeout();
                datagramSocket.setSoTimeout(Constants.ACK_TIMEOUT_MILLIS);
                for (int i = 1; i <= Constants.MAX_ACK_TRYOUTS; i++) {
                    try {
                        sendPacket(sendPacket);

                        if (isCanceled()) return null;

                        recvPacket = receiveIdentifier(ackIdentifier, Constants.ACK_TIMEOUT_MILLIS,
                                TimeUnit.MILLISECONDS);

                        if (recvPacket.getIdentifier() == ackIdentifier &&
                                recvPacket.getConnId() == connId) {
                            return recvPacket;
                        }
                    } catch (TimeoutException e) {
                        Log.w(TAG, "Ack identifier packet receiving timed out: %d", e);
                    }
                }
                datagramSocket.setSoTimeout(initSoTimeout);
            }
            return null;
        }

        public IdentifierPacket receiveIdentifier(byte identifier, int timeout, TimeUnit unit) throws TimeoutException {
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

        public Pair<Boolean, Pair<Integer, Map<String, Pair<Integer, String>>>> checkCanceled(Map<String, Pair<Integer, String>> resultMap, String[] idsCanceled) {
            if (canceled) {
                Log.d(TAG, "Sending canceled by sender, releasing resources and notifying listener");
                resultMap = genCancelResultMap(resultMap, idsCanceled, false);
                if (listener != null) listener.onComplete(org.exthmui.share.shared.misc.Constants.TransmissionStatus.SENDER_CANCELLED.getNumVal(),
                        resultMap);
                return new Pair<>(true, new Pair<>(org.exthmui.share.shared.misc.Constants.TransmissionStatus.SENDER_CANCELLED.getNumVal(), resultMap));
            } else if (remoteCanceled) {
                Log.d(TAG, "Sending canceled by receiver, releasing resources and notifying listener");
                resultMap = genCancelResultMap(resultMap, idsCanceled, true);
                if (listener != null) listener.onComplete(org.exthmui.share.shared.misc.Constants.TransmissionStatus.RECEIVER_CANCELLED.getNumVal(),
                        resultMap);
                return new Pair<>(true, new Pair<>(org.exthmui.share.shared.misc.Constants.TransmissionStatus.RECEIVER_CANCELLED.getNumVal(), resultMap));
            }
            return new Pair<>(false, null);
        }

        public Map<String, Pair<Integer, String>> genCancelResultMap(Map<String, Pair<Integer, String>> resultMap, String[] idsCanceled, boolean remoteCanceled) {
            for (String id : idsCanceled)
                resultMap.put(id, new Pair<>((remoteCanceled ? org.exthmui.share.shared.misc.Constants.TransmissionStatus.SENDER_CANCELLED : org.exthmui.share.shared.misc.Constants.TransmissionStatus.RECEIVER_CANCELLED).getNumVal(), null));
            return resultMap;
        }

        @Override
        public void onReceivePacket(AbstractCommandPacket<?> packet) {
            if (!this.packetReceived.isDone()) this.packetReceived.complete(packet);
            else packetsBlocked.add(packet);
        }
    }

    public interface ConnectionListener {
        void onConnectionEstablished(byte connId);
    }

    public interface ReceivingListener {
        void requestAcceptation(SenderInfo senderInfo, FileInfo[] fileInfos, CompletableFuture<Set<String>> idsAccepted);

        void onProgressUpdate(int status, long totalBytesToSend, long bytesSent,
                              long curFileBytesToSend, long curFileBytesSent, String curFileId);

        void onComplete(int status, Map<String, Pair<Integer, String>> resultMap);
    }

    public interface PacketListener {
        void onReceivePacket(AbstractCommandPacket<?> packet);
    }

    public interface OutputStreamFactory {
        OutputStream produce(FileInfo fileInfo);
    }
}
