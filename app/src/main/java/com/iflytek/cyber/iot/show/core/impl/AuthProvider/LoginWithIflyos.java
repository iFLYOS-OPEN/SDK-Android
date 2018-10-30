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

package com.iflytek.cyber.iot.show.core.impl.AuthProvider;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;

import com.iflytek.cyber.iot.show.core.R;
import com.iflytek.cyber.iot.show.core.impl.Logger.LoggerHandler;

import java.util.Observable;
import java.util.Observer;


class LoginWithIflyos implements Observer {

    static final String CBL_LOGIN_METHOD_KEY = "CBL";
    static final String LWA_LOGIN_METHOD_KEY = "LWA";

    private static final String sTag = "LoginWithIflyos";

    private final Context mContext;
    private final LoginWithIVSCBL mLwaCBL;
    private final SharedPreferences mPreferences;
    private final LoggerHandler mLogger;
    private final Handler mUIHandler;

//    private View mLoginView, mLogoutView;

    LoginWithIflyos(LoggerHandler logger,
                    Context context,
                    SharedPreferences sharedPreferences,
                    AuthProviderHandler authProvider) {
        mContext = context;
        mUIHandler = new Handler();
        mLogger = logger;
        mPreferences = sharedPreferences;
        mLwaCBL = new LoginWithIVSCBL(context, mPreferences, logger, authProvider);
    }

    void onInitialize() {
        mLwaCBL.onInitialize();
    }

    void onResume() {
    }

    void logout() {
        String loginMethod = mPreferences.getString(mContext.getString(R.string.preference_login_method), "");
        mLwaCBL.logout();
    }

    void logoutWithNotNotify() {
        mLwaCBL.logoutWithNotNotify();
    }

    //
    // For updating GUI
    //

    @Override
    public void update(final Observable o, final Object arg) {
        final String message = arg.toString();
        Log.d(sTag, message);
    }

    void login() {
        mLwaCBL.cancelPendingAuthorization();
        mLwaCBL.login();
    }

}
