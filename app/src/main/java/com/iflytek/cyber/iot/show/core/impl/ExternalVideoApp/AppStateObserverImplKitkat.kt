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


package com.iflytek.cyber.iot.show.core.impl.ExternalVideoApp

import android.app.ActivityManager
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import java.lang.ref.WeakReference

/**
 * Android 4.4 以前使用 GET_TASKS 权限用于获取应用状态
 */
internal class AppStateObserverImplKitkat(context: Context) : AppStateObserver() {
    private val handler: Handler
    private val contextReference = WeakReference(context)
    private val onStateChangeListeners = HashSet<OnStateChangeListener>()
    private var preAppState: AppState? = null

    init {
        val handlerThread = HandlerThread("AppState")
        handlerThread.start()
        handler = Handler(handlerThread.looper)
    }

    private val runnable = object : Runnable {
        override fun run() {
            val currentAppState = readCurrentAppState()

            if (preAppState?.packageName != currentAppState?.packageName ||
                    preAppState?.currentActivity != currentAppState?.currentActivity) {
                currentAppState?.let { appState ->
                    onStateChangeListeners.map {
                        try {
                            it.onStateChange(appState, preAppState)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            currentAppState?.let { appState ->
                preAppState = AppState(appState.packageName, appState.currentActivity, appState.version, false)
            }

            handler.postDelayed(this, 500)
        }
    }

    override fun addOnStateChangeListener(onStateChangeListener: OnStateChangeListener) {
        onStateChangeListeners.add(onStateChangeListener)
    }

    override fun removeOnStateChangeListener(onStateChangeListener: OnStateChangeListener) {
    }

    override fun start(): Boolean {
        val current = readCurrentAppState()
        return if (current == null) {
            false
        } else {
            handler.removeCallbacksAndMessages(null)
            handler.post(runnable)
            true
        }
    }

    /**
     * 读取前台应用状态
     *
     * 忽略此处的过时，只有小于 L 的版本才会执行到这里
     */
    @Suppress("DEPRECATION")
    private fun readCurrentAppState(): AppState? {
        val context = contextReference.get() ?: return null
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val tasks = activityManager?.getRunningTasks(1)
        val topActivity = tasks?.get(0)?.topActivity ?: return null

        val packages = context.packageManager.getInstalledPackages(0)
        var version = ""
        packages.map {
            if (it.packageName == topActivity.packageName) {
                version = it.versionName
            }
        }

        return AppState(topActivity.packageName, topActivity.className, version, true)
    }

}