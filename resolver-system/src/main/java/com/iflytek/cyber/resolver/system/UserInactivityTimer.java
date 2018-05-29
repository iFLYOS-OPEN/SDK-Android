package com.iflytek.cyber.resolver.system;

import com.google.gson.JsonObject;
import com.iflytek.cyber.CyberDelegate;

import java.util.Timer;
import java.util.TimerTask;

/**
 * UserInactivityTimer is work for UserInactivityReport Event
 * This event must be sent after an hour of inactivity,
 * and every hour after that until a user action is taken.
 * After a user activity is detected, the timer used to track inactivity must be reset to 0.
 */
public class UserInactivityTimer {
    private static UserInactivityTimer instance;
    private Timer timer;
    private TimerTask timerTask;

    private NotifyCallback notifyCallback;

    private long inactiveTimeInSeconds = 0;

    private static final long CYCLE_IN_SECONDS = 3600;

    private CyberDelegate delegate;

    private UserInactivityTimer() {
        timer = new Timer();
    }

    public NotifyCallback getNotifyCallback() {
        return notifyCallback;
    }

    public void setNotifyCallback(NotifyCallback notifyCallback) {
        this.notifyCallback = notifyCallback;
    }

    public static UserInactivityTimer get() {
        if (instance == null)
            instance = new UserInactivityTimer();
        return instance;
    }

    protected void setup(CyberDelegate delegate) {
        this.delegate = delegate;
    }

    protected void destroy() {
        timer.cancel();
        if (timerTask != null)
            timerTask.cancel();
    }

    /**
     * restart the schedule work of timer,
     * should be call when action was detected
     * that confirms a user is in the presence of the product,
     * such as interacting with on-product buttons,
     * speaking with device, or using a GUI affordance.
     */
    public void reset() {
        if (timerTask != null)
            timerTask.cancel();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                inactiveTimeInSeconds += CYCLE_IN_SECONDS;
                if (notifyCallback != null)
                    notifyCallback.onUserInactivityReportEventSent(inactiveTimeInSeconds);
                postReportEvent(inactiveTimeInSeconds);
            }
        };
        inactiveTimeInSeconds = 0;
        timer.schedule(timerTask, CYCLE_IN_SECONDS * 1000, CYCLE_IN_SECONDS * 1000);
    }

    private void postReportEvent(long inactiveTimeInSeconds) {
        if (delegate != null) {
            JsonObject payload = new JsonObject();
            payload.addProperty(SystemResolver.PAYLOAD_INACTIVE_TIME_IN_SECONDS, inactiveTimeInSeconds);
            delegate.postEvent(SystemResolver.NAME_INACTIVITY_REPORT, payload);
        }
    }

    public interface NotifyCallback {
        /**
         * call when achieving a multiple of 3600 (1 hour)
         *
         * @param inactiveTimeInSeconds duration in second
         */
        void onUserInactivityReportEventSent(long inactiveTimeInSeconds);
    }
}
