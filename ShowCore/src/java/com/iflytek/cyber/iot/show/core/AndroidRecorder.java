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

package com.iflytek.cyber.iot.show.core;

import android.content.Context;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.google.gson.JsonObject;
import com.iflytek.android.os.ThreadedHandler;
import com.iflytek.cyber.platform.Recorder;
import com.iflytek.cyber.platform.WakeWordDetector;
import com.iflytek.cyber.platform.WakeWordRoller;
import com.iflytek.cyber.platform.wwd.ivw.IvwWakeWordDetector;

import static android.media.AudioFormat.CHANNEL_IN_MONO;
import static android.media.AudioFormat.ENCODING_PCM_16BIT;

/**
 * 使用原生 Android API 实现的录音控件
 */
class AndroidRecorder extends Recorder
        implements WakeWordDetector.Listener {

    private static final String TAG = "AndroidRecorder";

    private static final int SAMPLE_RATE_16K = 16000;

    private final WakeWordDetector detector;
    private final WakeWordRoller roller = new WakeWordRoller();

    private boolean recording = false;
    private ThreadedHandler recordingHandler;

    private AudioRecord recorder;

    private byte[] buffer = new byte[getMinBufferSize()];

    AndroidRecorder(Context context, AudioListener listener) {
        this(context, 40, listener);
    }

    AndroidRecorder(Context context, int wakeupThreshold, AudioListener listener) {
        super(listener);
        detector = new IvwWakeWordDetector(context, wakeupThreshold, this);
    }

    private static int getMinBufferSize() {
        return AudioRecord.getMinBufferSize(SAMPLE_RATE_16K, CHANNEL_IN_MONO, ENCODING_PCM_16BIT);
    }

    private static AudioRecord createRecord(int audioSource,
                                            int sampleRateInHz, int channelConfig, int audioFormat) {
        return new AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat,
                AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat));
    }

    private static AudioRecord createRecord(int audioSource) {
        return createRecord(audioSource, SAMPLE_RATE_16K, CHANNEL_IN_MONO, ENCODING_PCM_16BIT);
    }

    @Override
    protected void onCreate() {
        Log.v(TAG, "onCreate");

        recordingHandler = new ThreadedHandler("recorder");

        recorder = createRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION);

        detector.create();
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "onDestroy");

        if (recordingHandler != null) {
            recordingHandler.quit();
            recordingHandler = null;
        }

        if (recorder != null) {
            recorder.release();
            recorder = null;
        }

        detector.destroy();
        roller.reset();
    }

    @Override
    protected void onStart() {
        Log.v(TAG, "onStart");

        recording = true;

        roller.reset();
        detector.start();

        recorder.startRecording();

        recordingHandler.post(new Runnable() {
            @Override
            public void run() {
                final int length = recorder.read(buffer, 0, buffer.length);
                if (!publish(buffer, length) && detector.write(buffer, length)) {
                    roller.write(buffer, length);
                }

                if (recordingHandler != null && recording) {
                    recordingHandler.post(this);
                }
            }
        });
    }

    @Override
    protected void onStop() {
        Log.v(TAG, "onStop");

        recording = false;

        if (recordingHandler != null) {
            recordingHandler.removeCallbacksAndMessages(null);
        }

        if (recorder != null && recorder.getState() != AudioRecord.STATE_UNINITIALIZED) {
            recorder.stop();
        }

        detector.stop();
    }

    @Override
    protected void onPoke(JsonObject initiator) {
        detector.stop();
        wakeup(initiator);
    }

    @Override
    public void onWakeup(long beginMs, long endMs) {
        if (beginMs == 0 && endMs == 0) {
            wakeup(INITIATOR_WAKEWORD);
            roller.reset();
            return;
        }

        Log.d(TAG, "wakeWordIndices: " + 500 + ", " + (500 + (endMs - beginMs)));

        final JsonObject initiator = makeWakeWordInitiator(500, 500 + (endMs - beginMs));
        wakeup(initiator);

        final byte[] preRoll = roller.read(beginMs);
        publish(preRoll, preRoll.length);
    }

}
