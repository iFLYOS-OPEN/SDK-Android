package com.iflytek.cyber.platform;

/**
 * 唤醒词检测器抽象
 */
public abstract class WakeWordDetector {

    protected final Listener listener;

    public WakeWordDetector(Listener listener) {
        this.listener = listener;
    }

    public abstract void create();

    public abstract void destroy();

    public abstract void start();

    public abstract void stop();

    public abstract boolean write(byte[] data, int length);

    public interface Listener {
        void onWakeup(long beginMs, long endMs);
    }

}
