package com.iflytek.cyber.inspector;

import android.app.Application;
import android.content.Context;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CoreApplication extends Application {

    private Retrofit retrofit;

    public static CoreApplication from(Context context) {
        return (CoreApplication) context.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        final Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();

        final OkHttpClient client = new OkHttpClient.Builder()
                .build();

        retrofit = new Retrofit.Builder()
                .client(client)
                .baseUrl(BuildConfig.API_SERVER)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    public <T> T createApi(Class<T> cls) {
        return retrofit.create(cls);
    }

}
