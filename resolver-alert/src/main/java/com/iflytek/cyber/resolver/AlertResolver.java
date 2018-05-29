package com.iflytek.cyber.resolver;


import android.content.Context;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iflytek.cyber.CyberDelegate;
import com.iflytek.cyber.resolver.player.PlayOrder;
import com.iflytek.cyber.resolver.timer.Scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AlertResolver extends ResolverModule {

    private Scheduler scheduler;
    private String token;

    public AlertResolver(Context context, CyberDelegate delegate) {
        super(context, delegate);
        scheduler = new Scheduler(context, this);
    }

    @Override
    public void updateContext() {
        if (!TextUtils.isEmpty(token)) {
            JsonObject payload = new JsonObject();
            final JsonArray arrays = getAlerts(scheduler.getAlertMap());
            payload.add("allAlerts", arrays);
            payload.add("activeAlerts", arrays);
            delegate.updateContext("AlertsState", payload);
        }
    }

    private JsonArray getAlerts(Map<String, Alert> alertMap) {
        final JsonArray array = new JsonArray();
        for (String key : alertMap.keySet()) {
            final Alert alert = alertMap.get(key);
            if (alert != null) {
                JsonObject object = new JsonObject();
                object.addProperty("type", alert.type);
                object.addProperty("token", alert.token);
                object.addProperty("scheduledTime", alert.scheduledTime);
                array.add(object);
            }
        }
        return array;
    }

    @Override
    public void resolve(JsonObject header, JsonObject payload, Callback callback) {
        String name = header.get("name").getAsString();
        token = payload.has("token") ? payload.get("token").getAsString() : null;

        if (TextUtils.equals(name, "SetAlert")) {
            String type = payload.get("type").getAsString();
            JsonArray orderArray = payload.get("assets").getAsJsonArray();
            String scheduledTime = payload.get("scheduledTime").getAsString();

            if (TextUtils.isEmpty(scheduledTime)) {
                notifySetAlertFailed();
                callback.next();
                return;
            }

            boolean hasLoopCount = payload.has("loopCount");
            long loopCount = hasLoopCount? payload.get("loopCount").getAsLong() : 0;
            long loopPauseInMilliSeconds = payload.has("loopPauseInMilliSeconds") ?
                    payload.get("loopPauseInMilliSeconds").getAsLong() : 0;
            final List<PlayOrder> playOrders = parseArray(orderArray);
            if (TextUtils.equals(type, "TIMER")) {
                scheduler.startTimer(token, type, scheduledTime, playOrders, hasLoopCount, loopCount, loopPauseInMilliSeconds);
            } else if (TextUtils.equals(type, "ALARM")) {
                scheduler.startAlarm(token, type, scheduledTime, playOrders, hasLoopCount, loopCount, loopPauseInMilliSeconds);
            } else if (TextUtils.equals(type, "REMINDER")) {
                scheduler.startReminder(token, type, scheduledTime, playOrders, hasLoopCount, loopCount, loopPauseInMilliSeconds);
            }
        } else if (TextUtils.equals(name, "DeleteAlert")) {
            notifyAlertStopped();
            scheduler.cancel(token);
        }
        callback.next();
    }

    private List<PlayOrder> parseArray(JsonArray array) {
        List<PlayOrder> playOrders = new ArrayList<>();
        Gson gson = new Gson();
        for (JsonElement element : array) {
            PlayOrder playOrder = gson.fromJson(element, PlayOrder.class);
            playOrders.add(playOrder);
        }
        return playOrders;
    }

    public void notifySetAlertSucceeded() {
        final JsonObject payload = new JsonObject();
        payload.addProperty("token", token);
        delegate.postEvent("SetAlertSucceeded", payload);
    }

    public void notifySetAlertFailed() {
        final JsonObject payload = new JsonObject();
        payload.addProperty("token", token);
        delegate.postEvent("SetAlertFailed", payload);
    }

    public void notifyDeleteAlertSucceeded() {
        final JsonObject payload = new JsonObject();
        payload.addProperty("token", token);
        delegate.postEvent("DeleteAlertSucceeded", payload);
    }

    public void notifyDeleteAlertFailed() {
        final JsonObject payload = new JsonObject();
        payload.addProperty("token", token);
        delegate.postEvent("DeleteAlertFailed", payload);
    }

    public void notifyAlertStarted() {
        final JsonObject payload = new JsonObject();
        payload.addProperty("token", token);
        delegate.postEvent("AlertStarted", payload);
    }

    public void notifyAlertStopped() {
        final JsonObject payload = new JsonObject();
        payload.addProperty("token", token);
        delegate.postEvent("AlertStopped", payload);
    }

    public void notifyAlertEnteredForeground() {
        final JsonObject payload = new JsonObject();
        payload.addProperty("token", token);
        delegate.postEvent("AlertEnteredForeground", payload);
    }
}
