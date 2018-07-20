package com.iflytek.cyber.resolver.timer;


import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.iflytek.cyber.resolver.ActiveAlert;
import com.iflytek.cyber.resolver.AlertResolver;
import com.iflytek.cyber.resolver.AllAlert;
import com.iflytek.cyber.resolver.player.PlayOrder;
import com.iflytek.cyber.resolver.player.Player;
import com.litesuits.orm.LiteOrm;

import java.util.ArrayList;
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
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ProgressRunnable progressRunnable = new ProgressRunnable();

    private final LiteOrm liteOrm;

    private Map<String, ScheduledFuture> schedulerMap = new HashMap<>();
    private Map<String, Player> playerMap = new HashMap<>();
    private Map<String, ActiveAlert> activeAlertMap = new HashMap<>();

    public Scheduler(Context context, AlertResolver resolver, LiteOrm liteOrm) {
        this.context = context;
        this.resolver = resolver;
        this.liteOrm = liteOrm;
    }

    public Map<String, ActiveAlert> getActiveAlertMap() {
        return activeAlertMap;
    }

    public void startTimer(final String token, final String type, final String scheduledTime,
                           final ArrayList<PlayOrder> playOrders,
                           final boolean hasLoopCount, final long loopCount,
                           final long loopPauseInMilliSeconds) {
        long delay = DateHelper.getTime(scheduledTime) - System.currentTimeMillis();
        AllAlert allAlert = liteOrm.queryById(token, AllAlert.class);
        if (allAlert == null) {
            final AllAlert alert = new AllAlert(type, token, scheduledTime, playOrders, hasLoopCount,
                    loopCount, loopPauseInMilliSeconds);
            liteOrm.insert(alert);
        }
        ScheduledFuture timerFuture = scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                AllAlert taskAlert = liteOrm.queryById(token, AllAlert.class);
                if (taskAlert != null) {
                    schedulerMap.remove(token);
                    playAlert(playOrders, token, type, scheduledTime,
                            hasLoopCount, loopCount, loopPauseInMilliSeconds);
                }
            }
        }, delay, TimeUnit.MILLISECONDS);

        resolver.notifySetAlertSucceeded();

        schedulerMap.put(token, timerFuture);
    }

    public void startAlarm(final String token, final String type, final String scheduledTime,
                           final ArrayList<PlayOrder> playOrders,
                           final boolean hasLoopCount, final long loopCount,
                           final long loopPauseInMilliSeconds) {
        long delay = DateHelper.getTime(scheduledTime) - System.currentTimeMillis();
        final AllAlert allAlert = liteOrm.queryById(token, AllAlert.class);
        if (allAlert == null) {
            final AllAlert alert = new AllAlert(type, token, scheduledTime, playOrders, hasLoopCount,
                    loopCount, loopPauseInMilliSeconds);
            liteOrm.insert(alert);
        }
        ScheduledFuture alarmFuture = scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                AllAlert taskAlert = liteOrm.queryById(token, AllAlert.class);
                if (taskAlert != null) {
                    schedulerMap.remove(token);
                    playAlert(playOrders, token, type, scheduledTime,
                            hasLoopCount, loopCount, loopPauseInMilliSeconds);
                }
            }
        }, delay, TimeUnit.MILLISECONDS);

        resolver.notifySetAlertSucceeded();

        schedulerMap.put(token, alarmFuture);
    }

    public void startReminder(final String token, final String type, final String scheduledTime,
                              final ArrayList<PlayOrder> playOrders,
                              final boolean hasLoopCount, final long loopCount,
                              final long loopPauseInMilliSeconds) {
        long delay = DateHelper.getTime(scheduledTime) - System.currentTimeMillis();
        AllAlert allAlert = liteOrm.queryById(token, AllAlert.class);
        if (allAlert == null) {
            final AllAlert alert = new AllAlert(type, token, scheduledTime, playOrders, hasLoopCount,
                    loopCount, loopPauseInMilliSeconds);
            liteOrm.insert(alert);
        }
        ScheduledFuture reminderFuture = scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                AllAlert taskAlert = liteOrm.queryById(token, AllAlert.class);
                if (taskAlert != null) {
                    schedulerMap.remove(token);
                    playAlert(playOrders, token, type, scheduledTime,
                            hasLoopCount, loopCount, loopPauseInMilliSeconds);
                }
            }
        }, delay, TimeUnit.MILLISECONDS);

        resolver.notifySetAlertSucceeded();

        schedulerMap.put(token, reminderFuture);
    }

    private void playAlert(List<PlayOrder> playOrders, final String token,
                           final String type,
                           final String scheduledTime,
                           final boolean hasLoopCount,
                           final long loopCount, final long loopPauseInMilliSeconds) {
        if (playOrders == null || playOrders.size() == 0) {
            return;
        }

        final int[] currentLoop = {1};
        final Player player = new Player(context);
        player.setPlayList(playOrders);

        player.setOnPlayListener(new Player.OnPlayListener() {
            @Override
            public void onPlayStarted() {
                requestAudioFocus(player);
                resolver.notifyAlertStarted();
                resolver.notifyAlertEnteredForeground();
                if (!activeAlertMap.containsKey(token)) {
                    final ActiveAlert alert = new ActiveAlert(type, token, scheduledTime);
                    activeAlertMap.put(token, alert);
                }
            }

            @Override
            public void onPlayStopped() {
                resolver.notifyAlertStopped();
            }

            @Override
            public void playListFinished() {
                abandonAudioFocus();
                if (activeAlertMap.containsKey(token)) {
                    activeAlertMap.remove(token);
                }
                if (currentLoop[0] == loopCount) {
                    player.release();
                    abandonAudioFocus();
                }
                if (hasLoopCount) { //没有 loopCount 循环字段，一直播放alert一小时
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (currentLoop[0] < loopCount) {
                                requestAudioFocus(player);
                                player.play();
                                currentLoop[0] += 1;
                            } else {
                                abandonAudioFocus();
                            }
                        }
                    }, loopPauseInMilliSeconds);
                } else {
                    requestAudioFocus(player);
                    player.replay();
                    progressRunnable.setPlayerAndToken(player, token);
                    handler.post(progressRunnable);
                }
            }
        });

        player.play();

        playerMap.put(token, player);
    }

    private class ProgressRunnable implements Runnable {

        private Player player;
        private String token;

        public void setPlayerAndToken(Player player, String token) {
            this.player = player;
            this.token = token;
        }

        @Override
        public void run() {
            long playTime = player.getPlayTime();
            if (System.currentTimeMillis() - playTime > 60 * 60 * 1000) { //play alert 1 hour
                player.stop();
                player.release();
                abandonAudioFocus();
                handler.removeCallbacksAndMessages(null);
                if (activeAlertMap.containsKey(token)) {
                    activeAlertMap.remove(token);
                }
            } else {
                handler.postDelayed(this, 1000);
            }
        }
    }

    public void cancel(String token) {
        final ScheduledFuture future = schedulerMap.get(token);
        final ActiveAlert activeAlert = activeAlertMap.get(token);
        if (activeAlert != null) {
            activeAlertMap.remove(token);
        }
        final AllAlert allAlert = liteOrm.queryById(token, AllAlert.class);
        if (allAlert != null) {
            liteOrm.delete(allAlert);
        }
        if (future != null) {
            boolean cancel = future.cancel(true);
            if (cancel) {
                resolver.notifyDeleteAlertSucceeded();
                schedulerMap.remove(token);
            } else {
                resolver.notifyDeleteAlertFailed();
            }
        } else {
            resolver.notifyDeleteAlertFailed();
        }
        handler.removeCallbacksAndMessages(null);
        Player player = playerMap.get(token);
        if (player != null) {
            player.stop();
            player.release();
            playerMap.remove(token);
        }
    }

    private AudioFocusChangeListener focusChangeListener = new AudioFocusChangeListener();

    private class AudioFocusChangeListener implements AudioManager.OnAudioFocusChangeListener {

        private Player player;

        void setPlayer(Player player) {
            this.player = player;
        }

        private float mVolume = 1f;

        @Override
        public void onAudioFocusChange(int focusChange) {
            Log.i(getClass().getSimpleName(), "onAudioFocusChange enter, " + focusChange);
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    player.setVolume(mVolume);
                    player.resume();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    player.stop();
                    abandonAudioFocus();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    player.pause();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    player.setVolume(0.2f);
                    break;
                default:
            }
        }
    }

    private void requestAudioFocus(final Player player) {
        final AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (null != audioManager) {
            focusChangeListener.setPlayer(player);
            audioManager.requestAudioFocus(focusChangeListener,
                    AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            resolver.onAlertActive();
        }
    }

    private void abandonAudioFocus() {
        final AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (null != audioManager) {
            audioManager.abandonAudioFocus(focusChangeListener);
            resolver.onAlertDeactive();
        }
    }
}
