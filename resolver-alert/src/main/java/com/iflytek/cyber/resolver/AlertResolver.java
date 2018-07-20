package com.iflytek.cyber.resolver;


import android.content.Context;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iflytek.cyber.CyberDelegate;
import com.iflytek.cyber.resolver.player.PlayOrder;
import com.iflytek.cyber.resolver.timer.DateHelper;
import com.iflytek.cyber.resolver.timer.Scheduler;
import com.litesuits.orm.LiteOrm;
import com.litesuits.orm.db.DataBaseConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AlertResolver extends ResolverModule {

    private final Scheduler scheduler;
    private final LiteOrm liteOrm;

    private String token;

    private OnAlertListener listener;

    public AlertResolver(Context context, CyberDelegate delegate) {
        super(context, delegate);
        DataBaseConfig config = new DataBaseConfig(context, "alert.db");
        config.dbVersion = 1;
        liteOrm = LiteOrm.newSingleInstance(config);

        scheduler = new Scheduler(context, this, liteOrm);
    }

    @Override
    public void onCreate() {
        updateAlert();
    }

    private void updateAlert() {
        final List<AllAlert> allAlerts = liteOrm.query(AllAlert.class);
        for (AllAlert alert : allAlerts) {
            if (alert != null) {
                long scheduledTime = DateHelper.getTime(alert.scheduledTime);
                if ((scheduledTime - System.currentTimeMillis()) > 0) {
                    //recover alert
                    recoverAlert(alert);
                }
            }
        }
    }

    private void recoverAlert(AllAlert alert) {
        if (TextUtils.equals(alert.type, "TIMER")) {
            scheduler.startTimer(alert.token, alert.type, alert.scheduledTime, alert.playOrders,
                    alert.hasLoopCount, alert.loopCount, alert.loopPauseInMilliSeconds);
        } else if (TextUtils.equals(alert.type, "ALARM")) {
            scheduler.startAlarm(alert.token, alert.type, alert.scheduledTime, alert.playOrders,
                    alert.hasLoopCount, alert.loopCount, alert.loopPauseInMilliSeconds);
        } else if (TextUtils.equals(alert.type, "REMINDER")) {
            scheduler.startReminder(alert.token, alert.type, alert.scheduledTime, alert.playOrders,
                    alert.hasLoopCount, alert.loopCount, alert.loopPauseInMilliSeconds);
        }
    }

    @Override
    public void updateContext() {
        JsonObject payload = new JsonObject();
        final JsonArray arrays = getActiveAlerts(scheduler.getActiveAlertMap());
        List<AllAlert> allAlerts = liteOrm.query(AllAlert.class);
        payload.add("allAlerts", getAllAlerts(allAlerts));
        payload.add("activeAlerts", arrays);
        delegate.updateContext("AlertsState", payload);
    }

    private JsonArray getAllAlerts(List<AllAlert> allAlerts) {
        final JsonArray array = new JsonArray();
        for (AllAlert alert : allAlerts) {
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

    private JsonArray getActiveAlerts(Map<String, ActiveAlert> alertMap) {
        final JsonArray array = new JsonArray();
        for (String key : alertMap.keySet()) {
            final ActiveAlert alert = alertMap.get(key);
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

            long alertTime = DateHelper.getTime(scheduledTime);
            if (alertTime - System.currentTimeMillis() < 0) {
                return;
            }

            boolean hasLoopCount = payload.has("loopCount");
            long loopCount = hasLoopCount? payload.get("loopCount").getAsLong() : 0;
            long loopPauseInMilliSeconds = payload.has("loopPauseInMilliSeconds") ?
                    payload.get("loopPauseInMilliSeconds").getAsLong() : 0;
            final ArrayList<PlayOrder> playOrders = parseArray(orderArray);
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

    private ArrayList<PlayOrder> parseArray(JsonArray array) {
        ArrayList<PlayOrder> playOrders = new ArrayList<>();
        Gson gson = new Gson();
        for (JsonElement element : array) {
            PlayOrder playOrder = gson.fromJson(element, PlayOrder.class);
            playOrders.add(playOrder);
        }
        return playOrders;
    }

    public void setListener(OnAlertListener listener) {
        this.listener = listener;
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

    public void onAlertActive() {
        delegate.activateChannel(CyberDelegate.CHANNEL_ALERTS);
        if (listener != null) {
            listener.onAlert(true);
        }
    }

    public void onAlertDeactive() {
        delegate.deactivateChannel(CyberDelegate.CHANNEL_ALERTS);
        if (listener != null) {
            listener.onAlert(false);
        }
    }

    public interface OnAlertListener {
        void onAlert(boolean active);
    }

}
