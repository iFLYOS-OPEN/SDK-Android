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

package com.iflytek.cyber.iot.show.core.ivw

import android.annotation.SuppressLint
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.text.TextUtils
import cn.iflyos.sdk.android.v3.logger.Logger
import cn.iflyos.sdk.android.v3.params.ParamsManager
import org.json.JSONException
import org.json.JSONObject
import java.io.File

class IvwHandler(private val listener: IvwHandlerListener?) {
    private var mHandlerThread: HandlerThread? = null
    private var mWakeUpHandler: WakeUpHandler? = null

    private val mWakeUpEnable: Boolean
    private val mWakeResPath: String

    private val mObj = Any()

    var wakeUpState: Boolean = false
        set(value) {
            if (value) {
                field = value
                listener?.onWakeUp(value, "")
            }
        }

    private var mIvwEngine: IVWEngine? = null
    private var mIvwListener = object : IVWEngine.IVWListener {
        override fun onWakeup(result: String) {
            Logger.w(TAG, "Wake up result: $result")
            try {
                val ivw = JSONObject(result)
                val rlt = ivw.optJSONArray("rlt")
                if (null != rlt && rlt.length() > 0) {
                    wakeUpState = true
                }
            } catch (e: JSONException) {
                Logger.e(TAG, "Convert ivw result to json happen exception, $e")
            }
        }
    }

    init {
        mWakeUpEnable = ParamsManager.getBoolean(ParamsManager.PARAMS_IVW,
                ParamsManager.PARAMS_KEY_IVW_WAKEUP_ENABLE, DEFAULT_WAKEUP_ENABLE)
        mWakeResPath = ParamsManager.getString(ParamsManager.PARAMS_IVW,
                ParamsManager.PARAMS_KEY_IVW_WAKEUP_RES_PATH, DEFAULT_WAKEUP_RES_PATH)

        // if wakeup enable, then new some object
        if (isWakeUpEnable()) {
            mIvwEngine = IVWEngine(mWakeResPath, mIvwListener, Logger.isLogOn())
//            mIvwEngine?.setLogOn(Logger.isLogOn())
            mIvwEngine?.start()

            mHandlerThread = HandlerThread("IVW_THREAD", Thread.MAX_PRIORITY)
            mHandlerThread?.let {
                it.start()
                mWakeUpHandler = WakeUpHandler(it.looper)
            }
        }
    }

    /**
     * Check wakeup enable
     */
    fun isWakeUpEnable(): Boolean {
        return mWakeUpEnable
                && !TextUtils.isEmpty(mWakeResPath)
                && File(mWakeResPath).exists()
    }

    /**
     * Write audio to ivw engine
     */
    fun write(audio: ByteArray?, len: Int) {
        synchronized(mObj) {
            mWakeUpHandler?.post {
                audio?.let {
                    mIvwEngine?.writeAudio(it, len)
                }
            }
        }
    }

    private fun stopIvw() {
        mIvwEngine?.stop()
    }

    fun release() {
        stopIvw()
        mIvwEngine?.destroy()
    }

    inner class WakeUpHandler(lopper: Looper) : Handler(lopper)

    interface IvwHandlerListener {
        fun onWakeUp(wakeup: Boolean, oriMsg: String)
    }

    companion object {
        const val TAG = "IvwHandler"
        @SuppressLint("SdCardPath")
        private const val IFLYOS_DEFAULT_PATH = "/sdcard/iflyos/" // won't be used
        private const val IFLYOS_IVW_DEFAULT_WAKEUP_FILE_NAME = "lan2-xiao3-fei1.jet"
        const val DEFAULT_WAKEUP_ENABLE = true
        const val DEFAULT_WAKEUP_RES_PATH = IFLYOS_DEFAULT_PATH + IFLYOS_IVW_DEFAULT_WAKEUP_FILE_NAME
    }
}