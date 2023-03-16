package org.exthmui.share.taskMgr.events;

import android.os.Bundle;

import java.util.EventObject;

public class ProgressUpdatedEvent extends EventObject {
    private final Bundle progress;

    public ProgressUpdatedEvent(Object source, Bundle progress) {
        super(source);
        this.progress = progress;
    }

    public Bundle getProgress() {
        return progress;
    }
}
