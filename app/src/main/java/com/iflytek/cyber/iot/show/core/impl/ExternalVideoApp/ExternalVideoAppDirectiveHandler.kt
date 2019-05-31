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
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import cn.iflyos.sdk.android.impl.externalvideoapp.ExternalVideoAppHandler
import cn.iflyos.sdk.android.v3.constant.ExternalVideoAppConstant
import cn.iflyos.sdk.android.v3.constant.ExternalVideoAppConstant.PKG_IQIYI_SPEAKER
import cn.iflyos.sdk.android.v3.constant.ExternalVideoAppConstant.PKG_IQIYI_TV
import cn.iflyos.sdk.android.v3.constant.ExternalVideoAppConstant.PKG_SHOW_CORE_OVERLAY
import cn.iflyos.sdk.android.v3.constant.ExternalVideoAppConstant.iQiYi
import cn.iflyos.sdk.android.v3.constant.ExternalVideoAppConstant.iQiYiSpeaker
import cn.iflyos.sdk.android.v3.iFLYOSManager
import cn.iflyos.sdk.android.v3.ipc.iFLYOSInterface
import com.gala.tv.voice.VoiceClient
import com.gala.tv.voice.VoiceEventFactory
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.iflytek.cyber.iot.show.core.LauncherActivity

/**
 * 外部视频播放器指令处理器
 */
class ExternalVideoAppDirectiveHandler : ExternalVideoAppHandler.OnDirectiveHandler, AppStateObserver.OnStateChangeListener {

    companion object {
        const val TAG = "ExternalVideoApp"
    }

    private var mVoiceClient: VoiceClient? = null
    private var context: Context? = null

    private val appStateMap = HashMap<String, AppStateObserver.AppState>()

    fun init(context: Context) {
        this.context = context
        VoiceClient.initialize(context.applicationContext, PKG_IQIYI_TV)
        mVoiceClient = VoiceClient.instance()
        mVoiceClient?.setListener(object : VoiceClient.ConnectionListener {
            override fun onConnected() {
                Log.d(TAG, "VoiceClient connected.")
            }

            override fun onDisconnected(p0: Int) {
                Log.d(TAG, "VoiceClient disconnected. Code: $p0")
            }

        })
        mVoiceClient?.connect()
        if (mVoiceClient == null)
            Log.e(TAG, "VoiceClient init failed")
    }

    /**
     * 重置 Context 中的外部视频应用状态
     */
    fun resetAppStates(packageNames: Array<String>) {
        appStateMap.clear()

        val packages = context?.packageManager?.getInstalledPackages(0)
        if (!packages.isNullOrEmpty()) {
            val payload = JsonObject()
            val states = JsonArray()
            packages.map {
                if (it.packageName in packageNames) {
                    appStateMap[it.packageName] =
                            AppStateObserver.AppState(it.packageName, null, it.versionName, false)

                    if (it.packageName == PKG_IQIYI_TV) {
                        val iqiyiState = JsonObject()
                        iqiyiState.addProperty("name", "iQiYi")
                        iqiyiState.addProperty("runningState", ExternalVideoAppConstant.STATE_IDLE)
                        iqiyiState.addProperty("version", it.versionName)
                        states.add(iqiyiState)
                    } else if (it.packageName == PKG_IQIYI_SPEAKER) {
                        val iqiyiState = JsonObject()
                        iqiyiState.addProperty("name", "iQiYi_speaker")
                        iqiyiState.addProperty("runningState", ExternalVideoAppConstant.STATE_IDLE)
                        iqiyiState.addProperty("version", it.versionName)
                        states.add(iqiyiState)
                    }
                }
            }
            payload.add("states", states)
            iFLYOSManager.getInstance()
                    .sendMsg(iFLYOSInterface.IFLYOS_DO_EXTVIDPLAYER_UPDATE_CONTEXT, payload.toString())
        }
    }

    override fun onStateChange(currentState: AppStateObserver.AppState, previousState: AppStateObserver.AppState?) {
        Log.d(TAG, "onStateChanged: {current: $currentState, pre: $previousState}")
        val videoAppPackages = arrayOf(PKG_IQIYI_SPEAKER, PKG_IQIYI_TV)
        if (!currentState.packageName.isNullOrEmpty() && currentState.packageName in videoAppPackages) {
            val manager = iFLYOSManager.getInstance()
            manager.sendMsg(iFLYOSInterface.IFLYOS_DO_EXTVIDPLAYER_REQUIRE_CHANNEL, "")
        } else if (previousState?.packageName in videoAppPackages) {
            val manager = iFLYOSManager.getInstance()
            manager.sendMsg(iFLYOSInterface.IFLYOS_DO_EXTVIDPLAYER_REPORT_STATUS, "false")
        }

        // update context
        previousState?.let { state ->
            if (!state.packageName.isNullOrEmpty() && state.packageName in videoAppPackages) {
                if (currentState.packageName == PKG_SHOW_CORE_OVERLAY) {
                    state.isForeground = true
                }
                appStateMap[state.packageName] = state
            }
        }
        if (!currentState.packageName.isNullOrEmpty() && currentState.packageName in videoAppPackages) {
            appStateMap[currentState.packageName] = currentState
        }
        updateContext()
    }

