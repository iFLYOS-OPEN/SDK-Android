package com.iflytek.cyber.resolver.visualactivitytracker;

import android.content.Context;

import com.google.gson.JsonObject;
import com.iflytek.cyber.CyberDelegate;
import com.iflytek.cyber.resolver.ResolverModule;

import static com.iflytek.cyber.CyberDelegate.CHANNEL_VISUAL;

@SuppressWarnings("unused")
public class VisualActivityTracker extends ResolverModule {

    private static final String TAG = "VisualActivityTracker";

    public VisualActivityTracker(Context context, CyberDelegate delegate) {
        super(context, delegate);
    }

    @Override
    public void updateContext() {
        final JsonObject visual = delegate.getChannelState(CHANNEL_VISUAL);
        if (visual != null) {
            final JsonObject payload = new JsonObject();
            payload.add("focused", visual);
            delegate.updateContext("ActivityState", payload);
        } else {
            delegate.removeContext("ActivityState");
        }
    }

    @Override
    public void resolve(JsonObject header, JsonObject payload, Callback callback) {
        callback.skip();
    }

}
