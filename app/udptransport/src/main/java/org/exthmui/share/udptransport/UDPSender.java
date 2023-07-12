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
import org.exthmui.share.udptransport.packets.AbstractCommandPacket;
import org.exthmui.share.udptransport.packets.FilePacket;
import org.exthmui.share.udptransport.packets.ResendRequestPacket;
import org.exthmui.utils.StackTraceUtils;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * UDPSender
 * <p>
 * initialize -> sendAsync
 */
public class UDPSender {
    public static final String TAG = "UDPSender";
    private final Object lock = new Object();

    @NonNull
    private final Context context;
    @NonNull
    private final SendingListener listener;

    @Nullable
    private TCPUtil tcpUtil;
    @Nullable
    private UDPUtil udpUtil;
    @Nullable
    private UDPUtil.Handler handler;

    long totalBytesToSend = 0;
    long bytesSent = 0;
    ReceiverInfo receiverInfo = null;
    FileInfo[] fileInfos = null;
    private int remoteUdpPort;
    private byte connId;

    private boolean canceled;
    private boolean remoteCanceled;

    private final ThreadPoolExecutor coreThreadPool = new ThreadPoolExecutor(1,
            1, 0L, TimeUnit.SECONDS, new SynchronousQueue<>(),
            r -> new Thread(r, r.toString()));
    private final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(0,
            2, 1L, TimeUnit.SECONDS, new SynchronousQueue<>(),
            r -> new Thread(r, r.toString()));

