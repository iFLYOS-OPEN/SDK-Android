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

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import com.iflytek.cyber.iot.show.core.setup.WelcomeFragment;
import com.iflytek.cyber.platform.AuthManager;
import com.iflytek.cyber.platform.DefaultTokenStorage;
import com.iflytek.cyber.platform.TokenManager;

public class SetupWizardActivity extends BaseActivity {

    private AuthManager authManager;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_wizard);

        authManager = new AuthManager(BuildConfig.CLIENT_ID);
        tokenManager = new TokenManager(new DefaultTokenStorage(this), authManager);

        initMainFragment();
    }

    public void initMainFragment() {
        if (tokenManager.hasToken()) {
            PackageManager pm = getPackageManager();
            pm.setComponentEnabledSetting(new ComponentName(this, LauncherActivity.class),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            startActivity(Intent.makeMainActivity(new ComponentName(this, LauncherActivity.class)));
            finish();
            pm.setComponentEnabledSetting(new ComponentName(this, SetupWizardActivity.class),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        } else {
            redirectTo(new WelcomeFragment());
        }

        // 让后台服务根据 token 情况决定是否活动
        startService(new Intent(this, CoreService.class));
    }

    public void redirectTo(Fragment fragment) {
        getSupportFragmentManager().popBackStack(
                null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.setup_page_popup_enter, 0)
                .replace(R.id.container, fragment)
                .commit();
    }

    public void navigateTo(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.setup_page_enter, R.anim.setup_page_exit,
                        R.anim.setup_page_pop_enter, R.anim.setup_page_pop_exit)
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

    public void finishSetup(String accessToken, String refreshToken, long expiresAt,
                            String operateToken) {
        tokenManager.updateToken(accessToken, refreshToken, expiresAt);
    }

    void debug_clearToken() {
        tokenManager.clearToken();
        initMainFragment();
    }

}
