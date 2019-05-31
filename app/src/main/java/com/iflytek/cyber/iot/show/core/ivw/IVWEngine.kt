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

/**
 * 唤醒引擎封闭类。
 */
@Suppress("FunctionName")
class IVWEngine {

    private var mResPath: String? = null

    private var mListener: IVWListener? = null

    private var mEngineContextHandle: Long = 0

    val version: String
        get() = jni_get_version(mEngineContextHandle)

    interface IVWListener {
        fun onWakeup(result: String)
    }

    constructor(resPath: String, listener: IVWListener) {
        mResPath = resPath
        mListener = listener

        mEngineContextHandle = jni_create(resPath, "wakeupCb")
    }

    constructor(resPath: String, listener: IVWListener, isLogOn: Boolean) {
        mResPath = resPath
        mListener = listener

        jni_set_log(isLogOn)
        mEngineContextHandle = jni_create(resPath, "wakeupCb")
    }

    //    /**
    //     * 设置门限等级。已废弃
    //     *
    //     * @param level
    //     * @return
    //     */
    //    public int setCMLevel(int level) {
    //        return jni_set_cmlevel(mEngineContextHandle, level);
    //    }
    //
    //    /**
    //     * 设置唤醒词门限。已废弃
    //     *
    //     * @param ncm 门限设置，例："0:1250,1:1300"，多个唤醒词用逗号隔开
    //     * @return
    //     */
    //    public int setKeywordNCM(String ncm) {
    //        return jni_set_keywordncm(mEngineContextHandle, ncm);
    //    }

    fun start(): Int {
        return jni_start(mEngineContextHandle)
    }

    fun writeAudio(buffer: ByteArray, len: Int): Int {
        return jni_write(mEngineContextHandle, buffer, len)
    }

    fun stop(): Int {
        return jni_stop(mEngineContextHandle)
    }

    fun destroy() {
        jni_destroy(mEngineContextHandle)
        mEngineContextHandle = INVALID_HANDLE.toLong()
    }

    fun setLogOn(isOn: Boolean) {
        jni_set_log(isOn)
    }

    private fun wakeupCb(result: String) {
        if (null != mListener) {
            mListener!!.onWakeup(result)
        }
    }

    private external fun jni_create(resPath: String, wakeupCbName: String): Long

    @Suppress("unused")
    companion object {
        init {
            System.loadLibrary("ivw-jni")
        }

        @JvmStatic
        private val INVALID_HANDLE = 0

        @JvmStatic
        val CMLEVEL_LOWEST = 0

        @JvmStatic
        val CMLEVEL_LOWER = 1

        @JvmStatic
        val CMLEVEL_LOW = 2

        @JvmStatic
        val CMLEVEL_NORMAL = 3

        @JvmStatic
        val CMLEVEL_HIGH = 4

        @JvmStatic
        val CMLEVEL_HIGHER = 5

        @JvmStatic
        val CMLEVEL_HIGHEST = 6

        @JvmStatic
        private external fun jni_destroy(handle: Long)

        @JvmStatic
        private external fun jni_set_cmlevel(handle: Long, level: Int): Int

        @JvmStatic
        private external fun jni_set_keywordncm(handle: Long, ncm: String): Int

        @JvmStatic
        private external fun jni_start(handle: Long): Int

        @JvmStatic
        private external fun jni_write(handle: Long, buffer: ByteArray, size: Int): Int

        @JvmStatic
        private external fun jni_stop(handle: Long): Int

        @JvmStatic
        private external fun jni_set_log(isOn: Boolean)

        @JvmStatic
        private external fun jni_get_version(handle: Long): String
    }
}
