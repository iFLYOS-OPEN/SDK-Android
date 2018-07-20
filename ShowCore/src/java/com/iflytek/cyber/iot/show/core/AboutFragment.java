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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.iflytek.cyber.platform.AuthManager;
import com.iflytek.cyber.platform.DefaultTokenStorage;
import com.iflytek.cyber.platform.DeviceId;
import com.iflytek.cyber.platform.TokenManager;

public class AboutFragment extends DialogFragment {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.AboutDialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_about, container, false);
        view.findViewById(R.id.close).setOnClickListener(v -> {
            dismiss();
        });
        view.findViewById(R.id.change_binding).setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.change_binding)
                    .setMessage("是否确定更改绑定")
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> changeBinding())
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        });
        TextView textView = view.findViewById(R.id.system_version);
        textView.setText(BuildConfig.VERSION_NAME);
        return view;
    }

    private void changeBinding(){
        Activity activity = getActivity();
        if (activity == null)
            return;

        AuthManager authManager = new AuthManager(BuildConfig.CLIENT_ID, DeviceId.get(activity));
        TokenManager tokenManager = new TokenManager(new DefaultTokenStorage(activity), authManager);
        tokenManager.clearToken();

        PackageManager pm = activity.getPackageManager();
        pm.setComponentEnabledSetting(new ComponentName(activity, SetupWizardActivity.class),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        startActivity(Intent.makeMainActivity(new ComponentName(activity, SetupWizardActivity.class)));
        activity.finish();
        pm.setComponentEnabledSetting(new ComponentName(activity, LauncherActivity.class),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }

    @Override
    public void onStart() {
        super.onStart();

        Dialog dialog = getDialog();
        if (dialog != null) {
            if (dialog.getWindow() != null) {
                Window window = dialog.getWindow();
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        getDialog().getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);

    }
}
