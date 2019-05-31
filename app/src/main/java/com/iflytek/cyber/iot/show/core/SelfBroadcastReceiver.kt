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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

abstract class SelfBroadcastReceiver(vararg actions: String) : BroadcastReceiver() {

    private val filter = IntentFilter()

    private var registered = false

    init {
        for (action in actions) {
            filter.addAction(action)
        }
    }

    protected abstract fun onReceiveAction(action: String, intent: Intent)

    fun getFilter() = filter

    override fun onReceive(context: Context, intent: Intent?) {
        intent ?: return
        val action = intent.action
        if (!action.isNullOrEmpty()) {
            onReceiveAction(action, intent)
        }
    }

    fun register(context: Context?) {
        if (!registered) {
            context?.registerReceiver(this, filter)
            registered = true
        }
    }

    fun unregister(context: Context?) {
        if (registered) {
            registered = false
            context?.unregisterReceiver(this)
        }
    }

}
