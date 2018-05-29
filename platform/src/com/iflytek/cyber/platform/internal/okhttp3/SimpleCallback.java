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

package com.iflytek.cyber.platform.internal.okhttp3;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;

@SuppressWarnings("NullableProblems")
public abstract class SimpleCallback implements Callback {

    public abstract void onSuccess(ResponseBody body, Response response);

    public abstract void onHttpFailure(int code, JsonObject body, Response response);

    public abstract void onNetworkFailure(Throwable t);

    @Override
    public final void onResponse(Call call, Response response) {
        final ResponseBody body = response.body();

        if (response.isSuccessful()) {
            onSuccess(body, response);
            return;
        }

        final int code = response.code();

        if (body == null) {
            onHttpFailure(code, null, response);
            return;
        }

        try {
            final String json = body.string();
            final JsonObject object = new JsonParser().parse(json).getAsJsonObject();
            onHttpFailure(code, object, response);
        } catch (Exception e) {
            onHttpFailure(code, null, response);
        }
    }

    @Override
    public final void onFailure(Call call, IOException e) {
        onNetworkFailure(e);
    }

}
