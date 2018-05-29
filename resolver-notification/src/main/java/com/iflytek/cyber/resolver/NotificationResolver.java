package com.iflytek.cyber.resolver;


import android.content.Context;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.text.TextUtils;

import com.google.gson.JsonObject;
import com.iflytek.cyber.CyberDelegate;

import java.io.IOException;

public class NotificationResolver extends ResolverModule {

    private Context context;
    private MediaPlayer player;

    public NotificationResolver(Context context, CyberDelegate delegate) {
        super(context, delegate);
        this.context = context;
    }

    @Override
    public void resolve(JsonObject header, JsonObject payload, Callback callback) {
        String name = header.get("name").getAsString();
        if (TextUtils.equals(name, "SetIndicator")) {
            boolean playAudioIndicator = payload.get("playAudioIndicator").getAsBoolean();
            if (playAudioIndicator) {
                String url = payload.get("asset").getAsJsonObject().get("url").getAsString();
                playAudio(url);
            }
        } else if (TextUtils.equals(name, "ClearIndicator")){
            clear();
        }

        callback.next();
    }

    private void playAudio(String url) {
        if (player == null) {
            player = new MediaPlayer();
        }

        if (player.isPlaying()) {
            player.stop();
            player.reset();
        }

        try {
            player.setDataSource(url);
            player.prepare();
            player.start();
        } catch (IOException e) {
            e.printStackTrace();
            playRing();
        }
    }

    private void playRing() {
        Uri uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION);
        Ringtone ringtone = RingtoneManager.getRingtone(context, uri);
        ringtone.play();
    }

    private void clear() {
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
    }
}
