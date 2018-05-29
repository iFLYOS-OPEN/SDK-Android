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
