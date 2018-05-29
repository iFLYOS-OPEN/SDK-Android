package com.iflytek.cyber;

import com.google.gson.JsonObject;

import java.util.UUID;

public class Entities {

    public static JsonObject newMessage(String namespace, String name, JsonObject payload) {
        final JsonObject header = new JsonObject();
        header.addProperty("messageId", UUID.randomUUID().toString());
        return newEntity(namespace, name, header, payload);
    }

    public static JsonObject newMessage(String namespace, String name, String dialogRequestId, JsonObject payload) {
        final JsonObject header = new JsonObject();
        header.addProperty("messageId", UUID.randomUUID().toString());
        header.addProperty("dialogRequestId", dialogRequestId);
        return newEntity(namespace, name, header, payload);
    }

    public static JsonObject newEntity(String namespace, String name, JsonObject payload) {
        return newEntity(namespace, name, new JsonObject(), payload);
    }

    private static JsonObject newEntity(String namespace, String name, JsonObject header, JsonObject payload) {
        final JsonObject entity = new JsonObject();
        header.addProperty("namespace", namespace);
        header.addProperty("name", name);
        entity.add("header", header);
        entity.add("payload", payload);
        return entity;
    }

}
