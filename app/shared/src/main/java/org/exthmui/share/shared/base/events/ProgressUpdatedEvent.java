package org.exthmui.share.shared.base.events;

import java.util.EventObject;

public class ProgressUpdatedEvent extends EventObject {

    private final double progress;

    public ProgressUpdatedEvent(Object source, double progress) {
        super(source);
        this.progress = progress;
    }

    public double getProgress() {
        return progress;
    }
}