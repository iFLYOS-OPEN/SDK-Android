package com.iflytek.cyber.platform;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

class ApiFactory {

    private final Retrofit retrofit;

    ApiFactory() {
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

    <T> T createApi(Class<T> cls) {
        return retrofit.create(cls);
    }

}
