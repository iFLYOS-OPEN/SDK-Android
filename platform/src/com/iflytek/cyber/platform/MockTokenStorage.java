package com.iflytek.cyber.platform;

public class MockTokenStorage extends TokenStorage {

    private final String token;

    public MockTokenStorage(String token) {
        this.token = token;
    }

    @Override
    protected String getRefreshToken() {
        return null;
    }

    @Override
    protected String getAccessToken() {
        return token;
    }

    @Override
    protected long getExpiresAt() {
        return 0;
    }

    @Override
    protected boolean hasToken() {
        return true;
    }

    @Override
    protected boolean hasValidAccessToken() {
        return true;
    }

    @Override
    protected void invalidateAccessToken() {
    }

    @Override
    protected void clearToken() {
    }

    @Override
    protected void updateToken(String accessToken, String refreshToken, long expiresAt) {
    }

}
