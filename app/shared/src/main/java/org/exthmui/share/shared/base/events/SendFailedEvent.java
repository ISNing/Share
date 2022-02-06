package org.exthmui.share.shared.base.events;

import org.exthmui.share.shared.Constants;

import java.util.EventObject;

public class SendFailedEvent extends EventObject {
    public Constants.TransmissionStatus status;
    public String reason;

    public SendFailedEvent(Object source, Constants.TransmissionStatus status, String reason) {
        super(source);
        this.status = status;
        this.reason = reason;
    }
}
