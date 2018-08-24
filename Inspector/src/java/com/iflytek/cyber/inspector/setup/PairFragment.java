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
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.iflytek.cyber.inspector.LauncherActivity;
import com.iflytek.cyber.inspector.R;
import com.iflytek.cyber.platform.AuthManager;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class PairFragment extends Fragment implements AuthManager.AuthorizeCallback {

    private static final String TAG = "PairFragment";

    private LauncherActivity activity;

    private String verificationUri;
    private String userCode;

    private View loading;
    private View pairing;
    private View failed;

    private ImageView code;

    private TextView error;
    private View retry;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (LauncherActivity) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pair, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        view.findViewById(R.id.back)
                .setOnClickListener(v -> activity.onBackPressed());

        loading = view.findViewById(R.id.loading);
        pairing = view.findViewById(R.id.pairing);
        failed = view.findViewById(R.id.failed);

        code = view.findViewById(R.id.code);

        error = view.findViewById(R.id.error);

        retry = view.findViewById(R.id.retry);
        retry.setOnClickListener(v -> pair());
    }

    @Override
    public void onResume() {
        super.onResume();
        pair();
    }

    @Override
    public void onPause() {
        super.onPause();
        activity.cancelAuthorize();
    }

    private void pair() {
        retry.setVisibility(View.GONE);
        failed.setVisibility(View.GONE);
        pairing.setVisibility(View.GONE);
        loading.setVisibility(View.VISIBLE);

        activity.requestAuthorize(this);
    }

    private void showError(String message) {
        error.setText(message);
        loading.setVisibility(View.GONE);
        pairing.setVisibility(View.GONE);
        failed.setVisibility(View.VISIBLE);
        retry.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPromptDeviceCode(String verificationUri, String userCode) {
        this.verificationUri = verificationUri;
        this.userCode = userCode;
        generateCode();
    }

    @Override
    public void onGetToken(String accessToken, String refreshToken, long expiresAt) {
        activity.finishSetup(accessToken, refreshToken, expiresAt);
        activity.redirectTo(new FinishFragment());
    }

    @Override
    public void onReject() {
        showError("您拒绝了授权。");
    }

    @Override
    public void onFailure(Throwable t) {
        showError("网络异常，请重试。");
    }

    private void generateCode() {
        if (verificationUri == null || userCode == null) {
            return;
        }

        final String url = verificationUri + "?user_code=" + userCode;
        final int size = getResources().getDimensionPixelSize(R.dimen.qr_size);

        Log.d(TAG, "verification uri: " + url);

        try {
            final Bitmap bitmap = new BarcodeEncoder().encodeBitmap(
                    url, BarcodeFormat.QR_CODE, size, size);

            code.setImageBitmap(bitmap);

            retry.setVisibility(View.GONE);
            failed.setVisibility(View.GONE);
            loading.setVisibility(View.GONE);
            pairing.setVisibility(View.VISIBLE);
        } catch (WriterException e) {
            e.printStackTrace();
            showError("二维码生成失败。");
        }
    }

}
