package com.iflytek.cyber.resolver.audioactivitytracker;

import android.content.Context;

import com.google.gson.JsonObject;
import com.iflytek.cyber.CyberDelegate;
import com.iflytek.cyber.resolver.ResolverModule;

import static com.iflytek.cyber.CyberDelegate.CHANNEL_ALERTS;
import static com.iflytek.cyber.CyberDelegate.CHANNEL_CONTENT;
import static com.iflytek.cyber.CyberDelegate.CHANNEL_DIALOG;

@SuppressWarnings("unused")
public class AudioActivityTracker extends ResolverModule {

    private static final String TAG = "AudioActivityTracker";

    public AudioActivityTracker(Context context, CyberDelegate delegate) {
        super(context, delegate);
    }

    @Override
    public void updateContext() {
        final JsonObject dialog = delegate.getChannelState(CHANNEL_DIALOG);
        final JsonObject alerts = delegate.getChannelState(CHANNEL_ALERTS);
        final JsonObject content = delegate.getChannelState(CHANNEL_CONTENT);

        final JsonObject payload = new JsonObject();
        boolean hasPayload = false;

        if (dialog != null) {
            payload.add("dialog", dialog);
            hasPayload = true;
        }

        if (alerts != null) {
            payload.add("alert", alerts);
            hasPayload = true;
        }

        if (content != null) {
            payload.add("content", content);
            hasPayload = true;
        }

        if (hasPayload) {
            delegate.updateContext("ActivityState", payload);
        }
    }

    @Override
    public void resolve(JsonObject header, JsonObject payload, Callback callback) {
        callback.skip();
    }

}
