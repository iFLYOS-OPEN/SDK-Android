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
