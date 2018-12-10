package com.iflytek.cyber.iot.show.core.utils

import android.content.Context
import android.net.ConnectivityManager

object ConnectivityUtils {
    fun isNetworkAvailable(context: Context): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val networkInfo = manager?.activeNetworkInfo
        return networkInfo?.isConnected == true
    }
}