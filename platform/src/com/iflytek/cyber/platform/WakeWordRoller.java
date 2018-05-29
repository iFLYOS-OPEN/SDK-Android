package com.iflytek.cyber.platform;

import java.io.EOFException;

import okio.Buffer;

public class WakeWordRoller {

    private final Buffer buffer = new Buffer();
    private long rolled = 0;

    private static long millisToSamples(long millis) {
        // 16kHz = 16000 samples per second
        return millis * 16;
    }

    private static long millisToBytes(long millis) {
        // 16-bit = 2 bytes per sample
        return millisToSamples(millis) * 2;
    }

    public void reset() {
        buffer.clear();
        rolled = 0;
    }

    public void write(byte[] data, int length) {
        try {
            buffer.write(data, 0, length);
            rolled += length;
            final long overhead = buffer.size() - millisToBytes(5000);
            if (overhead > 0) buffer.skip(overhead);
        } catch (EOFException ignored) {
        }
    }

    public byte[] read(long beginMs) {
        try {
            final long beginBytes = millisToBytes(beginMs);
            final long overhead = buffer.size() - ((rolled - beginBytes) + millisToBytes(500));
            buffer.skip(overhead);

            final byte[] data = buffer.readByteArray();
            reset();

            return data;
        } catch (EOFException ignored) {
            return new byte[0];
        }
    }

}
