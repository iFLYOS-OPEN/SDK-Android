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
