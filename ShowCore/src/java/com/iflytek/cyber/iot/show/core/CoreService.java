package com.iflytek.cyber.iot.show.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.JsonObject;
import com.iflytek.cyber.platform.AuthManager;
import com.iflytek.cyber.platform.CyberCore;
import com.iflytek.cyber.platform.CyberHandler;
import com.iflytek.cyber.platform.DefaultTokenStorage;
import com.iflytek.cyber.platform.DeviceId;
import com.iflytek.cyber.platform.Recorder;
import com.iflytek.cyber.platform.TokenManager;
import com.iflytek.cyber.platform.client.CyberClient;
import com.iflytek.cyber.platform.client.CyberOkHttpClient;
import com.iflytek.cyber.platform.internal.android.app.StickyService;
import com.iflytek.cyber.platform.resolver.ResolverManager;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okio.BufferedSource;
import okio.Source;

import static com.iflytek.cyber.platform.BuildConfig.IVS_SERVER;

public class CoreService extends StickyService implements
        CyberHandler,
        CyberClient.DirectiveListener,
        Recorder.AudioListener,
        AudioManager.OnAudioFocusChangeListener {

    public static final String ACTION_SERVICE_PREPARED = "com.iflytek.cyber.iot.show.core.action.SERVICE_PREPARED";

    public static final String ACTION_TOGGLE_WAKE_UP_STATE = "com.iflytek.cyber.iot.show.core.action.TOGGLE_WAKE_UP_STATE";
    public static final String ACTION_WAKE_UP_STATE_CHANGED = "com.iflytek.cyber.iot.show.core.action.WAKE_UP_STATE_CHANGED";

    public static final String ACTION_AUTH_STATE_CHANGED = BuildConfig.APPLICATION_ID + ".action.ACTION_AUTH_STATE_CHANGED";

    private static final String TAG = "CoreService";

    private static final String KEY_ENDPOINT = "endpoint";
    private static final String KEY_TOKEN = "access_token";

    private AudioManager am;
    private SharedPreferences pref;

    private AuthManager authManager;
    private TokenManager tokenManager;

    private Recorder recorder;

    private CyberCore cyber;
    private ResolverManager resolverManager;

    private long retryInterval = 1;
    private Handler retryHandler = new Handler(Looper.getMainLooper());

    private ConnectionState connectionState = ConnectionState.OFFLINE;

    private Handler contextUpdater = new Handler(Looper.getMainLooper());

    private Recorder.AudioListener audioListener;

    public class CoreServiceBinder extends Binder {
        public CoreService getService() {
            return CoreService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new CoreServiceBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "onCreate");

        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        pref = PreferenceManager.getDefaultSharedPreferences(this);

        authManager = new AuthManager(BuildConfig.CLIENT_ID);
        tokenManager = new TokenManager(new DefaultTokenStorage(this), authManager);

        recorder = new AndroidRecorder(this, this);

        cyber = new CyberCore(this);

        ResolverManager.init(this, cyber);
        resolverManager = ResolverManager.get();
        resolverManager.create();

        initClient();

        final JsonObject deviceState = new JsonObject();
        deviceState.addProperty("deviceId", DeviceId.get(this));
        cyber.updateContext("Device", "DeviceState", deviceState);

        sendBroadcast(new Intent(ACTION_SERVICE_PREPARED));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy");

        disconnect();

        tokenManager.destroy();
        authManager.cancel();
        cyber.destroy();
        recorder.destroy();
        contextUpdater.removeCallbacksAndMessages(null);
        resolverManager.destroy();
    }

    /**
     * 当认证状态发生变化时，前端应该 startService 通知改服务上线或下线。
     */
    @Override
    public void onStartCommand(Intent intent) {
        if (intent != null && ACTION_TOGGLE_WAKE_UP_STATE.equals(intent.getAction())) {
            if (recorder.isAwake()) {
                cancelRecognize();
            } else {
                recorder.poke(Recorder.INITIATOR_TAP);
            }
        } else if (connectionState == ConnectionState.OFFLINE) {
            if (tokenManager.hasToken()) {
                Log.d(TAG, "Connecting...");
                connect();
            }
        } else {
            if (!tokenManager.hasToken()) {
                Log.d(TAG, "Disconnecting...");
                disconnect();
            }
        }
    }


    public Recorder.AudioListener getAudioListener() {
        return audioListener;
    }

    public void setAudioListener(Recorder.AudioListener audioListener) {
        this.audioListener = audioListener;
    }

    @Override
    public void onWakeup(Source audio, JsonObject initiator) {
        Intent intent = new Intent(ACTION_WAKE_UP_STATE_CHANGED);
        intent.putExtra("state", true);
        sendBroadcast(intent);

        am.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE);

        resolverManager.updateContext();
        cyber.startRecognize(audio, initiator);

        if (audioListener != null)
            audioListener.onWakeup(audio, initiator);
    }

    @Override
    public void onVolumeChanged(int level) {
        if (audioListener != null)
            audioListener.onVolumeChanged(level);
    }

    @Override
    public void stopCapture() {
        Intent intent = new Intent(ACTION_WAKE_UP_STATE_CHANGED);
        intent.putExtra("state", false);
        sendBroadcast(intent);
        recorder.stop();
        cyber.finishRecognize();
    }

    private void cancelRecognize() {
        Intent intent = new Intent(ACTION_WAKE_UP_STATE_CHANGED);
        intent.putExtra("state", false);
        sendBroadcast(intent);

        am.abandonAudioFocus(this);

        recorder.sleep();
        cyber.cancelRecognize();
    }

    @Override
    public void expectSpeech(JsonObject initiator) {
        recorder.poke(initiator);
    }

    @Override
    @SuppressLint("ApplySharedPref")
    public void setEndpoint(String endpoint) {
        disconnect();
        pref.edit().putString(KEY_ENDPOINT, endpoint).commit();

        initClient();
        connect();
    }

    private void initClient() {
        final String endpoint = pref.getString(KEY_ENDPOINT, IVS_SERVER);
        CyberOkHttpClient.initClient(endpoint, new OkHttpClient.Builder().addInterceptor(chain -> {
            final Request request = chain.request()
                    .newBuilder()
                    .addHeader("Authorization", "Bearer " + tokenManager.getAccessToken())
                    .build();
            return chain.proceed(request);
        }).build(), this);
    }

    private void connect() {
        connectionState = ConnectionState.CONNECTING;
        tokenManager.retrieveAccessToken(new TokenManager.TokenCallback() {
            @Override
            public void onAccessToken(String accessToken) {
                CyberOkHttpClient.getClient().listen();
                pref.edit().putString(KEY_TOKEN, accessToken).apply();
            }

            @Override
            public void onAuthExpired() {
                Log.e(TAG, "Auth expired");
                connectionState = ConnectionState.OFFLINE;
                handleAuthExpired();
            }

            @Override
            public void onTokenRefreshFailed(Throwable t) {
                Log.e(TAG, "Failed to refresh token while connecting downchannel", t);
                scheduleToReconnect();
            }
        });
    }

    private void disconnect() {
        connectionState = ConnectionState.OFFLINE;
        recorder.stop();

        retryHandler.removeCallbacksAndMessages(null);
        retryInterval = 1;

        CyberOkHttpClient.getClient().disconnect();
    }

    private void handleAuthExpired() {
        sendBroadcast(new Intent(ACTION_AUTH_STATE_CHANGED));
    }

    private void scheduleToReconnect() {
        connectionState = ConnectionState.CONNECTING;

        retryHandler.removeCallbacksAndMessages(null);
        retryHandler.postDelayed(this::connect, TimeUnit.SECONDS.toMillis(retryInterval));
        Log.d(TAG, "scheduled to reconnect after " + retryInterval + " s");
        retryInterval += 2;
    }

    @Override
    public void onConnected() {
        Log.d(TAG, "Downchannel connected");

        contextUpdater.postDelayed(new Runnable() {
            @Override
            public void run() {
                resolverManager.updateContext();
                contextUpdater.postDelayed(this, TimeUnit.MINUTES.toMillis(15));
            }
        }, TimeUnit.MINUTES.toMillis(15));

        resolverManager.updateContext();
        cyber.synchronizeState();

        retryHandler.removeCallbacksAndMessages(null);
        retryInterval = 1;

        connectionState = ConnectionState.CONNECTED;
        recorder.start();
    }

    @Override
    public void onDisconnected(Throwable e) {
        if (CyberClient.CyberException.isAuthFailed(e)) {
            Log.e(TAG, "Failed to connect due to invalid token, try refresh...", e);
            tokenManager.invalidateAccessToken();
        }

        contextUpdater.removeCallbacksAndMessages(null);
        recorder.stop();

        if (connectionState == ConnectionState.OFFLINE) {
            Log.d(TAG, "Disconnected.");
            return;
        }

        scheduleToReconnect();
    }

    @Override
    public void onDirective(JsonObject directive) {
        resolverManager.resolve(directive);
    }

    @Override
    public void onAttachment(String cid, BufferedSource source) {
        cyber.attachmentReady(cid, source);
    }

    @Override
    public void onRecognizeStart(String dialogRequestId) {
        resolverManager.startDialog(dialogRequestId);
    }

    @Override
    public void onRecognizeDirective(String dialogRequestId, JsonObject directive) {
        resolverManager.resolve(directive);
    }

    @Override
    public void onRecognizeFailure(String dialogRequestId, Throwable e) {
        Log.e(TAG, "recognize failed", e);
        resolverManager.cancelDialog(dialogRequestId);

        Intent intent = new Intent(ACTION_WAKE_UP_STATE_CHANGED);
        intent.putExtra("state", false);
        sendBroadcast(intent);

        recorder.start();
    }

    @Override
    public void onRecognizeEnd(String dialogRequestId) {
        resolverManager.finishDialog(dialogRequestId);
        recorder.start();
    }

    @Override
    public void onDialogResolved() {
        am.abandonAudioFocus(this);
    }

    @Override
    public void onEventDirective(JsonObject directive) {
        resolverManager.resolve(directive);
    }

    @Override
    public void onEventFailure(Throwable e) {
        Log.e(TAG, "postEvent failed", e);
        // no-op
    }

    @Override
    public void onEventEnd() {
        // no-op
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
    }

    private enum ConnectionState {
        OFFLINE,
        CONNECTING,
        CONNECTED,
    }

}
