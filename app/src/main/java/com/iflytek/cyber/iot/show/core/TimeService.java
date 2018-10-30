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

import android.app.AlarmManager;
import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.NtpV3Packet;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.commons.net.ntp.TimeStamp;

import java.net.InetAddress;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class TimeService extends IntentService {

    private static final String TAG = "TimeService";

    private static final String NTP_SERVER = "time1.aliyun.com";

    public TimeService() {
        super("time");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        final NTPUDPClient client = new NTPUDPClient();
        client.setDefaultTimeout((int) TimeUnit.SECONDS.toMillis(10));
        try {
            client.open();
            final TimeInfo info = client.getTime(InetAddress.getByName(NTP_SERVER));
            handleResponse(info);
            Log.d(TAG, "Time sync done");
        } catch (Exception e) {
            Log.w(TAG, "Failed syncing time", e);
        }
    }

    private void handleResponse(TimeInfo info) {
        final NtpV3Packet message = info.getMessage();
        final TimeStamp time = message.getTransmitTimeStamp();

        Log.d(TAG, "Current time is " + new Date().toString());
        Log.d(TAG, "Setting time to " + time.getDate().toString());

        final AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarm.setTime(time.getTime());
    }

}
