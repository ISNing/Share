package org.exthmui.share.shared.events;

import java.util.EventObject;

public class ReceiveActionRejectEvent extends EventObject {

    private final String code;
    private final String requestId;

    public ReceiveActionRejectEvent(Object source, String code, String requestId) {
        super(source);
        this.code = code;
        this.requestId = requestId;
    }

    public String getCode() {
        return code;
    }

    public String getRequestId() {
        return requestId;
    }
}