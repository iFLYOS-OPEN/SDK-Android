package com.iflytek.cyber.resolver;

import com.google.gson.JsonObject;

public interface Resolver {

    void resolve(JsonObject header, JsonObject payload, Callback callback);

    interface Callback {

        void next();

        void skip();

    }

}
