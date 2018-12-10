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

package com.iflytek.cyber.iot.show.core

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.content.PermissionChecker
import android.text.TextUtils
import android.util.Log
import cn.iflyos.iace.core.Engine
import cn.iflyos.iace.core.PlatformInterface
import cn.iflyos.iace.iflyos.Alerts
import cn.iflyos.iace.iflyos.AuthProvider
import cn.iflyos.iace.iflyos.IflyosClient
import cn.iflyos.iace.iflyos.IflyosProperties
import cn.iflyos.iace.iflyos.config.IflyosConfiguration
import cn.iflyos.iace.logger.Logger
import cn.iflyos.iace.logger.config.LoggerConfiguration
import com.iflytek.cyber.iot.show.core.impl.Alerts.AlertsHandler
import com.iflytek.cyber.iot.show.core.impl.AudioPlayer.AudioPlayerHandler
import com.iflytek.cyber.iot.show.core.impl.AuthProvider.AuthProviderHandler
import com.iflytek.cyber.iot.show.core.impl.IflyosClient.IflyosClientHandler
import com.iflytek.cyber.iot.show.core.impl.Logger.LoggerHandler
import com.iflytek.cyber.iot.show.core.impl.MediaPlayer.MediaPlayerHandler
import com.iflytek.cyber.iot.show.core.impl.PlaybackController.PlaybackControllerHandler
import com.iflytek.cyber.iot.show.core.impl.SpeechRecognizer.SpeechRecognizerHandler
import com.iflytek.cyber.iot.show.core.impl.SpeechSynthesizer.SpeechSynthesizerHandler
import com.iflytek.cyber.iot.show.core.impl.TemplateRuntime.TemplateRuntimeHandler
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class EngineService : Service() {

    private var mEngine: Engine? = null
    private var mPreferences: SharedPreferences? = null

    // directive handler
    private var mAlerts: AlertsHandler? = null
    private var mAudioPlayer: AudioPlayerHandler? = null
    private var mAuthProvider: AuthProviderHandler? = null
    private var mIflyosClient: IflyosClientHandler? = null
    private var mLogger: LoggerHandler? = null
    private var mPlaybackController: PlaybackControllerHandler? = null
    private var mSpeechRecognizer: SpeechRecognizerHandler? = null
    private var mSpeechSynthesizer: SpeechSynthesizerHandler? = null
    private var mTemplateRuntime: TemplateRuntimeHandler? = null

    companion object {
        private const val sTag = "EngineService"
        private const val sDeviceConfigFile = "app_config.json"

        private const val ACTION_PREFIX = "com.iflytek.cyber.iot.show.core.service.action."
        const val ACTION_LOGIN = ACTION_PREFIX + "LOGIN"
        const val ACTION_START_ENGINE = ACTION_PREFIX + "START"
        const val ACTION_RECONNECT = ACTION_PREFIX + "RECONNECT"
        const val ACTION_TAP_TO_TALK = ACTION_PREFIX + "TAP_TO_TALK"
        const val ACTION_STOP_CAPTURE = ACTION_PREFIX + "STOP_CAPTURE"
        const val ACTION_LOGOUT = ACTION_PREFIX + "LOGOUT"
        const val ACTION_LOGOUT_NOT_NOTIFY = ACTION_PREFIX + "LOGOUT_NOT_NOTIFY"
        const val ACTION_UPDATE_VOLUME = ACTION_PREFIX + "ACTION_UPDATE_VOLUME"
        const val ACTION_OFFSET_VOLUME = ACTION_PREFIX + "ACTION_OFFSET_VOLUME"
        const val ACTION_UPDATE_MUTE = ACTION_PREFIX + "ACTION_UPDATE_MUTE"
        const val EXTRA_MUTE = "mute"
        const val EXTRA_VOLUME = "volume"
    }

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    private val binder = EngineBinder()

    open inner class EngineBinder : Binder() {
        fun getService(): EngineService {
            return this@EngineService
        }
    }

    private fun updateDevicePreferences(clientId: String,
                                        clientSecret: String,
                                        productId: String,
                                        productDsn: String) {
        mPreferences?.let { preferences ->
            val editor = preferences.edit()
            editor?.let {
                it.putString(getString(R.string.preference_client_id), clientId)
                        .putString(getString(R.string.preference_client_secret), clientSecret)
                        .putString(getString(R.string.preference_product_id), productId)

                val pdsn = preferences.getString(getString(R.string.preference_product_dsn), "")
                if (PermissionChecker.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                        == PermissionChecker.PERMISSION_GRANTED) {
                    if (TextUtils.isEmpty(pdsn)) {
                        val serial = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1)
                            Build.SERIAL
                        else
                            Build.getSerial()
                        it.putString(getString(R.string.preference_product_dsn), productDsn + serial)
                    }
                }
            }
            editor?.apply()
        }
    }

    private fun getConfigFromFile(): JSONObject? {
        var obj: JSONObject? = null
        try {
            assets.open(sDeviceConfigFile).use { `is` ->
                val buffer = ByteArray(`is`.available())
                `is`.read(buffer)
                val json = String(buffer, Charsets.UTF_8)
                obj = JSONObject(json)
            }
        } catch (e: Exception) {
            Log.w(sTag, String.format("Cannot read %s from assets directory. Error: %s",
                    sDeviceConfigFile, e.message))
        }

        var config: JSONObject? = null
        if (obj != null) {
            try {
                config = obj!!.getJSONObject("config")
            } catch (e: JSONException) {
                Log.w(sTag, "No device config specified in $sDeviceConfigFile")
            }

        }
        return config
    }

    fun recreate() {
        if (PermissionChecker.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PermissionChecker.PERMISSION_GRANTED && PermissionChecker.checkSelfPermission(
                        this, Manifest.permission.READ_PHONE_STATE)
                == PermissionChecker.PERMISSION_GRANTED)
            create()
    }

    private fun create() {
        mPreferences = getSharedPreferences(getString(R.string.preference_file_key),
                Context.MODE_PRIVATE)

        // Retrieve device config from config file and update preferences
        var clientId = ""
        var clientSecret = ""
        var productId = ""
        var productDsn = ""
        val config = getConfigFromFile()
        if (config != null) {
            try {
                clientId = BuildConfig.CLIENT_ID
                clientSecret = config.getString("clientSecret")
                productId = config.getString("productId")
                productDsn = config.getString("productDsn")
            } catch (e: JSONException) {
                Log.w(sTag, "Missing device info in app_config.json")
            }

        }
        updateDevicePreferences(clientId, clientSecret, productId, productDsn)

        try {
            startEngine()
        } catch (e: RuntimeException) {
            Log.e(sTag, "Could not start engine. Reason: ${e.message}")
            return
        }
    }

    fun getAuthToken(): String? {
        return mAuthProvider?.authToken
    }

    fun getLocalAccessToken(): String? {
        return mAuthProvider?.localAccessToken
    }

    fun getAuthState(): AuthProvider.AuthState? {
        return mAuthProvider?.authState
    }

    fun login() {
        mAuthProvider?.login()
    }

    fun logout() {
        mAudioPlayer?.mediaPlayer?.stop()
        mSpeechSynthesizer?.mediaPlayer?.stop()
        mAlerts?.mediaPlayer?.stop()
        mAuthProvider?.logout()
    }

    fun logoutWithNotNotify() {
        mAudioPlayer?.mediaPlayer?.stop()
        mSpeechSynthesizer?.mediaPlayer?.stop()
        mAlerts?.mediaPlayer?.stop()
        mAuthProvider?.logoutWithNotNotify()
    }

    fun addObserver(observer: Observer) {
        Log.d(sTag, "addObserver")
        mLogger?.addLogObserver(observer)
        mSpeechRecognizer?.addObserver(observer)
    }

    fun deleteObserver(observer: Observer) {
        mLogger?.deleteLogObserver(observer)
        mSpeechRecognizer?.deleteObserver(observer)
    }

    fun getPlaybackController(): PlaybackControllerHandler? {
        return mPlaybackController
    }

    fun getHandler(namespace: String): PlatformInterface? {
        return when (namespace) {
            "AudioPlayer" -> mAudioPlayer
            "SpeechRecognizer" -> mSpeechRecognizer
            "Alert" -> mAlerts
            "AuthProvider" -> mAuthProvider
            "IflyosClient" -> mIflyosClient
            "Logger" -> mLogger
            "PlaybackController" -> mPlaybackController
            "SpeechSynthesizer" -> mSpeechSynthesizer
            "TemplateRuntime" -> mTemplateRuntime
            else -> null
        }
    }

    private fun startEngine() {
        // Copy certs to the cache directory
        val cacheDir = cacheDir
        val appDataDir = File(cacheDir, "appdata")
        val certsDir = File(appDataDir, "certs")
        val curlLogs = File(appDataDir, "curlLogs")
        val wakeRes = File(appDataDir, "wakeres")
        certsDir.mkdirs()
        curlLogs.mkdirs()
        wakeRes.mkdirs()
        var certFile: File? = null
        try {
            val certAssets = assets.list("certs") ?: emptyArray()
            for (next in certAssets) {
                certFile = File(certsDir, next)
                copyAsset("certs/$next", certFile, false)
                break
            }
        } catch (e: IOException) {
            Log.w(sTag, "Cannot copy certs to cache directory. Error: " + e.message)
        }

        try {
            val wakeResAssets = assets.list("wakeres") ?: emptyArray()
            for (next in wakeResAssets) {
                val resFile = File(wakeRes, next)
                copyAsset("wakeres/$next", resFile, false)
            }
        } catch (e: IOException) {
            Log.w(sTag, "Cannot copy wakeres to cache directory. Error: " + e.message)
        }


        // Create AAC engine
        mEngine = Engine.create()

        // Configure the engine
        val productDsn = mPreferences?.getString(getString(R.string.preference_product_dsn), "")
        val clientId = mPreferences?.getString(getString(R.string.preference_client_id), "")
        val productId = mPreferences?.getString(getString(R.string.preference_product_id), "")

        val configureSucceeded = mEngine?.configure(arrayOf(IflyosConfiguration.createCurlConfig(certFile!!.absolutePath),
                //IflyosConfiguration.createCurlLogConfig(curlLogs.getAbsolutePath()),
                IflyosConfiguration.createDeviceInfoConfig(productDsn, clientId, productId),
                IflyosConfiguration.createMiscStorageConfig(appDataDir.path + "/miscStorage.sqlite"),
                IflyosConfiguration.createCertifiedSenderConfig(appDataDir.path + "/certifiedSender.sqlite"),
                IflyosConfiguration.createAlertsConfig(appDataDir.path + "/alerts.sqlite"),
                IflyosConfiguration.createSettingsConfig(appDataDir.path + "/settings.sqlite"),
                LoggerConfiguration.createSyslogSinkConfig("syslog", Logger.Level.VERBOSE)))
        if (configureSucceeded == true) {
            Log.d(sTag, "Configure succeeded")
        } else {
            throw RuntimeException("Engine configuration failed")
        }
        mEngine?.setProperty(IflyosProperties.IVS_ENDPOINT, "https://ivs.iflyos.cn")

        // Logger
        mLogger = LoggerHandler()
        if (mEngine?.registerPlatformInterface(mLogger) != true) {
            throw RuntimeException("Could not register Logger platform interface")
        }

        // Client
        mIflyosClient = IflyosClientHandler(this, mLogger)
        if (mEngine?.registerPlatformInterface(mIflyosClient) != true) {
            throw RuntimeException("Could not register IflyosClient platform interface")
        }

        // PlaybackController
        mPlaybackController = PlaybackControllerHandler(this, mLogger)
        if (mEngine?.registerPlatformInterface(mPlaybackController) != true) {
            throw RuntimeException("Could not register PlaybackController platform interface")
        }

        // SpeechRecognizer
        val wakeWordSupported = mEngine?.getProperty(IflyosProperties.WAKEWORD_SUPPORTED) == "true"
        mSpeechRecognizer = mIflyosClient?.setRecognizer(SpeechRecognizerHandler(
                this,
                mLogger,
                wakeWordSupported,
                true,
                wakeRes.absolutePath
        ))
        if (mEngine?.registerPlatformInterface(mSpeechRecognizer) != true)
            throw RuntimeException("Could not register SpeechRecognizer platform interface")


        // AudioPlayer
        mAudioPlayer = AudioPlayerHandler(MediaPlayerHandler(
                this,
                mLogger,
                "Audio Player",
                MediaPlayerHandler.SpeakerType.SYNCED,
                mPlaybackController))
        if (mEngine?.registerPlatformInterface(mAudioPlayer) != true) {
            throw RuntimeException("Could not register AudioPlayer platform interface")
        }

        // SpeechSynthesizer
        mSpeechSynthesizer = SpeechSynthesizerHandler(MediaPlayerHandler(
                this,
                mLogger,
                "Speech Synthesizer",
                MediaPlayerHandler.SpeakerType.SYNCED,
                null))
        if (mEngine?.registerPlatformInterface(mSpeechSynthesizer) != true) {
            throw RuntimeException("Could not register SpeechSynthesizer platform interface")
        }

        // TemplateRuntime
        mTemplateRuntime = TemplateRuntimeHandler(mLogger, mPlaybackController)
        if (mEngine?.registerPlatformInterface(mTemplateRuntime) != true) {
            throw RuntimeException("Could not register TemplateRuntime platform interface")
        }

        // Alerts
        mAlerts = AlertsHandler(
                this,
                mLogger!!,
                MediaPlayerHandler(
                        this,
                        mLogger,
                        "Alerts",
                        MediaPlayerHandler.SpeakerType.LOCAL,
                        null))
        if (mEngine?.registerPlatformInterface(mAlerts) != true) {
            throw RuntimeException("Could not register Alerts platform interface")
        }

        // AuthProvider
        mAuthProvider = AuthProviderHandler(this, mLogger!!)
        if (mEngine?.registerPlatformInterface(mAuthProvider) != true) {
            throw RuntimeException("Could not register AuthProvider platform interface")
        }

        // Start the engine
        if (mEngine?.start() != true) throw RuntimeException("Could not start engine")
        mAuthProvider?.onInitialize()
    }

    fun connectionStatus(): IflyosClient.ConnectionStatus? {
        return mIflyosClient?.connectionStatus
    }

    fun currentAlertState(): Alerts.AlertState? {
        return mAlerts?.currentState
    }

    fun stopAlert() {
        mAlerts?.onLocalStop()
    }

    fun stopSpeaking() {
        val player = mSpeechSynthesizer?.mediaPlayer
        if (player is MediaPlayerHandler) {
            if (player.isPlaying)
                player.stop()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!intent?.action.isNullOrEmpty()) {
            when (intent?.action) {
                ACTION_LOGIN -> {
                    login()
                }
                ACTION_START_ENGINE -> {
                    recreate()
                }
                ACTION_TAP_TO_TALK -> {
                    mSpeechRecognizer?.onTapToTalk()
                }
                ACTION_STOP_CAPTURE -> {
                    mSpeechRecognizer?.stopCapture()
                }
                ACTION_LOGOUT -> {
                    logout()
                }
                ACTION_LOGOUT_NOT_NOTIFY -> {
                    logoutWithNotNotify()
                }
                ACTION_RECONNECT -> {
                    mAuthProvider?.onInitialize()
                }
                ACTION_UPDATE_MUTE -> {
                    val mute = intent.getBooleanExtra(EXTRA_MUTE, false)
                    mAudioPlayer?.speaker?.localMuteSet(mute)
                    mSpeechSynthesizer?.speaker?.localMuteSet(mute)
                }
                ACTION_UPDATE_VOLUME -> {
                    val volume = intent.getByteExtra(EXTRA_VOLUME, 50)
                    mAudioPlayer?.speaker?.localVolumeSet(volume)
                    mSpeechSynthesizer?.speaker?.localVolumeSet(volume)
                }
                ACTION_OFFSET_VOLUME -> {
                    val volume = intent.getByteExtra(EXTRA_VOLUME, 50)
                    val current = mAudioPlayer?.speaker?.volume ?: 0.toByte()
                    mAudioPlayer?.speaker?.localVolumeSet(Math.max(Math.min(volume + current, 100), 0).toByte())
                    mSpeechSynthesizer?.speaker?.localVolumeSet(Math.max(Math.min(volume + current, 100), 0).toByte())
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    fun reconnectIVS() {
        mAuthProvider?.onInitialize()
    }

    fun speakerMute() = mAudioPlayer?.speaker?.isMuted

    fun speakerVolume() = mAudioPlayer?.speaker?.volume

    private fun copyAsset(assetPath: String, destFile: File, force: Boolean) {
        if (!destFile.exists() || force) {
            if (destFile.parentFile.exists() || destFile.parentFile.mkdirs()) {
                // Copy the asset to the dest path
                try {
                    assets.open(assetPath).use { `is` ->
                        FileOutputStream(destFile).use { os ->
                            val buf = ByteArray(1024)
                            var len = `is`.read(buf)
                            while (len > 0) {
                                os.write(buf, 0, len)
                                len = `is`.read(buf)
                            }
                        }
                    }
                } catch (e: IOException) {
                    Log.w(sTag, e.message)
                }

            } else {
                Log.w(sTag, "Could not create cache directory: " + destFile.parentFile)
            }
        } else {
            Log.w(sTag, String.format("Skipping existing file in cache: %s to: %s",
                    assetPath, destFile))
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        mSpeechRecognizer?.disableWakewordDetection()

        val result = mEngine?.stop()

        if (mLogger != null)
            mLogger?.postInfo(sTag, "Engine stopped with result: $result")
        else
            Log.i(sTag, "Engine stopped")

        System.exit(0) // End all
    }

    fun offsetVolume(offset: Byte) {
        val volume = mAudioPlayer?.speaker?.volume ?: 0
        mAudioPlayer?.speaker?.localVolumeSet(Math.max(Math.min(volume + offset, 100), 0).toByte())
        mSpeechSynthesizer?.speaker?.localVolumeSet(Math.max(Math.min(volume + offset, 100), 0).toByte())
    }
}
