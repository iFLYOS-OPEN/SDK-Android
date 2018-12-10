/*
 * Copyright (C) 2018 iFLYTEK CO.,LTD.
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

package com.iflytek.cyber.iot.show.core.impl.Logger

import android.util.Log
import cn.iflyos.iace.logger.Logger
import com.google.gson.JsonParser
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.util.*

class LoggerHandler : Logger() {
    private var mObservable: LoggerObservable? = null

    companion object {
        const val TEXT_LOG = 0
        const val BODY_TEMPLATE1 = 1
        const val BODY_TEMPLATE2 = 2
        const val LIST_TEMPLATE1 = 3
        const val WEATHER_TEMPLATE = 4
        const val SET_DESTINATION_TEMPLATE = 5
        const val LOCAL_SEARCH_LIST_TEMPLATE1 = 6
        const val RENDER_PLAYER_INFO = 7
        const val CBL_CODE = 8
        const val CBL_CODE_EXPIRED = 9
        const val JSON_TEXT = 10
        const val AUTH_LOG = 11
        const val IAT_LOG = 12
        const val RECORD_VOLUME = 13
        const val DIALOG_STATE = 14
        const val BODY_TEMPLATE3 = 15
        const val CONNECTION_STATE = 16
        const val EXCEPTION_LOG = 17
        const val AUTH_LOG_URL = 0
        const val AUTH_LOG_STATE = 1

        private const val sClientSourceTag = "CLI"
    }

    fun postVerbose(tag: String, message: String) {
        log(Logger.Level.VERBOSE, tag, message)
    }

    fun postInfo(tag: String, message: String) {
        log(Logger.Level.INFO, tag, message)
    }

    fun postWarn(tag: String, message: String) {
        log(Logger.Level.WARN, tag, message)
    }

    fun postError(tag: String, message: String) {
        log(Logger.Level.ERROR, tag, message)
    }

    fun postError(tag: String, thr: Throwable) {
        try {
            ByteArrayOutputStream().use { os ->
                val ps = PrintStream(os)
                thr.printStackTrace(ps)
                val str = os.toString()
                log(Logger.Level.ERROR, tag, str)

            }
        } catch (e: IOException) {
            Log.e(sClientSourceTag, "Error: ", e)
        }

    }

    // Client log for display cards
    fun postDisplayCard(template: JSONObject, logType: Int) {
        val level = Logger.Level.INFO
        val json = JSONObject()
        try {
            json.put("template", template)
            json.put("source", sClientSourceTag) // For log view filtering
            json.put("level", level.toString()) // For log view filtering

        } catch (e: JSONException) {
            Log.e(sClientSourceTag, "Error: ", e)
        }

        mObservable?.log(json, logType)
    }

    override fun logEvent(level: Level?, time: Long, source: String?, message: ByteArray?): Boolean {
        return logEvent(level, time, source, String(message
                ?: ByteArray(0), StandardCharsets.UTF_8))
    }

    override fun logEvent(level: Level?, time: Long, source: String?, message: String?): Boolean {
        if (level == Level.ERROR) {
            if (source == "IVS") {
                try {
                    val array = message?.split(Regex(":"), 3)
                    if (array?.size == 3) {
                        if (array[1] == "onExceptionReceived") {
                            when (array[0]) {
                                "MessageRequest", "AudioInputProcessor" -> {
                                    val jsonSplit = array[2].split(Regex("="), 2)
                                    if (jsonSplit.size == 2) {
                                        val exceptionJson = JsonParser().parse(
                                                jsonSplit[1].replace("\\", "")).asJsonObject
                                        if (exceptionJson.getAsJsonObject("header")
                                                        .getAsJsonPrimitive("name")
                                                        .asString == "Exception") {
                                            val payload = exceptionJson.getAsJsonObject("payload")
                                            postDisplayCard(
                                                    JSONObject(payload.toString()), EXCEPTION_LOG)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return true
    }

    class LoggerObservable : Observable() {

        fun log(message: String) {
            setChanged()
            notifyObservers(message)
        }

        fun log(obj: JSONObject) {
            setChanged()
            notifyObservers(LogEntry(TEXT_LOG, obj))
        }

        fun log(obj: JSONObject, logType: Int) {
            setChanged()
            notifyObservers(LogEntry(logType, obj))
        }
    }

    fun addLogObserver(observer: Observer) {
        if (mObservable == null) mObservable = LoggerObservable()
        mObservable?.addObserver(observer)
    }

    fun deleteLogObserver(observer: Observer) {
        mObservable?.deleteObserver(observer)
    }
}