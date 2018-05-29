package com.iflytek.cyber.resolver.speaker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.text.TextUtils;

import com.google.gson.JsonObject;
import com.iflytek.cyber.CyberDelegate;

/**
 * Receive action while volume changing by physical button
 */
class VolumeChangeReceiver extends BroadcastReceiver {

    static final String STREAM_MUTE_CHANGED_ACTION = "android.media.STREAM_MUTE_CHANGED_ACTION";
    static final String VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION";

    private static final String EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE";

    private static final String NAME_VOLUME_CHANGED = "VolumeChanged";
    private static final String NAME_MUTE_CHANGED = "MuteChanged";

    private static final String PAYLOAD_VOLUME = "volume";
    private static final String PAYLOAD_MUTE = "mute";

    private final CyberDelegate delegate;

    VolumeChangeReceiver(CyberDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || TextUtils.isEmpty(intent.getAction()))
            return;
        switch (intent.getAction()) {
            case VOLUME_CHANGED_ACTION: {
                int type = intent.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, -1);
                if (type == AudioManager.STREAM_MUSIC) {
                    sendChangeEvent(context, NAME_VOLUME_CHANGED);
                }
            }
            break;
            case STREAM_MUTE_CHANGED_ACTION: {
                int type = intent.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, -1);
                if (type == AudioManager.STREAM_MUSIC) {
                    sendChangeEvent(context, NAME_MUTE_CHANGED);
                }
            }
            break;
        }
    }

    private void sendChangeEvent(Context context, String name) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null)
            return;
        JsonObject payload = new JsonObject();
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        payload.addProperty(PAYLOAD_VOLUME, (int) (100f * volume / max));
        if (Build.VERSION.SDK_INT >= 23)
            payload.addProperty(PAYLOAD_MUTE, audioManager.isStreamMute(AudioManager.STREAM_MUSIC));
        else
            payload.addProperty(PAYLOAD_MUTE, volume == 0);
        delegate.postEvent(name, payload);
    }
}
