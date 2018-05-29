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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.iflytek.android.io.CloseableUtil;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import okhttp3.internal.io.FileSystem;
import okio.BufferedSource;
import okio.Okio;

public class DeviceId {

    private static final String WLAN_ADDRESS_FILE = "/sys/class/net/wlan0/address";

    private static final String KEY_DEVICE_ID = "device_id";

    private final SharedPreferences pref;

    private DeviceId(Context context) {
        this.pref = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static String get(Context context) {
        return new DeviceId(context).getDeviceId();
    }

    private String getDeviceId() {
        final String mac = getMacAddress();
        if (!TextUtils.isEmpty(mac)) {
            return mac;
        } else {
            return getRandomId();
        }
    }

    private String getMacAddress() {
        BufferedSource source = null;
        try {
            source = Okio.buffer(FileSystem.SYSTEM.source(new File(WLAN_ADDRESS_FILE)));
            return source.readUtf8().trim().replace(":", "");
        } catch (IOException e) {
            return null;
        } finally {
            CloseableUtil.safeClose(source);
        }
    }

    @SuppressLint("ApplySharedPref")
    private String getRandomId() {
        String id = pref.getString(KEY_DEVICE_ID, null);
        if (!TextUtils.isEmpty(id)) {
            return id;
        }

        id = UUID.randomUUID().toString();
        pref.edit().putString(KEY_DEVICE_ID, id).commit();

        return id;
    }

}
