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

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.iflytek.cyber.platform.BuildConfig;
import com.iflytek.cyber.platform.internal.okhttp3.SimpleCallback;

import java.io.EOFException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.Source;

public class CyberOkHttpClient extends CyberClient {

    private static final String TAG = "Client";

    private static final long PING_INTERVAL = TimeUnit.MINUTES.toMillis(5);

    private static CyberOkHttpClient instance;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private final String endpoint;
    private final OkHttpClient client;

    private Call pingCall;
    private Call directiveCall;
    private Call recognizeCall;

    private CyberOkHttpClient(String endpoint, OkHttpClient client, Gson gson, DirectiveListener listener) {
        super(gson, listener);
        this.endpoint = endpoint;
        this.client = client;
    }

    public static synchronized void initClient(String endpoint, OkHttpClient client, DirectiveListener listener) {
        instance = new CyberOkHttpClient(endpoint, client, new Gson(), listener);
    }

    public static CyberOkHttpClient getClient() {
        if (instance == null) {
            throw new IllegalStateException("not initialized");
        }
        return instance;
    }

    @Override
    public void listen() {
        final Request request = new Request.Builder()
                .get()
                .addHeader("Accept", "multipart/related; type=application/vnd.iflytek.cyber+opus")
                .url(endpoint + "/" + BuildConfig.IVS_VERSION + "/directives")
                .build();

        directiveCall = client.newBuilder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(1, TimeUnit.DAYS)
                .build()
                .newCall(request);

        directiveCall.enqueue(new SimpleCallback() {
            @Override
            public void onSuccess(ResponseBody body, Response response) {
                // 这里不应该将 directiveCall 设为 null，我们要把它留到需要强行 disconnect 的时候才 cancel

                uiHandler.post(listener::onConnected);

                new MultipartParser(gson, response, new MultipartParser.OnPartListener() {
                    @Override
                    public void onPart(MultipartParser.Part part) {
                        if (part instanceof MultipartParser.JsonPart) {
                            final MultipartParser.JsonPart p = (MultipartParser.JsonPart) part;
                            uiHandler.post(() -> listener.onDirective(p.json));
                        } else if (part instanceof MultipartParser.StreamPart) {
                            final MultipartParser.StreamPart p = (MultipartParser.StreamPart) part;
                            uiHandler.post(() -> listener.onAttachment(p.cid, p.stream));
                        }
                    }

                    @Override
                    public void onEnd() {
                        uiHandler.post(() -> handleError(new EOFException("Downchannel get terminated")));
                    }

                    @Override
                    public void onFailure(Throwable e) {
                        uiHandler.post(() -> handleError(e));
                    }
                }).parse();
            }

            @Override
            public void onHttpFailure(int code, JsonObject body, Response response) {
                directiveCall = null;
                Log.e(TAG, "Create downchannel failed: " + code);
                final CyberException e = new CyberHttpException(code, body);
                uiHandler.post(() -> handleError(e));
            }

            @Override
            public void onNetworkFailure(Throwable t) {
                directiveCall = null;
                Log.e(TAG, "Create downchannel failed", t);
                uiHandler.post(() -> handleError(t));
            }
        });

        uiHandler.postDelayed(this::ping, PING_INTERVAL);
    }

    @Override
    public void disconnect() {
        uiHandler.removeCallbacksAndMessages(null);

        if (pingCall != null) {
            pingCall.cancel();
            pingCall = null;
        }

        if (directiveCall != null) {
            directiveCall.cancel();
            directiveCall = null;
        }

        if (recognizeCall != null) {
            recognizeCall.cancel();
            recognizeCall = null;
        }
    }

