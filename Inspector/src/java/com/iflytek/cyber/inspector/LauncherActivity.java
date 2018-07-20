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

package com.iflytek.cyber.inspector;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import com.iflytek.cyber.inspector.setup.WelcomeFragment;
import com.iflytek.cyber.platform.AuthManager;
import com.iflytek.cyber.platform.DefaultTokenStorage;
import com.iflytek.cyber.platform.DeviceId;
import com.iflytek.cyber.platform.TokenManager;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class LauncherActivity extends AppCompatActivity {

    private AuthManager authManager;
    private TokenManager tokenManager;

    private SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pref = PreferenceManager.getDefaultSharedPreferences(this);

        updateClientId();
        initMainFragment();

        if (checkSelfPermission(RECORD_AUDIO) == PERMISSION_GRANTED) {
            initMainFragment();
        } else {
            requestPermission();
        }
    }

    private void requestPermission() {
        requestPermissions(new String[]{RECORD_AUDIO}, 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (grantResults[0] == PERMISSION_GRANTED) {
            initMainFragment();
        } else {
            requestPermission();
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (authManager != null) {
            authManager.cancel();
        }

        if (tokenManager != null) {
            tokenManager.destroy();
        }
    }

    public void updateClientId() {
        if (authManager != null) {
            authManager.cancel();
        }

        if (tokenManager != null) {
            tokenManager.destroy();
        }

        authManager = new AuthManager(pref.getString("client_id", null), DeviceId.get(this));
        tokenManager = new TokenManager(new DefaultTokenStorage(this), authManager);
    }

    public void initMainFragment() {
        if (tokenManager.hasToken()) {
            redirectTo(new MainFragment());
        } else {
            redirectTo(new WelcomeFragment());
        }
    }

    public void redirectTo(Fragment fragment) {
        getSupportFragmentManager().popBackStackImmediate(
                null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, fragment)
                .commit();
    }

    public void navigateTo(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        getSupportFragmentManager().popBackStack();
    }

    public void requestAuthorize(AuthManager.AuthorizeCallback callback) {
        authManager.authorize(callback);
    }

    public void cancelAuthorize() {
        authManager.cancel();
    }

    public void finishSetup(String accessToken, String refreshToken, long expiresAt) {
        tokenManager.updateToken(accessToken, refreshToken, expiresAt);
    }

    void debug_clearToken() {
        tokenManager.clearToken();
        initMainFragment();
    }

    void changeEndpoint() {
        navigateTo(new EndpointFragment());
    }

}
