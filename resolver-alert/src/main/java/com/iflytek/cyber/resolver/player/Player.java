package com.iflytek.cyber.resolver.player;


import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;
import java.util.List;

public class Player extends com.google.android.exoplayer2.Player.DefaultEventListener {

    private OnPlayListener onPlayListener;
    private final List<PlayOrder> playOrders;
    private int playIndex = -1;
    private long playTime;
    private SimpleExoPlayer player;

    private Context context;

    public Player(Context context) {
        this.context = context;
        this.playOrders = new ArrayList<>();
        BandwidthMeter meter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(meter);
        DefaultTrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        player = ExoPlayerFactory.newSimpleInstance(context, trackSelector);
    }

    public void setOnPlayListener(OnPlayListener onPlayListener) {
        this.onPlayListener = onPlayListener;
    }

    public void setPlayList(List<PlayOrder> playList) {
        playOrders.clear();
        playOrders.addAll(playList);
    }

    public long getPlayTime() {
        return playTime;
    }

    public void play() {
        playIndex = 0;
        playTime = System.currentTimeMillis();
        if (playOrders != null && playOrders.size() > 0) {
            final PlayOrder playOrder = playOrders.get(0);
            play(playOrder);
        }
    }

    public void replay() {
        playIndex = 0;
        if (playOrders != null && playOrders.size() > 0) {
            final PlayOrder playOrder = playOrders.get(0);
            play(playOrder);
        }
    }

    public void play(PlayOrder playOrder) {
        if (player == null) {
            return;
        }

        onPlayListener.onPlayStarted();
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context,
                Util.getUserAgent(context, "Alert"), null);
        MediaSource videoSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                .createMediaSource(Uri.parse(playOrder.url));
        player.addListener(this);
        player.prepare(videoSource);
        player.setPlayWhenReady(true);
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        super.onPlayerStateChanged(playWhenReady, playbackState);
        if (playbackState == com.google.android.exoplayer2.Player.STATE_ENDED) {
            if (playIndex < playOrders.size() - 1) {
                playNext();
            } else {
                onPlayListener.playListFinished();
            }
        }
    }

    public boolean isPlaying() {
        return player != null && player.getPlayWhenReady();
    }

    public void playNext() {
        playIndex += 1;
        final PlayOrder playOrder = playOrders.get(playIndex);
        play(playOrder);
    }

    public void playPrevious() {
        playIndex -= 1;
        final PlayOrder playOrder = playOrders.get(playIndex);
        play(playOrder);
    }

    public void pause() {
        if (player != null) {
            player.setPlayWhenReady(false);
        }
    }

    public void resume() {
        if (player != null) {
            player.setPlayWhenReady(true);
            onPlayListener.onPlayStarted();
        }
    }

    public void stop() {
        if (player != null && isPlaying()) {
            onPlayListener.onPlayStopped();
            player.stop();
        }
    }

    public void release() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    public void setVolume(float volume) {
        if (player != null) {
            player.setVolume(volume);
        }
    }

    public boolean playFinish() {
        return playIndex == playOrders.size() - 1;
    }

    public interface OnPlayListener {
        void onPlayStarted();

        void onPlayStopped();

        void playListFinished();
    }
}
