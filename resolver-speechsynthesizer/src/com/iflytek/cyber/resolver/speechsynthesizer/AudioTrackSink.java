package com.iflytek.cyber.resolver.speechsynthesizer;

import android.media.AudioTrack;

import java.io.IOException;

import okio.Buffer;
import okio.Sink;
import okio.Timeout;

class AudioTrackSink implements Sink {

    private final AudioTrack track;

    AudioTrackSink(AudioTrack track) {
        this.track = track;
    }

    @Override
    public void write(Buffer source, long byteCount) throws IOException {
        track.write(source.readByteArray(byteCount), 0, (int) byteCount);
    }

    @Override
    public void flush() throws IOException {
        track.flush();
    }

    @Override
    public Timeout timeout() {
        return Timeout.NONE;
    }

    @Override
    public void close() throws IOException {
    }

}
