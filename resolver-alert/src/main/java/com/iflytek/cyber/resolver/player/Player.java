package com.iflytek.cyber.resolver.player;


import android.media.MediaPlayer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Player implements MediaPlayer.OnCompletionListener {

    private OnPlayListener onPlayListener;
    private final MediaPlayer mediaPlayer;
    private final List<PlayOrder> playOrders;
    private int playIndex = -1;
    private long playTime;

    public Player() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this);
        this.playOrders = new ArrayList<>();
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
        final PlayOrder playOrder = playOrders.get(0);
        play(playOrder);
    }

    public void play(PlayOrder playOrder) {
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(playOrder.url);
            mediaPlayer.prepare();
            mediaPlayer.start();
            onPlayListener.onPlayStarted();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
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
        if (mediaPlayer.isPlaying()) {
            onPlayListener.onPlayStopped();
            mediaPlayer.stop();
        }
    }

    public void resume() {
        mediaPlayer.start();
        onPlayListener.onPlayStarted();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (playIndex < playOrders.size() - 1) {
            playNext();
        } else {
            onPlayListener.playListFinished();
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
