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

import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.iflytek.android.io.CloseableUtil;

import org.json.JSONException;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Response;
import okio.BufferedSource;
import okio.Okio;
import okio.Pipe;

class MultipartParser {

    private static final String TAG = "MultipartParser";

    private static final int TYPE_JSON = 1;
    private static final int TYPE_OCTET_STREAM = 2;

    private static final String FIELD_CONTENT_TYPE_JSON = "Content-Type: application/json";
    private static final String FIELD_CONTENT_TYPE_OCTET_STREAM = "Content-Type: application/octet-stream";

    private static final Pattern REG_BOUNDARY = Pattern.compile("boundary=(\")?([^;]+)\\1");
    private static final Pattern REG_CONTENT_ID = Pattern.compile("^Content-ID: <([^>]+)>$");

    private final Gson mGson;
    private final BufferedSource mBody;
    private final String mBoundary;
    private final OnPartListener mListener;

    @SuppressWarnings("ConstantConditions")
    MultipartParser(Gson gson, Response response, OnPartListener listener) {
        this(gson, response.body().source(), getBoundary(response.header("content-type")), listener);
    }

    private MultipartParser(Gson gson, BufferedSource body, String boundary, OnPartListener listener) {
        this.mGson = gson;
        this.mBody = body;
        this.mBoundary = boundary;
        this.mListener = listener;
    }

    private static String getBoundary(String header) {
        final Matcher matcher = REG_BOUNDARY.matcher(header);
        if (matcher.find()) {
            return matcher.group(2);
        } else {
            return null;
        }
    }

    private List<String> skipUntilLine(Predicate<String> p) throws IOException {
        ArrayList<String> lines = new ArrayList<>(10);
        String line;
        do {
            line = mBody.readUtf8LineStrict();
            lines.add(line);
        } while (!p.test(line));
        return lines;
    }

    void parse() {
        if (TextUtils.isEmpty(mBoundary)) {
            mListener.onFailure(new IllegalStateException("boundary should not be empty"));
            return;
        }

        do {
            try {
                if (!next()) {
                    break;
                }
            } catch (Exception e) {
                mListener.onFailure(e);
                return;
            }
        } while (true);
    }

    private boolean next() throws IOException, JSONException {
        List<String> lines = skipUntilLine(l -> l.startsWith("--" + mBoundary));
        String lastLineInLines = lines.get(lines.size() - 1);
        if (lastLineInLines.equals("--" + mBoundary + "--")) {
            mListener.onEnd();
            return false;//multipart end
        }

        lines = skipUntilLine(l -> l.equals(""));

        int contentType = 0;
        String contentId = "";

        for (String line : lines) {
            if (line.startsWith(FIELD_CONTENT_TYPE_JSON)) {
                contentType = TYPE_JSON;
            } else if (line.startsWith(FIELD_CONTENT_TYPE_OCTET_STREAM)) {
                contentType = TYPE_OCTET_STREAM;
            } else if ("".equals(contentId)) {
                final Matcher matcher = REG_CONTENT_ID.matcher(line);
                if (matcher.matches()) {
                    contentId = matcher.group(1);
                }
            }
        }

        if (contentType == TYPE_JSON) {
            final JsonReader jr = mGson.newJsonReader(new Reader() {
                @Override
                public int read(char[] cbuf, int off, int len) throws IOException {
                    if (len > 0) {
                        int cp = mBody.readUtf8CodePoint();
                        char[] cForCp = Character.toChars(cp);
                        int cpLen = Math.min(cForCp.length, len);
                        System.arraycopy(cForCp, 0, cbuf, off, cpLen);
                        return cpLen;
                    }
                    return 0;
                }

                @Override
                public void close() throws IOException {
                }
            });

            final DirectiveTop top = mGson.fromJson(jr, DirectiveTop.class);
            mListener.onPart(new JsonPart(top.directive));

            return true;
        }

        if (contentType == TYPE_OCTET_STREAM && !TextUtils.isEmpty(contentId)) {
            Log.d(TAG, "got audio: " + contentId);

            final Pipe pipe = new Pipe(20 * 1024 * 1024);
            mListener.onPart(new StreamPart(contentId, Okio.buffer(pipe.source())));

            try {
                StreamExtractor.demux(mBody, Okio.buffer(pipe.sink()));
            } catch (IOException e) {
                Log.e(TAG, "Failed demuxing audio", e);
                CloseableUtil.safeClose(pipe.sink());
                CloseableUtil.safeClose(pipe.source());
            }

            return true;
        }

        throw new IOException("neither json or stream content");
    }

    public interface Predicate<T> {
        boolean test(T input);
    }

    interface OnPartListener {

        void onPart(Part part);

        void onEnd();

        void onFailure(Throwable e);

    }

    private static class DirectiveTop {
        JsonObject directive;
    }

    static class Part {
    }

    static class JsonPart extends Part {

        final JsonObject json;

        JsonPart(JsonObject json) {
            this.json = json;
        }

    }

    static class StreamPart extends Part {

        final String cid;
        final BufferedSource stream;

        StreamPart(String cid, BufferedSource stream) {
            this.cid = cid;
            this.stream = stream;
        }

    }

}
