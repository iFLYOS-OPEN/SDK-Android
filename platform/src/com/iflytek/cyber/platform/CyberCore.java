/*
 * Copyright (C) 2018 iFLYTEK CO.,LTD.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iflytek.cyber.platform;

import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.iflytek.cyber.CyberDelegate;
import com.iflytek.cyber.Entities;
import com.iflytek.cyber.SpeechController;
import com.iflytek.cyber.platform.client.CyberClient;
import com.iflytek.cyber.platform.client.CyberOkHttpClient;

import java.util.UUID;

import okio.BufferedSource;
import okio.Source;

public class CyberCore {

    private static final String TAG = "CyberCore";

    private final CyberHandler core;

    private final AttachmentManager attachmentManager = new AttachmentManager();
    private final ContextManager contextManager = new ContextManager();
    private final FocusManager focusManager = new FocusManager();

    private SpeechController speechController = null;

    public CyberCore(CyberHandler core) {
        this.core = core;
    }

    public void destroy() {
        attachmentManager.destroy();
    }

    void setEndpoint(String endpoint) {
        core.setEndpoint(endpoint);
    }

    public void queryAttachment(String cid, CyberDelegate.AttachmentCallback callback) {
        attachmentManager.query(cid, callback);
    }

    public void attachmentReady(String cid, BufferedSource source) {
        attachmentManager.ready(cid, source);
    }

    public void updateContext(String namespace, String name, JsonObject payload) {
        contextManager.update(namespace, name, payload);
    }

    public void removeContext(String namespace, String name) {
        contextManager.remove(namespace, name);
    }

    public void activateChannel(String channel, String interfaceName) {
        focusManager.activate(channel, interfaceName);
    }

    public void deactivateChannel(String channel, String interfaceName) {
        focusManager.deactivate(channel, interfaceName);
    }

    public JsonObject getChannelState(String channel) {
        return focusManager.get(channel);
    }

    @Deprecated
    void setSpeechController(SpeechController speechController) {
        this.speechController = speechController;
    }

    @Deprecated
    public void startRecognize(Source source, JsonObject initiator) {
        speechController.start(source, initiator);
    }

    @Deprecated
    public void finishRecognize() {
        speechController.finish();
    }

    @Deprecated
    public void cancelRecognize() {
        CyberOkHttpClient.getClient().cancelEvent();
    }

    void stopCapture() {
        core.stopCapture();
    }

    void expectSpeech(JsonObject initiator) {
        core.expectSpeech(initiator);
    }

    public void dialogResolved() {
        core.onDialogResolved();
    }

    public void postEvent(String namespace, String name, JsonObject payload, boolean withContext) {
        final JsonObject envelope = new JsonObject();
        envelope.add("context", withContext ? contextManager.build() : new JsonArray());
        envelope.add("event", Entities.newMessage(namespace, name, payload));
        CyberOkHttpClient.getClient().postEvent(envelope, null, new CyberClient.EventCallback() {
            @Override
            public void onDirective(JsonObject directive) {
                core.onEventDirective(directive);
            }

            @Override
            public void onFailure(Throwable e) {
                core.onEventFailure(e);
            }

            @Override
            public void onEnd() {
                core.onEventEnd();
            }
        });
    }

    public void postEvent(String namespace, String name, JsonObject payload, Source audio) {
        final String dialogRequestId = UUID.randomUUID().toString();

        final JsonObject envelope = new JsonObject();
        envelope.add("context", contextManager.build());
        envelope.add("event", Entities.newMessage(namespace, name, dialogRequestId, payload));

        core.onRecognizeStart(dialogRequestId);

        CyberOkHttpClient.getClient().postEvent(envelope, audio, new CyberClient.EventCallback() {
            @Override
            public void onDirective(JsonObject directive) {
                core.onRecognizeDirective(dialogRequestId, directive);
            }

            @Override
            public void onFailure(Throwable e) {
                core.onRecognizeFailure(dialogRequestId, e);
            }

            @Override
            public void onEnd() {
                core.onRecognizeEnd(dialogRequestId);
            }
        });
    }

    @Deprecated
    public void reportExecutionFailure(String unparsedDirective, String message) {
        Log.e(TAG, "Directive execute failed: " + message + " - " + unparsedDirective);

        final JsonObject error = new JsonObject();
        error.addProperty("type", "UNEXPECTED_INFORMATION_RECEIVED");
        error.addProperty("message", message);

        final JsonObject payload = new JsonObject();
        payload.addProperty("unparsedDirective", unparsedDirective);
        payload.add("error", error);

        postEvent("System", "ExceptionEncountered", payload, true);
    }

    @Deprecated
    public void reportException(String unparsedDirective, Throwable t) {
        Log.e(TAG, "Directive execute error: " + unparsedDirective, t);

        final JsonObject error = new JsonObject();
        error.addProperty("type", "INTERNAL_ERROR");
        error.addProperty("message", t.getMessage());

        final JsonObject payload = new JsonObject();
        payload.addProperty("unparsedDirective", unparsedDirective);
        payload.add("error", error);

        postEvent("System", "ExceptionEncountered", payload, true);
    }

    @Deprecated
    public void synchronizeState() {
        postEvent("System", "SynchronizeState", new JsonObject(), true);
    }

}
