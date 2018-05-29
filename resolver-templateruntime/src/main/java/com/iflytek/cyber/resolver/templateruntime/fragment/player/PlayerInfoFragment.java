package com.iflytek.cyber.resolver.templateruntime.fragment.player;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.CircularProgressDrawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.iflytek.cyber.resolver.audioplayer.AudioPlayerResolver;
import com.iflytek.cyber.resolver.audioplayer.service.AudioPlayer;
import com.iflytek.cyber.resolver.audioplayer.service.AudioPlayerService;
import com.iflytek.cyber.resolver.audioplayer.service.model.AudioItem;
import com.iflytek.cyber.resolver.playbackcontroller.PlaybackControllerResolver;
import com.iflytek.cyber.resolver.templateruntime.GlideApp;
import com.iflytek.cyber.resolver.templateruntime.R;

import java.util.Locale;

public class PlayerInfoFragment extends Fragment implements AudioPlayer.PlayerInfoCallback, View.OnClickListener, SeekBar.OnSeekBarChangeListener {
    private static final String TAG_PREVIOUS = "PREVIOUS";
    private static final String TAG_NEXT = "NEXT";
    private static final String TAG_PLAY_PAUSE = "PLAY_PAUSE";

    private OnAlbumCallback onAlbumCallback;

    private AudioPlayerResolver audioPlayerResolver;
    private PlaybackControllerResolver playbackControllerResolver;

    private TextView tvTitle;
    private TextView tvArtist;
    private TextView tvSlogan;
    private ImageView ivPrevious;
    private ImageView ivPlayPause;
    private ImageView ivNext;
    private ImageView ivAlbum;
    private TextView tvCurrentPosition;
    private TextView tvDuration;
    private ImageView ivProviderLogo;
    private TextView tvProviderName;

    private SeekBar seekBar;
    private boolean seekBarDragging = false;
    private int seekBarProgressTarget = -1;

