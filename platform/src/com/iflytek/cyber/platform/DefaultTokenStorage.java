package com.iflytek.cyber.platform;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

@SuppressLint("ApplySharedPref")
public class DefaultTokenStorage extends TokenStorage {

    private static final String KEY_REFRESH_TOKEN = "auth_refresh_token";
    private static final String KEY_ACCESS_TOKEN = "auth_access_token";
    private static final String KEY_EXPIRES_AT = "auth_expires_at";

    private final SharedPreferences pref;

    public DefaultTokenStorage(Context context) {
        pref = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    protected String getRefreshToken() {
        return pref.getString(KEY_REFRESH_TOKEN, null);
    }

    @Override
    protected String getAccessToken() {
        return pref.getString(KEY_ACCESS_TOKEN, null);
    }

    @Override
    protected long getExpiresAt() {
        return pref.getLong(KEY_EXPIRES_AT, 0);
    }

    @Override
    protected void invalidateAccessToken() {
        pref.edit().remove(KEY_ACCESS_TOKEN).commit();
    }

    @Override
    protected void clearToken() {
        pref.edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .remove(KEY_EXPIRES_AT)
                .commit();
    }

    @Override
    protected void updateToken(String accessToken, String refreshToken, long expiresAt) {
        pref.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putLong(KEY_EXPIRES_AT, expiresAt)
                .commit();
    }

}