    public UDPSender(@NonNull Context context, @NonNull SendingListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void cancel() {
        synchronized (lock) {
            canceled = true;
            lock.notifyAll();
        }
    }

    public void initialize() throws SocketException {
        tcpUtil = new TCPUtil();
        udpUtil = new UDPUtil();
        udpUtil.setTAG(TAG);
        udpUtil.setStateChecker(this::checkCanceled);
    }

    public Future<Integer> sendAsync(@NonNull Entity[] entities, @NonNull FileInfo[] fileInfos,
                                     @NonNull SenderInfo sender, @NonNull SocketAddress tcpAddress) {
        if (entities.length != fileInfos.length) throw new IllegalArgumentException();
        return coreThreadPool.submit(() -> {
            try {
                send(entities, fileInfos, sender, tcpAddress);
            } catch (Throwable tr) {
                Log.e(TAG, String.format("Error occured while sending: %s(message: %s)", tr, tr.getMessage()));
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
        assert tcpUtil != null;
        tcpUtil.connect(tcpAddress);// (1)
        tcpUtil.writeJson(sender);// (2)
        tcpUtil.writeJson(fileInfos);// (3)
        listener.onProgressUpdate(
                org.exthmui.share.shared.misc.Constants.TransmissionStatus.WAITING_FOR_ACCEPTATION.getNumVal(),
                0, 0, null, 0, 0);
        String[] accepted = tcpUtil.readJson(String[].class);// (4)
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

        tcpUtil.setCommandListener(cmd -> {
            if (cmd.equals(Constants.COMMAND_CANCEL)) {
                remoteCanceled = true;
                synchronized (lock) {
                    lock.notifyAll();
                }
            } else if (StringUtils.startsWith(cmd, Constants.COMMAND_UDP_SOCKET_READY)) { // e.g. UDP_READY5000:-128
                    remoteUdpPort = Integer.parseInt(cmd.replace(Constants.COMMAND_UDP_SOCKET_READY, "").split(":")[0]);
                    connId = Byte.parseByte(cmd.replace(Constants.COMMAND_UDP_SOCKET_READY, "").split(":")[1]);
            }
        });// (4-6)
        while (remoteUdpPort == 0) {// (6)
            if (checkCanceled()) return;
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        assert udpUtil != null;
        udpUtil.addHandler(context, connId);
        handler = udpUtil.getHandler(connId);
        udpUtil.connect(new InetSocketAddress(tcpUtil.getInetAddress(), remoteUdpPort));
        udpUtil.startListening();
        tcpUtil.writeCommand(String.format(Locale.ROOT, "%s%d:%d", Constants.COMMAND_UDP_SOCKET_READY, udpUtil.getLocalPort(), connId));// (6)
        listener.onProgressUpdate(
                org.exthmui.share.shared.misc.Constants.TransmissionStatus.CONNECTION_ESTABLISHED.getNumVal(),
                totalBytesToSend, 0, null, 0, 0);
        for (Entity entity : entitiesToSend) {
            Log.d(TAG, "Start sending file: " + entity.getFileName());
            sendFile(entity, fileInfosToSend.get(entitiesToSend.indexOf(entity)));

            if (checkCanceled()) return;
        }
        @SuppressWarnings("unchecked") Map<String, Pair<Integer, String>> resultMap =
                tcpUtil.readJson(Map.class, new TypeToken<Map<String, Pair<Integer, String>>>() {
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
            assert udpUtil != null;
            assert handler != null;
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
            handler.sendIdentifier(Constants.START_IDENTIFIER, Constants.START_ACK_IDENTIFIER, ArrayUtils.addFirst(ByteUtils.shortToBytes(startPacket), curGroup));// (7), (8)
            while ((len = (inputStream.read(buf))) > 0) {
                bufTemp[curPacket - Short.MIN_VALUE] = buf.clone();
                handler.sendPacket(sendPacket.setPacketId(curPacket).setGroupId(curGroup).setData(buf, len));// (9)

                if (checkCanceled())
                    return remoteCanceled ? new ReceiverCancelledException(context) :
                            new SenderCancelledException(context);

                if (curPacket == endPacket) {
                    boolean packetAllSent = false;
                    while (!packetAllSent) {
                        handler.sendIdentifier(Constants.END_IDENTIFIER, Constants.END_ACK_IDENTIFIER, ArrayUtils.addFirst(ByteUtils.shortToBytes(curPacket), curGroup));// (10), (11)

                        ResendRequestPacket resendReq = handler.receivePacket(ResendRequestPacket::new, Constants.ACK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS, Constants.MAX_ACK_TRYOUTS);// (12)
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
                        handler.sendIdentifier(Constants.GROUP_ID_RESET_IDENTIFIER, Constants.GROUP_ID_RESET_ACK_IDENTIFIER, new byte[]{curGroup, startGroup});
                        curGroup = startGroup;
                        handler.sendIdentifier(Constants.START_IDENTIFIER, Constants.START_ACK_IDENTIFIER, ArrayUtils.addFirst(ByteUtils.shortToBytes(startPacket), curGroup));// (7), (8)
                    } else {
                        curGroup++;
                        handler.sendIdentifier(Constants.START_IDENTIFIER, Constants.START_ACK_IDENTIFIER, ArrayUtils.addFirst(ByteUtils.shortToBytes(startPacket), curGroup));// (7), (8)
                    }
                    curPacket = startPacket;
                } else {
                    curPacket++;
                }
            }

            curPacket--;
            boolean packetAllSent = false;
            while (!packetAllSent) {
                handler.sendIdentifier(Constants.END_IDENTIFIER, Constants.END_ACK_IDENTIFIER, ArrayUtils.addFirst(ByteUtils.shortToBytes(curPacket), curGroup));// (10), (11)

                ResendRequestPacket resendReq = handler.receivePacket(ResendRequestPacket::new, Constants.ACK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS, Constants.MAX_ACK_TRYOUTS);// (12)

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

            handler.sendIdentifier(Constants.FILE_END_IDENTIFIER, Constants.FILE_END_ACK_IDENTIFIER);// (7), (8)
            return new SuccessTransmissionResult(context);
        } catch (TransmissionException e) {
            return e;
        }
    }

    public void resendPackets(FilePacket[] packets, byte curGroup, short curGroupEndPacket) throws IOException, TimedOutException {
        assert handler != null;
        Log.d(TAG, String.format("Resend start: %s", Arrays.toString(Arrays.stream(packets).map(FilePacket::getPacketId).toArray())));
        handler.sendIdentifier(Constants.START_IDENTIFIER, Constants.START_ACK_IDENTIFIER, new byte[]{curGroup});
        for (FilePacket packet : packets)
            handler.sendPacket(packet);

        handler.sendIdentifier(Constants.END_IDENTIFIER, Constants.END_ACK_IDENTIFIER, ArrayUtils.addFirst(ByteUtils.shortToBytes(curGroupEndPacket), curGroup));// (10), (11)

        ResendRequestPacket resendReq = handler.receivePacket(ResendRequestPacket::new, Constants.ACK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS, Constants.MAX_ACK_TRYOUTS);// (12)

        short[] packetIds = resendReq.getPacketIds();
        Log.d(TAG, String.format("Resend requested: %s", Arrays.toString(packetIds)));
        if (packetIds.length > 0) {
            ArrayList<FilePacket> packetsToResend = new ArrayList<>();
            for (short packetId : packetIds) {
                for (FilePacket packet : packets) {
                    if (packet.getPacketId() == packetId) {
                        packetsToResend.add(packet);
                        break;
                    }
                }

            }
            resendPackets(packetsToResend.toArray(new FilePacket[0]), curGroup, curGroupEndPacket);// (13)
        }
    }

    public void releaseResources() {
        if (tcpUtil != null) {
            tcpUtil.releaseResources();
            tcpUtil = null;
        }
        if (udpUtil != null) {
            udpUtil.releaseResources();
            udpUtil = null;
        }
        coreThreadPool.shutdown();
        threadPool.shutdown();
    }

    public boolean checkCanceled() {
        if (canceled) {
            Log.d(TAG, "Sending canceled by sender");
            listener.onComplete(org.exthmui.share.shared.misc.Constants.TransmissionStatus.SENDER_CANCELLED.getNumVal(),
                    null);
            return true;
        } else if (remoteCanceled) {
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

    public interface PacketFactory<T extends AbstractCommandPacket<T>> {
        T produce(DatagramPacket datagramPacket);
    }
}
