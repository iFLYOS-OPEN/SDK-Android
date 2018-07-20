package com.iflytek.cyber.platform;

import com.google.gson.annotations.SerializedName;


public class ScopeData {

    @SerializedName("user_ivs_all")
    public UserIvsAll userIvsAll;

    public static class UserIvsAll {
        @SerializedName("device_id")
        public String deviceId;
    }
}
