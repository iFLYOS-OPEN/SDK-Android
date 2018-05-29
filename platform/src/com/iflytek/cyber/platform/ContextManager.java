package com.iflytek.cyber.platform;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.iflytek.cyber.Entities;

import java.util.HashMap;
import java.util.Map;

public class ContextManager {

    private static final String TAG = "ContextManager";

    private final Object lock = new Object();

    private final HashMap<String, HashMap<String, JsonObject>> context = new HashMap<>();

    public void update(String namespace, String name, JsonObject payload) {
        synchronized (lock) {
            HashMap<String, JsonObject> names;
            if (context.containsKey(namespace)) {
                names = context.get(namespace);
            } else {
                names = new HashMap<>();
                context.put(namespace, names);
            }
            names.put(name, payload);
        }
    }

    public void remove(String namespace, String name) {
        synchronized (lock) {
            final HashMap<String, JsonObject> names = context.get(namespace);
            if (names != null) {
                names.remove(name);
            }
        }
    }

    public JsonArray build() {
        synchronized (lock) {
            final JsonArray array = new JsonArray();
            for (Map.Entry<String, HashMap<String, JsonObject>> names : context.entrySet()) {
                for (Map.Entry<String, JsonObject> payload : names.getValue().entrySet()) {
                    final JsonObject item = Entities.newEntity(
                            names.getKey(), payload.getKey(), payload.getValue());
                    array.add(item);
                }
            }
            return array;
        }
    }

}
