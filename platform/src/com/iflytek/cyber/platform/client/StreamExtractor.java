package com.iflytek.cyber.platform.client;

import android.util.Log;

import java.io.IOException;

import okio.BufferedSink;
import okio.BufferedSource;

class StreamExtractor {

    private static final String TAG = "OpusDecoder";

    static void demux(BufferedSource source, BufferedSink dest) throws IOException {
        Log.d(TAG, "Start to demux opus stream");

        int pkgLen;
        while ((pkgLen = (source.readByte() & 0xff)) > 0) {
            dest.writeByte(pkgLen);
            dest.write(source, pkgLen);
            dest.flush();
        }

        dest.writeByte(0);
        dest.flush();
        dest.close();

        Log.d(TAG, "Opus stream demuxed finished");
    }

}
