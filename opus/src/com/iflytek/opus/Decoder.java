package com.iflytek.opus;

import java.io.IOException;
import java.nio.ByteBuffer;

@SuppressWarnings("JniMissingFunction")
public class Decoder {

    private static Decoder THIS;

    static {
        System.loadLibrary("jni_cyber_opus");
    }

    static {
        THIS = new Decoder();
    }

    private ByteBuffer opus = ByteBuffer.allocateDirect(256);
    private ByteBuffer pcm = ByteBuffer.allocateDirect(640);
    private long h;

    public Decoder() {
        h = create();
    }

    private static native long create();

    @SuppressWarnings("SpellCheckingInspection")
    private static native void destory(long h);

    private static native int decodeFrame(long h, ByteBuffer opus, int opusBytes, ByteBuffer pcm, int pcmBytes);

    private static native void reset(long h);

    public static Decoder getDecoder() {
        return THIS;
    }

    public void reset() {
        reset(this.h);
    }

    public byte[] decode(byte[] opus) throws IOException {
        this.opus.position(0);
        this.opus.limit(256);
        int len = opus[0] & 0xff;
        this.opus.put(opus, 0, len + 4);
        len = decodeFrame(this.h, this.opus, len + 4, this.pcm, 640);
        if (len <= 0) {
            throw new IOException("encode error:" + len);
        }
        pcm.position(0);
        pcm.limit(Math.min(len, 640));
        byte[] ret = new byte[len];
        pcm.get(ret);
        return ret;
    }

    public void destroy() {
        if (h != 0L) {
            destory(h);
            h = 0L;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        destroy();
    }

}
