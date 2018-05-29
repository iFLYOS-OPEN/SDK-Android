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
