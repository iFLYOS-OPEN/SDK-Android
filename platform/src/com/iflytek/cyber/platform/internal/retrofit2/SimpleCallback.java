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

package com.iflytek.cyber.platform.internal.retrofit2;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressWarnings("NullableProblems")
public abstract class SimpleCallback<T> implements Callback<T> {

    public abstract void onSuccess(T body, Response<T> response);

    public abstract void onHttpFailure(int code, JsonObject body, Response<T> response);

    public abstract void onNetworkFailure(Throwable t);

    @Override
    public final void onResponse(Call<T> call, Response<T> response) {
        if (response.isSuccessful()) {
            onSuccess(response.body(), response);
            return;
        }

        final int code = response.code();
        final ResponseBody body = response.errorBody();

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
    public final void onFailure(Call<T> call, Throwable t) {
        onNetworkFailure(t);
    }

}
