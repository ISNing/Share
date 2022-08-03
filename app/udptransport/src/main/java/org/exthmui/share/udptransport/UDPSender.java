package org.exthmui.share.udptransport;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.lang3.StringUtils;
import org.exthmui.share.shared.base.Entity;
import org.exthmui.share.shared.base.FileInfo;
import org.exthmui.share.shared.base.receive.SenderInfo;
import org.exthmui.share.shared.base.results.SuccessTransmissionResult;
import org.exthmui.share.shared.base.results.TransmissionResult;
import org.exthmui.share.shared.base.send.ReceiverInfo;
import org.exthmui.share.shared.exceptions.trans.ReceiverCancelledException;
import org.exthmui.share.shared.exceptions.trans.SenderCancelledException;
import org.exthmui.share.shared.misc.StackTraceUtils;
import org.exthmui.share.udptransport.packets.AbstractCommandPacket;
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
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

    private int udpPort;
    private byte connId;

    private boolean commandWatcherStopFlag;

    private final ThreadPoolExecutor coreThreadPool = new ThreadPoolExecutor(1,
            1, 0L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1),
            r -> new Thread(r, r.toString()));
    private final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(0,
            1, 1L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(4),
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

    private boolean canceled;
    private boolean remoteCanceled;

    private boolean udpReady;

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
        return coreThreadPool.submit((Callable<Integer>) () -> {
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
        listener.onProgressUpdate(
                org.exthmui.share.shared.misc.Constants.TransmissionStatus.CONNECTION_ESTABLISHED.getNumVal(),
                totalBytesToSend, 0, null, 0, 0);
        InputStream stream = entity.getInputStream(context);
        BufferedInputStream inputStream = stream instanceof BufferedInputStream ?
                (BufferedInputStream) stream : new BufferedInputStream(stream);
        assert datagramSocket != null;
        FilePacket sendPacket = new FilePacket();
        byte[][] bufTemp = new byte[Short.MAX_VALUE - Short.MIN_VALUE + 1][];
        byte[] buf = new byte[Constants.DATA_LEN_MAX_HI];
        byte curGroup = Byte.MIN_VALUE;
        short curPacket = Short.MIN_VALUE;
        int len = 0;
        sendIdentifier(Constants.START_IDENTIFIER, Constants.START_ACK_IDENTIFIER);// (7), (8)
        while ((len = (inputStream.read(buf))) > 0) {
            bufTemp[curPacket - Short.MIN_VALUE] = buf.clone();
            sendPacket(sendPacket.setPacketId(curPacket).setGroupId(curGroup).setData(buf, len));// (9)

            if (checkCanceled()) return remoteCanceled ? new ReceiverCancelledException(context) :
                    new SenderCancelledException(context);

            if (curPacket == Short.MAX_VALUE) {
                boolean packetAllSent = false;
                while (!packetAllSent) {
                    sendIdentifier(Constants.END_IDENTIFIER, Constants.END_ACK_IDENTIFIER);// (10), (11)

                    ResendRequestPacket resendReq = receivePacket(new ResendRequestPacket());// (12)
                    short[] packetIds = resendReq.getPacketIds();
                    if (packetIds.length > 0) {
                        FilePacket[] packetsToResend = new FilePacket[packetIds.length];
                        for (int i = 0; i < packetIds.length; i++) {
                            packetsToResend[i] = new FilePacket().setGroupId(curGroup).setPacketId(packetIds[i]).setData(bufTemp[packetIds[i]]);
                        }
                        resendPackets(packetsToResend, curGroup);// (13)
                    } else packetAllSent = true;
                }

                if (curGroup + 1 > Byte.MAX_VALUE) {
                    sendIdentifier(Constants.GROUP_ID_RESET_IDENTIFIER, Constants.GROUP_ID_RESET_ACK_IDENTIFIER, new byte[]{curGroup});
                    curGroup = Byte.MAX_VALUE;
                } else {
                    sendIdentifier(Constants.START_IDENTIFIER, Constants.START_ACK_IDENTIFIER, new byte[]{curGroup});// (7), (8)
                    curGroup++;
                }
            } else {
                curPacket++;
            }
        }
        boolean packetAllSent = false;
        while (!packetAllSent) {
            sendIdentifier(Constants.END_IDENTIFIER, Constants.END_ACK_IDENTIFIER, new byte[]{curGroup});// (10), (11)

            ResendRequestPacket resendReq = receivePacket(new ResendRequestPacket());// (12)
            short[] packetIds = resendReq.getPacketIds();
            if (packetIds.length > 0) {
                FilePacket[] packetsToResend = new FilePacket[packetIds.length];
                for (int i = 0; i < packetIds.length; i++) {
                    packetsToResend[i] = new FilePacket().setGroupId(curGroup).setPacketId(packetIds[i]).setData(bufTemp[packetIds[i]]);
                }
                resendPackets(packetsToResend, curGroup);// (13)
            } else packetAllSent = true;
        }

        sendIdentifier(Constants.FILE_END_IDENTIFIER, Constants.FILE_END_ACK_IDENTIFIER);// (7), (8)
        return new SuccessTransmissionResult(context);
    }

    public void resendPackets(FilePacket[] packets, byte curGroup) throws IOException {
        sendIdentifier(Constants.START_IDENTIFIER, Constants.START_ACK_IDENTIFIER, new byte[]{curGroup});
        for (FilePacket packet : packets)
            sendPacket(packet);
        sendIdentifier(Constants.END_IDENTIFIER, Constants.END_ACK_IDENTIFIER, new byte[]{curGroup});
        ResendRequestPacket resendReq = receivePacket(new ResendRequestPacket());// (12)
        short[] packetIds = resendReq.getPacketIds();
        if (packetIds.length > 0) {
            FilePacket[] packetsToResend = new FilePacket[packetIds.length];
            for (int i = 0; i < packetIds.length; i++) {
                packetsToResend[i] = new FilePacket().setPacketId(packetIds[i]);
            }
            resendPackets(packetsToResend, curGroup);// (13)
        }
    }

    public <T extends AbstractCommandPacket<T>> T receivePacket(T packet) throws IOException {
        assert datagramSocket != null;
        datagramSocket.receive(packet.toDatagramPacket());
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
    public IdentifierPacket sendIdentifier(byte identifier, Byte ackIdentifier) throws IOException {
        return sendIdentifier(identifier, ackIdentifier, null);
    }

    public IdentifierPacket sendIdentifier(byte identifier, Byte ackIdentifier, byte[] extra) throws IOException {
        assert datagramSocket != null;
        IdentifierPacket sendPacket = new IdentifierPacket().setIdentifier(identifier).setExtra(extra);
        if (ackIdentifier != null) {
            IdentifierPacket recvPacket = new IdentifierPacket();
            int initSoTimeout = datagramSocket.getSoTimeout();
            datagramSocket.setSoTimeout(Constants.ACK_TIMEOUT_MILLIS);
            for (int i = 0; i <= Constants.MAX_ACK_TRYOUTS; i++) {
                try {
                    sendPacket(sendPacket);

                    if (checkCanceled()) return null;

                    recvPacket = receivePacket(recvPacket);

                    if (recvPacket.getIdentifier() == ackIdentifier &&
                            recvPacket.getConnId() == connId) {
                        return recvPacket;
                    }
                } catch (SocketTimeoutException ignored) {
                }
            }
            datagramSocket.setSoTimeout(initSoTimeout);
        } else {
            sendPacket(sendPacket);
        }
        return null;
    }

    public void releaseResources() {
        commandWatcherStopFlag = true;
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
