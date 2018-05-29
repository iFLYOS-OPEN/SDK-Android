package com.iflytek.android.os;

import android.os.Handler;
import android.os.HandlerThread;

public class ThreadedHandler extends Handler {

    private final HandlerThread thread;

    public ThreadedHandler(String name) {
        this(createThread(name));
    }

    private ThreadedHandler(HandlerThread thread) {
        super(thread.getLooper());
        this.thread = thread;
    }

    private static HandlerThread createThread(String name) {
        final HandlerThread thread = new HandlerThread(name);
        thread.start();
        return thread;
    }

    public void quit() {
        removeCallbacksAndMessages(null);
        thread.quit();
    }

}
