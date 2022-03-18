package org.exthmui.share.shared;

public class Constants {
    /**
     * First placeholder: Code of connection type
     * Second placeholder: customized identifier
     */
    public static final String PEER_ID_STRING = "%s:%s";

    public static final String WORK_NAME_PREFIX_SEND = "WORK_SEND_TO_";
    public static final String WORK_NAME_PREFIX_RECEIVE = "WORK_RECEIVE_";

    public static final String NOTIFICATION_ID_PREFIX_SEND = "NOTIFICATION_SEND";
    public static final String NOTIFICATION_ID_PREFIX_RECEIVE = "NOTIFICATION_RECEIVE";

    public static final String CONNECTION_CODE_MSNEARSHARE = "msnearshare";
    public static final String CONNECTION_CODE_WIFIDIRECT = "wifidirect";

    public static final int COMPRESS_QUALITY=40;

    public enum FileTypes{
        UNKNOWN(-1),APK(0), AUDIO(1), CERTIFICATE(2),
        CODE(3), COMPRESSED(4),CONTACT(5), EVENTS(6),
        FONT(7), IMAGE(8), PDF(9), PRESENTATION(10),
        SPREADSHEETS(11), DOCUMENTS(12), TEXT(13), VIDEO(14),
        ENCRYPTED(15), GIF(16);
        private final int numVal;

        FileTypes(int numVal) {
            this.numVal = numVal;
        }

        public static DeviceType parse(int numVal) {
            for (DeviceType o : DeviceType.values()) {
                if (o.getNumVal() == numVal) {
                    return o;
                }
            }
            return null;
        }

        public int getNumVal() {
            return numVal;
        }
    }

    public enum DeviceType {
        DESKTOP(1, R.drawable.ic_desktop),LAPTOP(2, R.drawable.ic_laptop),
        PHONE(3, R.drawable.ic_phone),TABLET(4, R.drawable.ic_tablet),
        UNKNOWN(0, R.drawable.ic_device_unknown);
        private final int numVal;
        private final int imgRes;

        DeviceType(int numVal, int imgRes) {
            this.numVal = numVal;
            this.imgRes = imgRes;
        }

        public static DeviceType parse(int numVal) {
            for (DeviceType o : DeviceType.values()) {
                if (o.getNumVal() == numVal) {
                    return o;
                }
            }
            return null;
        }

        public int getNumVal() {
            return numVal;
        }
        public int getImgRes(){
            return imgRes;
        }
    }

    public enum ConnectionStatus{
        AVAILABLE(1, R.string.connection_status_available),
        UNAVAILABLE(2, R.string.connection_status_unavailable),
        TRANSMITTING(3, R.string.connection_status_unavailable),
        UNKNOWN(0, R.string.connection_status_unknown);
        private final int numVal;
        private final int strRes;

        ConnectionStatus(int numVal, int strRes) {
            this.numVal = numVal;
            this.strRes = strRes;
        }

        public int getNumVal() {
            return numVal;
        }

        public int getStrRes() {
            return strRes;
        }
    }

    public enum TransmissionStatus{
        UNKNOWN(0),
        WAITING_FOR_REQUEST(6),
        WAITING_FOR_ACCEPTATION(5),
        REJECTED(3),
        CONNECTION_ESTABLISHED(4),
        IN_PROGRESS(2),
        COMPLETED(1),

        UNKNOWN_ERROR(10),
        NETWORK_ERROR(11),
        TIMED_OUT(111),
        SENDER_CANCELLED(121),
        RECEIVER_CANCELLED(122),
        FILE_IO_ERROR(13),
        NO_ENOUGH_SPACE(131),
        REMOTE_ERROR(14),
        PEER_DISAPPEARED(15);
        private final int numVal;

        TransmissionStatus(int numVal) {
            this.numVal = numVal;
        }

        public static DeviceType parse(int numVal) {
            for (DeviceType o : DeviceType.values()) {
                if (o.getNumVal() == numVal) {
                    return o;
                }
            }
            return null;
        }

        public int getNumVal() {
            return numVal;
        }
    }

}