    private AudioItem currentAudioItem;
    private DataBindingCallback callback;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_player_info, container, false);
        tvTitle = view.findViewById(R.id.title);
        tvArtist = view.findViewById(R.id.artist);
        seekBar = view.findViewById(R.id.seek_bar);
        tvSlogan = view.findViewById(R.id.slogan);
        ivPlayPause = view.findViewById(R.id.play_pause);
        ivNext = view.findViewById(R.id.skip_next);
        ivPrevious = view.findViewById(R.id.skip_previous);
        tvCurrentPosition = view.findViewById(R.id.current_position);
        tvDuration = view.findViewById(R.id.duration);
        ivProviderLogo = view.findViewById(R.id.music_provider_logo);
        tvProviderName = view.findViewById(R.id.music_provider_name);
        ivAlbum = view.findViewById(R.id.album);
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checker.start();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ivPrevious.setOnClickListener(this);
        ivPrevious.setTag(TAG_PREVIOUS);
        ivNext.setOnClickListener(this);
        ivNext.setTag(TAG_NEXT);
        ivPlayPause.setOnClickListener(this);
        ivPlayPause.setTag(TAG_PLAY_PAUSE);

        seekBar.setMax(100);
        seekBar.setEnabled(false);
        seekBar.setOnSeekBarChangeListener(this);

        ivAlbum.setImageResource(R.drawable.ic_default_album);
        ivAlbum.post(new Runnable() {
            @Override
            public void run() {
                if (onAlbumCallback != null) {
                    Bitmap bitmap = getBitmapFromVectorDrawable(R.drawable.ic_default_album);
                    onAlbumCallback.onAlbumChanged(bitmap);
                }
            }
        });

        AudioPlayer audioPlayer = getAudioPlayer();
        if (audioPlayer != null)
            audioPlayer.setPlayerInfoCallback(this);

    }

    public void setup(AudioPlayerResolver audioPlayerResolver) {
        this.audioPlayerResolver = audioPlayerResolver;
    }

    public void setup(PlaybackControllerResolver playbackControllerResolver) {
        this.playbackControllerResolver = playbackControllerResolver;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public OnAlbumCallback getOnAlbumCallback() {
        return onAlbumCallback;
    }

    public void setOnAlbumCallback(OnAlbumCallback onAlbumCallback) {
        this.onAlbumCallback = onAlbumCallback;
    }

    public AudioItem getCurrentAudioItem() {
        return currentAudioItem;
    }

    @Override
    public void onAudioItemUpdated(final AudioItem audioItem) {
        currentAudioItem = audioItem;
        if (audioItem == null) {
            if (callback != null)
                callback.onDataBind(null, null);
            tvTitle.setText(null);
            tvArtist.setText(null);
            tvArtist.setVisibility(View.GONE);
            tvTitle.setVisibility(View.GONE);
            tvSlogan.setVisibility(View.VISIBLE);
            tvCurrentPosition.setText(format(0));
            tvDuration.setText(format(0));
            ivAlbum.setImageResource(R.drawable.ic_default_album);
            ivAlbum.post(new Runnable() {
                @Override
                public void run() {
                    if (onAlbumCallback != null) {
                        Bitmap bitmap = getBitmapFromVectorDrawable(R.drawable.ic_default_album);
                        onAlbumCallback.onAlbumChanged(bitmap);
                    }
                }
            });
            ivPlayPause.setImageResource(R.drawable.ic_play_circle_outline_white_24dp);
        } else if (!TextUtils.isEmpty(audioItem.audioItemId)) {
            PlayerInfoPayload payload = PlayerInfoMap.get().get(audioItem.audioItemId);
            if (payload != null) {
                if (callback != null)
                    callback.onDataBind(audioItem, payload);
                tvTitle.setText(payload.content.title);
                tvArtist.setText(payload.content.titleSubtext1);
                tvSlogan.setVisibility(View.GONE);
                tvArtist.setVisibility(View.VISIBLE);
                tvTitle.setVisibility(View.VISIBLE);
                if (payload.content.art != null && payload.content.art.sources != null
                        && payload.content.art.sources.size() > 0) {
                    GlideApp.with(getContext())
                            .asBitmap()
                            .load(payload.content.art.sources.get(0).url)
                            .placeholder(R.drawable.ic_default_album)
                            .error(R.drawable.ic_default_album)
                            .listener(new RequestListener<Bitmap>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e,
                                                            Object model,
                                                            Target<Bitmap> target,
                                                            boolean isFirstResource) {
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(Bitmap resource,
                                                               Object model,
                                                               Target<Bitmap> target,
                                                               DataSource dataSource,
                                                               boolean isFirstResource) {
                                    if (onAlbumCallback != null)
                                        onAlbumCallback.onAlbumChanged(resource);
                                    return false;
                                }
                            })
                            .into(ivAlbum);
                } else {
                    ivAlbum.setImageResource(R.drawable.ic_default_album);
                    ivAlbum.post(new Runnable() {
                        @Override
                        public void run() {
                            if (onAlbumCallback != null) {
                                Bitmap bitmap = getBitmapFromVectorDrawable(R.drawable.ic_default_album);
                                onAlbumCallback.onAlbumChanged(bitmap);
                            }
                        }
                    });
                }
                if (payload.content.provider != null &&
                        payload.content.provider.logo != null &&
                        payload.content.provider.logo.sources.size() > 0) {
                    tvProviderName.setVisibility(View.GONE);
                    ivProviderLogo.setVisibility(View.VISIBLE);
                    Glide.with(getContext())
                            .load(payload.content.provider.logo.sources.get(0).url)
                            .listener(new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                    ivProviderLogo.setVisibility(View.GONE);
                                    tvProviderName.setVisibility(View.VISIBLE);
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                    return false;
                                }
                            })
                            .into(ivProviderLogo);
                    tvProviderName.setText(getString(R.string.music_from_source, payload.content.provider.name));
                } else if (payload.content.provider != null && !TextUtils.isEmpty(payload.content.provider.name)) {
                    ivProviderLogo.setVisibility(View.GONE);
                    tvProviderName.setVisibility(View.VISIBLE);
                    tvProviderName.setText(getString(R.string.music_from_source, payload.content.provider.name));
                } else {
                    ivProviderLogo.setVisibility(View.GONE);
                    tvProviderName.setVisibility(View.GONE);
                }
            }
        }
    }

    public void requestFocus() {
        if (tvSlogan.getVisibility() == View.VISIBLE) {
            tvSlogan.requestFocus();
        }
        if (tvTitle.getVisibility() == View.VISIBLE) {
            tvTitle.requestFocus();
        }
    }


    public void abandonFocus() {
        tvSlogan.clearFocus();
        tvTitle.clearFocus();
    }

    private AudioPlayer getAudioPlayer() {
        if (audioPlayerResolver != null) {
            AudioPlayerService service = audioPlayerResolver.getPlayerService();
            if (service != null) {
                AudioPlayer audioPlayer = service.getAudioPlayer();
                if (audioPlayer != null) {

                } else {
                    Log.w(getClass().getSimpleName(), "AudioPlayer is null");
                }
                return audioPlayer;
            } else {
                Log.w(getClass().getSimpleName(), "AudioPlayerService is null");
            }
        } else
            Log.w(getClass().getSimpleName(), "AudioPlayerResolver is null");
        return null;
    }

    public void setDataBindingCallback(DataBindingCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onProgressUpdated(int position) {
        AudioPlayer audioPlayer = getAudioPlayer();
        if (seekBarDragging)
            return;
        if (audioPlayer != null) {
            if (seekBar.getMax() != audioPlayer.getDuration())
                seekBar.setMax(audioPlayer.getDuration());
            seekBar.setProgress(position);
        } else {
            seekBar.setProgress(0);
        }
    }

    @Override
    public void onPlaybackStateUpdated(PlaybackState playbackState) {
        if (playbackState.getState() == PlaybackState.STATE_PLAYING) {
            ivPlayPause.setImageResource(R.drawable.ic_pause_circle_outline_white_24dp);
            seekBar.setEnabled(true);
        } else {
            ivPlayPause.setImageResource(R.drawable.ic_play_circle_outline_white_24dp);
        }
        if (playbackState.getState() == PlaybackState.STATE_STOPPED) {
            seekBar.setEnabled(false);
            seekBar.setProgress(0);
        }
    }

    @Override
    public void onClick(View v) {
        AudioPlayer audioPlayer = getAudioPlayer();
        if (audioPlayer == null)
            return;
        switch (v.getTag().toString()) {
            case TAG_PREVIOUS:
                playbackControllerResolver.postCommand(PlaybackControllerResolver.PREVIOUS_COMMAND_ISSUED);
                break;
            case TAG_NEXT:
                playbackControllerResolver.postCommand(PlaybackControllerResolver.NEXT_COMMAND_ISSUED);
                break;
            case TAG_PLAY_PAUSE:
                if (audioPlayer.isPlaying()) {
                    playbackControllerResolver.postCommand(PlaybackControllerResolver.PAUSE_COMMAND_ISSUED);
                    audioPlayer.pause();
                } else {
                    playbackControllerResolver.postCommand(PlaybackControllerResolver.PLAY_COMMAND_ISSUED);
                    audioPlayer.start();
                }
                break;
        }
    }

    private String format(int duration) {
        return String.format(Locale.getDefault(), "%2d:%02d", duration / 1000 / 60, duration / 1000 % 60);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        AudioPlayer audioPlayer = getAudioPlayer();
        if (audioPlayer != null) {
            tvCurrentPosition.setText(format(progress));
            tvDuration.setText(format(audioPlayer.getDuration()));
        } else {
            tvCurrentPosition.setText(format(0));
            tvDuration.setText(format(0));
        }
        if (fromUser) {
            seekBarProgressTarget = progress;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        seekBarDragging = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        seekBarDragging = false;

        if (seekBarProgressTarget != -1) {
            AudioPlayer audioPlayer = getAudioPlayer();
            if (audioPlayer != null) {
                audioPlayer.seekTo(seekBarProgressTarget);
            }
            seekBarProgressTarget = -1;
        }
    }

    public interface OnAlbumCallback {
        void onAlbumChanged(Bitmap bitmap);
    }

    private ServiceChecker checker = new ServiceChecker(new ServiceChecker.Callback() {
        @Override
        public boolean onCheck() {
            return getAudioPlayer() != null;
        }

        @Override
        public void onFinish() {
            AudioPlayer audioPlayer = getAudioPlayer();
            if (audioPlayer != null)
                audioPlayer.setPlayerInfoCallback(PlayerInfoFragment.this);
        }
    });

    private static class ServiceChecker extends Handler {
        private Callback callback;

        ServiceChecker(Callback callback) {
            this.callback = callback;
        }

        public void start() {
            sendEmptyMessage(0);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (!callback.onCheck()) {
                sendEmptyMessageDelayed(0, 100);
            } else {
                callback.onFinish();
            }
        }

        interface Callback {
            boolean onCheck();

            void onFinish();
        }
    }

    private Bitmap getBitmapFromVectorDrawable(int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(getContext(), drawableId);

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    public interface DataBindingCallback {
        void onDataBind(AudioItem audioItem, PlayerInfoPayload payload);
    }
}
