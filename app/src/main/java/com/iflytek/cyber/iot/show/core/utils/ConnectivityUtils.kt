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
import android.net.ConnectivityManager
import java.net.HttpURLConnection
import java.net.URL

object ConnectivityUtils {
    private const val IVS_PING_URL = "https://ivs.iflyos.cn/ping"

    /**
     * 检查网络是否可用
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val networkInfo = manager?.activeNetworkInfo
        return networkInfo?.isConnected == true
    }

    /**
     * 检查 iFLYOS 是否可用
     */
    fun checkIvsAvailable(onSuccess: () -> Unit,
                          onFailed: ((throwable: Throwable?, responseCode: Int) -> Unit)? = null) {
        Thread(Runnable {
            var code = -1
            try {
                val url = URL(IVS_PING_URL)
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "GET"

                connection.connect()

                code = connection.responseCode
            } catch (e: Exception) {
                onFailed?.invoke(e, code)
            }

            if (code == 204) {
                // available
                onSuccess.invoke()
            } else {
                onFailed?.invoke(IllegalStateException("Response not succeed"), code)
            }
        }).start()
    }
}