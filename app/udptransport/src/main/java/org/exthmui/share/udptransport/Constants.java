package org.exthmui.share.udptransport;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exthmui.share.udptransport.exceptions.InvalidPacketException;
import org.exthmui.share.udptransport.packets.AbstractCommandPacket;
import org.exthmui.share.udptransport.packets.CommandPacket;
import org.exthmui.share.udptransport.packets.FilePacket;
import org.exthmui.share.udptransport.packets.IdentifierPacket;
import org.exthmui.share.udptransport.packets.ResendRequestPacket;

import java.lang.reflect.InvocationTargetException;
import java.net.DatagramPacket;
import java.nio.ByteOrder;
import java.util.function.Supplier;

/**
 * Packet structure:
 * Byte 1: Command
 * Byte 2: Connection Id (0 as well as {@link Byte#MIN_VALUE} is invalid)
 * Bytes 3+: Data {@link #DATA_LEN_MAX}
 *
 * @see org.exthmui.share.udptransport.packets.AbstractCommandPacket Base Packet Object
 * <p>
 *
 * +---------------------+                                                       +------------------------------+
 * |                     | ----(1) Connect TCP Socket--------------------------> |                              | \
 * |       Sender        | ----(2) Send SenderInfo(Json)-----------------------> |           Receiver           | |
 * |                     | ----(3) Send FileInfos(Json Array)------------------> |                              | +-- TCP
 * |                     | <------Send ids of files accepted(Json Array) (4)---- | (5) UDP Socket ready         | |
 * | (4-6) TCP monitoring| <------------------UDP Socket ready (Command) (6)---- |     (4-6) TCP Monitoring     | /
 * |      cancel cmd     |                                                       |          cancel cmd          |
 * |                     | ----(7) START_IDENTIFIER----------------------------> |                              | \
 * | (7-8) No ack ?      | <-------------------------START_ACK_IDENTIFER (8)---- |                              | |
 * |   sleep and resend  | ----(9) File packets--------------------------------> | (10)(13) Check packets loses | |
 * |        (counted)    | ----(10) END_IDENTIFIER-----------------------------> |              /|\             | +-- UDP
 * | (10-11) No ack ?    | <--------------------------END_ACK_IDENTIFER (11)---- |               |              | |
 * |   sleep and resend  | <----------------------Packet resend request (12)---- |               |              | |
 * |        (counted)    | ----(13) (Optional) Packet resend ------------------> +---------------+              | |
 * |                     | ----(To 7) ......------------------------------------ |                              | |
 * | (14-15) No ack ?    | ----(14) FILE_END_IDENTIFIER (After packet resend)--> |                              | |
 * |   sleep and resend  | <--------------------FILE_END_ACK_IDENTIFIER (15)---- |                              | |
 * |        (counted)    | ----(To 7) ......------------------------------------ |                              | /
 * |                     |                                                       |                              |
 * | (16) Check file     | ----(16) Send result(Json, mapping id and reason)---> |                              | --- TCP
 * |                     | --------------(17) Release resources----------------- |                              |
 * +---------------------+                                                       +------------------------------+
 */
public class Constants {
    public static final ByteOrder DEFAULT_ORDER = ByteOrder.BIG_ENDIAN;
    public static final byte PACKET_END_FLAG = 0x1;

    public enum UdpCommand {
        UNKNOWN((byte) 0x0, CommandPacket.class),
        /**
         * Command extra:
         * Byte 3: Group id
         * Byte 4-5: Packet id
         * Byte 6+: Data
         * <p>
         * File content as data
         */
        FILE_PACKET((byte) 0x1, FilePacket.class),
        /**
         * Command extra:
         * Byte 3: Identifier
         * Bytes 4-15: Reserved for identifiers
         * <p>
         * No data
         */
        IDENTIFIER((byte) 0x2, IdentifierPacket.class),
        /**
         * Command extra:
         * Byte 3: Group id
         * Byte 4+: Data
         * <p>
         * Packet ids as data
         */
        PACKET_RESEND_REQ((byte) 0x3, ResendRequestPacket.class);

        private final byte cmd;
        private final Class<? extends AbstractCommandPacket<?>> clazz;

        UdpCommand(byte cmd, Class<? extends AbstractCommandPacket<?>> clazz) {
            this.cmd = cmd;
            this.clazz = clazz;
        }

        public byte getCmd() {
            return cmd;
        }

        public Class<? extends AbstractCommandPacket<?>> getClazz() {
            return clazz;
        }

        @Nullable
        public static UdpCommand parse(byte cmd) {
            for (UdpCommand c : UdpCommand.values()) {
                if (c.getCmd() == cmd) return c;
            }
            return null;
        }

