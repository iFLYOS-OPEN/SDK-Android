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

package com.iflytek.cyber.iot.show.core.utils


import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.provider.Settings

import android.location.LocationManager.GPS_PROVIDER
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat

object GpsUtils {

    private fun openGpsSettings(context: Context) {
        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }

    fun checkGpsEnable(context: Context): Boolean {
        val manager =
                ContextCompat.getSystemService(context, LocationManager::class.java)
                        ?: return true // 跳过检查

        // 如果不支持 GPS 那也不强求了
        return !manager.allProviders.contains(GPS_PROVIDER) || manager.isProviderEnabled(GPS_PROVIDER)
    }

    fun requestGps(context: Context) {
        AlertDialog.Builder(context)
                .setPositiveButton("开启") { _, _ -> openGpsSettings(context) }
                .setNegativeButton("取消", null)
                .setMessage("开启位置信息，获取更准确的天气信息")
                .show()
    }
}
