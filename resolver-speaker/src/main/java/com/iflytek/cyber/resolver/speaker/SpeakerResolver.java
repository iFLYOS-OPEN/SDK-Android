package com.iflytek.cyber.resolver.speaker;

import android.content.Context;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.util.Log;

import com.google.gson.JsonObject;
import com.iflytek.cyber.CyberDelegate;
import com.iflytek.cyber.resolver.ResolverModule;

import static android.media.AudioManager.STREAM_MUSIC;

public class SpeakerResolver extends ResolverModule {

    private static final String TAG = "SpeakerResolver";

    private static final String NAME_SET_MUTE = "SetMute";
    private static final String NAME_SET_VOLUME = "SetVolume";
    private static final String NAME_ADJUST_VOLUME = "AdjustVolume";

    private static final String NAME_VOLUME_CHANGED = "VolumeChanged";
    private static final String NAME_MUTE_CHANGED = "MuteChanged";

    private static final String PAYLOAD_VOLUME = "volume";
    private static final String PAYLOAD_MUTE = "mute";   // For directive
    private static final String PAYLOAD_MUTED = "muted"; // For event

    private final AudioManager am;
    private final VolumeChangeReceiver receiver;

    private OnVolumeChangedListener volumeListener;

    public SpeakerResolver(Context context, CyberDelegate delegate) {
        super(context, delegate);
        am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        receiver = new VolumeChangeReceiver(this);
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
        if (am == null) {
            Log.e(TAG, "AudioManager is null");
            return;
        }

        final JsonObject payload = new JsonObject();
        payload.addProperty(PAYLOAD_VOLUME, AudioManagerCompat.getStreamVolumePercent(
                am, STREAM_MUSIC));
        payload.addProperty(PAYLOAD_MUTED, AudioManagerCompat.isStreamMute(
                am, STREAM_MUSIC));
        delegate.updateContext("VolumeState", payload);
    }

    @Override
    public void resolve(JsonObject header, JsonObject payload, Callback callback) {
        if (am == null) {
            Log.e(TAG, "AudioManager is null");
            callback.skip();
            return;
        }

        switch (header.get("name").getAsString()) {
            case NAME_SET_MUTE:
                final boolean mute = payload.get(PAYLOAD_MUTE).getAsBoolean();
                AudioManagerCompat.setStreamMute(am, STREAM_MUSIC, mute);
                if (am.isVolumeFixed()) {
                    sendChangeEvent(NAME_VOLUME_CHANGED);
                    callback.next();
                    return;
                }
                callback.next();
                break;
            case NAME_SET_VOLUME:
                final int volume = payload.get(PAYLOAD_VOLUME).getAsInt();
                AudioManagerCompat.setStreamVolumePercent(am, STREAM_MUSIC, volume);
                if (am.isVolumeFixed()) {
                    sendChangeEvent(NAME_VOLUME_CHANGED);
                    callback.next();
                    return;
                }
                callback.next();
                break;
            case NAME_ADJUST_VOLUME:
                final int adjust = payload.get(PAYLOAD_VOLUME).getAsInt();
                AudioManagerCompat.adjustStreamVolumePercent(am, STREAM_MUSIC, adjust);
                callback.next();
                break;
            default:
                callback.skip();
                break;
        }
    }

    void onVolumeChanged() {
        sendChangeEvent(NAME_VOLUME_CHANGED);
    }

    void onMuteChanged() {
        sendChangeEvent(NAME_MUTE_CHANGED);
    }

    private void sendChangeEvent(String name) {
        final int volume = AudioManagerCompat.getStreamVolumePercent(am, STREAM_MUSIC);
        final boolean muted = AudioManagerCompat.isStreamMute(am, STREAM_MUSIC);

        updateContext();

        JsonObject payload = new JsonObject();
        payload.addProperty(PAYLOAD_VOLUME, volume);
        payload.addProperty(PAYLOAD_MUTED, muted);
        delegate.postEvent(name, payload, true);

        if (volumeListener != null) {
            volumeListener.onVolumeChanged(muted ? 0 : volume);
        }
    }

    public void setVolumeListener(OnVolumeChangedListener volumeListener) {
        this.volumeListener = volumeListener;
    }

    public interface OnVolumeChangedListener {
        void onVolumeChanged(int volume);
    }

}
