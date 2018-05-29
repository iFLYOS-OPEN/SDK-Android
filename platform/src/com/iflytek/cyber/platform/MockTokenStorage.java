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
