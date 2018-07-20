package com.iflytek.cyber.resolver.audioplayer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.iflytek.cyber.CyberDelegate;
import com.iflytek.cyber.resolver.ResolverModule;
import com.iflytek.cyber.resolver.audioplayer.service.AudioPlayer;
import com.iflytek.cyber.resolver.audioplayer.service.AudioPlayerService;
import com.iflytek.cyber.resolver.audioplayer.service.model.AudioItem;

public class AudioPlayerResolver extends ResolverModule {
    private static final String NAME_PLAY = "Play";
    private static final String NAME_CLEAR_QUEUE = "ClearQueue";
    private static final String NAME_STOP = "Stop";

    private static final String PAYLOAD_PLAY_BEHAVIOR = "playBehavior";
    private static final String PAYLOAD_CLEAR_BEHAVIOR = "clearBehavior";
    private static final String PAYLOAD_AUDIO_ITEM = "audioItem";

    private AudioPlayerService playerService;

    public AudioPlayerService getPlayerService() {
        return playerService;
    }

    public AudioPlayerResolver(Context context, CyberDelegate delegate) {
        super(context, delegate);

        Intent player = new Intent(context, AudioPlayerService.class);
        context.bindService(player, connection, Context.BIND_AUTO_CREATE);
        DelegateStore.get().setup(delegate);
    }

    @Override
    public void updateContext() {
        if (playerService == null) {
            return;
        }

        final AudioPlayer audioPlayer = playerService.getAudioPlayer();

        if (audioPlayer == null) {
            return;
        }

        if (TextUtils.isEmpty(audioPlayer.getAudioItemId()))
            return;
        String token = audioPlayer.getAudioItemById(audioPlayer.getAudioItemId()).stream.token;
        if (!TextUtils.isEmpty(token)) {
            AudioPlayer player = playerService.getAudioPlayer();
            JsonObject payload = new JsonObject();
            payload.addProperty("token", token);
            payload.addProperty("offsetInMilliseconds", player.getCurrentPosition());
            payload.addProperty("playerActivity", getPlaybackState(player.getPlayerActivity()));
            delegate.updateContext("PlaybackState", payload);
        }
    }

    public void setupToken(String token) {
        Intent setupToken = new Intent(context, AudioPlayerService.class);
        setupToken.setAction(AudioPlayerService.ACTION_SETUP_TOKEN);
        setupToken.putExtra(AudioPlayerService.EXTRA_TOKEN, token);
        context.startService(setupToken);
    }

    private String getPlaybackState(int activity) {
        String playbackState = "IDLE";
        if (activity == AudioPlayer.ACTIVITY_PLAYING) {
            playbackState = "PLAYING";
        } else if (activity == AudioPlayer.ACTIVITY_PAUSED) {
            playbackState = "PAUSED";
        } else if (activity == AudioPlayer.ACTIVITY_STOPPED) {
            playbackState = "STOPPED";
        } else if (activity == AudioPlayer.ACTIVITY_BUFFER_UNDERRUN) {
            playbackState = "BUFFER_UNDERRUN";
        } else if (activity == AudioPlayer.ACTIVITY_FINISHED) {
            playbackState = "FINISHED";
        } else if (activity == AudioPlayer.ACTIVITY_IDLE) {
            playbackState = "IDLE";
        }
        return playbackState;
    }

    @Override
    public void resolve(JsonObject header, JsonObject payload, Callback callback) {
        switch (header.get("name").getAsString()) {
            case NAME_PLAY:
                Intent play = new Intent(context, AudioPlayerService.class);
                play.setAction(AudioPlayerService.ACTION_PLAY);
                JsonObject audioItemJson = payload.getAsJsonObject(PAYLOAD_AUDIO_ITEM);
                AudioItem audioItem = new Gson().fromJson(audioItemJson, AudioItem.class);
                play.putExtra(AudioPlayerService.EXTRA_AUDIO_ITEM, audioItem);
                play.putExtra(AudioPlayerService.EXTRA_PLAY_BEHAVIOR,
                        payload.get(PAYLOAD_PLAY_BEHAVIOR).getAsString());
                context.startService(play);
                break;
            case NAME_CLEAR_QUEUE:
                Intent clearQueue = new Intent(context, AudioPlayerService.class);
                clearQueue.setAction(AudioPlayerService.ACTION_CLEAR_QUEUE);
                clearQueue.putExtra(AudioPlayerService.EXTRA_CLEAR_BEHAVIOR,
                        payload.get(PAYLOAD_CLEAR_BEHAVIOR).getAsString());
                context.startService(clearQueue);
                break;
            case NAME_STOP:
                Intent stop = new Intent(context, AudioPlayerService.class);
                stop.setAction(AudioPlayerService.ACTION_STOP);
                context.startService(stop);
                break;
            default:
                callback.skip();
                return;
        }
        callback.next();
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            final AudioPlayerService.ServiceBinder binder = (AudioPlayerService.ServiceBinder) service;
            playerService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            playerService = null;
        }
    };
}
