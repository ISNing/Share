package org.exthmui.share.shared.misc;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import org.exthmui.share.shared.R;

public abstract class Constants {
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

    public static final String NOTIFICATION_PROGRESS_CHANNEL_GROUP_ID = "org.exthmui.share.notification.channel.group.PROGRESSES";
    public static final String NOTIFICATION_SERVICE_CHANNEL_GROUP_ID = "org.exthmui.share.notification.channel.group.SERVICES";
    public static final String NOTIFICATION_REQUEST_CHANNEL_GROUP_ID = "org.exthmui.share.notification.channel.group.PROGRESSES";

    public enum FileType {
        UNKNOWN(-1), APK(0), AUDIO(1), CERTIFICATE(2),
        CODE(3), COMPRESSED(4), CONTACT(5), EVENTS(6),
        FONT(7), IMAGE(8), PDF(9), PRESENTATION(10),
        SPREADSHEETS(11), DOCUMENTS(12), TEXT(13), VIDEO(14),
        ENCRYPTED(15), GIF(16);
        private final int numVal;

        FileType(int numVal) {
            this.numVal = numVal;
        }

        public static FileType parse(int numVal) {
            for (FileType o : FileType.values()) {
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
        DESKTOP(1, R.string.device_type_desktop, R.drawable.ic_desktop), LAPTOP(2, R.string.device_type_laptop, R.drawable.ic_laptop),
        PHONE(3, R.string.device_type_phone, R.drawable.ic_phone), TABLET(4, R.string.device_type_tablet, R.drawable.ic_tablet),
        UNKNOWN(0, R.string.device_type_unknown, R.drawable.ic_device_unknown);
        private final int numVal;
        @StringRes
        private final int friendlyNameRes;
        @DrawableRes
        private final int imgRes;

        DeviceType(int numVal, @StringRes int friendlyNameRes, @DrawableRes int imgRes) {
            this.numVal = numVal;
            this.friendlyNameRes = friendlyNameRes;
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

        public int getFriendlyNameRes() {
            return friendlyNameRes;
        }

        public int getImgRes() {
            return imgRes;
        }
    }

    public enum ConnectionStatus {
        AVAILABLE(1, R.string.connection_status_available),
        UNAVAILABLE(2, R.string.connection_status_unavailable),
        TRANSMITTING(3, R.string.connection_status_transmitting),
        UNKNOWN(0, R.string.connection_status_unknown);
        private final int numVal;
        @StringRes
        private final int strRes;

        ConnectionStatus(int numVal, @StringRes int strRes) {
            this.numVal = numVal;
            this.strRes = strRes;
        }

        public int getNumVal() {
            return numVal;
        }

        @StringRes
        public int getStrRes() {
            return strRes;
        }
    }

    public enum TransmissionStatus {
        UNKNOWN(0, R.string.transmission_status_unknown, R.string.transmission_status_unknown_detailed),
        INITIALIZING(1, R.string.transmission_status_initializing, R.string.transmission_status_initializing_detailed),
        WAITING_FOR_REQUEST(7, R.string.transmission_status_waiting_for_request, R.string.transmission_status_waiting_for_request_detailed),
        WAITING_FOR_ACCEPTATION(6, R.string.transmission_status_waiting_for_acceptation, R.string.transmission_status_waiting_for_acceptation_detailed),
        REJECTED(4, R.string.transmission_status_rejected, R.string.transmission_status_rejected_detailed),
        CONNECTION_ESTABLISHED(5, R.string.transmission_status_connection_established, R.string.transmission_status_connection_established_detailed),
        IN_PROGRESS(3, R.string.transmission_status_in_progress, R.string.transmission_status_in_progress_detailed),
        COMPLETED(2, R.string.transmission_status_completed, R.string.transmission_status_completed_detailed),

        UNKNOWN_ERROR(10, R.string.transmission_status_unknown_error, R.string.transmission_status_unknown_error_detailed),
        NETWORK_ERROR(11, R.string.transmission_status_network_error, R.string.transmission_status_network_error_detailed),
        TIMED_OUT(111, R.string.transmission_status_timed_out, R.string.transmission_status_timed_out_detailed),
        CANCELLED(12, R.string.transmission_status_cancelled, R.string.transmission_status_cancelled_detailed),
        SENDER_CANCELLED(121, R.string.transmission_status_sender_cancelled, R.string.transmission_status_sender_cancelled_detailed),
        RECEIVER_CANCELLED(122, R.string.transmission_status_receiver_cancelled, R.string.transmission_status_receiver_cancelled_detailed),
        FILE_IO_ERROR(13, R.string.transmission_status_file_io_error, R.string.transmission_status_file_io_error_detailed),
        NO_ENOUGH_SPACE(131, R.string.transmission_status_no_enough_space, R.string.transmission_status_no_enough_space_detailed),
        REMOTE_ERROR(14, R.string.transmission_status_remote_error, R.string.transmission_status_remote_error_detailed),
        PEER_DISAPPEARED(15, R.string.transmission_status_peer_disappeared, R.string.transmission_status_peer_disappeared_detailed);
        private final int numVal;
        @StringRes
        private final int strRes;
        @StringRes
        private final int strResDetailed;

        TransmissionStatus(int numVal, @StringRes int friendlyStringRes, int strResDetailed) {
            this.numVal = numVal;
            this.strRes = friendlyStringRes;
            this.strResDetailed = strResDetailed;
        }

        @NonNull
        public static TransmissionStatus parse(int numVal) {
            for (TransmissionStatus o : TransmissionStatus.values()) {
                if (o.getNumVal() == numVal) {
                    return o;
                }
            }
            return UNKNOWN;
        }

        public int getNumVal() {
            return numVal;
        }

        @StringRes
        public int getStrRes() {
            return strRes;
        }

        @StringRes
        public int getStrResDetailed() {
            return strResDetailed;
        }
    }
}
