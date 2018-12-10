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

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.iflytek.cyber.iot.show.core.BuildConfig;
import com.iflytek.cyber.iot.show.core.EngineService;
import com.iflytek.cyber.iot.show.core.R;
import com.iflytek.cyber.iot.show.core.model.ContentStorage;

import androidx.navigation.fragment.NavHostFragment;

public class AboutFragment extends BaseFragment {
    private boolean changeBinding = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false);
        view.findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavHostFragment.findNavController(AboutFragment.this).navigateUp();
            }
        });
        view.findViewById(R.id.change_binding).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = getContext();
                if (context != null)
                    new AlertDialog.Builder(context)
                            .setTitle("更改绑定")
                            .setMessage("是否确定更改绑定")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    AboutFragment.this.changeBinding();
                                }
                            })
                            .setNegativeButton(android.R.string.no, null)
                            .show();
            }
        });
        TextView textView = view.findViewById(R.id.system_version);
        textView.setText(BuildConfig.VERSION_NAME);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getLauncher() != null) {
            getLauncher().hideSimpleTips();
        }
    }

    private void changeBinding() {
        changeBinding = true;
        Activity activity = getActivity();
        if (activity == null)
            return;

        ContentStorage.get().saveContent(null);

        Intent intent = new Intent(activity, EngineService.class);
        intent.setAction(EngineService.ACTION_LOGOUT);
        activity.startService(intent);

        clearBackStack();
        NavHostFragment.findNavController(this).navigate(R.id.welcome_fragment);
    }

    private void clearBackStack() {
        NavHostFragment.findNavController(this).popBackStack(R.id.main_fragment, true);
        NavHostFragment.findNavController(this).popBackStack(R.id.splash_fragment, true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getLauncher() != null) {
            if (!changeBinding)
                getLauncher().showSimpleTips();
        }
    }
}
