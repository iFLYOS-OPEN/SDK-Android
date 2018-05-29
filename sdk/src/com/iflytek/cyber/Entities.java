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
