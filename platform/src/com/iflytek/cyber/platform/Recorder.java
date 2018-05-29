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

package com.iflytek.cyber.platform;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.JsonObject;
import com.iflytek.android.io.CloseableUtil;

import okio.BufferedSink;
import okio.Okio;
import okio.Pipe;
import okio.Source;

/**
 * 录音机抽象
 */
public abstract class Recorder {

    public static final JsonObject INITIATOR_PRESS_AND_HOLD = makeInitiator("PRESS_AND_HOLD");
    public static final JsonObject INITIATOR_TAP = makeInitiator("TAP");
    public static final JsonObject INITIATOR_WAKEWORD = makeInitiator("WAKEWORD");

    private static final float AUDIO_METER_MAX_DB = 10f;
    private static final float AUDIO_METER_MIN_DB = -2f;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private final AudioListener listener;

    private float noiseLevel = 75f;

    private BufferedSink consumer;

    protected Recorder(AudioListener listener) {
        this.listener = listener;
        uiHandler.post(this::onCreate);
    }

    private static float calculateRms(byte data[], int length) {
        long l1 = 0;
        long l2 = 0;
        int k = length / 2;

        for (int i = length; i >= 2; i -= 2) {
            int j1 = (short) ((data[i - 1] << 8) + (0xff & data[i - 2]));
            l1 += j1;
            l2 += j1 * j1;
        }

        return (float) Math.sqrt((l2 * (long) k - l1 * l1) / (long) (k * k));
    }

    private static int convertRmsDbToVolume(float rmsDb) {
        int i = (int) ((Math.min(Math.max(rmsDb, AUDIO_METER_MIN_DB), AUDIO_METER_MAX_DB) + 2.0f) * 100f / 12f);
        return i < 30 ? 0 : i / 10 * 10;
    }

    private static JsonObject makeInitiator(String type) {
        final JsonObject initiator = new JsonObject();
        initiator.addProperty("type", type);
        return initiator;
    }

    protected static JsonObject makeWakeWordInitiator(long beginMs, long endMs) {
        final long beginSamples = millisToSamples(beginMs);
        final long endSamples = millisToSamples(endMs);

        final JsonObject wakeWordIndices = new JsonObject();
        wakeWordIndices.addProperty("startIndexInSamples", beginSamples);
        wakeWordIndices.addProperty("endIndexInSamples", endSamples);

        final JsonObject payload = new JsonObject();
        payload.add("wakeWordIndices", wakeWordIndices);

        final JsonObject initiator = INITIATOR_WAKEWORD.deepCopy();
        initiator.add("payload", payload);

        return initiator;
    }

    private static long millisToSamples(long millis) {
        // 16kHz = 16000 samples per second
        return millis * 16;
    }

    private static long millisToBytes(long millis) {
        // 16-bit = 2 bytes per sample
        return millisToSamples(millis) * 2;
    }

    public final void start() {
        if (consumer != null) {
            CloseableUtil.safeClose(consumer);
            consumer = null;
        }

        uiHandler.post(this::onStart);
    }

    public final void stop() {
        if (consumer != null) {
            CloseableUtil.safeClose(consumer);
            consumer = null;
        }

        uiHandler.post(this::onStop);
    }

    public final void sleep() {
        if (consumer != null) {
            CloseableUtil.safeClose(consumer);
            consumer = null;
        }
    }

    public final void poke(JsonObject initiator) {
        uiHandler.post(() -> this.onPoke(initiator));
    }

    public final void destroy() {
        stop();
        uiHandler.removeCallbacksAndMessages(null);
        uiHandler.post(this::onDestroy);
    }

    public final boolean isAwake() {
        return consumer != null;
    }

    /**
     * 创建录音机
     */
    protected abstract void onCreate();

    /**
     * 销毁录音机
     */
    protected abstract void onDestroy();

    /**
     * 开启录音机，开始监听唤醒
     */
    protected abstract void onStart();

    /**
     * 关闭录音机，停止监听唤醒
     */
    protected abstract void onStop();

    /**
     * 外部唤醒录音机，使用上次相同的拾音参数
     */
    protected abstract void onPoke(JsonObject initiator);

    /**
     * 内部唤醒录音机，当实现者自己的唤醒引擎触发时调用
     */
    protected final void wakeup(JsonObject initiator) {
        if (consumer != null) {
            return;
        }

        final Pipe pipe = new Pipe(millisToBytes(5000));
        consumer = Okio.buffer(pipe.sink());

        uiHandler.post(() -> listener.onWakeup(pipe.source(), initiator));
    }

    /**
     * 向外部发布音频数据
     *
     * @param data   数据
     * @param length 长度
     * @return {@code true} 表示数据已被消费；{@code false} 表示未唤醒，实现者可以用来计算唤醒
     */
    protected final boolean publish(byte[] data, int length) {
        if (consumer == null) {
            return false;
        }

        float rms = calculateRms(data, length);

        if (noiseLevel < rms) {
            noiseLevel = 0.999f * noiseLevel + 0.001f * rms;
        } else {
            noiseLevel = 0.95f * noiseLevel + 0.05f * rms;
        }

        float db = -120f;
        if ((double) noiseLevel > 0d && (double) (rms / noiseLevel) > 0.000001d) {
            db = 10f * (float) Math.log10(rms / noiseLevel);
        }

        final int volume = convertRmsDbToVolume(db);
        uiHandler.post(() -> listener.onVolumeChanged(volume));

        try {
            consumer.write(data, 0, length);
            consumer.flush();
            return true;
        } catch (Exception e) {
            stop();
            return true;
        }
    }

    public interface AudioListener {

        void onWakeup(Source audio, JsonObject initiator);

        void onVolumeChanged(int level);

    }

}
