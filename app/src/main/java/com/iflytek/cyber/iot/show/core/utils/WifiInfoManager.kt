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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.util.*

class WifiInfoManager private constructor() {

    private val listeners = ArrayList<NetworkStateListener>()
    private val rssiListeners = ArrayList<WifiRssiListener>()

    private val uiHandler = Handler(Looper.getMainLooper())

    private var receiver: BroadcastReceiver? = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            for (listener in rssiListeners) {
                listener.onChange()
            }
        }
    }

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    init {
        if (Build.VERSION.SDK_INT >= 21)
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    uiHandler.post {
                        for (listener in listeners) {
                            listener.onAvailable(network)
                        }
                    }
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    uiHandler.post {
                        for (listener in listeners) {
                            listener.onLost(network)
                        }
                    }
                }
            }
    }

    fun getWifiInfo(context: Context): WifiInfo? {
        val manager = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as? WifiManager
        return manager?.connectionInfo
    }

    fun getWifiSignalLevel(context: Context): Int {
        val wifiInfo = getWifiInfo(context)
        return if (wifiInfo != null) {
            WifiManager.calculateSignalLevel(wifiInfo.rssi, 5)
        } else {
            0
        }
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val manager = context.applicationContext
                .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        return if (manager != null) {
            val networkInfo = manager.activeNetworkInfo
            networkInfo != null && networkInfo.isAvailable
        } else {
            false
        }
    }

    fun registerNetworkCallback(context: Context, networkStateListener: NetworkStateListener) {
        listeners.add(networkStateListener)
        val manager = context.applicationContext
                .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (manager != null && Build.VERSION.SDK_INT >= 21) {
            val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
            manager.registerNetworkCallback(request, networkCallback)
        }
    }

    fun unregisterNetworkCallback(context: Context) {
        val manager = context.applicationContext
                .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (manager != null && Build.VERSION.SDK_INT >= 21) {
            manager.unregisterNetworkCallback(networkCallback)
        }
    }

    fun registerWifiRssiCallback(context: Context, listener: WifiRssiListener) {
        rssiListeners.add(listener)
        context.registerReceiver(receiver, IntentFilter(WifiManager.RSSI_CHANGED_ACTION))
    }

    fun unregisterWifiRssiCallback(context: Context) {
        if (receiver != null) {
            context.unregisterReceiver(receiver)
        }
        receiver = null
    }

    interface NetworkStateListener {
        fun onAvailable(network: Network)

        fun onLost(network: Network)
    }

    interface WifiRssiListener {
        fun onChange()
    }

    companion object {

        val manager = WifiInfoManager()
    }

}