        @NonNull
        public static AbstractCommandPacket<?> parse(DatagramPacket packet) throws IllegalArgumentException {
            try {
                return new CommandPacket(packet).getCommand().getClazz().getConstructor(DatagramPacket.class).newInstance(packet);
            } catch (InvocationTargetException e) {
                if (e.getTargetException() instanceof IllegalArgumentException)
                    throw new InvalidPacketException(e);
                else throw new RuntimeException(e);
            } catch (IllegalAccessException | InstantiationException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }


    public static final String COMMAND_CANCEL = "CANCEL";
    public static final String COMMAND_UDP_SOCKET_READY = "UDP_READY";

    public static final int MAX_UDP_PACKETS_RETENTION = 2000;

    public static final int IDENTIFIER_TIMEOUT_MILLIS = 10000;
    public static final int MAX_ACK_TRYOUTS = 50;
    public static final int ACK_TIMEOUT_MILLIS = 1000;

    /**
     * The number of packet a group contains
     */
    public static final int GROUP_DATA = 30000;
    /**
     * The length of data of a packet
     */
    public static final int DATA_LEN_MAX = 1000;
    public static final int DATA_LEN_MAX_HI = 25500;
    public static final int BUF_LEN_MAX_HI = 65507;
    public static final byte START_GROUP_ID = Byte.MIN_VALUE;
    public static final byte END_GROUP_ID = Byte.MAX_VALUE;
    public static final short START_PACKET_ID = Short.MIN_VALUE;
    public static final short END_PACKET_ID = 30000;

    public static final String STRING_CHARSET = "UTF-8";

    public enum Identifier {
        START((byte) 0x0, false, () -> Identifier.valueOf("START_ACK")),
        START_ACK((byte) 0x1, true, () -> Identifier.valueOf("START")),
        GROUP_ID_RESET((byte) 0x2, false, () -> Identifier.valueOf("GROUP_ID_RESET_ACK")),
        GROUP_ID_RESET_ACK((byte) 0x3, true, () -> Identifier.valueOf("GROUP_ID_RESET")),
        /**
         * Extra define:
         * Byte 1: Group ID
         * Byte 2-3: End Packet ID
         * Byte 4: Is Resend END:          0: Is Not resend
         * Otherwise: Is resend
         */
        END((byte) 0x4, false, () -> Identifier.valueOf("END_ACK")),
        END_ACK((byte) 0x5, true, () -> Identifier.valueOf("END")),
        FILE_END((byte) 0x6, false, () -> Identifier.valueOf("FILE_END_ACK")),
        FILE_END_ACK((byte) 0x7, true, () -> Identifier.valueOf("FILE_END"));

        private final byte identifier;
        private final boolean isAck;
        private final Supplier<Identifier> correspondIdentifier;

        Identifier(byte identifier, boolean isAck, Supplier<Identifier> correspondIdentifier) {
            this.identifier = identifier;
            this.isAck = isAck;
            this.correspondIdentifier = correspondIdentifier;
        }

        public byte getIdentifier() {
            return identifier;
        }

        public boolean isAck() {
            return isAck;
        }

        public Identifier getCorrespondIdentifier() {
            return correspondIdentifier.get();
        }

        @Nullable
        public static Identifier parse(byte identifier) {
            for (Identifier i : Identifier.values()) {
                if (i.getIdentifier() == identifier) return i;
            }
            return null;
        }
    }

    /**
     * Group id and packet id to start as extra
     */
    public static final ByteUtils.Tip START_ID_GROUP_ID_TIP = new ByteUtils.Tip(0, 0);
    public static final ByteUtils.Tip START_ID_START_PACKET_ID_TIP = new ByteUtils.Tip(1, 2);

    /**
     * Group id before and after reset as extra
     * Receiver will reset current group id io {@link Byte#MIN_VALUE}
     */
    public static final ByteUtils.Tip GROUP_ID_RESET_ID_GROUP_ID_BEF_TIP = new ByteUtils.Tip(0, 0);
    public static final ByteUtils.Tip GROUP_ID_RESET_ID_GROUP_ID_AFT_TIP = new ByteUtils.Tip(1, 1);
    /**
     * Group id and end packet id to end as extra
     */
    public static final ByteUtils.Tip END_ID_GROUP_ID_TIP = new ByteUtils.Tip(0, 0);
    public static final ByteUtils.Tip END_ID_END_PACKET_ID_TIP = new ByteUtils.Tip(1, 2);

    public static final String FILE_INFO_EXTRA_KEY_MD5 = "md5";
}
