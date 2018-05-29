package com.iflytek.cyber.resolver.audioplayer.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import com.iflytek.cyber.resolver.audioplayer.service.model.AudioItem;

/**
 * AudioPlayerService for url type
 */
public class AudioPlayerService extends Service {

    public static final String EXTRA_AUDIO_ITEM = "audioItem";
    public static final String EXTRA_PLAY_BEHAVIOR = "playBehaviour";
    public static final String EXTRA_CLEAR_BEHAVIOR = "clearBehaviour";

    private static final String ACTION_PREFIX = "com.iflytek.cyber.resolver.audioplayer.action.";
    public static final String ACTION_PLAY = ACTION_PREFIX + "ACTION_PLAY";
    public static final String ACTION_STOP = ACTION_PREFIX + "ACTION_STOP";
    public static final String ACTION_CLEAR_QUEUE = ACTION_PREFIX + "ACTION_CLEAR_QUEUE";
    public static final String ACTION_PREPARED = ACTION_PREFIX + "ACTION_PREPARED";
    public static final String ACTION_SERVICE_STARTED = ACTION_PREFIX + "ACTION_SERVICE_STARTED";

    private AudioPlayer audioPlayer;

    @Override
    public IBinder onBind(Intent intent) {
        return new ServiceBinder();
    }

    public AudioPlayer getAudioPlayer() {
        return audioPlayer;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        audioPlayer = new AudioPlayer(getBaseContext());

        sendBroadcast(new Intent(ACTION_PREPARED));
        Log.d(getClass().getSimpleName(), "AudioPlayer prepared");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (audioPlayer != null)
            audioPlayer.release();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && !TextUtils.isEmpty(intent.getAction()))
            switch (intent.getAction()) {
                case ACTION_PLAY:
                    AudioItem audioItem = intent.getParcelableExtra(EXTRA_AUDIO_ITEM);
                    String playBehavior = intent.getStringExtra(EXTRA_PLAY_BEHAVIOR);
                    audioPlayer.updateAudioItem(playBehavior, audioItem);
                    Log.w(getClass().getSimpleName(), "ACTION_PLAY: " + playBehavior);
                    break;
                case ACTION_STOP:
                    audioPlayer.pause();
                    break;
                case ACTION_CLEAR_QUEUE:
                    audioPlayer.clearQueue(intent.getStringExtra(EXTRA_CLEAR_BEHAVIOR));
                    break;
            }
        sendBroadcast(new Intent(ACTION_SERVICE_STARTED));
        return super.onStartCommand(intent, flags, startId);
    }

    public class ServiceBinder extends Binder {
        public AudioPlayerService getService() {
            return AudioPlayerService.this;
        }
    }

}
