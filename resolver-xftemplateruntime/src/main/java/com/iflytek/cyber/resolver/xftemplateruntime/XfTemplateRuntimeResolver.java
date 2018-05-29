package com.iflytek.cyber.resolver.xftemplateruntime;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.iflytek.cyber.CyberDelegate;
import com.iflytek.cyber.resolver.ResolverModule;
import com.iflytek.cyber.resolver.xftemplateruntime.model.XfTemplatePayload;

public class XfTemplateRuntimeResolver extends ResolverModule {
    private static final String NAME_RENDER_MEDIA = "RenderMedia";

    private XfTemplateRuntimeCallback callback;

    public XfTemplateRuntimeResolver(Context context, CyberDelegate delegate) {
        super(context, delegate);
    }

    @Override
    public void resolve(JsonObject header, JsonObject payload, Callback callback) {
        switch (header.get("name").getAsString()) {
            case NAME_RENDER_MEDIA:
                XfTemplatePayload xfTemplatePayload = new Gson().fromJson(payload, XfTemplatePayload.class);
                if (this.callback != null)
                    this.callback.onNewXfFragment(PlayerFragment.generatePlayerFragment(xfTemplatePayload));
                else
                    Log.e(getClass().getSimpleName(), "callback is null");
                callback.next();
                break;
            default:
                callback.skip();
                break;
        }
    }

    public void setXfTemplateRuntimeCallback(XfTemplateRuntimeCallback callback) {
        this.callback = callback;
    }

    public interface XfTemplateRuntimeCallback {
        void onNewXfFragment(Fragment fragment);
    }
}
