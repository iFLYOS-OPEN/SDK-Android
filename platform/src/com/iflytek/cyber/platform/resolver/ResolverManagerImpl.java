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

package com.iflytek.cyber.platform.resolver;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.iflytek.cyber.platform.CyberCore;
import com.iflytek.cyber.resolver.Resolver;
import com.iflytek.cyber.resolver.ResolverModule;

import java.util.LinkedList;
import java.util.Map;

class ResolverManagerImpl extends ResolverManager {

    private static final String TAG = "ResolverManagerImpl";

    private final CyberCore core;

    private final Gson gson = new Gson();

    private final LinkedList<JsonObject> queue = new LinkedList<>();
    private final Handler queueHandler = new Handler(Looper.getMainLooper());

    private boolean ongoing = false;
    private String activeDialogRequestId;
    private Resolver executingResolver;

    ResolverManagerImpl(CyberCore core) {
        super();
        this.core = core;
    }

    // FIXME: 规范要求返回未经解析的 directive，但是现在我们的 Multipart 解析逻辑还不支持
    private String unparsed(JsonObject directive) {
        final JsonObject envelope = new JsonObject();
        envelope.add("directive", directive);
        return envelope.toString();
    }

    @Override
    public void resolve(JsonObject directive) {
        assertMainThread();

        final DirectiveHeader header = DirectiveUtil.parseHeader(gson, directive);
        if (header == null) {
            core.reportExecutionFailure(unparsed(directive),
                    "Invalid directive header");
            return;
        }

        final Resolver resolver = resolvers.get(header.namespace);
        if (resolver == null) {
            core.reportExecutionFailure(unparsed(directive),
                    "No registered resolver for namespace: " + header.namespace);
            return;
        }

        if (TextUtils.isEmpty(header.dialogRequestId)) {
            Log.d(TAG, "Resolving standalone: " + header.messageId + " - " + header.namespace + ":" + header.name);
            execute(header, resolver, directive, null);
            return;
        }

        if (!header.dialogRequestId.equals(activeDialogRequestId)) {
            Log.d(TAG, "Dropping canceled " + header.dialogRequestId + ":" + header.messageId);
            return;
        }

        queue.offer(directive);
        Log.d(TAG, "Enqueued " + header.messageId + " - " + header.namespace + ":" + header.name);

        if (executingResolver == null) {
            queueHandler.post(this::runQueue);
        }
    }

    @Override
    public void updateContext() {
        for (Map.Entry<String, Resolver> entry : resolvers.entrySet()) {
            final Resolver resolver = entry.getValue();
            if (resolver instanceof ResolverModule) {
                try {
                    ((ResolverModule) resolver).updateContext();
                } catch (Exception e) {
                    Log.e(TAG, "Failed updating context for " + entry.getKey(), e);
                }
            }
        }
    }

    @Override
    public void startDialog(String dialogRequestId) {
        assertMainThread();

        if (activeDialogRequestId != null) {
            Log.d(TAG, "New dialog came, killing former dialog " + activeDialogRequestId);
            cancel();
        }

        activeDialogRequestId = dialogRequestId;
        ongoing = true;
        Log.d(TAG, "Start dialog " + activeDialogRequestId);
    }

    @Override
    public void finishDialog(String dialogRequestId) {
        assertMainThread();

        if (dialogRequestId.equals(activeDialogRequestId)) {
            Log.d(TAG, "Finish dialog " + dialogRequestId);
            ongoing = false;
        }
    }

    @Override
    public void cancelDialog(String dialogRequestId) {
        assertMainThread();

        if (dialogRequestId.equals(activeDialogRequestId)) {
            Log.d(TAG, "Cancel dialog " + dialogRequestId);
            cancel();
        }
    }

    private void cancel() {
        final Resolver executing = executingResolver;

        ongoing = false;
        activeDialogRequestId = null;
        executingResolver = null;
        queue.clear();
        queueHandler.removeCallbacksAndMessages(null);

        if (executing instanceof ResolverModule) {
            ((ResolverModule) executing).onCancel();
        }
    }

    private void runQueue() {
        assertMainThread();

        final JsonObject directive = queue.poll();

        if (directive == null) {
            Log.d(TAG, "Queue emptied " + activeDialogRequestId);
            executingResolver = null;
            if (!ongoing) {
                Log.d(TAG, "Completed dialog");
                queue.clear();
                queueHandler.removeCallbacksAndMessages(null);
                core.dialogResolved();
            }
            return;
        }

        final DirectiveHeader header = DirectiveUtil.parseHeader(gson, directive);
        assert header != null;

        final Resolver resolver = resolvers.get(header.namespace);
        if (resolver == null) {
            core.reportExecutionFailure(unparsed(directive),
                    "No registered resolver for namespace: " + header.namespace);
            queueHandler.post(this::runQueue);
            return;
        }

        executingResolver = resolver;

        Log.d(TAG, "Resolving " + header.messageId + " - " + header.namespace + ":" + header.name);
        execute(header, resolver, directive, this::runQueue);
    }

    private void execute(DirectiveHeader header, Resolver resolver, JsonObject directive, Runnable onNext) {
        final JsonObject headerJson = directive.getAsJsonObject("header");
        final JsonObject payloadJson = directive.getAsJsonObject("payload");

        try {
            resolver.resolve(headerJson, payloadJson, new Resolver.Callback() {
                @Override
                public void next() {
                    if (onNext != null) {
                        queueHandler.post(onNext);
                    }
                }

                @Override
                public void skip() {
                    core.reportExecutionFailure(unparsed(directive),
                            "Unhandled directive: " + header.namespace + " " + header.name);
                    if (onNext != null) {
                        queueHandler.post(onNext);
                    }
                }
            });
        } catch (Exception e) {
            core.reportException(unparsed(directive), e);
            if (onNext != null) {
                queueHandler.post(onNext);
            }
        }
    }

    @Override
    public void create() {
        for (Map.Entry<String, Resolver> entry : resolvers.entrySet()) {
            final Resolver resolver = entry.getValue();
            if (resolver instanceof ResolverModule) {
                try {
                    ((ResolverModule) resolver).onCreate();
                } catch (Exception e) {
                    Log.e(TAG, "Failed calling onCreate for " + entry.getKey(), e);
                }
            }
        }
    }

    @Override
    public void destroy() {
        for (Map.Entry<String, Resolver> entry : resolvers.entrySet()) {
            final Resolver resolver = entry.getValue();
            if (resolver instanceof ResolverModule) {
                try {
                    ((ResolverModule) resolver).onDestroy();
                } catch (Exception e) {
                    Log.e(TAG, "Failed calling onDestroy for " + entry.getKey(), e);
                }
            }
        }
    }

    private void assertMainThread() {
        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
            throw new IllegalStateException("This method should be called on main thread");
        }
    }

}
