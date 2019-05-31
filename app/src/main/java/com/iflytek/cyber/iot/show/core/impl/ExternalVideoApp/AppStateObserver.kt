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

import android.content.Context
import android.os.Build

/**
 * 外部应用状态订阅器
 */
abstract class AppStateObserver {

    companion object {
        private var observer: AppStateObserver? = null

        fun getInstance(context: Context): AppStateObserver {
            observer?.let {
                return it
            } ?: run {
                val newObserver =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            AppStateObserverImplLollipop(context)
                        } else {
                            AppStateObserverImplKitkat(context)
                        }
                observer = newObserver
                return newObserver
            }
        }
    }

    abstract fun addOnStateChangeListener(onStateChangeListener: OnStateChangeListener)

    abstract fun removeOnStateChangeListener(onStateChangeListener: OnStateChangeListener)

    /**
     * 尝试开始记录应用状态变更
     * @return true or false 开始成功或失败
     */
    abstract fun start(): Boolean

    interface OnStateChangeListener {
        fun onStateChange(currentState: AppState, previousState: AppState?)
    }

    data class AppState(val packageName: String?, val currentActivity: String?,
                        val version: String, var isForeground: Boolean)
}