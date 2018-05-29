package com.iflytek.cyber.resolver.speaker;

import android.content.Context;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;

import com.google.gson.JsonObject;
import com.iflytek.cyber.CyberDelegate;
import com.iflytek.cyber.resolver.ResolverModule;

public class SpeakerResolver extends ResolverModule {
    private static final String NAME_SET_MUTE = "SetMute";
    private static final String NAME_SET_VOLUME = "SetVolume";
    private static final String NAME_ADJUST_VOLUME = "AdjustVolume";
    private static final String NAME_VOLUME_CHANGED = "VolumeChanged";

    private static final String PAYLOAD_VOLUME = "volume";
    private static final String PAYLOAD_MUTE = "mute";
    private static final String PAYLOAD_MUTED = "muted";

    private AudioManager audioManager;
    private VolumeChangeReceiver receiver;

    public SpeakerResolver(Context context, CyberDelegate delegate) {
        super(context, delegate);
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        receiver = new VolumeChangeReceiver(delegate);
    }

    @Override
    public void onCreate() {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(VolumeChangeReceiver.VOLUME_CHANGED_ACTION);
        filter.addAction(VolumeChangeReceiver.STREAM_MUTE_CHANGED_ACTION);
        context.registerReceiver(receiver, filter);
    }

    @Override
    public void onDestroy() {
        context.unregisterReceiver(receiver);
    }

    @Override
    public void updateContext() {
        JsonObject payload = new JsonObject();
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        payload.addProperty("volume", (int) (100f * volume / max));
        if (Build.VERSION.SDK_INT >= 23)
            payload.addProperty(PAYLOAD_MUTED, audioManager.isStreamMute(AudioManager.STREAM_MUSIC));
        else
            payload.addProperty(PAYLOAD_MUTED, volume == 0);
        delegate.updateContext("VolumeState", payload);
    }

    @Override
    public void resolve(JsonObject header, JsonObject payload, Callback callback) {
        if (audioManager == null) {
            Log.e(getClass().getSimpleName(), "Cannot get AudioManager");
            callback.skip();
            // send directive not resolve event
            return;
        }
        if (audioManager.isVolumeFixed()) {
            // Indicates if the device implements a fixed volume policy.
            sendChangeEvent(NAME_VOLUME_CHANGED);
        } else {
            switch (header.get("name").getAsString()) {
                case NAME_SET_MUTE:
                    boolean mute = payload.get(PAYLOAD_MUTE).getAsBoolean();
                    if (Build.VERSION.SDK_INT < 23) {
                        audioManager.setStreamMute(AudioManager.STREAM_MUSIC, mute);
                    } else {
                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                                mute ? AudioManager.ADJUST_MUTE : AudioManager.ADJUST_UNMUTE, 0);
                    }
                    break;
                case NAME_SET_VOLUME:
                    int volume = payload.get(PAYLOAD_VOLUME).getAsInt();
                    // volume as type Long and value between 0 and 100,
                    // but we only need Integer
                    int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    volume = (int) (1f * volume / 100 * max);
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                            volume, 0);
                    break;
                case NAME_ADJUST_VOLUME:
                    int adjust = payload.get(PAYLOAD_VOLUME).getAsInt();
                    // volume as type Long and value between -100 and 100,
                    // but we only need Integer
                    int maxV = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    int currentV = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    adjust = (int) (1f * adjust / 100 * maxV);
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                            Math.min(Math.max(currentV + adjust, 0), maxV), 0);
                    break;
                default:
                    callback.skip();
                    return;
            }
        }
        callback.next();
    }

    private void sendChangeEvent(String name) {
        JsonObject payload = new JsonObject();
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        payload.addProperty(PAYLOAD_VOLUME, (int) (100f * volume / max));
        if (Build.VERSION.SDK_INT >= 23)
            payload.addProperty(PAYLOAD_MUTED, audioManager.isStreamMute(AudioManager.STREAM_MUSIC));
        else
            payload.addProperty(PAYLOAD_MUTED, volume == 0);
        delegate.postEvent(name, payload);
    }
}
