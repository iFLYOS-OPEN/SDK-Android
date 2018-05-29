package com.iflytek.cyber.resolver.xftemplateruntime;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.media.MediaCodec;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.iflytek.cyber.resolver.xftemplateruntime.model.Video;
import com.iflytek.cyber.resolver.xftemplateruntime.model.XfTemplatePayload;

public class PlayerFragment extends Fragment {
    private SurfaceView surfaceView;
    private TextView tvTitle;

    private SimpleExoPlayer player;

    private XfTemplatePayload payload;

    public PlayerCallback playerCallback;

    private AudioFocusListener focusListener;

    public static PlayerFragment generatePlayerFragment(XfTemplatePayload xfTemplatePayload) {
        PlayerFragment playerFragment = new PlayerFragment();
        playerFragment.payload = xfTemplatePayload;
        return playerFragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_player, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        surfaceView = view.findViewById(R.id.surface);
        tvTitle = view.findViewById(R.id.media_title);
        tvTitle.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (tvTitle.isAttachedToWindow()) {
                    tvTitle.animate().alpha(0).setDuration(300).start();
                }
            }
        }, 3000);

        String url = null;
        if (payload.videoMedia != null) {
            if (payload.videoMedia.sources != null && payload.videoMedia.sources.size() > 0) {
                Video.Source source = payload.videoMedia.sources.get(0);
                url = source.url;
            }
        }
        if (TextUtils.isEmpty(url)) {
            new AlertDialog.Builder(getContext())
                    .setTitle("错误")
                    .setMessage("无法解析该视频链接")
                    .setPositiveButton(android.R.string.yes, null)
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            if (playerCallback != null)
                                playerCallback.onComplete();
                        }
                    })
                    .show();
            return;
        }
        String title = payload.title;

        tvTitle.setText(title);

        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector =
                new DefaultTrackSelector(videoTrackSelectionFactory);

        player = ExoPlayerFactory.newSimpleInstance(getContext(), trackSelector);
        player.setVideoSurfaceView(surfaceView);

        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(getContext(),
                Util.getUserAgent(getContext(), "XfTemplateRuntimeResolver"), bandwidthMeter);
        MediaSource videoSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                .createMediaSource(Uri.parse(url));
        player.setPlayWhenReady(true);
        player.setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
        player.prepare(videoSource, true, true);
        player.addListener(new DefaultEventListener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                super.onPlayerStateChanged(playWhenReady, playbackState);
                if (playbackState == Player.STATE_ENDED) {
                    if (playerCallback != null)
                        playerCallback.onComplete();
                }
            }
        });
        player.addVideoListener(new SimpleExoPlayer.VideoListener() {
            @Override
            public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) surfaceView.getLayoutParams();
                if (params.height != surfaceView.getWidth() * height / width) {
                    params.height = surfaceView.getWidth() * height / width;
                    surfaceView.setLayoutParams(params);
                }
            }

            @Override
            public void onRenderedFirstFrame() {
            }
        });

        surfaceView.getHolder().setKeepScreenOn(true);

        focusListener = new AudioFocusListener();
        int result = requestAudioFocus();
        if (result != 1)
            Log.e(getClass().getSimpleName(), "Request focus failed");
    }

    private int requestAudioFocus() {
        final AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        if (null != audioManager) {
            return audioManager.requestAudioFocus(focusListener,
                    AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
        return 0;
    }

    private void abandonAudioFocus() {
        final AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        if (null != audioManager) {
            audioManager.abandonAudioFocus(focusListener);
        }
    }


    public void setPlayerCallback(PlayerCallback playerCallback) {
        this.playerCallback = playerCallback;
    }

    public void removePlayerCallback(PlayerCallback playerCallback) {
        this.playerCallback = playerCallback;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        player.release();
        abandonAudioFocus();
    }

    public interface PlayerCallback {
        void onComplete();
    }

    private class AudioFocusListener implements AudioManager.OnAudioFocusChangeListener {
        private final float mLowVolPercent = 0.2f;
        float mVolume = 1f;

        @Override
        public void onAudioFocusChange(int focusChange) {
            Log.i(getClass().getSimpleName(), "onAudioFocusChange enter, " + focusChange);

            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    player.setVolume(mVolume);
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    player.stop();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    player.stop();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    player.setVolume(mLowVolPercent * mVolume);
                    break;
                default:

            }//end of switch
        }
    }
}
