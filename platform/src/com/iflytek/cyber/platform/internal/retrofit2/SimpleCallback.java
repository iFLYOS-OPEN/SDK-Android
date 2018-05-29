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
