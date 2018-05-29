package com.iflytek.cyber.platform.resolver;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

class DirectiveUtil {

    static DirectiveHeader parseHeader(Gson gson, JsonObject directive) {
        final DirectiveHeader header = gson.fromJson(directive.get("header"), DirectiveHeader.class);
        if (header == null) {
            return null;
        }

        if (TextUtils.isEmpty(header.namespace) || TextUtils.isEmpty(header.name)
                || TextUtils.isEmpty(header.messageId)) {
            return null;
        }

        return header;
    }

}
