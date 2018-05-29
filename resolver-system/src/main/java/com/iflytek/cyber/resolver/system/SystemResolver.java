package com.iflytek.cyber.resolver.system;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iflytek.cyber.CyberDelegate;
import com.iflytek.cyber.resolver.ResolverModule;
import com.iflytek.cyber.sdk.BuildConfig;

import java.util.HashMap;
import java.util.Map;

public class SystemResolver extends ResolverModule {
    private static final String NAME_UPDATE_SETTINGS = "UpdateSettings";
    private static final String NAME_SETTINGS_UPDATED = "SettingsUpdated";
    private static final String NAME_RESET_USER_INACTIVITY = "ResetUserInactivity";
    protected static final String NAME_INACTIVITY_REPORT = "UserInactivityReport"; // for in-package-use
    private static final String NAME_REPORT_SOFTWARE_INFO = "ReportSoftwareInfo";
    private static final String NAME_SOFTWARE_INFO = "SoftwareInfo";
    private static final String NAME_SET_ENDPOINT = "SetEndpoint";

    public static final String NAME_SYNCHRONIZE_STATE = "SynchronizeState";

    private static final String PAYLOAD_SETTINGS = "settings";
    private static final String PAYLOAD_ENDPOINT = "endpoint";
    private static final String PAYLOAD_FIRMWARE_VERSION = "firmwareVersion";
    protected static final String PAYLOAD_INACTIVE_TIME_IN_SECONDS = "inactiveTimeInSeconds"; // for in-package-use

    private static final String PARAMS_KEY = "key";
    private static final String PARAMS_VALUE = "value";

    private static final String VALUE_DO_NOT_DISTURB = "doNotDisturb";

    private SharedPreferences defaultPreferences;

    public SystemResolver(Context context, CyberDelegate delegate) {
        super(context, delegate);
        defaultPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        UserInactivityTimer.get().setup(delegate);
        SystemEventPoster.get().setup(delegate);
    }

    @Override
    public void resolve(JsonObject header, JsonObject payload, Callback callback) {
        switch (header.get("name").getAsString()) {
            case NAME_UPDATE_SETTINGS:
                JsonElement jsonElement = payload.get(PAYLOAD_SETTINGS);
                if (jsonElement.isJsonArray()) {
                    JsonArray array = jsonElement.getAsJsonArray();
                    for (int i = 0; i < array.size(); i++) {
                        JsonObject pair = array.get(i).getAsJsonObject();
                        if (VALUE_DO_NOT_DISTURB.equals(pair.get(PARAMS_KEY).getAsString())) {
                            boolean enable = pair.get(PARAMS_VALUE).getAsBoolean();
                            defaultPreferences.edit()
                                    .putBoolean(VALUE_DO_NOT_DISTURB, enable)
                                    .apply();
                            // if no exception, send settings update event
                            Map<String, Object> contentValues = new HashMap<>();
                            contentValues.put(VALUE_DO_NOT_DISTURB, enable);
                            sendSettingsUpdated(contentValues);
                        }
                    }
                }
                break;
            case NAME_RESET_USER_INACTIVITY:
                // there is a timer to count the duration after last user activity,
                // this directive is use to reset the timer
                // timer should be singleton
                UserInactivityTimer.get().reset();
                break;
            case NAME_REPORT_SOFTWARE_INFO:
                // send software info event
                JsonObject softwareInfoPayload = new JsonObject();
                softwareInfoPayload.addProperty(PAYLOAD_FIRMWARE_VERSION, BuildConfig.VERSION_CODE);
                delegate.postEvent(NAME_SOFTWARE_INFO, softwareInfoPayload);
                break;
            case NAME_SET_ENDPOINT:
                // set endpoint
                String endpoint = payload.get(PAYLOAD_ENDPOINT).getAsString();
                delegate.setEndpoint(endpoint);
                break;
            default:
                callback.skip();
                return;
        }
        callback.next();
    }

    private void sendSettingsUpdated(Map<String, Object> contentValues) {
        JsonObject payload = new JsonObject();
        JsonArray settings = new JsonArray();
        payload.add(PAYLOAD_SETTINGS, settings);
        for (String key : contentValues.keySet()) {
            JsonObject item = new JsonObject();
            item.addProperty(PARAMS_KEY, key);
            Object value = contentValues.get(key);
            if (value instanceof String) {
                item.addProperty(PARAMS_VALUE, (String) value);
            } else if (value instanceof Boolean) {
                item.addProperty(PARAMS_VALUE, (Boolean) value);
            } else if (value instanceof Number) {
                item.addProperty(PARAMS_VALUE, (Number) value);
            } else if (value instanceof Character) {
                item.addProperty(PARAMS_VALUE, (Character) value);
            } else {
                item.addProperty(PARAMS_VALUE, value.toString());
            }
        }
        delegate.postEvent(NAME_SETTINGS_UPDATED, payload);
    }
}
