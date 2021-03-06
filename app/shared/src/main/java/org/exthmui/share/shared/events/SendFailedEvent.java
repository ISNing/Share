package org.exthmui.share.shared.events;

import org.exthmui.share.shared.misc.Constants;

import java.util.EventObject;

public class SendFailedEvent extends EventObject {
    public final Constants.TransmissionStatus status;
    public final String reason;

    public SendFailedEvent(Object source, Constants.TransmissionStatus status, String reason) {
        super(source);
        this.status = status;
        this.reason = reason;
    }
}
