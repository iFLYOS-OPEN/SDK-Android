/*
 * Copyright (C) 2018 iFLYTEK CO.,LTD.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iflytek.cyber.iot.show.core.impl.MediaPlayer;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.iflytek.cyber.iot.show.core.impl.Logger.LoggerHandler;
import com.iflytek.cyber.iot.show.core.impl.PlaybackController.PlaybackControllerHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import cn.iflyos.iace.iflyos.Speaker;
import cn.iflyos.iace.utils.OpusDongHelper;

public class MediaPlayerHandler extends cn.iflyos.iace.iflyos.MediaPlayer {

    private static final String sTag = "MediaPlayer";
    private static final String sFileName = "iflyos_media"; // Note: not thread safe

    // For volume controls
    public enum SpeakerType {
        SYNCED, LOCAL
    }

    private static List<SpeakerHandler> localSpeakers = new ArrayList<>();

    private final Context mContext;
    private final LoggerHandler mLogger;
    private final String mName;
    private final SpeakerHandler mSpeaker;
    private final MediaSourceFactory mMediaSourceFactory;
    private PlaybackControllerHandler mPlaybackController;
    private SimpleExoPlayer mPlayer;

    public MediaPlayerHandler(Context context,
                              LoggerHandler logger,
                              String name,
                              @Nullable SpeakerType speakerType,
                              @Nullable PlaybackControllerHandler controller) {
        mContext = context;
        mLogger = logger;
        mName = name;
        mSpeaker = new SpeakerHandler(speakerType);
        mMediaSourceFactory = new MediaSourceFactory(mContext, mLogger, mName);

        if (controller != null) {
            mPlaybackController = controller;
            mPlaybackController.setMediaPlayer(this);
        }
        initializePlayer();
    }

    private void initializePlayer() {
        mPlayer = ExoPlayerFactory.newSimpleInstance(mContext, new DefaultTrackSelector());
        mPlayer.addListener(new PlayerEventListener());
        mPlayer.setPlayWhenReady(false);
    }

    public SimpleExoPlayer getPlayer() {
        return mPlayer;
    }

    private void resetPlayer() {
        mPlayer.setRepeatMode(Player.REPEAT_MODE_OFF);
        mPlayer.setPlayWhenReady(false);
    }

    public boolean isPlaying() {
        return mPlayer != null && mPlayer.getPlayWhenReady()
                && (mPlayer.getPlaybackState() == Player.STATE_BUFFERING
                || mPlayer.getPlaybackState() == Player.STATE_READY);
    }

    public long getDuration() {
        long duration = mPlayer.getDuration();
        return duration != C.TIME_UNSET ? duration : 0;
    }

    public Speaker getSpeaker() {
        return mSpeaker;
    }

    //
    // Handle playback directives from AAC engine
    //

    @Override
    public boolean prepare(boolean isOpusDong) {
        mLogger.postVerbose(sTag, String.format("(%s) Handling prepare()", mName));
        resetPlayer();
        mContext.getFileStreamPath(sFileName).delete();
        try (RandomAccessFile os = new RandomAccessFile(mContext.getFileStreamPath(sFileName), "rw")) {
            if (isOpusDong) {
                try (ByteArrayOutputStream bufStream = new ByteArrayOutputStream();
                     FileChannel fc = os.getChannel()) {
                    byte[] buffer = new byte[4096];
                    int size;
                    while (!isClosed()) {
                        while ((size = read(buffer)) > 0) bufStream.write(buffer, 0, size);
                    }
                    OpusDongHelper.decodeArrayToChannel(bufStream.toByteArray(), fc);
                } catch (Exception e) {
                    mLogger.postError(sTag, e);
                    return false;
                }

            } else {
                byte[] buffer = new byte[4096];
                int size;
                while (!isClosed()) {
                    while ((size = read(buffer)) > 0) os.write(buffer, 0, size);
                }
            }
        } catch (IOException e) {
            mLogger.postError(sTag, e);
            return false;
        }

        try {
            Uri uri = Uri.fromFile(mContext.getFileStreamPath(sFileName));
            MediaSource mediaSource = mMediaSourceFactory.createFileMediaSource(uri);
            mPlayer.prepare(mediaSource, true, false);
            return true;
        } catch (Exception e) {
            mLogger.postError(sTag, e.getMessage());
            String message = e.getMessage() != null ? e.getMessage() : "";
            mediaError(MediaError.MEDIA_ERROR_UNKNOWN, message);
            return false;
        }
    }

    @Override
    public boolean prepare(String url) {
        mLogger.postVerbose(sTag, String.format("(%s) Handling prepare(url)", mName));
        resetPlayer();
        Uri uri = Uri.parse(url);
        try {
            MediaSource mediaSource = mMediaSourceFactory.createHttpMediaSource(uri);
            mPlayer.prepare(mediaSource, true, false);
            return true;
        } catch (Exception e) {
            String message = e.getMessage() != null ? e.getMessage() : "";
            mLogger.postError(sTag, message);
            mediaError(MediaError.MEDIA_ERROR_UNKNOWN, message);
            return false;
        }
    }

    @Override
    public boolean play() {
        mLogger.postVerbose(sTag, String.format("(%s) Handling play()", mName));
        mPlayer.setPlayWhenReady(true);
        return true;
    }

    @Override
    public boolean stop() {
        mLogger.postVerbose(sTag, String.format("(%s) Handling stop()", mName));
        mPlayer.setPlayWhenReady(false);
        return true;
    }

    @Override
    public boolean pause() {
        mLogger.postVerbose(sTag, String.format("(%s) Handling pause()", mName));
        mPlayer.setPlayWhenReady(false);
        return true;
    }

    @Override
    public boolean resume() {
        mLogger.postVerbose(sTag, String.format("(%s) Handling resume()", mName));
        mPlayer.setPlayWhenReady(true);
        return true;
    }

    @Override
    public boolean setPosition(long position) {
        mLogger.postVerbose(sTag, String.format("(%s) Handling setPosition(%s)", mName, position));
        mPlayer.seekTo(position);
        return true;
    }

    @Override
    public long getPosition() {
        return Math.abs(mPlayer.getCurrentPosition());
    }

    //
    // Handle ExoPlayer state changes and notify AAC engine
    //

    private void onPlaybackStarted() {
        mLogger.postVerbose(sTag, String.format("(%s) Media State Changed. STATE: PLAYING", mName));
        mediaStateChanged(MediaState.PLAYING);
        if (mPlaybackController != null) {
            mPlaybackController.start();
        }
    }

    private void onPlaybackStopped() {
        mLogger.postVerbose(sTag, String.format("(%s) Media State Changed. STATE: STOPPED", mName));
        mediaStateChanged(MediaState.STOPPED);
        if (mPlaybackController != null) {
            mPlaybackController.stop();
        }
    }

    private void onPlaybackFinished() {
        if (isRepeating()) {
            mPlayer.seekTo(0);
            mPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
        } else {
            mPlayer.setRepeatMode(Player.REPEAT_MODE_OFF);
            if (mPlaybackController != null) {
                mPlaybackController.reset();
            }
            mLogger.postVerbose(sTag, String.format("(%s) Media State Changed. STATE: STOPPED", mName));
            mediaStateChanged(MediaState.STOPPED);
            if (mPlaybackController != null) {
                mPlaybackController.stop();
            }
        }
    }

    private void onPlaybackBuffering() {
        mLogger.postVerbose(sTag, String.format("(%s) Media State Changed. STATE: BUFFERING", mName));
        mediaStateChanged(MediaState.BUFFERING);
    }

    //
    // ExoPlayer event listener
    //
    private class PlayerEventListener extends Player.DefaultEventListener {

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            Log.e("PlayerEventListener", "playback state: " + playbackState);
            switch (playbackState) {
                case Player.STATE_ENDED:
                    if (playWhenReady) onPlaybackFinished();
                    break;
                case Player.STATE_READY:
                    if (playWhenReady) onPlaybackStarted();
                    else onPlaybackStopped();
                    break;
                case Player.STATE_BUFFERING:
                    if (playWhenReady) onPlaybackBuffering();
                    break;
                default:
                    // Disregard other states
                    break;
            }
        }

        @Override
        public void onPlayerError(ExoPlaybackException e) {
            String message;
            if (e.type == ExoPlaybackException.TYPE_SOURCE) {
                message = "ExoPlayer Source Error: " + e.getSourceException().getMessage();
            } else if (e.type == ExoPlaybackException.TYPE_RENDERER) {
                message = "ExoPlayer Renderer Error: " + e.getRendererException().getMessage();
            } else if (e.type == ExoPlaybackException.TYPE_UNEXPECTED) {
                message = "ExoPlayer Unexpected Error: " + e.getUnexpectedException().getMessage();
            } else {
                message = e.getMessage();
            }
            mLogger.postError(sTag, "PLAYER ERROR: " + message);
            mediaError(MediaError.MEDIA_ERROR_INTERNAL_DEVICE_ERROR, message);
        }
    }

    //
    // SpeakerHandler
    //

    public class SpeakerHandler extends Speaker {

        //        private SeekBar mVolumeControl;
//        private TextView mMuteButton;
        private byte mVolume = 50;
        private boolean mIsMuted = false;

        public static final String ACTION_MUTE_CHANGED = "com.iflytek.cyber.iot.show.core.ACTION_MUTE_CHANGED";
        public static final String ACTION_VOLUME_CHANGED = "com.iflytek.cyber.iot.show.core.ACTION_VOLUME_CHANGED";
        public static final String EXTRA_MUTE = "mute";
        public static final String EXTRA_VOLUME = "volume";

        SpeakerHandler(@Nullable SpeakerType type) {
            super();
            if (type != SpeakerType.SYNCED) {
                // Link all non synced speakers with the same UI control
                localSpeakers.add(this);
            } else {
                // Link mute button to synced speakers only
            }
            setupUIVolumeControls(type);
        }

        @Override
        public boolean setVolume(byte volume) {
            mLogger.postInfo(sTag, String.format("(%s) Handling setVolume(%s)", mName, volume));
            mVolume = volume;
            if (mIsMuted) {
                mPlayer.setVolume(0);
                updateUIVolume((byte) 0);
            } else {
                float channelVolume = volume / 100f;
                mPlayer.setVolume(channelVolume);
                updateUIVolume(volume);
            }
            return true;
        }

        @Override
        public boolean adjustVolume(byte value) {
            return setVolume((byte) (mVolume + value));
        }

        @Override
        public byte getVolume() {
            if (mIsMuted) return 0;
            else return mVolume;
        }

        @Override
        public boolean setMute(boolean mute) {
            if (mute && !mIsMuted) {
                mLogger.postInfo(sTag, String.format("Handling mute (%s)", mName));
                updateMuteButton(true);
            } else if (!mute && mIsMuted) {
                mLogger.postInfo(sTag, String.format("Handling unmute (%s)", mName));
                updateMuteButton(false);
            }

            mIsMuted = mute;
            if (mute) {
                mPlayer.setVolume(0);
                updateUIVolume((byte) 0);
            } else {
                mPlayer.setVolume(mVolume / 100f);
                updateUIVolume(mVolume);
            }
            return true;
        }

        @Override
        public boolean isMuted() {
            return mIsMuted;
        }

        private void setupUIVolumeControls(@Nullable final SpeakerType type) {
            updateUIVolume(mVolume);
            updateMuteButton(mIsMuted);
        }

        private void updateMuteButton(final boolean isMuted) {
            Intent intent = new Intent(ACTION_MUTE_CHANGED);
            intent.putExtra(EXTRA_MUTE, isMuted);
            mContext.sendBroadcast(intent);
        }

        private void updateUIVolume(final byte volume) {
            Intent intent = new Intent(ACTION_VOLUME_CHANGED);
            intent.putExtra(EXTRA_VOLUME, volume);
            mContext.sendBroadcast(intent);
        }
    }
}
