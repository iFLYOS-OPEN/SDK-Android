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

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class TokenManager {

    private static final String TAG = "TokenManager";

    private final TokenStorage storage;
    private final AuthManager am;

    private boolean refreshing = false;
    private List<TokenCallback> callbacks = new ArrayList<>();

    public TokenManager(TokenStorage storage, AuthManager am) {
        this.storage = storage;
        this.am = am;
    }

    public void destroy() {
    }

    public String getAccessToken() {
        return storage.getAccessToken();
    }

    public boolean hasToken() {
        return storage.hasToken();
    }

    public boolean hasValidAccessToken() {
        return storage.hasValidAccessToken();
    }

    public void invalidateAccessToken() {
        storage.invalidateAccessToken();
    }

    public void clearToken() {
        storage.clearToken();
    }

    public void updateToken(String accessToken, String refreshToken, long expiresAt) {
        storage.updateToken(accessToken, refreshToken, expiresAt);
    }

    public void retrieveAccessToken(TokenCallback callback) {
        if (!hasToken()) {
            callback.onAuthExpired();
            return;
        }

        if (hasValidAccessToken()) {
            callback.onAccessToken(getAccessToken());
            return;
        }

        callbacks.add(callback);

        if (refreshing) {
            return;
        }

        refreshing = true;

        am.refresh(storage.getRefreshToken(), new AuthManager.RefreshCallback() {
            @Override
            public void onGetToken(String accessToken, String refreshToken, long expiresAt) {
                refreshing = false;
                handleRefreshSuccess(accessToken, refreshToken, expiresAt);
            }

            @Override
            public void onAuthExpired() {
                refreshing = false;
                handleRefreshExpired();
            }

            @Override
            public void onFailure(Throwable t) {
                refreshing = false;
                handleRefreshFailure(t);
            }
        });
    }

    private void handleRefreshSuccess(String accessToken, String refreshToken, long expiresAt) {
        updateToken(accessToken, refreshToken, expiresAt);
        for (TokenCallback callback : callbacks) {
            callback.onAccessToken(accessToken);
        }
    }

    private void handleRefreshExpired() {
        clearToken();
        for (TokenCallback callback : callbacks) {
            callback.onAuthExpired();
        }
    }

    private void handleRefreshFailure(Throwable t) {
        Log.e(TAG, "Failed to refresh token", t);
        for (TokenCallback callback : callbacks) {
            callback.onTokenRefreshFailed(t);
        }
    }

    public interface TokenCallback {

        void onAccessToken(String accessToken);

        void onAuthExpired();

        void onTokenRefreshFailed(Throwable t);

    }

}
