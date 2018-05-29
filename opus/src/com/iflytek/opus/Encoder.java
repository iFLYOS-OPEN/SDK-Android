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

package com.iflytek.opus;

import java.io.IOException;
import java.nio.ByteBuffer;

@SuppressWarnings("JniMissingFunction")
public class Encoder {

    private static Encoder THIS;

    static {
        System.loadLibrary("jni_cyber_opus");
    }

    private ByteBuffer opus = ByteBuffer.allocateDirect(256);
    private ByteBuffer pcm = ByteBuffer.allocateDirect(640);
    private long h;

    public Encoder() {
        h = Encoder.create();
    }

    private static native long create();

    @SuppressWarnings("SpellCheckingInspection")
    private static native void destory(long h);

    private static native int encodeFrame(long h, ByteBuffer pcm, int pcmBytes, ByteBuffer opus);

    private static native void reset(long h);

    public static synchronized Encoder getEncoder() {
        if (THIS == null) {
            THIS = new Encoder();
        }
        return THIS;
    }

    public void reset() {
        Encoder.reset(this.h);
    }

    public byte[] encode(byte[] pcm) throws IOException {
        if (pcm.length != 640) {
            throw new IllegalArgumentException("opus encoder only support 640 bytes one frame");
        }
        this.pcm.position(0);
        this.pcm.limit(640);
        this.pcm.put(pcm);
        int len = Encoder.encodeFrame(this.h, this.pcm, 640, this.opus);
        if (len <= 0) {
            throw new IOException("encode error:" + len);
        }
        opus.position(0);
        opus.limit(len + 1);
        byte[] ret = new byte[len + 1];
        opus.get(ret);
        return ret;
    }

    public void destroy() {
        if (h != 0L) {
            Encoder.destory(h);
            h = 0L;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        destroy();
    }

}