    private void ping() {
        final Request request = new Request.Builder()
                .get()
                .url(endpoint + "/ping")
                .build();

        pingCall = client.newBuilder()
                .readTimeout(5, TimeUnit.SECONDS)
                .build()
                .newCall(request);

        pingCall.enqueue(new SimpleCallback() {
            @Override
            public void onSuccess(ResponseBody body, Response response) {
                pingCall = null;
                uiHandler.postDelayed(() -> ping(), PING_INTERVAL);
            }

            @Override
            public void onHttpFailure(int code, JsonObject body, Response response) {
                pingCall = null;
                Log.e(TAG, "Ping failed: " + code);
                final CyberException e = new CyberHttpException(code, body);
                uiHandler.post(() -> handleError(e));
            }

            @Override
            public void onNetworkFailure(Throwable t) {
                pingCall = null;
                Log.e(TAG, "Ping failed", t);
                uiHandler.post(() -> handleError(t));
            }
        });
    }

    private void handleError(Throwable tr) {
        disconnect();
        uiHandler.removeCallbacksAndMessages(null);
        uiHandler.post(() -> listener.onDisconnected(tr));
    }

    @Override
    public void postEvent(JsonObject envelope, Source audio, EventCallback callback) {
        if (recognizeCall != null && audio != null) {
            recognizeCall.cancel();
            recognizeCall = null;
        }

        final MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("metadata", null, RequestBody.create(
                        MediaType.parse("application/json"),
                        gson.toJson(envelope)));

        if (audio != null) {
            builder.addFormDataPart("audio", null, new RequestBody() {
                @Override
                public MediaType contentType() {
                    return MediaType.parse("application/octet-stream");
                }

                @Override
                public void writeTo(BufferedSink sink) throws IOException {
                    final Buffer buffer = new Buffer();
                    long length;
                    while ((length = audio.read(buffer, 1024)) != -1) {
                        sink.write(buffer, length);
                        sink.flush();
                    }
                }
            });
        }

        final Request request = new Request.Builder()
                .post(builder.build())
                .addHeader("Accept", "multipart/related; type=application/vnd.iflytek.cyber+opus")
                .url(endpoint + "/" + BuildConfig.IVS_VERSION + "/events")
                .build();

        final Call call = client.newBuilder()
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .connectTimeout(2, TimeUnit.SECONDS)
                .build()
                .newCall(request);

        call.enqueue(new SimpleCallback() {
            @Override
            public void onSuccess(ResponseBody body, Response response) {
                if (audio != null) {
                    recognizeCall = null;
                }

                if (response.code() == 204) {
                    uiHandler.post(callback::onEnd);
                    return;
                }

                new MultipartParser(gson, response, new MultipartParser.OnPartListener() {
                    @Override
                    public void onPart(MultipartParser.Part part) {
                        if (part instanceof MultipartParser.JsonPart) {
                            final MultipartParser.JsonPart p = (MultipartParser.JsonPart) part;
                            uiHandler.post(() -> callback.onDirective(p.json));
                        } else if (part instanceof MultipartParser.StreamPart) {
                            final MultipartParser.StreamPart p = (MultipartParser.StreamPart) part;
                            uiHandler.post(() -> listener.onAttachment(p.cid, p.stream));
                        }
                    }

                    @Override
                    public void onEnd() {
                        uiHandler.post(callback::onEnd);
                    }

                    @Override
                    public void onFailure(Throwable e) {
                        uiHandler.post(() -> callback.onFailure(e));
                    }
                }).parse();
            }

            @Override
            public void onHttpFailure(int code, JsonObject body, Response response) {
                if (audio != null) {
                    recognizeCall = null;
                }

                Log.e(TAG, "Post event failed: " + code);
                final CyberException e = new CyberHttpException(code, body);
                uiHandler.post(() -> {
                    callback.onFailure(e);
                    if (code == 401) {
                        handleError(e);
                    }
                });
            }

            @Override
            public void onNetworkFailure(Throwable t) {
                if (audio != null) {
                    recognizeCall = null;
                }

                Log.e(TAG, "Post event failed", t);
                uiHandler.post(() -> callback.onFailure(t));
            }
        });

        if (audio != null) {
            recognizeCall = call;
        }
    }

    @Override
    public void cancelEvent() {
        if (recognizeCall != null) {
            recognizeCall.cancel();
            recognizeCall = null;
        }
    }

    public static class CyberHttpException extends CyberException {

        CyberHttpException(int code, JsonObject body) {
            super(code, body == null ? null : body.toString());
        }

    }

}
