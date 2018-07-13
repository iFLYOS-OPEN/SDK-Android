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

package com.iflytek.cyber.iot.show.core.setup;

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
import com.iflytek.cyber.iot.show.core.R;
import com.iflytek.cyber.iot.show.core.SetupWizardActivity;
import com.iflytek.cyber.platform.AuthManager;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class PairFragment extends Fragment implements AuthManager.AuthorizeCallback {

    private static final String TAG = "PairFragment";

    private SetupWizardActivity activity;

    private BindingManager bm;

    private String deviceId;
    private String bindingCode;
    private String operateToken;

    private String verificationUri;
    private String userCode;

    private View loading;
    private View pairing;
    private View failed;

    private ImageView code;

    private TextView error;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (SetupWizardActivity) context;
        bm = new BindingManager(context, this);
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

        view.findViewById(R.id.retry)
                .setOnClickListener(v -> pair());
    }

    @Override
    public void onResume() {
        super.onResume();
        pair();
    }

    @Override
    public void onPause() {
        super.onPause();
        bm.cancel();
        activity.cancelAuthorize();
    }

    private void pair() {
        failed.setVisibility(View.GONE);
        pairing.setVisibility(View.GONE);
        loading.setVisibility(View.VISIBLE);

        activity.requestAuthorize(this);
        bm.request();
    }

    private void showError(String message) {
        error.setText(message);
        loading.setVisibility(View.GONE);
        pairing.setVisibility(View.GONE);
        failed.setVisibility(View.VISIBLE);
    }

    void handleBindingSucceed(String deviceId, String code, String operateToken) {
        this.deviceId = deviceId;
        this.bindingCode = code;
        this.operateToken = operateToken;
        generateCode();
    }

    void handleBindingFailed() {
        activity.cancelAuthorize();
        showError("网络异常，请重试。");
    }

    @Override
    public void onPromptDeviceCode(String verificationUri, String userCode) {
        this.verificationUri = verificationUri;
        this.userCode = userCode;
        generateCode();
    }

    @Override
    public void onGetToken(String accessToken, String refreshToken, long expiresAt) {
        activity.finishSetup(accessToken, refreshToken, expiresAt, operateToken);
        activity.redirectTo(new FinishFragment());
    }

    @Override
    public void onReject() {
        bm.cancel();
        showError("您拒绝了授权。");
    }

    @Override
    public void onFailure(Throwable t) {
        Log.e(TAG, "Auth failed", t);
        bm.cancel();
        showError("网络异常，请重试。");
    }

    private void generateCode() {
        if (verificationUri == null || userCode == null || deviceId == null || bindingCode == null) {
            return;
        }

        final String url = verificationUri + "?user_code=" + userCode + "#" + deviceId + "," + bindingCode;
        final int size = getResources().getDimensionPixelSize(R.dimen.qr_size);

        try {
            final Bitmap bitmap = new BarcodeEncoder().encodeBitmap(
                    url, BarcodeFormat.QR_CODE, size, size);

            code.setImageBitmap(bitmap);

            failed.setVisibility(View.GONE);
            loading.setVisibility(View.GONE);
            pairing.setVisibility(View.VISIBLE);
        } catch (WriterException e) {
            e.printStackTrace();
            showError("二维码生成失败。");
        }
    }

}
