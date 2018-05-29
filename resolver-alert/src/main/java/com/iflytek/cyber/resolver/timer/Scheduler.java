package com.iflytek.cyber.resolver.timer;


import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.util.Log;

import com.iflytek.cyber.resolver.Alert;
import com.iflytek.cyber.resolver.AlertResolver;
import com.iflytek.cyber.resolver.player.PlayOrder;
import com.iflytek.cyber.resolver.player.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Scheduler {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final AlertResolver resolver;
    private final Context context;

    private Map<String, ScheduledFuture> schedulerMap = new HashMap<>();
    private Map<String, Player> playerMap = new HashMap<>();
    private Map<String, Alert> alertMap = new HashMap<>();

    public Scheduler(Context context, AlertResolver resolver) {
        this.context = context;
        this.resolver = resolver;
    }

    public Map<String, Alert> getAlertMap() {
        return alertMap;
    }

    public void startTimer(final String token, final String type, final String scheduledTime,
                           final List<PlayOrder> playOrders,
                           final boolean hasLoopCount, final long loopCount,
                           final long loopPauseInMilliSeconds) {
        long delay = DateHelper.getTime(scheduledTime) - System.currentTimeMillis();
        final Alert alert = new Alert(type, token, scheduledTime);
        alertMap.put(token, alert);
        ScheduledFuture timerFuture = scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                requestAudioFocus();
                schedulerMap.remove(token);
                alertMap.remove(token);
                playAlert(playOrders, token, hasLoopCount, loopCount, loopPauseInMilliSeconds);
            }
        }, delay, TimeUnit.MILLISECONDS);

        resolver.notifySetAlertSucceeded();

        schedulerMap.put(token, timerFuture);
    }

    public void startAlarm(final String token, final String type, final String scheduledTime,
                           final List<PlayOrder> playOrders,
                           final boolean hasLoopCount, final long loopCount,
                           final long loopPauseInMilliSeconds) {
        long delay = DateHelper.getTime(scheduledTime) - System.currentTimeMillis();
        final Alert alert = new Alert(type, token, scheduledTime);
        alertMap.put(token, alert);
        ScheduledFuture alarmFuture = scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                requestAudioFocus();
                schedulerMap.remove(token);
                alertMap.remove(token);
                playAlert(playOrders, token, hasLoopCount, loopCount, loopPauseInMilliSeconds);
            }
        }, delay, TimeUnit.MILLISECONDS);

        resolver.notifySetAlertSucceeded();

        schedulerMap.put(token, alarmFuture);
    }

    public void startReminder(final String token, final String type, final String scheduledTime,
                              final List<PlayOrder> playOrders,
                              final boolean hasLoopCount, final long loopCount,
                              final long loopPauseInMilliSeconds) {
        long delay = DateHelper.getTime(scheduledTime) - System.currentTimeMillis();
        final Alert alert = new Alert(type, token, scheduledTime);
        alertMap.put(token, alert);
        ScheduledFuture reminderFuture = scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                requestAudioFocus();
                schedulerMap.remove(token);
                alertMap.remove(token);
                playAlert(playOrders, token, hasLoopCount, loopCount, loopPauseInMilliSeconds);
            }
        }, delay, TimeUnit.MILLISECONDS);

        resolver.notifySetAlertSucceeded();

        schedulerMap.put(token, reminderFuture);
    }

    private void playAlert(List<PlayOrder> playOrders, String token, final boolean hasLoopCount,
                           final long loopCount, final long loopPauseInMilliSeconds) {
        if (playOrders == null || playOrders.size() == 0) {
            return;
        }

        final int[] currentLoop = {1};
        final Player player = new Player();
        player.setPlayList(playOrders);

        player.setOnPlayListener(new Player.OnPlayListener() {
            @Override
            public void onPlayStarted() {
                resolver.notifyAlertStarted();
                resolver.notifyAlertEnteredForeground();
            }

            @Override
            public void onPlayStopped() {
                resolver.notifyAlertStopped();
            }

            @Override
            public void playListFinished() {
                abandonAudioFocus();
                if (hasLoopCount) { //没有 loopCount 循环字段，一直播放alert一小时
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (currentLoop[0] < loopCount) {
                                requestAudioFocus();
                                player.play();
                                currentLoop[0] += 1;
                            } else {
                                abandonAudioFocus();
                            }
                        }
                    }, loopPauseInMilliSeconds);
                }
            }
        });

        player.play();

        playerMap.put(token, player);

        if (!hasLoopCount) {
            startCounter(player);
        }
    }

    private void startCounter(final Player player) {
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                player.pause();
            }
        }, 60, TimeUnit.MINUTES);
    }

    public void cancel(String token) {
        final ScheduledFuture future = schedulerMap.get(token);
        final Alert alert = alertMap.get(token);
        if (alert != null) {
            alertMap.remove(token);
        }
        if (future != null) {
            boolean cancel = future.cancel(true);
            if (cancel) {
                resolver.notifyDeleteAlertSucceeded();
            } else {
                resolver.notifyDeleteAlertFailed();
            }

            schedulerMap.remove(token);
        } else {
            resolver.notifyDeleteAlertFailed();
        }
        Player player = playerMap.get(token);
        if (player != null) {
            player.pause();
            playerMap.remove(token);
        }
    }

    private void requestAudioFocus() {
        final AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (null != audioManager) {
            audioManager.requestAudioFocus(onAudioFocusChangeListener,
                    AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
        }
    }

    private void abandonAudioFocus() {
        final AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (null != audioManager) {
            audioManager.abandonAudioFocus(onAudioFocusChangeListener);
        }
    }

    private AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            Log.d("Scheduler", "Focus changed: " + focusChange);
        }
    };
}
