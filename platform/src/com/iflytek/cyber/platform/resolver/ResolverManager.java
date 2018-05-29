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
