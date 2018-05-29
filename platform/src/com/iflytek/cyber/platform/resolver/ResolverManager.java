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

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.JsonObject;
import com.iflytek.cyber.platform.CyberCore;
import com.iflytek.cyber.resolver.Resolver;

import java.util.HashMap;

public abstract class ResolverManager {

    private static final String TAG = "ResolverManager";

    private static ResolverManager instance;

    final HashMap<String, Resolver> resolvers = new HashMap<>();

    ResolverManager() {
    }

    public static void init(Context context, CyberCore core) {
        instance = new ResolverManagerImpl(core);
        ManifestParser.parse(context, core, instance);
    }

    public static ResolverManager get() {
        return instance;
    }

    public final void register(String namespace, Resolver resolver) {
        if (TextUtils.isEmpty(namespace)) {
            Log.e(TAG, "Namespace should not be empty");
            return;
        }

        resolvers.put(namespace, resolver);
        Log.d(TAG, "Registered resolver: " + namespace);
    }

    @SuppressWarnings("unchecked")
    public final <T extends Resolver> T peek(String namespace, Class<T> type) {
        final Resolver resolver = resolvers.get(namespace);
        if (type.isInstance(resolver)) {
            return (T) resolver;
        } else {
            return null;
        }
    }

    public abstract void resolve(JsonObject directive);

    public abstract void updateContext();

    public abstract void startDialog(String dialogRequestId);

    public abstract void finishDialog(String dialogRequestId);

    public abstract void cancelDialog(String dialogRequestId);

    public abstract void create();

    public abstract void destroy();

}
