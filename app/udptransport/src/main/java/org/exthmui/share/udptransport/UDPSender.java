package org.exthmui.share.udptransport;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.exthmui.share.shared.base.Entity;
import org.exthmui.share.shared.base.FileInfo;
import org.exthmui.share.shared.base.receive.SenderInfo;
import org.exthmui.share.shared.base.results.SuccessTransmissionResult;
import org.exthmui.share.shared.base.results.TransmissionResult;
import org.exthmui.share.shared.base.send.ReceiverInfo;
import org.exthmui.share.shared.exceptions.trans.ReceiverCancelledException;
import org.exthmui.share.shared.exceptions.trans.SenderCancelledException;
import org.exthmui.share.shared.exceptions.trans.TimedOutException;
import org.exthmui.share.shared.exceptions.trans.TransmissionException;
import org.exthmui.share.shared.misc.StackTraceUtils;
import org.exthmui.share.udptransport.packets.AbstractCommandPacket;
import org.exthmui.share.udptransport.packets.CommandPacket;
import org.exthmui.share.udptransport.packets.FilePacket;
import org.exthmui.share.udptransport.packets.IdentifierPacket;
import org.exthmui.share.udptransport.packets.ResendRequestPacket;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * UDPSender
 *
 * initialize -> sendAsync
 */
public class UDPSender {
    public static final String TAG = "UDPSender";
    private static final Gson GSON = new Gson();

    @NonNull
    private final Context context;
    @NonNull
    private final SendingListener listener;

    @Nullable
    private Socket socket;
    @Nullable
    private DataInputStream in;
    @Nullable
    private DataOutputStream out;
    @Nullable
    private DatagramSocket datagramSocket;

    long totalBytesToSend = 0;
    long bytesSent = 0;
    ReceiverInfo receiverInfo = null;
    FileInfo[] fileInfos = null;

    public final List<AbstractCommandPacket<?>> packetsBlocked = new ArrayList<>();
    @NonNull
    public CompletableFuture<AbstractCommandPacket<?>> packetReceived = new CompletableFuture<>();

    private int udpPort;
    private byte connId;

    private boolean commandWatcherStopFlag;

    private boolean canceled;
    private boolean remoteCanceled;

    private boolean udpReady;

