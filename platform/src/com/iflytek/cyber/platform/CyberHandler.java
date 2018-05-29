package com.iflytek.cyber.platform;

import com.google.gson.JsonObject;

public interface CyberHandler {

    void setEndpoint(String endpoint);

    void stopCapture();

    void expectSpeech(JsonObject initiator);

    void onRecognizeStart(String dialogRequestId);

    void onRecognizeDirective(String dialogRequestId, JsonObject directive);

    void onRecognizeFailure(String dialogRequestId, Throwable e);

    void onRecognizeEnd(String dialogRequestId);

    void onDialogResolved();

    void onEventDirective(JsonObject directive);

    void onEventFailure(Throwable e);

    void onEventEnd();

}
