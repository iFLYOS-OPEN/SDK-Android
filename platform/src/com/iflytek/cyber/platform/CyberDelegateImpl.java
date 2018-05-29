package com.iflytek.cyber.platform;

import com.google.gson.JsonObject;
import com.iflytek.cyber.CyberDelegate;
import com.iflytek.cyber.SpeechController;

import okio.Source;

public class CyberDelegateImpl extends CyberDelegate {

    private final CyberCore core;
    private final String namespace;

    public CyberDelegateImpl(CyberCore core, String namespace) {
        this.core = core;
        this.namespace = namespace;
    }

    @Override
    public void updateContext(String name, JsonObject payload) {
        core.updateContext(namespace, name, payload);
    }

    @Override
    public void removeContext(String name) {
        core.removeContext(namespace, name);
    }

    @Override
    public void postEvent(String name, JsonObject payload, boolean withContext) {
        core.postEvent(namespace, name, payload, withContext);
    }

    @Override
    public void postEvent(String name, JsonObject payload, Source audio) {
        core.postEvent(namespace, name, payload, audio);
    }

    @Override
    public void queryAttachment(String cid, AttachmentCallback callback) {
        core.queryAttachment(cid, callback);
    }

    @Override
    public void setEndpoint(String endpoint) {
        core.setEndpoint(endpoint);
    }

    @Override
    @Deprecated
    public void registerSpeechController(SpeechController controller) {
        core.setSpeechController(controller);
    }

    @Override
    public void stopCapture() {
        core.stopCapture();
    }

    @Override
    public void expectSpeech(JsonObject initiator) {
        core.expectSpeech(initiator);
    }

}
