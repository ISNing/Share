package org.exthmui.share.taskMgr.events;

import android.os.Bundle;

import androidx.annotation.NonNull;

import java.util.EventObject;

public class ProgressUpdatedEvent extends EventObject {
    @NonNull
    private final Bundle progress;

    public ProgressUpdatedEvent(Object source, @NonNull Bundle progress) {
        super(source);
        this.progress = progress;
    }

    @NonNull
    public Bundle getProgress() {
        return progress;
    }
}
