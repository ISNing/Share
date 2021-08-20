package org.exthmui.share.base;

public interface SenderInfo<T> {
    SendingWorker send(T peer, List<Entity> entities);
    void initialize();
    boolean isAvailable();
}
