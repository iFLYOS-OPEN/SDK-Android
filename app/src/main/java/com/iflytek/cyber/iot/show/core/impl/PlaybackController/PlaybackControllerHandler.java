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

package com.iflytek.cyber.iot.show.core.impl.PlaybackController;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.iflytek.cyber.iot.show.core.impl.Logger.LoggerHandler;
import com.iflytek.cyber.iot.show.core.impl.MediaPlayer.MediaPlayerHandler;

import java.lang.ref.WeakReference;
import java.util.Formatter;
import java.util.Locale;

import cn.iflyos.iace.iflyos.MediaPlayer;
import cn.iflyos.iace.iflyos.PlaybackController;

public class PlaybackControllerHandler extends PlaybackController {

    private static final String sTag = "PlaybackController";
    private static final int SHOW_PROGRESS = 0;

    private final Context mContext;
    private final LoggerHandler mLogger;
    private final ProgressHandler mProgressHandler;
    private final StringBuilder mStringBuilder;
    private final Formatter mFormatter;
    private MediaPlayerHandler mMediaPlayer;
    private Handler mUIHandler;

    private PlaybackCallback playbackCallback;

//    private ImageButton mControlPrev, mControlPlayPause, mControlNext;
//    private TextView mProgressTime, mEndTime, mTitle, mArtist, mProvider;
//    private ProgressBar mProgress;

    public PlaybackControllerHandler(Context context, LoggerHandler logger) {
        mContext = context;
        mLogger = logger;
        mUIHandler = new Handler(Looper.getMainLooper());
        mProgressHandler = new ProgressHandler(this);
        mStringBuilder = new StringBuilder();
        mFormatter = new Formatter(mStringBuilder, Locale.US);
        setupGUI();
    }

    public void setPlaybackCallback(PlaybackCallback playbackCallback) {
        this.playbackCallback = playbackCallback;
    }

    public void previousButtonPressed() {
        super.previousButtonPressed();
        mLogger.postVerbose(sTag, "Calling previousButtonPressed()");
    }

    public void playButtonPressed() {
        super.playButtonPressed();
        mLogger.postVerbose(sTag, "Calling playButtonPressed()");
    }

    public void pauseButtonPressed() {
        super.pauseButtonPressed();
        mLogger.postVerbose(sTag, "Calling pauseButtonPressed()");
    }

    public void nextButtonPressed() {
        super.nextButtonPressed();
        mLogger.postVerbose(sTag, "Calling nextButtonPressed()");
    }

    public void setMediaPlayer(MediaPlayerHandler mediaPlayer) {
        mMediaPlayer = mediaPlayer;
    }

    public MediaPlayerHandler getMediaPlayer() {
        return mMediaPlayer;
    }


    //
    // GUI updates
    //

    private void setupGUI() {
//        mControlPrev = mActivity.findViewById( R.id.prevControlButton );
//        mControlPlayPause = mActivity.findViewById( R.id.playControlButton );
//        mControlNext = mActivity.findViewById( R.id.nextControlButton );
//        mProgress = mActivity.findViewById( R.id.mediaProgressBar );
//        mProgressTime = mActivity.findViewById( R.id.mediaProgressTime );
//        mEndTime = mActivity.findViewById( R.id.mediaEndTime );
//        mTitle = mActivity.findViewById( R.id.mediaTitle );
//        mArtist = mActivity.findViewById( R.id.mediaArtist );
//        mProvider = mActivity.findViewById( R.id.mediaProvider );
//
//        mControlPrev.setOnClickListener( new View.OnClickListener() {
//            @Override
//            public void onClick( View v ) { previousButtonPressed(); }
//        });
//        mControlPlayPause.setOnClickListener( new View.OnClickListener() {
//            @Override
//            public void onClick( View v ) {
//                if ( mMediaPlayer.isPlaying() ) pauseButtonPressed();
//                else playButtonPressed();
//            }
//        });
//
//        mControlNext.setOnClickListener( new View.OnClickListener() {
//            @Override
//            public void onClick( View v ) { nextButtonPressed(); }
//        });
//
//        mActivity.runOnUiThread( new Runnable() {
//            @Override
//            public void run() {
//                mControlPrev.setEnabled( false );
//                mControlPlayPause.setEnabled( false );
//                mControlNext.setEnabled( false );
//            }
//        });
    }

