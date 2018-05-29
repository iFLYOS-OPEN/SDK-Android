package com.iflytek.cyber.resolver.speechsynthesizer;

import android.util.Log;

import com.iflytek.opus.Decoder;

import java.io.IOException;

import okio.Buffer;
import okio.BufferedSource;
import okio.Source;
import okio.Timeout;

class OpusDecoderSource implements Source {

    private static final String TAG = "OpusDecoderSource";

    private final Decoder decoder = new Decoder();
    private final byte[] opus = new byte[256];

    private final BufferedSource source;

    OpusDecoderSource(BufferedSource source) {
        this.source = source;
        Log.d(TAG, "Decoding stream started");
    }

    @Override
    public long read(Buffer sink, long byteCount) throws IOException {
        int read = 0;
        while (read < byteCount) {
            opus[0] = source.readByte();
            int pkgLen = opus[0] & 0xff;
            if (pkgLen == 0) {
                return -1;
            }

            int off = 0;
            do {
                int r = source.read(opus, 4 + off, pkgLen - off);
                if (r < 0) {
                    break;
                }
                off += r;
            } while (off < pkgLen);

            byte[] pcm = decoder.decode(opus);

            sink.write(pcm);
            sink.flush();

            read += pcm.length;
        }

        return read;
    }

    @Override
    public Timeout timeout() {
        return Timeout.NONE;
    }

    @Override
    public void close() throws IOException {
        decoder.destroy();
        Log.d(TAG, "Decoding finished");
    }

}
