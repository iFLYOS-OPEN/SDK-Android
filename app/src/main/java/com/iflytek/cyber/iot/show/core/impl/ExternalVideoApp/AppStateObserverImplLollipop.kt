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

import android.annotation.TargetApi
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import androidx.annotation.RequiresApi
import java.lang.ref.WeakReference

/**
 * Android 5.0 及以后使用查看使用情况来获取前台应用
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal class AppStateObserverImplLollipop(context: Context) : AppStateObserver() {
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
        onStateChangeListeners.remove(onStateChangeListener)
    }

    override fun start(): Boolean {
        // try to read state once, if failed means not get permission yet
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
     * 读取当前前台应用状态
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private fun readCurrentAppState(): AppState? {
        val context = contextReference.get() ?: return null
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val ts = System.currentTimeMillis()
        usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, 0, ts)
        val usageEvents = usageStatsManager.queryEvents(0, ts)
        if (usageEvents != null) {
            var lastEvent: UsageEvents.Event? = null
            while (usageEvents.hasNextEvent()) {
                val event = UsageEvents.Event()
                usageEvents.getNextEvent(event)

                if (event.packageName.isNullOrEmpty() || event.className.isNullOrEmpty()) {
                    continue
                }
                if (lastEvent == null || lastEvent.timeStamp < event.timeStamp) {
                    lastEvent = event
                }
            }
            if (lastEvent == null) {
                return null
            }
            val packages = context.packageManager.getInstalledPackages(0)
            var version = ""
            packages.map {
                if (it.packageName == lastEvent.packageName) {
                    version = it.versionName
                }
            }
            return AppState(lastEvent.packageName, lastEvent.className, version, true)
        }
        return null
    }

}