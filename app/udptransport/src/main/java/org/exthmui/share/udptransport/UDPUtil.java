package org.exthmui.share.udptransport;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import org.exthmui.share.shared.exceptions.trans.TimedOutException;
import org.exthmui.share.udptransport.packets.AbstractCommandPacket;
import org.exthmui.share.udptransport.packets.CommandPacket;
import org.exthmui.share.udptransport.packets.IdentifierPacket;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class UDPUtil {
    public static final String SUB_TAG = "UDPUtil";
    private static final Map<Byte, Handler> CONN_ID_HANDLER_MAP = new HashMap<>();
    private final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(0,
            2, 1L, TimeUnit.SECONDS, new SynchronousQueue<>(),
            r -> new Thread(r, r.toString()));
    private final DatagramSocket datagramSocket;
    public String TAG = SUB_TAG;
    @Nullable
    private StateChecker stateChecker;

    private boolean stopInterceptorFlag = false;

    private final Runnable packetInterceptor = () -> {
        while (!stopInterceptorFlag) {
            try {
                DatagramPacket packet = receivePacket();
                CommandPacket commandPacket = new CommandPacket(packet);
                Handler handler = CONN_ID_HANDLER_MAP.get(commandPacket.getConnId());
                if (handler != null) {
                    if (!handler.packetReceived.isDone())
                        handler.packetReceived.complete(packet);
                    else {
                        if (handler.packetsBlocked.size() == Constants.MAX_UDP_PACKETS_RETENTION) {
                            handler.packetReceived = new CompletableFuture<>();
                            handler.packetReceived.complete(handler.packetsBlocked.get(0));
                            handler.packetsBlocked.remove(0);
                        } else handler.packetsBlocked.add(packet);
                    }
                } else Log.e(TAG, String.format("Packet with connection id %d received, but no " +
                        "correspond handler found", commandPacket.getConnId()));
            } catch (SocketException e) {
                // Ignore socket closing
                if (!Objects.equals(e.getMessage(), "Socket closed"))
                    e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    public UDPUtil() throws SocketException {
        this(new DatagramSocket());
        datagramSocket.setReuseAddress(true);
    }

    public UDPUtil(int port) throws SocketException {
        this(new DatagramSocket(port));
        datagramSocket.setReuseAddress(true);
    }

    public UDPUtil(DatagramSocket socket) {
        datagramSocket = socket;
    }

    public void startListening() {
        stopInterceptorFlag = false;
        threadPool.execute(packetInterceptor);
    }

    public void stopListening() {
        stopInterceptorFlag = true;
    }

    public void connect(SocketAddress udpAddress) throws IOException {
        assert datagramSocket != null;
        datagramSocket.connect(udpAddress);
    }

    public void addHandler(@NonNull Handler handler) {
        CONN_ID_HANDLER_MAP.put(handler.getConnId(), handler);
    }

    public void addHandler(@NonNull Context context, byte connId) {
        addHandler(new Handler(context, connId));
    }

    public void removeHandler(@NonNull Handler handler) {
        CONN_ID_HANDLER_MAP.remove(handler.getConnId(), handler);
    }

    public void removeHandler(byte connId) {
        CONN_ID_HANDLER_MAP.remove(connId);
    }

    @Nullable
    public Handler getHandler(byte connId) {
        return CONN_ID_HANDLER_MAP.get(connId);
    }

    public void releaseResources() {
        stopListening();
        Utils.silentClose(datagramSocket);
        threadPool.shutdown();
        stateChecker = null;
    }

    private DatagramPacket receivePacket() throws IOException {
        assert datagramSocket != null;
        DatagramPacket p = new DatagramPacket(new byte[Constants.BUF_LEN_MAX_HI], Constants.BUF_LEN_MAX_HI);
        datagramSocket.receive(p);
        p.setData(Arrays.copyOfRange(p.getData(), p.getOffset(), p.getOffset() + p.getLength()));
        return p;
    }

    private <T extends AbstractCommandPacket<T>> T receivePacket(UDPSender.PacketFactory<T> factory) throws IOException {
        assert datagramSocket != null;
        DatagramPacket p = new DatagramPacket(new byte[Constants.BUF_LEN_MAX_HI], Constants.BUF_LEN_MAX_HI);
        datagramSocket.receive(p);
        p.setData(Arrays.copyOfRange(p.getData(), p.getOffset(), p.getOffset() + p.getLength()));
        return factory.produce(p);
    }

    private boolean checkCanceled() {
        return stateChecker != null && stateChecker.isCanceled();
    }

    public int getLocalPort() {
        return datagramSocket.getLocalPort();
    }

    public void setStateChecker(@Nullable StateChecker stateChecker) {
        this.stateChecker = stateChecker;
    }

    public void setTAG(String TAG) {
        this.TAG = String.format("%s/%s", TAG, SUB_TAG);
    }

    public interface StateChecker {
        boolean isCanceled();
    }

    public class Handler {
        public final List<DatagramPacket> packetsBlocked = new ArrayList<>(Constants.MAX_UDP_PACKETS_RETENTION);
        private final byte connId;
        @NonNull
        private final Context context;
        @NonNull
        public CompletableFuture<DatagramPacket> packetReceived = new CompletableFuture<>();

        public Handler(@NonNull Context context, byte connId) {
            this.context = context;
            this.connId = connId;
        }

        private DatagramPacket receiveBarePacket(int timeout, TimeUnit unit) throws TimeoutException {
            DatagramPacket packet = null;
            do {
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
            } while (packet == null);
            return packet;
        }

        public <T extends AbstractCommandPacket<T>> T receivePacket(UDPSender.PacketFactory<T> factory, int timeout, TimeUnit unit, int maxTryouts) throws TimedOutException {
            int tryouts = 0;
            while (true) {
                tryouts++;
                try {
                    return receivePacket(factory, timeout, unit);
                } catch (TimedOutException e) {
                    if (tryouts >= maxTryouts)
                        throw new TimedOutException(context, e);
                }
            }
        }

        public <T extends AbstractCommandPacket<T>> T receivePacket(UDPSender.PacketFactory<T> factory, int timeout, TimeUnit unit) throws TimedOutException {
            assert datagramSocket != null;
            T targetPacket = null;
            List<DatagramPacket> packetsRejected = new ArrayList<>();
            DatagramPacket datagramPacket;
            do {
                try {
                    datagramPacket = receiveBarePacket(timeout, unit);
                } catch (TimeoutException e) {
                    throw new TimedOutException(context, e);
                }
                try {
                    targetPacket = factory.produce(datagramPacket);
                } catch (IllegalArgumentException ignored) {
                    packetsRejected.add(datagramPacket);
                }
            } while (targetPacket == null);

            reinsertPackets(packetsRejected);
            return targetPacket;
        }

        private void reinsertPackets(List<DatagramPacket> packets) {
            if (!packetsBlocked.isEmpty()) {
                packetReceived.complete(packets.get(0));
                packets.remove(0);
            }
            if (!packets.isEmpty()) {
                packetsBlocked.addAll(0, packets);
            }
        }

        public <T extends AbstractCommandPacket<T>> void sendPacket(@NonNull T packet) throws IOException {
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
                IdentifierPacket recvPacket;
                try {
                    sendPacket(sendPacket);

                    if (checkCanceled()) return null;

                    recvPacket = receivePacket(IdentifierPacket::new, Constants.ACK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS, Constants.MAX_ACK_TRYOUTS);

                    Log.d(TAG,
                            String.format("Ack Identifier \"%d\" received, connId: %d", recvPacket.getIdentifier(), recvPacket.getConnId()));
                    if (recvPacket.getIdentifier() == ackIdentifier &&
                            recvPacket.getConnId() == connId) {
                        return recvPacket;
                    }
                } catch (TimedOutException e) {
                    Log.w(TAG, String.format("Ack identifier packet receiving timed out: %s", e), e);
                    throw e;
                }
            } else {
                sendPacket(sendPacket);
            }
            return null;
        }

        public IdentifierPacket receiveIdentifier(byte identifier, int timeout, TimeUnit unit) throws TimedOutException {
            return receivePacket(datagramPacket -> {
                IdentifierPacket p = new IdentifierPacket(datagramPacket);
                if (p.getIdentifier() != identifier) throw new IllegalArgumentException();
                return p;
            }, timeout, unit);
        }

        public byte getConnId() {
            return connId;
        }
    }
}
