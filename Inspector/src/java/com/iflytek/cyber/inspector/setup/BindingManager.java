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

package com.iflytek.cyber.inspector.setup;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.JsonObject;
import com.iflytek.cyber.inspector.CoreApplication;
import com.iflytek.cyber.platform.DeviceId;
import com.iflytek.cyber.platform.internal.retrofit2.SimpleCallback;

import retrofit2.Call;
import retrofit2.Response;

class BindingManager {

    private static final String TAG = "BindingManager";

    private final Context context;
    private final PairFragment controller;

    private final BindingApi api;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private Call<Binding> bindingCall = null;

    BindingManager(Context context, PairFragment controller) {
        this.context = context;
        this.controller = controller;
        this.api = CoreApplication.from(context).createApi(BindingApi.class);
    }

    void request(String modelId) {
        cancel();

        final String deviceId = DeviceId.get(context);

        bindingCall = api.requestBind(modelId, deviceId);
        bindingCall.enqueue(new SimpleCallback<Binding>() {
            @Override
            public void onSuccess(Binding body, Response<Binding> response) {
                bindingCall = null;
                uiHandler.post(() -> controller.handleBindingSucceed(
                        deviceId, body.code, body.operateToken));
            }

            @Override
            public void onHttpFailure(int code, JsonObject body, Response<Binding> response) {
                bindingCall = null;
                Log.e(TAG, "Binding device failed with code: " + code);
                uiHandler.post(controller::handleBindingFailed);
            }

            @Override
            public void onNetworkFailure(Throwable t) {
                bindingCall = null;
                Log.e(TAG, "Binding device failed", t);
                uiHandler.post(controller::handleBindingFailed);
            }
        });
    }

    void cancel() {
        if (bindingCall != null) {
            bindingCall.cancel();
            bindingCall = null;
        }
    }

}
