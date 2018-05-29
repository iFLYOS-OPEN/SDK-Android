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
