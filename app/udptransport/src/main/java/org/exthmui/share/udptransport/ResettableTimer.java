package org.exthmui.share.udptransport;

import java.util.Timer;
import java.util.TimerTask;

public class ResettableTimer {

    private Timer timer;
    private final int delay;
    private final Runnable runnable;

    public ResettableTimer(int delay, Runnable runnable) {
        this.delay = delay;
        this.runnable = runnable;
    }

    private void start() {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                runnable.run();
            }
        }, delay, delay);
    }

    public void stop() {
        if (timer != null) timer.cancel();
        timer = null;
    }

    public void reset() {
        stop();
        start();
    }
}