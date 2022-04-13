package org.exthmui.share.shared.events;

import org.exthmui.share.shared.base.discover.Discoverer;

import java.util.EventObject;

public class DiscovererErrorOccurredEvent extends EventObject {

    private final String message;
    private final String messageLocalized;

    public DiscovererErrorOccurredEvent(Discoverer source, String message, String messageLocalized) {
        super(source);
        this.message = message;
        this.messageLocalized = messageLocalized;
    }

    public String getMessage() {
        return message;
    }

    public String getMessageLocalized() {
        return messageLocalized;
    }
}