    private fun updateContext() {
        val payload = JsonObject()
        val states = JsonArray()
        appStateMap.map {
            it.value.let { appState ->
                val iqiyiState = JsonObject()
                when (appState.packageName) {
                    PKG_IQIYI_SPEAKER -> {
                        iqiyiState.addProperty("name", "iQiYi_speaker")
                        iqiyiState.addProperty("latestPage", appState.currentActivity)
                    }
                    PKG_IQIYI_TV -> {
                        iqiyiState.addProperty("name", "iQiYi")
                    }
                }
                if (appState.isForeground)
                    iqiyiState.addProperty("runningState", ExternalVideoAppConstant.STATE_FOREGROUND)
                else
                    iqiyiState.addProperty("runningState", ExternalVideoAppConstant.STATE_BACKGROUND)
                iqiyiState.addProperty("version", appState.version)
                states.add(iqiyiState)
            }
        }
        payload.add("states", states)
        iFLYOSManager.getInstance().sendMsg(
                iFLYOSInterface.IFLYOS_DO_EXTVIDPLAYER_UPDATE_CONTEXT, payload.toString())
    }

    override fun onOperate(payload: String) {
        val payloadJson = JsonParser().parse(payload).asJsonObject
        when (payloadJson.get("name").asString) {
            ExternalVideoAppConstant.TYPE_IQIYI -> {
                handleIQiYiOperate(payloadJson.get("actionType").asString,
                        payloadJson.get("token").asString,
                        payloadJson.get("data").asJsonObject)
            }
        }
    }

    override fun onChannelStatusChanged(allowActive: Boolean, isForeground: Boolean) {
    }

