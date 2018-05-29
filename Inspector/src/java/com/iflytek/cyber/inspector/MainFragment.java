package com.iflytek.cyber.inspector;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import com.iflytek.cyber.platform.resolver.ResolverManager;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okio.BufferedSource;
import okio.Source;

import static com.iflytek.cyber.platform.BuildConfig.IVS_SERVER;

public class MainFragment extends Fragment implements
        CyberHandler,
        CyberClient.DirectiveListener,
        Recorder.AudioListener,
        AudioManager.OnAudioFocusChangeListener {

    private static final String TAG = "CoreService";

    private static final String KEY_ENDPOINT = "endpoint";

    private LauncherActivity activity;

    private AudioManager am;
    private SharedPreferences pref;

    private AuthManager authManager;
    private TokenManager tokenManager;

    private Recorder recorder;

    private CyberCore cyber;
    private ResolverManager resolverManager;

    private long retryInterval = 1;
    private Handler retryHandler = new Handler(Looper.getMainLooper());

    private EngineState state = EngineState.OFF;
    private ConnectionState connectionState = ConnectionState.OFFLINE;

    private Gson gson;

    private Button wakeButton;
    private TextView directivesText;
    private ProgressBar volumeBar;

    private JsonObject initiator;

    private Handler contextUpdater = new Handler(Looper.getMainLooper());

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (LauncherActivity) context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate");

        //noinspection ConstantConditions
        am = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        pref = PreferenceManager.getDefaultSharedPreferences(getContext());

        authManager = new AuthManager(pref.getString("client_id", null));
        tokenManager = new TokenManager(new DefaultTokenStorage(getContext()), authManager);

        recorder = new SimpleRecorder(this);

        cyber = new CyberCore(this);

        ResolverManager.init(getContext(), cyber);
        resolverManager = ResolverManager.get();
        resolverManager.create();

        gson = new GsonBuilder().setPrettyPrinting().create();

        initClient();

        final JsonObject deviceState = new JsonObject();
        deviceState.addProperty("deviceId", DeviceId.get(activity));
        cyber.updateContext("Device", "DeviceState", deviceState);
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

        resolverManager.destroy();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        view.findViewById(R.id.clear).setOnClickListener(v -> activity.debug_clearToken());
        view.findViewById(R.id.set_endpoint).setOnClickListener(v -> activity.changeEndpoint());

        directivesText = view.findViewById(R.id.directives);

        wakeButton = view.findViewById(R.id.wake);
        wakeButton.setOnClickListener(v -> {
            directivesText.setText("");
            initiator = Recorder.INITIATOR_TAP;
            switchEngineState(EngineState.AWAKE);
        });

        volumeBar = view.findViewById(R.id.volume);
    }

    @Override
    @SuppressLint("ApplySharedPref")
    public void onResume() {
        super.onResume();
        if (tokenManager.hasToken()) {
            Log.d(TAG, "Connecting...");

            final String customEndpoint = pref.getString("custom_endpoint", "");
            final boolean useProdEndpoint = pref.getBoolean("prod", true);
            if (useProdEndpoint) {
                pref.edit().remove(KEY_ENDPOINT).commit();
            } else {
                pref.edit().putString(KEY_ENDPOINT, customEndpoint).commit();
            }

            initClient();
            connect();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        disconnect();
        contextUpdater.removeCallbacksAndMessages(null);
    }

    private void printStackTrace(Throwable e) {
        final StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        directivesText.append(writer.toString());
        directivesText.append("\n");
    }

    @Override
    public void onWakeup(Source audio, JsonObject initiator) {
        am.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE);

        resolverManager.updateContext();
        cyber.startRecognize(audio, initiator);
    }

    @Override
    public void onVolumeChanged(int level) {
        volumeBar.setProgress(level);
    }

    @Override
    public void stopCapture() {
        switchEngineState(EngineState.PROCESSING);
        cyber.finishRecognize();
    }

    @Override
    public void expectSpeech(JsonObject initiator) {
        this.initiator = initiator;
        switchEngineState(EngineState.AWAKE);
    }

    @Override
    @SuppressLint("ApplySharedPref")
    public void setEndpoint(String endpoint) {
        disconnect();
        pref.edit()
                .putString(KEY_ENDPOINT, endpoint)
                .putString("custom_endpoint", endpoint)
                .putBoolean("prod", false)
                .commit();

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
                directivesText.append("[Access Token]\n" + accessToken + "\n\n");
                CyberOkHttpClient.getClient().listen();
            }

            @Override
            public void onAuthExpired() {
                Log.e(TAG, "Auth expired");

                directivesText.append("[授权已过期]\n\n");

                connectionState = ConnectionState.OFFLINE;
                handleAuthExpired();
            }

            @Override
            public void onTokenRefreshFailed(Throwable t) {
                Log.e(TAG, "Failed to refresh token while connecting downchannel", t);

                directivesText.append("[Token 刷新失败]\n");
                printStackTrace(t);

                scheduleToReconnect();
            }
        });
    }

    private void disconnect() {
        connectionState = ConnectionState.OFFLINE;
        switchEngineState(EngineState.OFF);

        retryHandler.removeCallbacksAndMessages(null);
        retryInterval = 1;

        CyberOkHttpClient.getClient().disconnect();
    }

    private void handleAuthExpired() {
        // TODO: 告诉前端登录已过期
    }

    private void scheduleToReconnect() {
        connectionState = ConnectionState.CONNECTING;

        directivesText.append("[将在 " + retryInterval + " 秒后重试连接 IVS]\n\n");

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
        switchEngineState(EngineState.STANDBY);

        directivesText.append("[IVS 已连接]\n\n");
    }

    @Override
    public void onDisconnected(Throwable e) {
        directivesText.append("[IVS 连接断开]\n");
        printStackTrace(e);

        if (CyberClient.CyberException.isAuthFailed(e)) {
            Log.e(TAG, "Failed to connect due to invalid token, try refresh...", e);
            tokenManager.invalidateAccessToken();
        }

        contextUpdater.removeCallbacksAndMessages(null);
        switchEngineState(EngineState.OFF);

        if (connectionState == ConnectionState.OFFLINE) {
            Log.d(TAG, "Disconnected.");
            return;
        }

        scheduleToReconnect();
    }

    @Override
    public void onDirective(JsonObject directive) {
        directivesText.append("[下行指令]\n");
        directivesText.append(gson.toJson(directive));
        directivesText.append("\n\n");

        resolverManager.resolve(directive);
    }

    @Override
    public void onAttachment(String cid, BufferedSource source) {
        directivesText.append("[开始接收音频 " + cid + "]\n\n");
        cyber.attachmentReady(cid, source);
    }

    @Override
    public void onRecognizeStart(String dialogRequestId) {
        resolverManager.startDialog(dialogRequestId);

        directivesText.append("[识别事件开始: " + dialogRequestId + "]\n");
        directivesText.append("\n\n");
    }

    @Override
    public void onRecognizeDirective(String dialogRequestId, JsonObject directive) {
        directivesText.append("[识别事件返回指令]\n");
        directivesText.append(gson.toJson(directive));
        directivesText.append("\n\n");

        resolverManager.resolve(directive);
    }

    @Override
    public void onRecognizeFailure(String dialogRequestId, Throwable e) {
        Log.e(TAG, "recognize failed", e);
        resolverManager.cancelDialog(dialogRequestId);

        directivesText.append("[识别事件处理错误]\n");
        printStackTrace(e);

        switchEngineState(EngineState.STANDBY);
    }

    @Override
    public void onRecognizeEnd(String dialogRequestId) {
        resolverManager.finishDialog(dialogRequestId);

        directivesText.append("[所有指令接收完成]\n\n");
        switchEngineState(EngineState.STANDBY);
    }

    @Override
    public void onDialogResolved() {
        directivesText.append("[所有指令执行完成]\n\n");
        am.abandonAudioFocus(this);
    }

    @Override
    public void onEventDirective(JsonObject directive) {
        directivesText.append("[普通事件返回指令]\n");
        directivesText.append(gson.toJson(directive));
        directivesText.append("\n\n");

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

    private void switchEngineState(EngineState state) {
        if (this.state == state) {
            return;
        }

        Log.d(TAG, "Switching engine from " + this.state + " to " + state);

        if (this.state == EngineState.OFF && state == EngineState.STANDBY) {
            wakeButton.setText("点击唤醒");
            wakeButton.setEnabled(true);
        } else if (this.state == EngineState.STANDBY && state == EngineState.AWAKE) {
            wakeButton.setText("已唤醒，请说话");
            wakeButton.setEnabled(false);
            recorder.poke(initiator);
        } else if (this.state == EngineState.AWAKE && state == EngineState.PROCESSING) {
            wakeButton.setText("正在处理…");
            wakeButton.setEnabled(false);
            recorder.stop();
        } else if (this.state == EngineState.PROCESSING && state == EngineState.STANDBY) {
            wakeButton.setText("点击唤醒");
            wakeButton.setEnabled(true);
        } else if (this.state == EngineState.AWAKE && state == EngineState.STANDBY) {
            wakeButton.setText("点击唤醒");
            wakeButton.setEnabled(true);
            recorder.stop();
        } else if (state == EngineState.OFF) {
            wakeButton.setText("引擎关闭");
            wakeButton.setEnabled(false);
            recorder.stop();
        } else {
            throw new IllegalStateException("Invalid state transition " + this.state + " -> " + state);
        }

        this.state = state;
    }

    private enum EngineState {
        OFF,
        STANDBY,
        AWAKE,
        PROCESSING,
    }

    private enum ConnectionState {
        OFFLINE,
        CONNECTING,
        CONNECTED,
    }

}
