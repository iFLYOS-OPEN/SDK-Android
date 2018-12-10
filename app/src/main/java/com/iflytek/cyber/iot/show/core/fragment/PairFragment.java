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

package com.iflytek.cyber.iot.show.core.fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.iflytek.cyber.iot.show.core.EngineService;
import com.iflytek.cyber.iot.show.core.LauncherActivity;
import com.iflytek.cyber.iot.show.core.R;
import com.iflytek.cyber.iot.show.core.TimeService;
import com.iflytek.cyber.iot.show.core.impl.Logger.LogEntry;
import com.iflytek.cyber.iot.show.core.impl.Logger.LoggerHandler;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Observable;
import java.util.Observer;

import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import cn.iflyos.iace.iflyos.AuthProvider;

public class PairFragment extends BaseFragment implements Observer {

    private static final String TAG = "PairFragment";

    private Handler handler = new Handler(Looper.getMainLooper());

    private LauncherActivity activity;

    private String verificationUri;
    private String userCode;

    private View view;
    private View loading;
    private View failed;
    private View tips;

    private ImageView code;

    private TextView error;

    private boolean shouldShowTips;
    private boolean isExit = false;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (LauncherActivity) context;

        if (getArguments() != null) {
            shouldShowTips = getArguments().getBoolean("shouldShowTips", false);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        if (container != null) {
            container.removeAllViews();
        }
        return inflater.inflate(R.layout.fragment_pair, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (getLauncher() != null) {
            getLauncher().hideSimpleTips();
        }

        view.findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isExit) {
                    return;
                }
                isExit = true;
                NavHostFragment.findNavController(PairFragment.this).navigateUp();
            }
        });

        loading = view.findViewById(R.id.loading);
        failed = view.findViewById(R.id.failed);
        tips = view.findViewById(R.id.tv_tips);

        code = view.findViewById(R.id.code);

        error = view.findViewById(R.id.error);

        view.findViewById(R.id.retry)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PairFragment.this.pair();
                    }
                });

        activity.addObserver(this);
    }

    private void requestAuthorize() {
        Intent intent = new Intent(activity, EngineService.class);
        intent.setAction(EngineService.ACTION_LOGIN);
        activity.startService(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        activity.startService(new Intent(activity, TimeService.class));
        pair();
    }

    @Override
    public void onPause() {
        super.onPause();
        //activity.cancelAuthorize();
    }

    private void pair() {
        failed.setVisibility(View.INVISIBLE);
        code.setVisibility(View.INVISIBLE);
        loading.setVisibility(View.VISIBLE);
        tips.setVisibility(View.VISIBLE);
        //activity.requestAuthorize(this);
        requestAuthorize();
    }

    private void showError(String message) {
        error.setText(message);
        loading.setVisibility(View.INVISIBLE);
        code.setVisibility(View.INVISIBLE);
        tips.setVisibility(View.INVISIBLE);
        failed.setVisibility(View.VISIBLE);
    }

    private void navigateFinish() {
        if (null != getActivity()) {
            Navigation.findNavController(getActivity(), R.id.fragment).navigate(R.id.action_to_finish_fragment);
        }
    }

    private void generateCode() {
        if (verificationUri == null || userCode == null) {
            return;
        }

        final String url = verificationUri + "?user_code=" + userCode;
        final int size = code.getHeight();

        try {
            final Bitmap bitmap = new BarcodeEncoder().encodeBitmap(
                    url, BarcodeFormat.QR_CODE, size, size);

            code.setImageBitmap(bitmap);

            failed.setVisibility(View.INVISIBLE);
            loading.setVisibility(View.INVISIBLE);
            code.setVisibility(View.VISIBLE);
        } catch (WriterException e) {
            e.printStackTrace();
            showError("二维码生成失败。");
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        if (!(o instanceof LoggerHandler.LoggerObservable)) {
            return;
        }

        if (!(arg instanceof LogEntry)) {
            return;
        }

        JSONObject template = null;
        try {
            template = ((LogEntry) arg).getJson().getJSONObject("template");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (template == null) {
            return;
        }

        if (((LogEntry) arg).getType() == LoggerHandler.CBL_CODE) {
            final int type = template.optInt("type");
            if (type == LoggerHandler.AUTH_LOG_URL) {
                this.verificationUri = template.optString("verification_uri");
                this.userCode = template.optString("user_code");
                code.post(new Runnable() {
                    @Override
                    public void run() {
                        generateCode();
                    }
                });
            }
        } else if (((LogEntry) arg).getType() == LoggerHandler.AUTH_LOG) {
            String authState = template.optString("auth_state");
            if (TextUtils.equals(authState, AuthProvider.AuthState.REFRESHED.toString())) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        navigateFinish();
                    }
                });
            } else if (TextUtils.equals(authState, AuthProvider.AuthState.UNINITIALIZED.toString())) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        showError("网络异常，请重试。");
                    }
                });
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getLauncher() != null && shouldShowTips) {
            getLauncher().showSimpleTips();
        }
    }
}
