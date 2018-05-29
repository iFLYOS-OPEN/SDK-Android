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

package com.iflytek.cyber.inspector;

import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.google.gson.JsonObject;
import com.iflytek.android.os.ThreadedHandler;
import com.iflytek.cyber.platform.Recorder;

import static android.media.AudioFormat.CHANNEL_IN_MONO;
import static android.media.AudioFormat.ENCODING_PCM_16BIT;

class SimpleRecorder extends Recorder {

    private static final String TAG = "SimpleRecorder";

    private static final int SAMPLE_RATE_16K = 16000;

    private boolean recording = false;
    private ThreadedHandler recordingHandler;

    private AudioRecord recorder;

    private byte[] buffer = new byte[getMinBufferSize()];

    protected SimpleRecorder(AudioListener listener) {
        super(listener);
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
    }

    @Override
    protected void onStart() {
    }

    @Override
    protected void onStop() {
        Log.v(TAG, "onStop");

        recording = false;

        if (recordingHandler != null) {
            recordingHandler.removeCallbacksAndMessages(null);
        }

        if (recorder != null) {
            recorder.stop();
        }
    }

    @Override
    protected void onPoke(JsonObject initiator) {
        Log.v(TAG, "onPoke");

        wakeup(initiator);
        recording = true;

        recorder.startRecording();

        recordingHandler.post(new Runnable() {
            @Override
            public void run() {
                final int length = recorder.read(buffer, 0, buffer.length);
                publish(buffer, length);

                if (recordingHandler != null && recording) {
                    recordingHandler.post(this);
                }
            }
        });
    }

}
