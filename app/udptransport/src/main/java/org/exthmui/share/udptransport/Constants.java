package org.exthmui.share.udptransport;

import java.nio.ByteOrder;

/**
 * Packet structure:
 * Byte 1: Command
 * Byte 2: Connection Id (0 as well as {@link Byte#MIN_VALUE} is invalid)
 * Bytes 3+: Data {@link #DATA_LEN_MAX}
 *
 * @see org.exthmui.share.udptransport.packets.AbstractCommandPacket Base Packet Object
 *
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


    public static final String COMMAND_CANCEL = "CANCEL";
    public static final String COMMAND_UDP_SOCKET_READY = "UDP_READY";

    /**
     * Command extra:
     * Byte 3: Group id
     * Byte 4-5: Packet id
     * File content as data
     */
    public static final byte COMMAND_FILE_PACKET = 0x0;
    /**
     * Command extra:
     * Byte 3: Identifier
     * Bytes 4-5: Reserved for identifiers
     */
    public static final byte COMMAND_IDENTIFIER = 0x1;
    /**
     * Byte 3: Group id
     * Packet ids as data
     */
    public static final byte COMMAND_PACKET_RESEND_REQ = 0x3;

    public static final int IDENTIFIER_TIMEOUT_MILLIS = 5000;
    public static final int MAX_ACK_TRYOUTS = 5;
    public static final int ACK_TIMEOUT_MILLIS = 500;

    /**
     * The number of packet a group contains
     */
    public static final int GROUP_DATA = 30000;
    /**
     * The length of data of a packet
     */
    public static final int DATA_LEN_MAX = 1000;
    public static final int DATA_LEN_MAX_HI = 1400;

    public static final String STRING_CHARSET = "UTF-8";

    /**
     * Group id to start as extra
     */
    public static final byte START_IDENTIFIER = 0x0;
    public static final byte START_ACK_IDENTIFIER = 0x1;

    /**
     * Group id before reset as extra
     * Receiver will reset current group id io {@link Byte#MIN_VALUE}
     */
    public static final byte GROUP_ID_RESET_IDENTIFIER = 0x2;
    public static final byte GROUP_ID_RESET_ACK_IDENTIFIER = 0x3;

    /**
     * Group id to end as extra
     */
    public static final byte END_IDENTIFIER = 0x4;
    public static final byte END_ACK_IDENTIFIER = 0x5;

    public static final byte FILE_END_IDENTIFIER = 0x6;
    public static final byte FILE_END_ACK_IDENTIFIER = 0x7;
}
