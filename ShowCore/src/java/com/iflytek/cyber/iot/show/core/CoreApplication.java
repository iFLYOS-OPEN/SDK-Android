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

package com.iflytek.cyber.iot.show.core;

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
