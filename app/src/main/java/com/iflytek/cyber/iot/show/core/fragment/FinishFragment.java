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

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.iflytek.cyber.iot.show.core.R;

import java.util.concurrent.TimeUnit;

import androidx.navigation.fragment.NavHostFragment;

public class FinishFragment extends BaseFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_finish, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        view.postDelayed(new Runnable() {
            @Override
            public void run() {
                clearBackStack();
                NavHostFragment.findNavController(FinishFragment.this).navigate(R.id.main_fragment);
            }
        }, TimeUnit.SECONDS.toMillis(3));

        if (getLauncher() != null) {
            getLauncher().hideSimpleTips();
        }
    }

    private void clearBackStack() {
        NavHostFragment.findNavController(this).popBackStack(R.id.pair_fragment, true);
        NavHostFragment.findNavController(this).popBackStack(R.id.wifi_fragment, true);
        NavHostFragment.findNavController(this).popBackStack(R.id.welcome_fragment, true);
        NavHostFragment.findNavController(this).popBackStack(R.id.splash_fragment, true);
    }
}
