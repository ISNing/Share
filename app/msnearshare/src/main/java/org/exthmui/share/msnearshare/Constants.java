package org.exthmui.share.msnearshare;

import com.microsoft.connecteddevices.remotesystems.commanding.nearshare.NearShareStatus;

import java.util.HashMap;

public abstract class Constants {
    public static final HashMap<NearShareStatus, org.exthmui.share.shared.Constants.TransmissionStatus> STATUS_MAPPING = new HashMap<>() {
        {
            put(NearShareStatus.UNKNOWN, org.exthmui.share.shared.Constants.TransmissionStatus.UNKNOWN_ERROR);
            put(NearShareStatus.COMPLETED, org.exthmui.share.shared.Constants.TransmissionStatus.COMPLETED);
            put(NearShareStatus.IN_PROGRESS, org.exthmui.share.shared.Constants.TransmissionStatus.IN_PROGRESS);
            put(NearShareStatus.TIMED_OUT, org.exthmui.share.shared.Constants.TransmissionStatus.TIMED_OUT);
            put(NearShareStatus.CANCELLED, org.exthmui.share.shared.Constants.TransmissionStatus.RECEIVER_CANCELLED);
            put(NearShareStatus.DENIED_BY_REMOTE_SYSTEM, org.exthmui.share.shared.Constants.TransmissionStatus.REJECTED);
        }
    };
}
