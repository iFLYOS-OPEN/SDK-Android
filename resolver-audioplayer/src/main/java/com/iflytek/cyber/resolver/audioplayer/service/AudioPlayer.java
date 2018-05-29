package com.iflytek.cyber.resolver.audioplayer.service;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;

import com.google.gson.JsonObject;
import com.iflytek.cyber.CyberDelegate;
import com.iflytek.cyber.resolver.audioplayer.DelegateStore;
import com.iflytek.cyber.resolver.audioplayer.service.model.AudioItem;
import com.iflytek.cyber.resolver.audioplayer.service.model.ProgressReport;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AudioPlayer {
    private static final String NAME_PLAYBACK_FAILED = "PlaybackFailed";
    private static final String NAME_PLAYBACK_FINISHED = "PlaybackFinished";
    private static final String NAME_PLAYBACK_NEARLY_FINISHED = "PlaybackNearlyFinished";
    private static final String NAME_PLAYBACK_PAUSED = "PlaybackPaused";
    private static final String NAME_PLAYBACK_QUEUE_CLEARED = "PlaybackQueueCleared";
    private static final String NAME_PLAYBACK_RESUMED = "PlaybackResumed";
    private static final String NAME_PLAYBACK_STARTED = "PlaybackStarted";
    private static final String NAME_PLAYBACK_STOPPED = "PlaybackStopped";
    private static final String NAME_PLAYBACK_STUTTER_FINISHED = "PlaybackStutterFinished";
    private static final String NAME_PLAYBACK_STUTTER_STARTED = "PlaybackStutterStarted";
    private static final String NAME_PROGRESS_REPORT_DELAY_ELAPSED = "ProgressReportDelayElapsed";
    private static final String NAME_PROGRESS_REPORT_INTERVAL_ELAPSED = "ProgressReportIntervalElapsed";
    private static final String NAME_STREAM_METADATA_EXTRACTED = "StreamMetadataExtracted";

    private static final String PAYLOAD_CURRENT_PLAYBACK_STATE = "currentPlaybackState";
    private static final String PAYLOAD_ERROR = "error";
    private static final String PAYLOAD_MESSAGE = "message";
    private static final String PAYLOAD_METADATA = "metadata";
    private static final String PAYLOAD_OFFSET_IN_MILLISECONDS = "offsetInMilliseconds";
    private static final String PAYLOAD_PLAYER_ACTIVITY = "playerActivity";
    private static final String PAYLOAD_STUTTER_DURATION_IN_MILLISECONDS = "stutterDurationInMilliseconds";
    private static final String PAYLOAD_TOKEN = "token";
    private static final String PAYLOAD_TYPE = "type";

    private static final String BEHAVIOR_CLEAR_ALL = "CLEAR_ALL";
    private static final String BEHAVIOR_CLEAR_ENQUEUE = "CLEAR_ENQUEUED";
    private static final String BEHAVIOR_ENQUEUE = "ENQUEUE";
    private static final String BEHAVIOR_REPLACE_ALL = "REPLACE_ALL";
    private static final String BEHAVIOR_REPLACE_ENQUEUED = "REPLACE_ENQUEUED";

    public static final int ACTIVITY_BUFFER_UNDERRUN = 1;
    public static final int ACTIVITY_FINISHED = 2;
    public static final int ACTIVITY_PAUSED = 3;
    public static final int ACTIVITY_IDLE = 0;
    public static final int ACTIVITY_PLAYING = 4;
    public static final int ACTIVITY_STOPPED = 5;

    private static final String MEDIA_ERROR_UNKNOWN = "MEDIA_ERROR_UNKNOWN";
    private static final String MEDIA_ERROR_INVALID_REQUEST = "MEDIA_ERROR_INVALID_REQUEST";
    private static final String MEDIA_ERROR_SERVICE_UNAVAILABLE = "MEDIA_ERROR_SERVICE_UNAVAILABLE";
    private static final String MEDIA_ERROR_INTERNAL_SERVER_ERROR = "MEDIA_ERROR_INTERNAL_SERVER_ERROR";
    private static final String MEDIA_ERROR_INTERNAL_DEVICE_ERROR = "MEDIA_ERROR_INTERNAL_DEVICE_ERROR";


    private static final String MEDIA_SESSION_TAG = AudioPlayer.class.getSimpleName();
    private static final int STREAM_TYPE = AudioManager.STREAM_MUSIC;

    private final PlaybackState.Builder mPlaybackStateBuilder = new PlaybackState.Builder();
    private final AudioFocusListener mAudioFocusListener = new AudioFocusListener();

    private MediaPlayer mPlayer;
    private MediaSession mMediaSession;
    private List<AudioItem> audioItems;
    private PlaybackState currentPlaybackState;
    private ProgressHandler progressHandler;
    private Context mContext;

    private String audioItemId; // mark current data source audioItemId
    private float mVolumeLeft = 1f, mVolumeRight = 1f;
    private int mPlaybackState = PlaybackState.STATE_NONE;
    private long stutterStart = 0;
    private int currentPosition = 0;
    private boolean isPaused = false;
    private int playerActivity = ACTIVITY_IDLE;
    private boolean cached = true;

    private PlayerInfoCallback playerInfoCallback;

    AudioPlayer(Context context) {
        mPlayer = new MediaPlayer();
        mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                int result = requestAudioFocus();
                if (result == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
                    Log.w(AudioPlayer.class.getSimpleName(), "Request audio focus failed");
                }
                start();

                final AudioItem audioItem = getAudioItemById(audioItemId);
                if (audioItem != null && audioItem.stream.offsetInMilliseconds > 0) {
                    mp.seekTo((int) audioItem.stream.offsetInMilliseconds);
                }
            }
        });
        audioItems = new ArrayList<>();
        mContext = context;

        mPlaybackStateBuilder.setActions(generateAvailableActions());
        mMediaSession = new MediaSession(context, MEDIA_SESSION_TAG);
        MediaSessionCallback mCallback = new MediaSessionCallback();
        mMediaSession.setCallback(mCallback);
        mMediaSession.setActive(true);

        mPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                if (extra == MediaPlayer.MEDIA_ERROR_UNSUPPORTED) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            AudioItem audioItem = getAudioItemById(audioItemId);
                            if (audioItem == null) return;
                            try {
                                URL url = new URL(audioItem.stream.url);
                                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                                conn.setInstanceFollowRedirects(false);
                                String redirectUrl = conn.getHeaderField("Location");
                                audioItem.stream.url = redirectUrl;
                                mPlayer.setDataSource(redirectUrl);
                                prepare();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                    return true;
                }
                return false;
            }
        });
        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                playerActivity = ACTIVITY_FINISHED;
                postDefaultPayloadEvent(NAME_PLAYBACK_FINISHED);

                next();
            }
        });
        mPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {

            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                // handle post nearly finish
                if (percent == 100 && !cached) {
                    Log.w(AudioPlayer.this.getClass().getSimpleName(), String.valueOf(percent));
                    postDefaultPayloadEvent(NAME_PLAYBACK_NEARLY_FINISHED);
                    cached = true;
                }
            }
        });
        mPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                JsonObject payload = new JsonObject();
                JsonObject currentPlaybackState = new JsonObject();
                AudioItem audioItem = getAudioItemById(audioItemId);
                if (audioItem != null) {
                    currentPlaybackState.addProperty(PAYLOAD_TOKEN, audioItem.stream.token);
                    payload.addProperty(PAYLOAD_TOKEN, audioItem.stream.token);
                }
                currentPlaybackState.addProperty(PAYLOAD_OFFSET_IN_MILLISECONDS, mPlayer.getCurrentPosition());
                switch (playerActivity) {
                    case ACTIVITY_BUFFER_UNDERRUN:
                        currentPlaybackState.addProperty(PAYLOAD_PLAYER_ACTIVITY, "BUFFER_UNDERRUN");
                        break;
                    case ACTIVITY_FINISHED:
                        currentPlaybackState.addProperty(PAYLOAD_PLAYER_ACTIVITY, "FINISHED");
                        break;
                    case ACTIVITY_IDLE:
                        currentPlaybackState.addProperty(PAYLOAD_PLAYER_ACTIVITY, "IDLE");
                        break;
                    case ACTIVITY_PAUSED:
                        currentPlaybackState.addProperty(PAYLOAD_PLAYER_ACTIVITY, "PAUSED");
                        break;
                    case ACTIVITY_PLAYING:
                        currentPlaybackState.addProperty(PAYLOAD_PLAYER_ACTIVITY, "PLAYING");
                        break;
                    case ACTIVITY_STOPPED:
                        currentPlaybackState.addProperty(PAYLOAD_PLAYER_ACTIVITY, "STOPPED");
                        break;
                }
                payload.add(PAYLOAD_CURRENT_PLAYBACK_STATE, currentPlaybackState);
                JsonObject error = new JsonObject();
                error.addProperty(PAYLOAD_TYPE, what == MediaPlayer.MEDIA_ERROR_UNKNOWN ?
                        "UNKNOWN" : "SERVER_DIED");
                switch (extra) {
                    case MediaPlayer.MEDIA_ERROR_IO:
                        error.addProperty(PAYLOAD_MESSAGE, "IO");
                        break;
                    case MediaPlayer.MEDIA_ERROR_MALFORMED:
                        error.addProperty(PAYLOAD_MESSAGE, "MALFORMED");
                        break;
                    case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                        error.addProperty(PAYLOAD_MESSAGE, "UNSUPPORTED");
                        break;
                    case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                        error.addProperty(PAYLOAD_MESSAGE, "TIMED_OUT");
                        break;
                }
                payload.add(PAYLOAD_ERROR, error);
                postEvent(NAME_PLAYBACK_FAILED, payload);
                return false;
            }
        });

        progressHandler = new ProgressHandler(
                mPlayer, new ProgressHandler.HandlerCallback() {
            boolean stutter = false;

            @Override
            public void onPositionUpdated(int position) {
                if (mPlayer.isPlaying()) {
                    // handle if stutter
                    if (position == currentPosition && !stutter) {
                        stutterStart = System.currentTimeMillis();
                        stutter = true;
                        postStutterStarted();
                        playerActivity = ACTIVITY_BUFFER_UNDERRUN;
                    } else {
                        stutter = false;
                        playerActivity = ACTIVITY_PLAYING;
                    }

                }
                currentPosition = position;
            }

            @Override
            public void onPositionUpdatedCallback(int position) {
                if (playerInfoCallback != null && isPlaying())
                    playerInfoCallback.onProgressUpdated(position);
            }

            @Override
            public void onProgressReportDelay(int offsetInMilliseconds) {
                postProgress(NAME_PROGRESS_REPORT_DELAY_ELAPSED, offsetInMilliseconds);
            }

            @Override
            public void onProgressReportInterval(int offsetInMilliseconds) {
                postProgress(NAME_PROGRESS_REPORT_INTERVAL_ELAPSED, offsetInMilliseconds);
            }
        });
        progressHandler.startUpdating();
        progressHandler.startUpdatingCallback();
    }

    public int getPlayerActivity() {
        return playerActivity;
    }

    public long getCurrentPosition() {
        return mPlayer.getCurrentPosition();
    }

    private void postProgress(String name, int offsetInMilliseconds) {
        JsonObject payload = new JsonObject();
        AudioItem audioItem = getAudioItemById(audioItemId);
        if (audioItem != null) {
            payload.addProperty(PAYLOAD_TOKEN, audioItem.stream.token);
        }
        payload.addProperty(PAYLOAD_OFFSET_IN_MILLISECONDS, offsetInMilliseconds);
        postEvent(name, payload);
    }

    /**
     * post stutter started event
     * notice that you should update {@link #stutterStart} value before call this method
     */
    private void postStutterStarted() {
        JsonObject payload = new JsonObject();
        AudioItem audioItem = getAudioItemById(audioItemId);
        if (audioItem != null) {
            payload.addProperty(PAYLOAD_TOKEN, audioItem.stream.token);
        }
        payload.addProperty(PAYLOAD_OFFSET_IN_MILLISECONDS, mPlayer.getCurrentPosition());
        postEvent(NAME_PLAYBACK_STUTTER_STARTED, payload);
    }

    /**
     * post stutter finished event,
     * method will reset {@link #stutterStart} value to 0.
     */
    private void postStutterFinished() {
        JsonObject payload = new JsonObject();
        AudioItem audioItem = getAudioItemById(audioItemId);
        if (audioItem != null) {
            payload.addProperty(PAYLOAD_TOKEN, audioItem.stream.token);
        }
        payload.addProperty(PAYLOAD_OFFSET_IN_MILLISECONDS, mPlayer.getCurrentPosition());
        if (stutterStart != 0) {
            payload.addProperty(PAYLOAD_STUTTER_DURATION_IN_MILLISECONDS,
                    System.currentTimeMillis() - stutterStart);
            stutterStart = 0;
        }
        postEvent(NAME_PLAYBACK_STUTTER_FINISHED, payload);
    }

    private void postDefaultPayloadEvent(String name) {
        Log.w(getClass().getSimpleName(), "POST " + name);
        JsonObject payload = new JsonObject();
        AudioItem audioItem = getAudioItemById(audioItemId);
        if (audioItem != null) {
            payload.addProperty(PAYLOAD_TOKEN, audioItem.stream.token);
        }
        payload.addProperty(PAYLOAD_OFFSET_IN_MILLISECONDS, mPlayer.getCurrentPosition());
        postEvent(name, payload);
    }

    public PlayerInfoCallback getPlayerInfoCallback() {
        return playerInfoCallback;
    }

    public void setPlayerInfoCallback(PlayerInfoCallback playerInfoCallback) {
        this.playerInfoCallback = playerInfoCallback;
    }

    /**
     * call cyber delegate to post event
     *
     * @param name    header name
     * @param payload payload json object
     */
    private void postEvent(String name, JsonObject payload) {
        CyberDelegate delegate = DelegateStore.get().getDelegate();
        if (delegate != null)
            delegate.postEvent(name, payload, true);
        else
            Log.e(getClass().getSimpleName(), "CyberDelegate is null!");
    }

    private static long generateAvailableActions() {
        long action = PlaybackState.ACTION_PAUSE
                | PlaybackState.ACTION_PLAY
                | PlaybackState.ACTION_PLAY_PAUSE
                | PlaybackState.ACTION_REWIND
                | PlaybackState.ACTION_SEEK_TO
                | PlaybackState.ACTION_SKIP_TO_NEXT
                | PlaybackState.ACTION_SKIP_TO_PREVIOUS
                | PlaybackState.ACTION_SKIP_TO_QUEUE_ITEM
                | PlaybackState.ACTION_STOP;
        if (Build.VERSION.SDK_INT >= 24)
            action |= PlaybackState.ACTION_PREPARE;
        return action;
    }

    private int requestAudioFocus() {
        final AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        if (null != audioManager) {
            return audioManager.requestAudioFocus(mAudioFocusListener,
                    STREAM_TYPE, AudioManager.AUDIOFOCUS_GAIN);
        }
        return 0;
    }

    private long getLongId(String audioItemId) {
        return audioItemId.hashCode();
    }

    private void updatePlaybackState(final int newState) {
        final PlaybackState.Builder builder = new PlaybackState.Builder();

        final AudioItem item = getAudioItemById(audioItemId);
        if (item != null) {
            builder.setActions(generateAvailableActions())
                    .setActiveQueueItemId(getLongId(item.audioItemId))
                    .setBufferedPosition(mPlayer.getCurrentPosition())
                    .setState(newState, mPlayer.getCurrentPosition(), 1f);
            currentPlaybackState = builder.build();

            mPlaybackState = newState;

            mMediaSession.setPlaybackState(currentPlaybackState);

            if (playerInfoCallback != null)
                playerInfoCallback.onPlaybackStateUpdated(currentPlaybackState);
        }
    }

    private void abandonAudioFocus() {
        final AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        if (null != audioManager) {
            audioManager.abandonAudioFocus(mAudioFocusListener);
        }
    }

    private void setVolume(final float leftVolume, final float rightVolume) {
        mVolumeLeft = leftVolume;
        mVolumeRight = rightVolume;
        if (null != mPlayer) {
            mPlayer.setVolume(leftVolume, rightVolume);
        } else {
            Log.e(getClass().getSimpleName(), "setVolume failed, media player is null!");
        }
    }

    private int getAudioItemIndexById(@NonNull String audioItemId) {
        for (int i = 0; i < audioItems.size(); i++) {
            if (!TextUtils.isEmpty(audioItemId) && audioItemId.equals(audioItems.get(i).audioItemId)) {
                return i;
            }
        }
        return -1;
    }

    public AudioItem getAudioItemById(@NonNull String audioItemId) {
        for (AudioItem audioItem : audioItems) {
            if (!TextUtils.isEmpty(audioItem.audioItemId)
                    && audioItemId.equals(audioItem.audioItemId)) {
                return audioItem;
            }
        }
        return null;
    }

    protected void updateAudioItem(String playBehavior, AudioItem audioItem) {
        switch (playBehavior) {
            case BEHAVIOR_ENQUEUE:
                audioItems.add(audioItem);
                break;
            case BEHAVIOR_REPLACE_ALL:
                audioItems = new ArrayList<>();
                audioItems.add(audioItem);
                switchTo(0);
                break;
            case BEHAVIOR_REPLACE_ENQUEUED:
                int index = getAudioItemIndexById(audioItemId);
                while (audioItems.size() > index + 1) {
                    audioItems.remove(audioItems.size() - 1);
                }
                audioItems.add(audioItem);
                break;
        }
    }

    private void restart() {
        mPlayer.reset();
        AudioItem audioItem = getAudioItemById(audioItemId);
        if (audioItem != null) {
            try {
                mPlayer.setDataSource(audioItem.stream.url);
                prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void resetMediaPlayer() {
        setVolume(mVolumeLeft, mVolumeRight);
        mPlayer.reset();
    }

    protected void reset() {
        mVolumeLeft = 1.0f;
        mVolumeRight = 1.0f;

        resetMediaPlayer();

        mPlaybackState = PlaybackState.STATE_NONE;

        mPlaybackStateBuilder.setActiveQueueItemId(MediaSession.QueueItem.UNKNOWN_ID);
        mPlaybackStateBuilder.setBufferedPosition(0);
        mPlaybackStateBuilder.setState(mPlaybackState, 0, 1f);
        currentPlaybackState = mPlaybackStateBuilder.build();
        mMediaSession.setPlaybackState(currentPlaybackState);
    }

    public void start() {
        int result = requestAudioFocus();
        if (result == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
            Log.w(AudioPlayer.class.getSimpleName(), "Request audio focus failed");
        }
        updatePlaybackState(PlaybackState.STATE_PLAYING);
        try {
            if (!isPaused) {
                mPlayer.start();
                postStutterFinished();
                JsonObject payload = new JsonObject();
                JsonObject metadata = new JsonObject();
                if (Build.VERSION.SDK_INT >= 26) {
                    MediaPlayer.DrmInfo drmInfo = mPlayer.getDrmInfo();
                    if (drmInfo != null)
                        for (UUID uuid : drmInfo.getSupportedSchemes()) {
                            metadata.addProperty(uuid.toString(), new String(drmInfo.getPssh().get(uuid)));
                        }
                }
                payload.add(PAYLOAD_METADATA, metadata);
                AudioItem audioItem = getAudioItemById(audioItemId);
                if (audioItem != null) {
                    payload.addProperty(PAYLOAD_TOKEN, audioItem.stream.token);
                }
                postEvent(NAME_STREAM_METADATA_EXTRACTED, payload);
            } else {
                isPaused = false;
                mPlayer.start();
                postDefaultPayloadEvent(NAME_PLAYBACK_RESUMED);
            }
            playerActivity = ACTIVITY_PLAYING;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void stop() {
        if (mPlaybackState != PlaybackState.STATE_STOPPED) {
            isPaused = false;
            mPlayer.stop();
            updatePlaybackState(PlaybackState.STATE_STOPPED);
            abandonAudioFocus();
            playerActivity = ACTIVITY_STOPPED;
        }
        postDefaultPayloadEvent(NAME_PLAYBACK_STOPPED);
    }

    void release() {
        mPlayer.release();
        mContext = null;
        playerActivity = ACTIVITY_IDLE;
    }

    private void previous() {
        int index = getAudioItemIndexById(audioItemId);
        if (index > 0) {
            index--;
            if (index < audioItems.size()) {
                switchTo(index);
            }
        }
    }

    private void next() {
        int index = getAudioItemIndexById(audioItemId);
        if (index != -1) {
            index++;
            if (index < audioItems.size()) {
                switchTo(index);
            }
        }
    }

    public void pause() {
        if (currentPlaybackState.getState() == PlaybackState.STATE_PAUSED)
            mAudioFocusListener.onPausedByUser();
        isPaused = true;
        postDefaultPayloadEvent(NAME_PLAYBACK_PAUSED);
        updatePlaybackState(PlaybackState.STATE_PAUSED);
        mPlayer.pause();
        playerActivity = ACTIVITY_PAUSED;
    }

    public boolean isPlaying() {
        return mPlayer.isPlaying();
    }

    public void seekTo(long offset) {
        if (Build.VERSION.SDK_INT >= 26)
            mPlayer.seekTo(offset, MediaPlayer.SEEK_PREVIOUS_SYNC);
        else
            mPlayer.seekTo((int) offset);
        if (isPaused)
            start();
    }

    public int getDuration() {
        return mPlayer.getDuration();
    }

    private void switchTo(int index) {
        AudioItem audioItem = null;
        if (index >= 0 && index < audioItems.size())
            audioItem = audioItems.get(index);
        if (audioItem != null) {
            try {
                mPlayer.reset();
                mPlayer.setDataSource(audioItem.stream.url);

                audioItemId = audioItem.audioItemId;
                progressHandler.updateAudioItemId(audioItemId);
                ProgressReport progressReport = audioItem.stream.progressReport;
                if (progressReport != null) {
                    if (progressReport.progressReportDelayInMilliseconds > 0)
                        progressHandler.setProgressReportDelayed(audioItemId, progressReport.progressReportDelayInMilliseconds);
                    if (progressReport.progressReportIntervalInMilliseconds > 0)
                        progressHandler.setProgressReportInterval(audioItemId, progressReport.progressReportIntervalInMilliseconds);
                }

                prepare();
            } catch (Exception e) {
                Log.e(getClass().getSimpleName(), "play url: " + audioItem.stream.url);
                JsonObject payload = new JsonObject();
                JsonObject currentPlaybackState = new JsonObject();
                payload.add(PAYLOAD_CURRENT_PLAYBACK_STATE, currentPlaybackState);
                currentPlaybackState.addProperty(PAYLOAD_TOKEN, audioItem.stream.token);
                currentPlaybackState.addProperty(PAYLOAD_OFFSET_IN_MILLISECONDS, mPlayer.getCurrentPosition());
                currentPlaybackState.addProperty(PAYLOAD_PLAYER_ACTIVITY, ACTIVITY_STOPPED);
                payload.addProperty(PAYLOAD_TOKEN, audioItem.stream.token);
                JsonObject error = new JsonObject();
                if (e instanceof IllegalArgumentException || e instanceof IllegalStateException) {
                    error.addProperty(PAYLOAD_TYPE, MEDIA_ERROR_INVALID_REQUEST);
                } else if (e instanceof IOException) {
                    error.addProperty(PAYLOAD_TYPE, MEDIA_ERROR_INTERNAL_DEVICE_ERROR);
                } else if (e instanceof SecurityException) {
                    error.addProperty(PAYLOAD_TYPE, MEDIA_ERROR_INTERNAL_SERVER_ERROR);
                } else {
                    error.addProperty(PAYLOAD_TYPE, MEDIA_ERROR_UNKNOWN);
                }
                error.addProperty(PAYLOAD_MESSAGE, e.getMessage());
                payload.add(PAYLOAD_ERROR, error);
                postEvent(NAME_PLAYBACK_FAILED, payload);
                e.printStackTrace();

                audioItems.remove(audioItem);
            }
            if (playerInfoCallback != null)
                playerInfoCallback.onAudioItemUpdated(audioItem);
            else
                Log.w(getClass().getSimpleName(), "Callback is null.");
        } else {
            if (mPlayer.isPlaying()) {
                mPlayer.stop();
            }
        }
    }

    protected void clearQueue(@NonNull String clearBehavior) {
        switch (clearBehavior) {
            case BEHAVIOR_CLEAR_ALL:
                audioItems = new ArrayList<>();
                stop();
                break;
            case BEHAVIOR_CLEAR_ENQUEUE:
                int index = getAudioItemIndexById(audioItemId);
                while (audioItems.size() > index + 1) {
                    audioItems.remove(audioItems.size() - 1);
                }
                break;
        }
        postEvent(NAME_PLAYBACK_QUEUE_CLEARED, new JsonObject());
    }

    private void prepare() {
        mPlayer.prepareAsync();
        stutterStart = System.currentTimeMillis();
        postStutterStarted();
        JsonObject payload = new JsonObject();
        AudioItem audioItem = getAudioItemById(audioItemId);
        if (audioItem != null) {
            payload.addProperty(PAYLOAD_TOKEN, audioItem.stream.token);
        }
        payload.addProperty(PAYLOAD_OFFSET_IN_MILLISECONDS, mPlayer.getCurrentPosition());
        postEvent(NAME_PLAYBACK_STARTED, payload);

        cached = false;
    }

    public String getAudioItemId() {
        return audioItemId;
    }

    private class MediaSessionCallback extends MediaSession.Callback {
        @Override
        public boolean onMediaButtonEvent(@NonNull Intent mediaButtonIntent) {
            Log.i(getClass().getSimpleName(), "session callback onMediaButtonEvent enter");
            final String action = mediaButtonIntent.getAction();
            final KeyEvent keyEvent = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (Intent.ACTION_MEDIA_BUTTON.equalsIgnoreCase(action)
                    && null != keyEvent) {
                final int keyCode = keyEvent.getKeyCode();
                switch (keyCode) {
                    case KeyEvent.KEYCODE_MEDIA_CLOSE:
                        stop();
                        release();
                        break;
                    case KeyEvent.KEYCODE_MEDIA_NEXT:
                        next();
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PAUSE:
                        pause();
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PLAY:
                        start();
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                        if (isPlaying()) {
                            pause();
                        } else {
                            start();
                        }
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                        previous();
                        break;
                    case KeyEvent.KEYCODE_MEDIA_REWIND:
                        restart();
                        break;
                    case KeyEvent.KEYCODE_MEDIA_STOP:
                        stop();
                        break;
                    default:
                        Log.w(getClass().getSimpleName(), "Unsupported key event: " + keyCode);
                        return false;
                }//end of switch

                return true;
            } else {
                Log.e(getClass().getSimpleName(), "Unknown intent action: " + action + ", or null key event: " + keyEvent);
                return false;
            }
        }

        @Override
        public void onPause() {
            Log.i(getClass().getSimpleName(), "Session callback onPause enter");
            pause();
        }

        @Override
        public void onPlay() {
            Log.i(getClass().getSimpleName(), "Session callback onPlay enter");
            start();
        }

        @Override
        public void onPrepare() {
            Log.i(getClass().getSimpleName(), "Session callback onPrepare enter");
            prepare();
        }

        @Override
        public void onRewind() {
            Log.i(getClass().getSimpleName(), "Session callback onRewind enter");
            restart();
        }

        @Override
        public void onSeekTo(long pos) {
            Log.i(getClass().getSimpleName(), "Session callback onSeekTo enter");
            seekTo(pos);
        }

        @Override
        public void onSkipToNext() {
            Log.i(getClass().getSimpleName(), "Session callback onSkipToNext enter");
            next();
        }

        @Override
        public void onSkipToPrevious() {
            Log.i(getClass().getSimpleName(), "Session callback onSkipToPrevious enter");
            previous();
        }

        @Override
        public void onSkipToQueueItem(long id) {
            Log.i(getClass().getSimpleName(), "Session callback onSkipToQueueItem enter");
            int index = 0; // default
            for (int i = 0; i < audioItems.size(); i++) {
                AudioItem audioItem = audioItems.get(i);
                if (getLongId(audioItem.audioItemId) == id)
                    index = i;
            }
            switchTo(index);
        }

        @Override
        public void onStop() {
            Log.i(getClass().getSimpleName(), "Session callback onStop enter");
            stop();
        }
    }


    private class AudioFocusListener implements AudioManager.OnAudioFocusChangeListener {
        private final float mLowVolPercent = 0.2f;
        private boolean mPauseByLossFocus = false;

        @Override
        public void onAudioFocusChange(int focusChange) {
            Log.i(getClass().getSimpleName(), "onAudioFocusChange enter, " + focusChange);

            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    setVolume(mVolumeLeft, mVolumeRight);

                    if (mPauseByLossFocus) {
                        mPauseByLossFocus = false;
                        start();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    stop();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    if (isPlaying()) {
                        mPauseByLossFocus = true;
                        pause();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    if (isPlaying()) {
                        mPlayer.setVolume(mLowVolPercent * mVolumeLeft, mLowVolPercent * mVolumeRight);
                    }
                    break;
                default:

            }//end of switch
        }

        void onPausedByUser() {
            mPauseByLossFocus = false;
        }

        boolean isPauseByLossFocus() {
            return mPauseByLossFocus;
        }
    }

    public interface PlayerInfoCallback {
        void onPlaybackStateUpdated(PlaybackState playbackState);

        void onAudioItemUpdated(AudioItem audioItem);

        void onProgressUpdated(int position);
    }
}
