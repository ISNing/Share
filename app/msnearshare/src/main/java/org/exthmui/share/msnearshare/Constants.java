package org.exthmui.share.msnearshare;

import com.microsoft.connecteddevices.remotesystems.commanding.nearshare.NearShareStatus;

import org.exthmui.share.shared.exceptions.trans.ReceiverCancelledException;
import org.exthmui.share.shared.exceptions.trans.TimedOutException;
import org.exthmui.share.shared.exceptions.trans.TransmissionException;
import org.exthmui.share.shared.exceptions.trans.UnknownErrorException;

import java.util.HashMap;

public abstract class Constants {
    public static final HashMap<NearShareStatus, org.exthmui.share.shared.misc.Constants.TransmissionStatus> STATUS_MAPPING = new HashMap<>() {
        {
            put(NearShareStatus.UNKNOWN, org.exthmui.share.shared.misc.Constants.TransmissionStatus.UNKNOWN_ERROR);
            put(NearShareStatus.COMPLETED, org.exthmui.share.shared.misc.Constants.TransmissionStatus.COMPLETED);
            put(NearShareStatus.IN_PROGRESS, org.exthmui.share.shared.misc.Constants.TransmissionStatus.IN_PROGRESS);
            put(NearShareStatus.TIMED_OUT, org.exthmui.share.shared.misc.Constants.TransmissionStatus.TIMED_OUT);
            put(NearShareStatus.CANCELLED, org.exthmui.share.shared.misc.Constants.TransmissionStatus.RECEIVER_CANCELLED);
            put(NearShareStatus.DENIED_BY_REMOTE_SYSTEM, org.exthmui.share.shared.misc.Constants.TransmissionStatus.REJECTED);
        }
    };
    public static final HashMap<NearShareStatus, Class<? extends TransmissionException>> EXCEPTION_MAPPING = new HashMap<>() {
        {
            put(NearShareStatus.UNKNOWN, UnknownErrorException.class);
            put(NearShareStatus.TIMED_OUT, TimedOutException.class);
            put(NearShareStatus.CANCELLED, ReceiverCancelledException.class);
        }
    };
}
