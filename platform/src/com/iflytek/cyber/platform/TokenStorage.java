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

package com.iflytek.cyber.platform;

import android.text.TextUtils;

import java.util.concurrent.TimeUnit;

abstract class TokenStorage {

    protected abstract String getRefreshToken();

    protected abstract String getAccessToken();

    protected abstract long getExpiresAt();

    protected boolean hasToken() {
        return !TextUtils.isEmpty(getRefreshToken());
    }

    protected boolean hasValidAccessToken() {
        return !TextUtils.isEmpty(getAccessToken()) &&
                getExpiresAt() > TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) + 60;
    }

    protected abstract void invalidateAccessToken();

    protected abstract void clearToken();

    protected abstract void updateToken(String accessToken, String refreshToken, long expiresAt);

}