    override fun onReleaseChannel() {
        iFLYOSManager.getInstance().sendMsg(iFLYOSInterface.IFLYOS_DO_EXTVIDPLAYER_REPORT_STATUS, "false")

        context?.let { context ->
            try {
                val intent = Intent(context, LauncherActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                context.applicationContext.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

//        Log.d(TAG, "state: ${appStateMap[PKG_IQIYI_SPEAKER]}")
//        if (appStateMap[PKG_IQIYI_SPEAKER]?.isForeground == true) {
//            val uri = Uri.parse("iqiyi://com.qiyi.video.speaker/app?from=other&command=exit_app")
//            try {
//                val intent = Intent(Intent.ACTION_VIEW, uri)
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                context?.startActivity(intent)
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }
    }

    private fun handleIQiYiOperate(actionType: String, token: String, data: JsonObject) {
        when (actionType) {
            iQiYi.ACTION_TYPE_PLAYVIDEO, iQiYi.ACTION_TYPE_DETAIL,
            iQiYi.ACTION_TYPE_ALBUMLIST, iQiYi.ACTION_TYPE_PERSON_CENTER,
            iQiYi.ACTION_TYPE_WEB_PAGE, iQiYi.ACTION_TYPE_SUBJECT -> {
                val intent = Intent(iQiYi.makeAction(PKG_IQIYI_TV, actionType))
                data.addProperty("customer", "iflyos")
                val bundle = Bundle()
                bundle.putString("playInfo", data.toString())
                intent.putExtras(bundle)
                context?.sendBroadcast(intent)
            }
            // ActionType above is to use broadcast
            // ActionType below is to use event
            iQiYi.ACTION_TYPE_EPISODE_DIRECTION -> {
                // ignore
            }
            iQiYi.ACTION_TYPE_EPISODE_INDEX -> {
                // ignore
            }
            iQiYi.ACTION_TYPE_KEYWORDS -> {
                val keywords = data.get("keywords").asString
                val keywordsEvent = VoiceEventFactory.createKeywordsEvent(keywords)
                val result = mVoiceClient?.dispatchVoiceEvent(keywordsEvent)
                if (result != true) {
                    handleOperateFailed(token, message = "iQiYi sdk dispatchVoiceEvent failed. actionType: $actionType")
                }
                Log.d(TAG, "handle actionType: '$actionType', result: $result")
            }
            iQiYi.ACTION_TYPE_SEEK_OFFSET -> {
                val offset = data.get("offset").asLong
                val seekOffset = VoiceEventFactory.createSeekOffsetEvent(offset)
                val result = mVoiceClient?.dispatchVoiceEvent(seekOffset)
                if (result != true) {
                    handleOperateFailed(token, message = "iQiYi sdk dispatchVoiceEvent failed. actionType: $actionType")
                }
                Log.d(TAG, "handle actionType: '$actionType', result: $result")
            }
            iQiYi.ACTION_TYPE_SEEK_TO -> {
                val offset = data.get("offset").asLong
                val seekTo = VoiceEventFactory.createSeekToEvent(offset)
                val result = mVoiceClient?.dispatchVoiceEvent(seekTo)
                if (result != true) {
                    handleOperateFailed(token, message = "iQiYi sdk dispatchVoiceEvent failed. actionType: $actionType")
                }
                Log.d(TAG, "handle actionType: '$actionType', result: $result")
            }
            iQiYiSpeaker.ACTION_TYPE_APP -> {
                val command = (data.get("command") as? JsonPrimitive)?.asString
                val param = (data.get("param")as? JsonPrimitive)?.asString
                val from = (data.get("from")as? JsonPrimitive)?.asString ?: "other"
                val uriBuilder = Uri.Builder()
                uriBuilder.scheme("iqiyi")
                uriBuilder.authority(PKG_IQIYI_SPEAKER)
                uriBuilder.path("/app")
                uriBuilder.appendQueryParameter("from", from)
                if (!command.isNullOrEmpty()) {
                    uriBuilder.appendQueryParameter("command", command)
                }
                if (!param.isNullOrEmpty()) {
                    uriBuilder.appendQueryParameter("param", param)
                }
                val uri = uriBuilder.build()
                try {
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context?.startActivity(intent)
                } catch (e: Exception) {
                    handleOperateFailed(token, message = "iQiYi speaker starting failed. actionType: $actionType, uri: $uri")
                    e.printStackTrace()
                }
            }
            iQiYiSpeaker.ACTION_TYPE_HOMEPAGE -> {
                val command = (data.get("command") as? JsonPrimitive)?.asString
                val param = (data.get("param")as? JsonPrimitive)?.asString
                val from = (data.get("from")as? JsonPrimitive)?.asString ?: "other"
                val uriBuilder = Uri.Builder()
                uriBuilder.scheme("iqiyi")
                uriBuilder.authority(PKG_IQIYI_SPEAKER)
                uriBuilder.path("/homepage")
                uriBuilder.appendQueryParameter("from", from)
                if (!command.isNullOrEmpty()) {
                    uriBuilder.appendQueryParameter("command", command)
                }
                if (!param.isNullOrEmpty()) {
                    uriBuilder.appendQueryParameter("param", param)
                }
                val uri = uriBuilder.build()
                try {
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context?.startActivity(intent)
                } catch (e: Exception) {
                    handleOperateFailed(token, message = "iQiYi speaker starting failed. actionType: $actionType, uri: $uri")
                    e.printStackTrace()
                }
            }
            iQiYiSpeaker.ACTION_TYPE_PLAYER -> {
                val command = (data.get("command") as? JsonPrimitive)?.asString
                val param = (data.get("param")as? JsonPrimitive)?.asString
                val from = (data.get("from")as? JsonPrimitive)?.asString ?: "other"
                val uriBuilder = Uri.Builder()
                uriBuilder.scheme("iqiyi")
                uriBuilder.authority(PKG_IQIYI_SPEAKER)
                uriBuilder.path("/player")
                uriBuilder.appendQueryParameter("from", from)
                if (!command.isNullOrEmpty()) {
                    uriBuilder.appendQueryParameter("command", command)
                }
                if (!param.isNullOrEmpty()) {
                    uriBuilder.appendQueryParameter("param", param)
                }
                val uri = uriBuilder.build()
                try {
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context?.startActivity(intent)
                } catch (e: Exception) {
                    handleOperateFailed(token, message = "iQiYi speaker starting failed. actionType: $actionType, uri: $uri")
                    e.printStackTrace()
                }
            }
            else -> {
                handleOperateFailed(token, actionType)
                Log.w(TAG, "Unknown actionType: '$actionType', ignore it.")
            }
        }
    }

    private fun handleOperateFailed(token: String, unsupportedActionType: String = "", message: String = "") {
        val paramList = ArrayList<String>()
        paramList.add(token)
        if (mVoiceClient?.isConnected == true) {
            if (unsupportedActionType.isEmpty())
                paramList.add("INTERNAL_ERROR")
            else
                paramList.add("UNSUPPORTED_ACTION_TYPE")
        } else {
            paramList.add("APP_NOT_INSTALLED")
        }
        paramList.add(message)
        iFLYOSManager.getInstance().sendMsg(iFLYOSInterface.IFLYOS_DO_EXTVIDPLAYER_REPORT_ERROR,
                iFLYOSInterface.packParams(paramList))
    }
}