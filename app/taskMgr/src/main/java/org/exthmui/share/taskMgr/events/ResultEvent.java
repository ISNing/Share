package org.exthmui.share.taskMgr.events;

import androidx.annotation.NonNull;

import org.exthmui.share.taskMgr.Result;

import java.util.EventObject;

public class ResultEvent extends EventObject {
    @NonNull
    private final Result result;

    public ResultEvent(Object source, @NonNull Result result) {
        super(source);
        this.result = result;
    }

    @NonNull
    public Result getResult() {
        return result;
    }
}
