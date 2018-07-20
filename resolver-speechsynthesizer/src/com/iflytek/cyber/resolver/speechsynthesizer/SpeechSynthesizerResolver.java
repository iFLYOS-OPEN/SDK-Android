package com.iflytek.cyber.resolver.speechsynthesizer;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.JsonObject;
import com.iflytek.android.io.CloseableUtil;
import com.iflytek.android.os.ThreadedHandler;
import com.iflytek.cyber.CyberDelegate;
import com.iflytek.cyber.resolver.ResolverModule;

import java.io.IOException;

import okio.Buffer;
import okio.BufferedSource;
import okio.Sink;
import okio.Source;

import static android.media.AudioFormat.CHANNEL_OUT_MONO;
import static android.media.AudioFormat.ENCODING_PCM_16BIT;

@SuppressWarnings("unused")
public class SpeechSynthesizerResolver extends ResolverModule implements
        CyberDelegate.AttachmentCallback,
        AudioManager.OnAudioFocusChangeListener {

    private static final String TAG = "SpeechSynthesizer";

    private final AudioManager audioManager;

    private final AudioTrack track;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private boolean playing = false;
    private ThreadedHandler playingHandler;

    private String cid;
    private String token;
    private Callback callback;

    private BufferedSource source;
    private Sink target;

    private OnSpeakingListener speakingListener;

    public SpeechSynthesizerResolver(Context context, CyberDelegate delegate) {
        super(context, delegate);

        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        track = new AudioTrack(
                AudioManager.STREAM_MUSIC, 16000, CHANNEL_OUT_MONO, ENCODING_PCM_16BIT,
                AudioTrack.getMinBufferSize(16000, CHANNEL_OUT_MONO, ENCODING_PCM_16BIT),
                AudioTrack.MODE_STREAM);
    }

    @Override
    public void updateContext() {
        if (!TextUtils.isEmpty(token)) {
            long offsetInMilliseconds = track.getPlaybackHeadPosition() / track.getSampleRate() * 1000;
            JsonObject payload = new JsonObject();
            payload.addProperty("token", token);
            payload.addProperty("offsetInMilliseconds", offsetInMilliseconds);
            payload.addProperty("playerActivity", playing ? "PLAYING" : "FINISHED");
            delegate.updateContext("SpeechState", payload);
        }
    }

    @Override
    public void resolve(JsonObject header, JsonObject payload, Callback callback) {
        final String name = header.get("name").getAsString();

        if (!"Speak".equals(name)) {
            callback.skip();
            return;
        }

        cleanup();

        this.cid = payload.get("url").getAsString().split(":")[1];
        this.token = payload.has("token") ? payload.get("token").getAsString() : null;
        this.callback = callback;

        delegate.queryAttachment(cid, this);
    }

    @Override
    public void onAttachment(String cid, BufferedSource source) {
        if (!cid.equals(this.cid)) {
            CloseableUtil.safeClose(source);
            return;
        }

        this.source = source;
        this.target = new AudioTrackSink(track);

        track.play();
        requestFocus();
        playing = true;

        notifySpeechStarted();

        playingHandler = new ThreadedHandler("playing");
        playingHandler.post(() -> {
            final Source decoder = new OpusDecoderSource(source);
            try {
                Log.d(TAG, "start playing");
                run(decoder);
                Log.d(TAG, "play finish");
            } catch (Exception e) {
                Log.e(TAG, "play failed", e);
            } finally {
                CloseableUtil.safeClose(decoder);
                uiHandler.post(() -> finish(cid));
            }
        });
    }

    private void finish(String cid) {
        if (!cid.equals(this.cid)) {
            return;
        }

        notifySpeechFinished();
        cleanup();
        callback.next();
    }

    @Override
    public void onNotFound(String cid) {
        if (!cid.equals(this.cid)) {
            return;
        }

        cleanup();
        callback.next();
    }

    @Override
    public void onCancel() {
        cleanup();
    }

    private void run(Source source) throws IOException {
        final Buffer buffer = new Buffer();
        long length;
        while (playing && (length = source.read(buffer, 640)) != -1) {
            target.write(buffer, length);
        }
    }

    private void cleanup() {
        playing = false;
        releaseFocus();

        uiHandler.removeCallbacksAndMessages(null);

        if (playingHandler != null) {
            playingHandler.quit();
        }

        CloseableUtil.safeClose(source);
        CloseableUtil.safeClose(target);
    }

    private void notifySpeechStarted() {
        delegate.activateChannel(CyberDelegate.CHANNEL_DIALOG);

        final JsonObject payload = new JsonObject();
        payload.addProperty("token", token);
        delegate.postEvent("SpeechStarted", payload);

        if (speakingListener != null) {
            speakingListener.onSpeaking(true);
        }
    }

    private void notifySpeechFinished() {
        delegate.activateChannel(CyberDelegate.CHANNEL_DIALOG);

        final JsonObject payload = new JsonObject();
        payload.addProperty("token", token);
        delegate.postEvent("SpeechFinished", payload);

        if (speakingListener != null) {
            speakingListener.onSpeaking(false);
        }
    }

    private void requestFocus() {
        audioManager.requestAudioFocus(this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
        Log.d(TAG, "Requested focus");
    }

    private void releaseFocus() {
        audioManager.abandonAudioFocus(this);
        Log.d(TAG, "Released focus");
    }

    @Override
    public void onAudioFocusChange(int focus) {
        Log.d(TAG, "Focus changed: " + focus);
    }

    public void setSpeakingListener(OnSpeakingListener speakingListener) {
        this.speakingListener = speakingListener;
    }

    public interface OnSpeakingListener {
        void onSpeaking(boolean speaking);
    }

}
