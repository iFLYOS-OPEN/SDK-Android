package com.iflytek.cyber.resolver.audioplayer.service;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;

import com.google.android.exoplayer2.ExoPlayer;

public class ProgressHandler extends Handler {
    private static final int MSG_UPDATE = 0x1001;
    private static final int MSG_UPDATE_PROGRESS_EVENT = 0x1002;
    private static final int MSG_UPDATE_CALLBACK = 0x1003;
    private static final int PROGRESS_DELAYED = 1;
    private static final int PROGRESS_INTERVAL = 2;
    private static final String INTERVAL = "interval";

    private static final long DELAY = 100;
    private static final long DELAY_CALLBACK = 1000;

    private ExoPlayer player;
    private String currentAudioItemId = null; // to mark the current audio item

    private HandlerCallback onPositionUpdateListener;

    ProgressHandler(@NonNull ExoPlayer mediaPlayer,
                    @NonNull HandlerCallback onPositionUpdateListener) {
        this.onPositionUpdateListener = onPositionUpdateListener;
        player = mediaPlayer;
    }

    void updateAudioItemId(String audioItemId) {
        currentAudioItemId = audioItemId;
    }

    void setProgressReportDelayed(String audioItem, long delay) {
        Message message = Message.obtain();
        message.what = MSG_UPDATE_PROGRESS_EVENT;
        message.arg1 = PROGRESS_DELAYED;
        message.obj = audioItem;
        sendMessageDelayed(message, delay);
    }

    void setProgressReportInterval(String audioItem, long interval) {
        Message message = Message.obtain();
        message.what = MSG_UPDATE_PROGRESS_EVENT;
        message.arg1 = PROGRESS_INTERVAL;
        Bundle bundle = new Bundle();
        bundle.putLong(INTERVAL, interval);
        message.setData(bundle);
        message.obj = audioItem;
        sendMessageDelayed(message, interval);
    }

    @Override
    public void handleMessage(Message msg) {
        if (player == null)
            return;
        try {
            switch (msg.what) {
                case MSG_UPDATE:
                    onPositionUpdateListener.onPositionUpdated(player.getCurrentPosition());
                    startUpdating(DELAY);
                    break;
                case MSG_UPDATE_PROGRESS_EVENT:
                    String audioItem = msg.obj.toString();
                    if (audioItem.equals(currentAudioItemId)) {
                        if (msg.arg1 == PROGRESS_INTERVAL) {
                            onPositionUpdateListener.onProgressReportInterval(player.getCurrentPosition());
                            long interval = msg.getData().getLong(INTERVAL);
                            sendMessageDelayed(msg, interval);
                        } else if (msg.arg1 == PROGRESS_DELAYED) {
                            onPositionUpdateListener.onProgressReportDelay(player.getCurrentPosition());
                        }
                    }
                    break;
                case MSG_UPDATE_CALLBACK:
                    if (onPositionUpdateListener != null)
                        onPositionUpdateListener.onPositionUpdatedCallback(player.getCurrentPosition());
                    sendEmptyMessageDelayed(MSG_UPDATE_CALLBACK, DELAY_CALLBACK);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void startUpdating() {
        sendEmptyMessage(MSG_UPDATE_CALLBACK);
    }

    private void startUpdating(long delay) {
        if (delay == 0)
            startUpdating();
        Message message = Message.obtain();
        message.what = MSG_UPDATE;
        sendMessageDelayed(message, delay);
    }

    public void startUpdatingCallback() {
        Message message = Message.obtain();
        message.what = MSG_UPDATE_CALLBACK;
        sendMessage(message);
    }

    public interface HandlerCallback {
        /**
         * update current position of MediaPlayer whatever player is not stopped
         *
         * @param position current position of MediaPlayer in milliseconds, if player is stopped position would be 0.
         */
        void onPositionUpdated(long position);

        void onPositionUpdatedCallback(long position);

        void onProgressReportDelay(long offsetInMilliseconds);

        void onProgressReportInterval(long offsetInMilliseconds);
    }
}
