package com.iflytek.cyber.resolver.system;

import com.google.gson.JsonObject;
import com.iflytek.cyber.CyberDelegate;

public class SystemEventPoster {
    private static SystemEventPoster instance;

    private CyberDelegate delegate;

    private SystemEventPoster() {

    }

    protected void setup(CyberDelegate delegate) {
        this.delegate = delegate;
    }

    @SuppressWarnings("All")
    public static SystemEventPoster get() {
        if (instance == null)
            instance = new SystemEventPoster();
        return instance;
    }

    /**
     * post SynchronizeState event to synchronize all the state of Context
     */
    public static void postSynchronizeState() {
        SystemEventPoster poster = SystemEventPoster.get();
        poster.postSynchronizeStateEvent();
    }

    private void postSynchronizeStateEvent() {
        if (delegate != null) {
            delegate.postEvent(SystemResolver.NAME_SYNCHRONIZE_STATE, new JsonObject(), true);
        }
    }
}
