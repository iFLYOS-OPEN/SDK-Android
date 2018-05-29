package com.iflytek.cyber.resolver.playbackcontroller;

import android.content.Context;

import com.google.gson.JsonObject;
import com.iflytek.cyber.CyberDelegate;
import com.iflytek.cyber.resolver.ResolverModule;

public class PlaybackControllerResolver extends ResolverModule {
    public static final String PLAY_COMMAND_ISSUED = "PlayCommandIssued";
    public static final String PAUSE_COMMAND_ISSUED = "PauseCommandIssued";
    public static final String NEXT_COMMAND_ISSUED = "NextCommandIssued";
    public static final String PREVIOUS_COMMAND_ISSUED = "PreviousCommandIssued";


    public PlaybackControllerResolver(Context context, CyberDelegate delegate) {
        super(context, delegate);
    }

    @Override
    public void resolve(JsonObject header, JsonObject payload, Callback callback) {
        callback.next();
    }

    public void postCommand(String name) {
        delegate.postEvent(name, new JsonObject(), true);
    }
}
