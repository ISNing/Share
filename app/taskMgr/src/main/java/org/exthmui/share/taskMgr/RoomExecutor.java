package org.exthmui.share.taskMgr;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class RoomExecutor {

    private final Executor mIOExecutor;

    public RoomExecutor() {
        mIOExecutor = Executors.newSingleThreadExecutor();
    }

    public Executor getDiskIO() {
        return mIOExecutor;
    }

}