    private final ThreadPoolExecutor coreThreadPool = new ThreadPoolExecutor(1,
            1, 0L, TimeUnit.SECONDS, new SynchronousQueue<>(),
            r -> new Thread(r, r.toString()));
    private final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(0,
            2, 1L, TimeUnit.SECONDS, new SynchronousQueue<>(),
            r -> new Thread(r, r.toString()));
    private final Runnable commandWatcher = () -> {
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
    private final Runnable packetInterceptor = () -> {
        while (udpReady) {
            try {
                CommandPacket packet = receivePacket(new CommandPacket(new DatagramPacket(new byte[Constants.BUF_LEN_MAX_HI], Constants.BUF_LEN_MAX_HI)));
                if (!packetReceived.isDone())
                    packetReceived.complete(packet);
                else packetsBlocked.add(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    public UDPSender(@NonNull Context context, @NonNull SendingListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void cancel() {
        canceled = true;
    }

    public void initialize() throws SocketException {
        socket = new Socket();
        socket.setTcpNoDelay(true);
        socket.setReuseAddress(true);
        datagramSocket = new DatagramSocket();
        datagramSocket.setReuseAddress(true);
    }

    public void connectTcp(SocketAddress tcpAddress) throws IOException {
        assert socket != null;
        Log.d(TAG, "Trying to connect to receiver under tcp socket: " + tcpAddress);
        socket.connect(tcpAddress);
        in = Utils.getDataInput(socket);
        out = Utils.getDataOutput(socket);
    }

    public void connectUdp(SocketAddress udpAddress) throws IOException {
        assert datagramSocket != null;
        datagramSocket.connect(udpAddress);
        threadPool.execute(packetInterceptor);
    }

    public void writeJson(Object object) throws IOException {
        assert socket != null;
        String jsonStr = GSON.toJson(object);
        Log.d(TAG, "Trying to send \"" + jsonStr + "\" -> " + socket.getInetAddress());
        assert out != null;
        out.writeUTF(jsonStr);
    }

    public void writeCommand(String command) throws IOException {
        assert out != null;
        out.writeUTF(command);
    }

    public <T> T readJson(Class<T> classOfT) throws IOException {
        assert in != null;
        return GSON.fromJson(in.readUTF(), classOfT);
    }

    public <T> T readJson(Class<T> classOfT, Type typeOfT) throws IOException {
        assert in != null;
        return GSON.fromJson(in.readUTF(), typeOfT);
    }

    public void dealWithCommand(String cmd) {
        switch (cmd) {
            case (Constants.COMMAND_CANCEL):
                remoteCanceled = true;
                break;
            default:
                /*
                e.g. UDP_READY5000:-128
                 */
                if (StringUtils.startsWith(cmd, Constants.COMMAND_UDP_SOCKET_READY)) {
                    udpPort = Integer.parseInt(cmd.replace(Constants.COMMAND_UDP_SOCKET_READY, "").split(":")[0]);
                    connId = Byte.parseByte(cmd.replace(Constants.COMMAND_UDP_SOCKET_READY, "").split(":")[1]);
                    udpReady = true;
                }
                break;
        }
    }

    public Future<Integer> sendAsync(@NonNull Entity[] entities, @NonNull FileInfo[] fileInfos,
                            @NonNull SenderInfo sender, @NonNull SocketAddress tcpAddress) {
        if (entities.length != fileInfos.length) throw new IllegalArgumentException();
        return coreThreadPool.submit(() -> {
            try {
                send(entities, fileInfos, sender, tcpAddress);
            } catch (Throwable tr) {
                Log.e(TAG, tr.getMessage());
                Log.e(TAG, StackTraceUtils.getStackTraceString(tr.getStackTrace()));
                listener.onComplete(
                        org.exthmui.share.shared.misc.Constants.TransmissionStatus.ERROR.getNumVal(),
                        null);
            } finally {
                releaseResources();
            }
                return null;//TODO
        });
    }

    private void send(@NonNull Entity[] entities, @NonNull FileInfo[] fileInfos,
                          @NonNull SenderInfo sender, @NonNull SocketAddress tcpAddress) throws IOException {
        if (entities.length != fileInfos.length) throw new IllegalArgumentException();
        connectTcp(tcpAddress);// (1)
        writeJson(sender);// (2)
        writeJson(fileInfos);// (3)
        listener.onProgressUpdate(
                org.exthmui.share.shared.misc.Constants.TransmissionStatus.WAITING_FOR_ACCEPTATION.getNumVal(),
                0, 0, null, 0, 0);
        String[] accepted = readJson(String[].class);// (4)
        if (accepted == null || accepted.length == 0) {
            Log.d(TAG, "No file were accepted");
            listener.onComplete(
                    org.exthmui.share.shared.misc.Constants.TransmissionStatus.REJECTED.getNumVal(),
                    null);
            return;
        } else listener.onAccepted(accepted);
        List<Entity> entitiesToSend = new ArrayList<>();
        List<FileInfo> fileInfosToSend = new ArrayList<>();
        List<String> acceptedIdsAsList = Arrays.asList(accepted);
        for (FileInfo fileInfo : fileInfos) {
            if (acceptedIdsAsList.contains(fileInfo.getId())) {
                totalBytesToSend += fileInfo.getFileSize();
                fileInfosToSend.add(fileInfo);
                entitiesToSend.add(entities[Arrays.asList(fileInfos).indexOf(fileInfo)]);
            }
        }
        threadPool.execute(commandWatcher);// (4-6)
        while (!udpReady) {// (6)
            if (checkCanceled()) return;
        }
        assert socket != null;
        connectUdp(new InetSocketAddress(socket.getInetAddress(), udpPort));
        listener.onProgressUpdate(
                org.exthmui.share.shared.misc.Constants.TransmissionStatus.CONNECTION_ESTABLISHED.getNumVal(),
                totalBytesToSend, 0, null, 0, 0);
        for (Entity entity : entitiesToSend) {
            Log.d(TAG, "Start sending file: " + entity.getFileName());
            sendFile(entity, fileInfosToSend.get(entitiesToSend.indexOf(entity)));

            if (checkCanceled()) return;
        }
        commandWatcherStopFlag = true;
        @SuppressWarnings("unchecked") Map<String, Pair<Integer, String>> resultMap =
                readJson(Map.class, new TypeToken<Map<String, Pair<Integer, String>>>() {
                }.getType());// (16)
        for (String id : resultMap.keySet()) {
            if ((Objects.requireNonNull(resultMap.get(id)).first &
                    org.exthmui.share.shared.misc.Constants.TransmissionStatus.ERROR.getNumVal()) !=
                            Objects.requireNonNull(resultMap.get(id)).first) {
                listener.onComplete(org.exthmui.share.shared.misc.Constants.TransmissionStatus.REMOTE_ERROR.getNumVal(),
                        resultMap);
                return;
            }
        }
        listener.onComplete(
                org.exthmui.share.shared.misc.Constants.TransmissionStatus.COMPLETED.getNumVal(),
                resultMap);
    }

    public TransmissionResult sendFile(Entity entity, FileInfo fileInfo) throws IOException {
        try {
            listener.onProgressUpdate(
                    org.exthmui.share.shared.misc.Constants.TransmissionStatus.CONNECTION_ESTABLISHED.getNumVal(),
                    totalBytesToSend, 0, null, 0, 0);
            InputStream stream = entity.getInputStream(context);
            BufferedInputStream inputStream = stream instanceof BufferedInputStream ?
                    (BufferedInputStream) stream : new BufferedInputStream(stream);
            assert datagramSocket != null;
            FilePacket sendPacket = new FilePacket();
            final byte startGroup = Constants.START_GROUP_ID;
            final byte endGroup = Constants.END_GROUP_ID;
            final short startPacket = Constants.START_PACKET_ID;
            final short endPacket = Constants.END_PACKET_ID;
            byte curGroup = startGroup;
            short curPacket = startPacket;
            byte[][] bufTemp = new byte[Short.MAX_VALUE - Short.MIN_VALUE + 1][];
            byte[] buf = new byte[Constants.DATA_LEN_MAX_HI];
            int len = 0;
            sendIdentifier(Constants.START_IDENTIFIER, Constants.START_ACK_IDENTIFIER, ArrayUtils.addFirst(ByteUtils.shortToBytes(startPacket), curGroup));// (7), (8)
            while ((len = (inputStream.read(buf))) > 0) {
                bufTemp[curPacket - Short.MIN_VALUE] = buf.clone();
                sendPacket(sendPacket.setPacketId(curPacket).setGroupId(curGroup).setData(buf, len));// (9)

                if (checkCanceled())
                    return remoteCanceled ? new ReceiverCancelledException(context) :
                            new SenderCancelledException(context);

                if (curPacket == endPacket) {
                    boolean packetAllSent = false;
                    while (!packetAllSent) {
                        ResendRequestPacket resendReq = null;
                        boolean timedout;
                        int tryouts = 0;
                        do {
                            tryouts++;
                            timedout = false;
                            sendIdentifier(Constants.END_IDENTIFIER, Constants.END_ACK_IDENTIFIER, ArrayUtils.addFirst(ByteUtils.shortToBytes(curPacket), curGroup));// (10), (11)

                            try {
                                resendReq = receivePacket(new ResendRequestPacket(), Constants.ACK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);// (12)
                            } catch (TimeoutException e) {
                                if (tryouts >= Constants.MAX_ACK_TRYOUTS)
                                    return new TimedOutException(context, e);
                                timedout = true;
                            }
                        } while (timedout);
                        short[] packetIds = resendReq.getPacketIds();
                        if (packetIds.length > 0) {
                            FilePacket[] packetsToResend = new FilePacket[packetIds.length];
                            for (int i = 0; i < packetIds.length; i++) {
                                packetsToResend[i] = new FilePacket().setGroupId(curGroup).setPacketId(packetIds[i]).setData(bufTemp[packetIds[i]]);
                            }
                            resendPackets(packetsToResend, curGroup, curPacket);// (13)
                        } else packetAllSent = true;
                    }

                    if (curGroup + 1 > endGroup) {
                        sendIdentifier(Constants.GROUP_ID_RESET_IDENTIFIER, Constants.GROUP_ID_RESET_ACK_IDENTIFIER, new byte[]{curGroup, startGroup});
                        curGroup = startGroup;
                        sendIdentifier(Constants.START_IDENTIFIER, Constants.START_ACK_IDENTIFIER, ArrayUtils.addFirst(ByteUtils.shortToBytes(startPacket), curGroup));// (7), (8)
                    } else {
                        curGroup++;
                        sendIdentifier(Constants.START_IDENTIFIER, Constants.START_ACK_IDENTIFIER, ArrayUtils.addFirst(ByteUtils.shortToBytes(startPacket), curGroup));// (7), (8)
                    }
                    curPacket = startPacket;
                } else {
                    curPacket++;
                }
            }

            curPacket--;
            boolean packetAllSent = false;
            while (!packetAllSent) {
                ResendRequestPacket resendReq = null;
                boolean timedout;
                int tryouts = 0;
                do {
                    tryouts++;
                    timedout = false;
                    sendIdentifier(Constants.END_IDENTIFIER, Constants.END_ACK_IDENTIFIER, ArrayUtils.addFirst(ByteUtils.shortToBytes(curPacket), curGroup));// (10), (11)

                    try {
                        resendReq = receivePacket(new ResendRequestPacket(), Constants.ACK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);// (12)
                    } catch (TimeoutException e) {
                        if (tryouts >= Constants.MAX_ACK_TRYOUTS)
                            return new TimedOutException(context, e);
                        timedout = true;
                    }
                } while (timedout);

                short[] packetIds = resendReq.getPacketIds();
                if (packetIds.length > 0) {
                    FilePacket[] packetsToResend = new FilePacket[packetIds.length];
                    for (int i = 0; i < packetIds.length; i++) {
                        if (bufTemp[packetIds[i] - Short.MIN_VALUE] == null) break;
                        packetsToResend[i] = new FilePacket().setGroupId(curGroup).setPacketId(packetIds[i]).setData(bufTemp[packetIds[i] - Short.MIN_VALUE]);
                        if (bufTemp[packetIds[i] - Short.MIN_VALUE].length != Constants.DATA_LEN_MAX_HI)
                            break;
                    }
                    resendPackets(packetsToResend, curGroup, curPacket);// (13)
                } else packetAllSent = true;
            }

            sendIdentifier(Constants.FILE_END_IDENTIFIER, Constants.FILE_END_ACK_IDENTIFIER);// (7), (8)
            return new SuccessTransmissionResult(context);
        } catch (TransmissionException e) {
            return e;
        }
    }

    public void resendPackets(FilePacket[] packets, byte curGroup, short curGroupEndPacket) throws IOException, TimedOutException {
        sendIdentifier(Constants.START_IDENTIFIER, Constants.START_ACK_IDENTIFIER, new byte[]{curGroup});
        for (FilePacket packet : packets)
            sendPacket(packet);

        ResendRequestPacket resendReq = null;
        boolean timedout;
        int tryouts = 0;
        do {
            tryouts++;
            timedout = false;
            sendIdentifier(Constants.END_IDENTIFIER, Constants.END_ACK_IDENTIFIER, ArrayUtils.addFirst(ByteUtils.shortToBytes(curGroupEndPacket), curGroup));// (10), (11)

            try {
                resendReq = receivePacket(new ResendRequestPacket(), Constants.ACK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);// (12)
            } catch (TimeoutException e) {
                if (tryouts >= Constants.MAX_ACK_TRYOUTS) throw new TimedOutException(context, e);
                timedout = true;
            }
        } while (timedout);

        short[] packetIds = resendReq.getPacketIds();
        if (packetIds.length > 0) {
            FilePacket[] packetsToResend = new FilePacket[packetIds.length];
            for (int i = 0; i < packetIds.length; i++) {
                for (FilePacket packet : packets) {
                    if (packet.getPacketId() == packetIds[i]) {
                        packetsToResend[i] = packet;
                        break;
                    }
                }

            }
            resendPackets(packetsToResend, curGroup, curGroupEndPacket);// (13)
        }
    }

    private AbstractCommandPacket<?> receivePacket(int timeout, TimeUnit unit) throws TimeoutException {
        AbstractCommandPacket<?> packet = null;
        try {
            if (timeout < 0) {
                packet = packetReceived.get();
            } else packet = packetReceived.get(timeout, unit);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (!packetReceived.isDone()) packetReceived.cancel(true);
            packetReceived = new CompletableFuture<>();
        }
        if (!packetsBlocked.isEmpty()) {
            packetReceived.complete(packetsBlocked.get(0));
            packetsBlocked.remove(0);
        }
        return packet;
    }

    private <T extends AbstractCommandPacket<T>> T receivePacket(T packet) throws IOException {
        assert datagramSocket != null;
        DatagramPacket p = packet.toDatagramPacket();
        datagramSocket.receive(p);
        p.setData(Arrays.copyOfRange(p.getData(), p.getOffset(), p.getOffset() + p.getLength()));
        return packet;
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

    public <T extends AbstractCommandPacket<T>> void sendPacket(T packet) throws IOException {
        assert datagramSocket != null;
        packet.setConnId(connId);
        DatagramPacket p = packet.toDatagramPacket();
        datagramSocket.send(p);
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

                    if (checkCanceled()) return null;

                    receivePacket(recvPacket, Constants.ACK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

                    Log.d(TAG,
                            String.format("Ack Identifier \"%d\" received", recvPacket.getIdentifier()));
                    if (recvPacket.getIdentifier() == ackIdentifier &&
                            recvPacket.getConnId() == connId) {
                        return recvPacket;
                    }
                } catch (TimeoutException e) {
                    Log.w(TAG, "Ack identifier packet receiving timed out: %s", e);
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

    public void releaseResources() {
        commandWatcherStopFlag = true;
        udpReady = false;
        Utils.silentClose(in);
        in = null;
        Utils.silentClose(out);
        out = null;
        Utils.silentClose(socket);
        socket = null;
        Utils.silentClose(datagramSocket);
        datagramSocket = null;
        coreThreadPool.shutdown();
        threadPool.shutdown();
    }

    public boolean checkCanceled() {
        if(canceled) {
            Log.d(TAG, "Sending canceled by sender");
            listener.onComplete(org.exthmui.share.shared.misc.Constants.TransmissionStatus.SENDER_CANCELLED.getNumVal(),
                    null);
            return true;
        } else if(remoteCanceled) {
            Log.d(TAG, "Sending canceled by receiver");
            listener.onComplete(org.exthmui.share.shared.misc.Constants.TransmissionStatus.RECEIVER_CANCELLED.getNumVal(),
                    null);
            return true;
        }
        return false;
    }

    public interface SendingListener {
        void onAccepted(String[] fileIdsAccepted);

        void onProgressUpdate(int status, long totalBytesToSend, long bytesSent, String curFileId,
                              long curFileBytesToSend, long curFileBytesSent);

        void onComplete(int status, Map<String, Pair<Integer, String>> resultMap);
    }
}
