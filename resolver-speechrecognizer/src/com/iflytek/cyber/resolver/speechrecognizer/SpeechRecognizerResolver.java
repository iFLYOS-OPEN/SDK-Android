package com.iflytek.cyber.resolver.speechrecognizer;

import android.content.Context;

import com.google.gson.JsonObject;
import com.iflytek.cyber.CyberDelegate;
import com.iflytek.cyber.SpeechController;
import com.iflytek.cyber.resolver.ResolverModule;

import okio.Source;

public class SpeechRecognizerResolver extends ResolverModule implements SpeechController {

    private static final String TAG = "SpeechRecognizer";

    private IATCallback iatCallback;

    public SpeechRecognizerResolver(Context context, CyberDelegate delegate) {
        super(context, delegate);
        delegate.registerSpeechController(this);
    }

    @Override
    public void start(Source source, JsonObject initiator) {
        final JsonObject payload = new JsonObject();
        payload.addProperty("profile", "FAR_FIELD");
        payload.addProperty("format", "AUDIO_L16_RATE_16000_CHANNELS_1");

        if (initiator != null) {
            payload.add("initiator", initiator);
        }

        delegate.postEvent("Recognize", payload, source);
    }

    @Override
    public void finish() {
    }

    @Override
    public void cancel() {
    }

    @Override
    public void resolve(JsonObject header, JsonObject payload, Callback callback) {
        final String name = header.get("name").getAsString();
        switch (name) {
            case "StopCapture":
                delegate.stopCapture();
                callback.next();
                break;
            case "IntermediateText":
                if (iatCallback != null)
                    iatCallback.onReceiveIAT(payload.get("text").getAsString());
                callback.next();
                break;
            case "ExpectSpeech":
                delegate.expectSpeech(payload.getAsJsonObject("initiator"));
                callback.next();
                break;
            default:
                callback.skip();
                break;
        }
    }

    public void setIatCallback(IATCallback iatCallback) {
        this.iatCallback = iatCallback;
    }

    public interface IATCallback {
        void onReceiveIAT(String text);
    }

}
