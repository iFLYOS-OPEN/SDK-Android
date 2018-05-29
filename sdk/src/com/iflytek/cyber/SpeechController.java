package com.iflytek.cyber;

import com.google.gson.JsonObject;

import okio.Source;

@Deprecated
public interface SpeechController {

    void start(Source source, JsonObject initiator);

    void finish();

    void cancel();

}
