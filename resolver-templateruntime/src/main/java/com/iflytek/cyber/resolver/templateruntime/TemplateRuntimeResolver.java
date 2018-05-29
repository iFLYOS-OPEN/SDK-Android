package com.iflytek.cyber.resolver.templateruntime;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.iflytek.cyber.CyberDelegate;
import com.iflytek.cyber.resolver.ResolverModule;
import com.iflytek.cyber.resolver.templateruntime.fragment.body1.Body1Fragment;
import com.iflytek.cyber.resolver.templateruntime.fragment.body2.Body2Fragment;
import com.iflytek.cyber.resolver.templateruntime.fragment.player.PlayerInfoMap;
import com.iflytek.cyber.resolver.templateruntime.fragment.player.PlayerInfoPayload;
import com.iflytek.cyber.resolver.templateruntime.fragment.weather.WeatherFragment;

public class TemplateRuntimeResolver extends ResolverModule {
    private TemplateRuntimeCallback templateRuntimeCallback;

    public TemplateRuntimeCallback getTemplateRuntimeCallback() {
        return templateRuntimeCallback;
    }

    public void setTemplateRuntimeCallback(TemplateRuntimeCallback templateRuntimeCallback) {
        this.templateRuntimeCallback = templateRuntimeCallback;
    }

    public TemplateRuntimeResolver(Context context, CyberDelegate delegate) {
        super(context, delegate);
    }

    @Override
    public void resolve(JsonObject header, JsonObject payload, Callback callback) {
        String name = header.get("name").getAsString();
        Log.w(getClass().getSimpleName(), name);
        Log.w(getClass().getSimpleName(), payload.toString());
        switch (name) {
            case Constant.NAME_RENDER_PLAYER_INFO:
                // render player info not means replace the previous player view
                PlayerInfoPayload playerInfoPayload = new Gson().fromJson(payload, PlayerInfoPayload.class);
                PlayerInfoMap.get().put(playerInfoPayload.audioItemId, playerInfoPayload);
                if (templateRuntimeCallback != null)
                    templateRuntimeCallback.onPlayerInfoUpdate(playerInfoPayload);
                callback.next();
                break;
            case Constant.NAME_RENDER_TEMPLATE:
                String type = payload.get(Constant.PAYLOAD_TYPE).getAsString();
                switch (type) {
                    case Constant.TYPE_WEATHER_TEMPLATE:
                        if (templateRuntimeCallback != null)
                            templateRuntimeCallback.onNewTemplateFragment(WeatherFragment.generate(payload));
                        break;
                    case Constant.TYPE_BODY_TEMPLATE_1:
                        if (templateRuntimeCallback != null) {
                            templateRuntimeCallback.onNewTemplateFragment(Body1Fragment.generate(payload));
                        }
                        break;
                    case Constant.TYPE_BODY_TEMPLATE_2:
                        if (templateRuntimeCallback != null) {
                            templateRuntimeCallback.onNewTemplateFragment(Body2Fragment.generate(payload));
                        }
                        break;
                    default:
                        Log.w(getClass().getSimpleName(), "template type not supported yet: " + type);
                        break;
                }
                callback.next();
                break;
            default:
                Log.w(getClass().getSimpleName(), "unhandled Template Runtime type: " + name);
                callback.skip();
                break;
        }
    }

    public interface TemplateRuntimeCallback {
        void onNewTemplateFragment(Fragment fragment);

        /**
         * as a notification, not necessary to update manually
         *
         * @param payload player info object
         */
        void onPlayerInfoUpdate(PlayerInfoPayload payload);
    }
}