    public void setPlayerInfo(final String title, final String artist, final String provider) {
//        mActivity.runOnUiThread( new Runnable() {
//            @Override
//            public void run() {
//                mTitle.setText( title );
//                mArtist.setText( artist );
//                mProvider.setText( provider );
//            }
//        });
    }

    public void start() {
//        if ( mMediaPlayer == null ) { return; }
//        mActivity.runOnUiThread( new Runnable() {
//            @Override
//            public void run() {
////                mControlPlayPause.setImageResource( R.drawable.control_selector_pause );
//                mControlPrev.setEnabled( true );
//                mControlPlayPause.setEnabled( true );
//                mControlNext.setEnabled( true );
//                mProgress.setMax( 1000 );
//
//            }
//        });
        mProgressHandler.sendEmptyMessage(SHOW_PROGRESS);
        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                if (playbackCallback != null) {
                    playbackCallback.onPlaybackStateChanged(MediaPlayer.MediaState.PLAYING);
                }
            }
        });
    }

    public void stop() {
        if (mMediaPlayer == null) {
            return;
        }
        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
//                mControlPlayPause.setImageResource( R.drawable.control_selector_play );
                if (playbackCallback != null) {
                    playbackCallback.onPlaybackStateChanged(MediaPlayer.MediaState.STOPPED);
                }
                mProgressHandler.removeMessages(SHOW_PROGRESS);
            }
        });
    }

    public void reset() {
        if (mMediaPlayer == null) {
            return;
        }
        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
//                mControlPlayPause.setImageResource( R.drawable.control_selector_play );
//                mControlPrev.setEnabled( false );
//                mControlPlayPause.setEnabled( false );
//                mControlNext.setEnabled( false );
//                mProgressHandler.removeMessages( SHOW_PROGRESS );
//                resetProgress();
//                setPlayerInfo( "", "", "" );
            }
        });
    }

    public void updateControlButton(final String name, final boolean enabled) {
        if (mMediaPlayer == null) {
            return;
        }
//        mActivity.runOnUiThread( new Runnable() {
//            @Override
//            public void run() {
//                switch ( name ) {
//                    case "PREVIOUS":
//                        mControlPrev.setEnabled( enabled );
//                        break;
//                    case "PLAY_PAUSE":
//                        mControlPlayPause.setEnabled( enabled );
//                        break;
//                    case "NEXT":
//                        mControlNext.setEnabled( enabled );
//                        break;
//                }
//            }
//        });
    }

    private void resetProgress() {
//        mActivity.runOnUiThread( new Runnable() {
//            @Override
//            public void run() {
//                mProgress.setProgress( 0 );
//                mProgressTime.setText( "0:00" );
//                mEndTime.setText( "0:00" );
//            }
//        });
    }

    private long setProgress() {
        if (mMediaPlayer == null) return 0;

        long position = mMediaPlayer.getPosition();
        long duration = mMediaPlayer.getDuration();
//        if ( mProgress != null ) {
//            if ( duration > 0 ) {
//                long pos = 1000L * position / duration;
//                mProgress.setProgress( ( int ) pos);
//            }
//        }
//
//        mEndTime.setText( stringForTime( ( int ) duration ) );
//        mProgressTime.setText( stringForTime( ( int ) position ) );

        return position;
    }

    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        mStringBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    private static class ProgressHandler extends Handler {

        private final WeakReference<PlaybackControllerHandler> mController;

        ProgressHandler(PlaybackControllerHandler controller) {
            mController = new WeakReference<>(controller);
        }

        @Override
        public void handleMessage(Message msg) {
            PlaybackControllerHandler controller = mController.get();

            long pos;
            switch (msg.what) {
                case SHOW_PROGRESS:
                    pos = controller.setProgress();
                    if (controller.getMediaPlayer().isPlaying()) {
                        if (controller.playbackCallback != null) {
                            controller.playbackCallback.onPositionUpdated(pos);
                        }
                        msg = obtainMessage(SHOW_PROGRESS);
                        sendMessageDelayed(msg, 1000 - (pos % 1000));
                    }
                    break;
            }
        }
    }

    public interface PlaybackCallback {
        void onPositionUpdated(long position);

        void onPlaybackStateChanged(MediaPlayer.MediaState state);
    }
}
