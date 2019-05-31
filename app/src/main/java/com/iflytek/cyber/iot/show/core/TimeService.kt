/*
 * Copyright (C) 2019 iFLYTEK CO.,LTD.
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

package com.iflytek.cyber.iot.show.core

import android.Manifest
import android.app.AlarmManager
import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.PermissionChecker
import org.apache.commons.net.ntp.NTPUDPClient
import org.apache.commons.net.ntp.TimeInfo
import java.net.InetAddress
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 时间同步服务，需要有 SET_TIME 权限才能设置成功
 */
class TimeService : IntentService("time") {

    override fun onHandleIntent(intent: Intent?) {
        val client = NTPUDPClient()
        client.defaultTimeout = TimeUnit.SECONDS.toMillis(10).toInt()
        try {
            client.open()
            val info = client.getTime(InetAddress.getByName(NTP_SERVER))
            handleResponse(info)
            Log.d(TAG, "Time sync done")
        } catch (e: Exception) {
            Log.w(TAG, "Failed syncing time", e)
        }

    }

    private fun handleResponse(info: TimeInfo) {
        val message = info.message
        val time = message.transmitTimeStamp

        Log.d(TAG, "Current time is " + Date().toString())
        Log.d(TAG, "Setting time to " + time.date.toString())

        if (PermissionChecker.checkSelfPermission(this, Manifest.permission.SET_TIME)
                == PermissionChecker.PERMISSION_GRANTED) {
            val alarm = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarm.setTime(time.time)
        }
    }

    companion object {

        private const val TAG = "TimeService"

        private const val NTP_SERVER = "time1.aliyun.com" // 使用阿里云的时间同步接口
    }

}
