package org.exthmui.share.udptransport;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
import org.exthmui.share.udptransport.packets.FilePacket;
import org.exthmui.share.udptransport.packets.IdentifierPacket;
import org.exthmui.share.udptransport.packets.ResendRequestPacket;
import org.exthmui.utils.FileUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
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
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

    private int serverTcpPort;
    private int serverUdpPort;
    private final boolean lazyInit;
    private final boolean validateMd5;

    @Nullable
    private ServerSocket serverSocket;
    @Nullable
    private UDPUtil udpUtil;

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

    public UDPReceiver(@NonNull Context context, @NonNull OutputStreamFactory outputStreamFactory, @NonNull InputStreamFactory inputStreamFactory, @NonNull ConnectionListener listener, int serverTcpPort,
                       int serverUdpPort, boolean lazyInit, boolean validateMd5) throws IOException {
        this.context = context;
        this.outputStreamFactory = outputStreamFactory;
        this.inputStreamFactory = inputStreamFactory;
        this.listener = listener;
        this.serverTcpPort = serverTcpPort;
        this.serverUdpPort = serverUdpPort;
        this.lazyInit = lazyInit;
        this.validateMd5 = validateMd5;
        if (!lazyInit) initialize();

        connectionWatcher = () -> {
            while (isRunning) {
                try {
                    for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++)
                        if (i != 0 /* 0 is a invalid connection id*/ && !CONN_ID_HANDLER_MAP.containsKey((byte) i)) {
                            assert serverSocket != null;
                            Socket socket = serverSocket.accept();
                            assert udpUtil != null;
                            udpUtil.addHandler(context, (byte) i);
                            ConnectionHandler handler = new ConnectionHandler((byte) i, socket);
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
        if (!tcpReady) tcpReady();
        if (!udpReady) udpReady();
    }

    private void tcpReady() throws IOException {
        serverSocket = new ServerSocket(serverTcpPort);
        serverSocket.setReuseAddress(true);
        serverTcpPort = serverSocket.getLocalPort();
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
        udpUtil = new UDPUtil(serverUdpPort);
        udpUtil.setTAG(TAG);
        serverUdpPort = udpUtil.getLocalPort();
        udpUtil.startListening();
    }

    private void udpStop() {
        assert udpUtil != null;
        udpUtil.releaseResources();
    }

    public void startReceive() throws IOException {
        isRunning = true;
        if (lazyInit) initialize();
        threadPool.execute(connectionWatcher);
    }

    public void stopReceive() {
        tcpStop();
        udpStop();
    }

    @Nullable
    public ConnectionHandler getHandler(byte connId) {
        return CONN_ID_HANDLER_MAP.get(connId);
    }

    public class ConnectionHandler {
        private final Object handlerLock = new Object();

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
        private final TCPUtil tcpUtil;

        @NonNull
        private final UDPUtil.Handler handler;

        @Nullable
        private ReceivingListener listener;

        public void cancel() {
            synchronized (handlerLock) {
                canceled = true;
                handlerLock.notifyAll();
            }
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
            this.remoteTcpPort = socket.getPort();
            assert udpUtil != null;
            handler = Objects.requireNonNull(udpUtil.getHandler(connId));
            tcpUtil = new TCPUtil(socket);
            tcpUtil.setTAG(TAG);
            tcpUtil.initialize();
            tcpUtil.setCommandListener(cmd -> {
                synchronized (handlerLock) {
                    if (cmd.equals(Constants.COMMAND_CANCEL)) {
                        remoteCanceled = true;
                        handlerLock.notifyAll();
                    } else if (StringUtils.startsWith(cmd, Constants.COMMAND_UDP_SOCKET_READY)) {// e.g. UDP_READY5000:-128
                        remoteUdpPort = Integer.parseInt(cmd.replace(Constants.COMMAND_UDP_SOCKET_READY, "").split(":")[0]);
                        handlerLock.notifyAll();
                    }
                }
            });// (4-6)
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
            SenderInfo senderInfo = tcpUtil.readJson(SenderInfo.class);// (2)
            this.senderInfo = senderInfo;
            FileInfo[] fileInfos = tcpUtil.readJson(FileInfo[].class);// (3)
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
            tcpUtil.writeJson(accepted);// (4)
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

            assert udpUtil != null;
            tcpUtil.writeCommand(String.format(Locale.ROOT, "%s%d:%d", Constants.COMMAND_UDP_SOCKET_READY, serverUdpPort, getConnId()));// (6)

            while (remoteUdpPort == 0) {
                Pair<Boolean, Pair<TransmissionResult, Map<String, TransmissionResult>>> p =
                        checkCanceled(resultMap, accepted);
                if (p.first) return p.second;
                synchronized (handlerLock) {
                    handlerLock.wait();
                }
            }
            udpUtil.connect(new InetSocketAddress(address, remoteUdpPort));

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

                byte[][] bufTemp = new byte[Short.MAX_VALUE - Short.MIN_VALUE + 1][];
                Byte curGroup = null;
                short startPacketId = Short.MIN_VALUE;
                short endPacketId = Short.MAX_VALUE;
                boolean groupIdExceeded = false;
                boolean fileEnded = false;

                boolean waitingForResending = false;
                ResendRequestPacket resendPacket = null;

                {
                    IdentifierPacket packet = handler.receiveIdentifier(Constants.Identifier.START, Constants.IDENTIFIER_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);// (7)
                    curGroup = ByteUtils.cutBytesByTip(Constants.START_ID_GROUP_ID_TIP, packet.getExtra())[0];
                    handler.sendIdentifier(Constants.Identifier.START_ACK, null);// (8)
                }

                AbstractCommandPacket<?> packet;
                do {
                    try {
                        if (waitingForResending && resendPacket != null)
                            handler.sendPacket(resendPacket);// (11)
                        packet = handler.receivePacket(-1, null);
                    } catch (TimedOutException ignored) {
                        continue;
                    }
                    if (packet == null) continue;
                    if (groupIdExceeded) {
                        if (!(packet instanceof IdentifierPacket)) continue;
                        IdentifierPacket identifierPacket = (IdentifierPacket) packet;
                        if (identifierPacket.getIdentifier() != Constants.Identifier.GROUP_ID_RESET ||
                                ByteUtils.cutBytesByTip(Constants.GROUP_ID_RESET_ID_GROUP_ID_BEF_TIP, identifierPacket.getExtra())[0] != curGroup)
                            continue;
                        curGroup = ByteUtils.cutBytesByTip(Constants.GROUP_ID_RESET_ID_GROUP_ID_AFT_TIP, identifierPacket.getExtra())[0];
                        groupIdExceeded = false;
                        handler.sendIdentifier(Constants.Identifier.GROUP_ID_RESET_ACK, null);
                    }
                    switch (packet.getCommand()) {
                        case FILE_PACKET:// (7), (13)
                            FilePacket filePacket = (FilePacket) packet;
                            if (filePacket.getGroupId() != curGroup) continue;
                            byte[] data = bufTemp[filePacket.getPacketId() - Short.MIN_VALUE] = filePacket.getData();
                            curFileBytesReceived += data.length;
                            bytesReceived += data.length;
                            listener.onProgressUpdate(
                                    org.exthmui.share.shared.misc.Constants.TransmissionStatus.IN_PROGRESS.getNumVal(),
                                    totalBytesToSend, bytesReceived, senderInfo, fileInfos, fileInfo.getId(), fileInfo.getFileSize(), curFileBytesReceived);
                            break;
                        case IDENTIFIER:
                            IdentifierPacket identifierPacket = (IdentifierPacket) packet;
                            Log.d(String.format(TAG + "/ConnectionHandler(ConnId: %d)", getConnId()),
                                    String.format("Identifier \"%s\" received", identifierPacket.getIdentifier()));

                            switch (identifierPacket.getIdentifier()) {
                                case START:// (7)
                                    if (ByteUtils.cutBytesByTip(Constants.START_ID_GROUP_ID_TIP, identifierPacket.getExtra())[0] != curGroup)
                                        continue;
                                    handler.sendIdentifier(Constants.Identifier.START_ACK, null);// (8)
                                    waitingForResending = false;
                                    startPacketId = ByteUtils.bytesToShort(ByteUtils.cutBytesByTip(Constants.START_ID_START_PACKET_ID_TIP, identifierPacket.getExtra()));
                                    break;
                                case END:// (10)
                                    if (waitingForResending && identifierPacket.getExtra()[3] == 0) {
                                        Log.d(String.format(TAG + "/ConnectionHandler(ConnId: %d)", getConnId()),
                                                "Waiting for resending, but received END, ignoring");
                                        continue;
                                    }
                                    Log.d(String.format(TAG + "/ConnectionHandler(ConnId: %d)", getConnId()),
                                            String.format("Identifier group id: %d, current group: %d", ByteUtils.cutBytesByTip(Constants.END_ID_GROUP_ID_TIP, identifierPacket.getExtra())[0], curGroup));
                                    if (ByteUtils.cutBytesByTip(Constants.END_ID_GROUP_ID_TIP, identifierPacket.getExtra())[0] != curGroup)
                                        continue;
                                    handler.sendIdentifier(Constants.Identifier.END_ACK, null);// (11)
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
                                    handler.sendPacket(resendPacket);// (11)

                                    if (idsToResendAsSet.isEmpty()) {
                                        for (int i = startPacketId; i <= endPacketId; i++) {
                                            byte[] buf = bufTemp[i - Short.MIN_VALUE];
                                            if (buf == null) break;
                                            outputStream.write(buf, 0, buf.length);
                                        }
                                        outputStream.flush();
                                        if (curGroup == Byte.MAX_VALUE) groupIdExceeded = true;
                                        else curGroup++;
                                    } else {
                                        Log.d(String.format(TAG + "/ConnectionHandler(ConnId: %d)", getConnId()),
                                                String.format("Resend required: %s", Arrays.toString(idsToResend)));
                                        waitingForResending = true;
                                    }
                                    break;
                                case GROUP_ID_RESET:
                                    if (ByteUtils.cutBytesByTip(Constants.GROUP_ID_RESET_ID_GROUP_ID_BEF_TIP, identifierPacket.getExtra())[0] != curGroup)
                                        continue;
                                    curGroup = ByteUtils.cutBytesByTip(Constants.GROUP_ID_RESET_ID_GROUP_ID_AFT_TIP, identifierPacket.getExtra())[0];
                                    handler.sendIdentifier(Constants.Identifier.GROUP_ID_RESET_ACK, null);
                                    break;
                                case FILE_END:// (14)
                                    fileEnded = true;
                                    handler.sendIdentifier(Constants.Identifier.FILE_END_ACK, null);// (15)
                                    break;
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

        public void releaseResources() {
            commandWatcherStopFlag = true;
            tcpUtil.releaseResources();
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
    }

    public int getServerTcpPort() {
        return serverTcpPort;
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

    public interface OutputStreamFactory {
        OutputStream produce(FileInfo fileInfo);
    }

    public interface InputStreamFactory {
        InputStream produce(FileInfo fileInfo);
    }
}
