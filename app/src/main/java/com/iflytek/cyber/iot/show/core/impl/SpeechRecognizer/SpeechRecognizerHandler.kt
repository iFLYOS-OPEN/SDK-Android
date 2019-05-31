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

package com.iflytek.cyber.iot.show.core.impl.SpeechRecognizer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.app.ActivityCompat
import cn.iflyos.sdk.android.v3.constant.iFLYOSEvent
import cn.iflyos.sdk.android.v3.iFLYOSManager
import cn.iflyos.sdk.android.v3.iFLYOSManager.iFLYOSListener
import cn.iflyos.sdk.android.v3.iface.SpeechRecognizer
import cn.iflyos.sdk.android.v3.ipc.iFLYOSInterface
import cn.iflyos.sdk.android.v3.logger.Logger
import cn.iflyos.sdk.android.v3.params.ParamsManager
import cn.iflyos.sdk.android.v3.utils.FileUtil
import com.iflytek.cyber.iot.show.core.ivw.IvwHandler
import com.iflytek.cyber.iot.show.core.utils.RecordVolumeUtils
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

/**
 * 语音识别处理器
 *
 * 识别逻辑：
 *  1. 向 SDK 传入录音文件路径
 *  2. SDK 回调开始录音后，持续向录音文件写入录音数据
 *  3. SDK 回调停止录音后，停止写入数据
 *
 * 录音逻辑：
 *  在获取到录音权限后，会一直开启录音，写入唤醒引擎，保证唤醒引擎可以一直保持正常工作。
 */
class SpeechRecognizerHandler(private var mContext: Context, val iFLYOSListener: iFLYOSListener? = null) : SpeechRecognizer() {
    private val mExecutor = Executors.newFixedThreadPool(2) // 录音线程池
    private var mAudioInput: AudioRecord? = null
    private var mReader: AudioReader? = null
    private var preventReWakeUp = false // 是否阻止重复唤醒
    private val mIvwListener = object : IvwHandler.IvwHandlerListener {
        override fun onWakeUp(wakeup: Boolean, oriMsg: String) {
            iFLYOSListener?.let { listener ->
                // after wakeup ->
                // 1. send dialog start;
                // 2. open fifo stream;
                // 3. set isReadySendAudio to true then audio will send to native
                // 4. start audio input

                // 首先调用 wakeUpHandler，若返回 true 表示处理此次唤醒
                if (wakeUpHandler?.invoke() == true) {
                    if (wakeup) {
                        // 若阻止重复唤醒，则在开始识别之后不处理唤醒逻辑
                        if (preventReWakeUp && isStartRecording)
                            return
                        val pref = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                        val profile = pref.getString(PREF_KEY_PROFILE, PREF_VALUE_FAR_FIELD)
                                ?: PREF_VALUE_FAR_FIELD
                        // 获取唤醒引擎【近场】【远场】配置，若传参不合法，则默认远场
                        iFLYOSManager.getInstance().sendMsg(iFLYOSInterface.IFLYOS_DO_DIALOG_START, profile)
                        createFifoOutStream()
                        isReadySendAudio = true
                        innerStartAudioInput()
                    } else {
                        isReadySendAudio = false
                        innerStopAudioInput()
                    }

                    listener.onEvent(iFLYOSEvent.EVENT_SPEECH_RECOGNIZER_WAKEUP, wakeup.toString())
                }
            }
        }
    }

    private var mIvwHandler: IvwHandler = IvwHandler(mIvwListener)

    /**
     * return if handle this wake up, false would ignore
     */
    var wakeUpHandler: (() -> Boolean)? = null

    private val mFIFOAudioPath: String
    private var isDirectWriteAudio = false
    private var isStartRecording = false

    private var mFifoOutStream: FileOutputStream? = null // 录音文件输出流，用于写入录音数据
    private var mFifoAudio: FileUtil.DataFileHelper? = null
    private var isSaveFIFOAudio = false

    init {
        mAudioInput = createAudioInput()
        if (mIvwHandler.isWakeUpEnable()) startRecording()

        mFIFOAudioPath = ParamsManager.workDir + "pcm/" // 录音保存路径
        if (isSaveFIFOAudio) mFifoAudio = FileUtil.createFileHelper(mFIFOAudioPath)
    }

    override fun startAudioInput() {
        mIvwHandler.wakeUpState = true
    }

    fun getProfile(): String {
        val pref = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return pref.getString(PREF_KEY_PROFILE, PREF_VALUE_FAR_FIELD)
                ?: PREF_VALUE_FAR_FIELD
    }

    fun setProfile(profile: String) {
        val supportedProfiles = arrayOf(PREF_VALUE_FAR_FIELD, PREF_VALUE_NEAR_FIELD)
        if (profile in supportedProfiles) {
            mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)?.let { pref ->
                Log.d(TAG, "profile set to $profile")
                pref.edit()
                        .putString(PREF_KEY_PROFILE, profile)
                        .apply()
            } ?: run {
                Log.e(TAG, "Set profile failed. Cannot get preference.")
            }
        } else {
            Log.e(TAG, "Set profile failed. Unsupported profile($profile), profile should be one of ${Arrays.toString(supportedProfiles)}")
        }
    }

    private fun innerStartAudioInput(): Boolean {
        mAudioInput?.let { audioInput ->
            if (audioInput.state != AudioRecord.STATE_INITIALIZED) {
                if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) {
                    Logger.w(TAG, "Cannot start audio input. Microphone permission not granted")
                    iFLYOSListener?.onEvent(iFLYOSEvent.EVENT_CREATE_AUDIO_RECORD_FAILED, PERMISSION_DENIED)
                    return false
                } else {
                    // Retry AudioRecord initialization. Microphone permission may have been granted
                    mAudioInput = createAudioInput()
                    if (mAudioInput?.state != AudioRecord.STATE_INITIALIZED) {
                        // Cause other application is using AudioRecord now
                        Logger.w(TAG, "Cannot initialize AudioRecord")
                        iFLYOSListener?.onEvent(iFLYOSEvent.EVENT_CREATE_AUDIO_RECORD_FAILED, RECORD_UNINITIALIZED)
                        return false
                    }
                }
            }

            mFifoAudio?.let { fifoAudio ->
                val index = System.currentTimeMillis()
                fifoAudio.createFile("PCM_Audio_$index", ".pcm", false)
            }

            isStartRecording = startRecording()
            return isStartRecording
        } ?: run {
            Logger.w(TAG, "Cannot start audio input. AudioRecord could not be created")
            iFLYOSListener?.onEvent(iFLYOSEvent.EVENT_CREATE_AUDIO_RECORD_FAILED, RECORD_UNINITIALIZED)
            return false
        }
    }

    override fun getFifoFileName(): String {
        return FIFO_FILE_NAME
    }

    override fun stopAudioInput() {
        mIvwHandler.wakeUpState = false
        isReadySendAudio = false
        innerStopAudioInput()
    }

    private fun innerStopAudioInput() {
        mAudioInput?.let {
            mFifoAudio?.closeFile()
            stopRecording()
        } ?: Logger.w(TAG, "Call stopAudioInput function but AudioRecord was never initialized")
    }

    override fun startWriteAudio() {
        isDirectWriteAudio = true
        startAudioInput()
    }

    /**
     * Write Audio directly
     */
    override fun writeAudio(audio: ByteArray?, len: Int) {
        if (len != 0) {
            audio?.run {
                write(this, len, 1)
            }
        }
    }

    override fun stopWriteAudio() {
        isDirectWriteAudio = false
    }

    fun restartRecording() {
        // 触发开始录音，用于申请录音权限后无需重新创建Handler就可以开始录音
        if (!isStartRecording) {
            mAudioInput = createAudioInput()
            if (mIvwHandler.isWakeUpEnable()) startRecording()
        }
    }

    override fun startRecording(): Boolean {
        Log.d(TAG, "startRecording")
        if (mReader?.isRunning == true) {
            Log.d(TAG, "mReader isRunning")
            Logger.w(TAG, "Call startRecording function but AudioRecorder thread is already running")
            return false
        } else {
            try {
                // Start audio recording

                Log.d(TAG, "mAudioInput startRecording()")
                mAudioInput?.startRecording()
                if (mAudioInput?.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                    Log.e(TAG, "startRecording failed")
                    iFLYOSListener?.onEvent(iFLYOSEvent.EVENT_CREATE_AUDIO_RECORD_FAILED, RECORD_UNINITIALIZED)
                            ?: run {
                                Log.e(TAG, "iFLYOSListener not setup yet.")
                            }
                }
                mExecutor.submit(AudioReader())

                // Read recorded audio samples and pass to engine
                // Submit the audio reader thread
            } catch (e: IllegalStateException) {
                iFLYOSListener?.onEvent(iFLYOSEvent.EVENT_CREATE_AUDIO_RECORD_FAILED, RECORD_UNINITIALIZED)
                Logger.e(TAG, "AudioRecord cannot start recording. Error: " + e.message)
                return false
            } catch (e: RejectedExecutionException) {
                Logger.e(TAG, "Audio reader task cannot be scheduled for execution. Error: " + e.message)
                return false
            }

            return true
        }
    }

    override fun stopRecording(): Boolean {
        isStartRecording = false
        return if (mIvwHandler.isWakeUpEnable()) true else stopAudioSource()

    }

    private fun stopAudioSource(): Boolean {
        // Cancel the audio reader and stop recording
        mReader?.cancel()

        try {
            mAudioInput?.stop()
        } catch (e: IllegalStateException) {
            Logger.e(TAG, "AudioRecord cannot stop recording. Error: " + e.message)
            return false
        }

        return true
    }

    private fun closeFifoOutStream() {
        mFifoOutStream?.let { fos ->
            try {
                fos.close()
            } catch (e: IOException) {
                Logger.e(TAG, "Close fifo file happen exception, $e")
            }

            mFifoOutStream = null
        }

        isDirectWriteAudio = false
    }

    // Create Audio Input
    private fun createAudioInput(): AudioRecord? {
        var audioRecord: AudioRecord? = null
        val sampleRateInHz = 16000
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRateInHz,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        try {
            audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateInHz,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize)
            if (audioRecord.recordingState != AudioRecord.RECORDSTATE_STOPPED) {
                Log.e(TAG, "Create recorder failed.")
            }
        } catch (e: IllegalArgumentException) {
            Logger.e(TAG, "Cannot create audio input. Error: " + e.message)
        }

        return audioRecord
    }

    // Create fifo stream
    private fun createFifoOutStream() {
        if (mFifoOutStream == null) {
            try {
                mFifoOutStream = FileOutputStream(iFLYOSManager.getInstance().micAudioFilePath)
            } catch (e: FileNotFoundException) {
                Logger.e(TAG, "Open pipe failed. Error: " + e.message)
            }

        }
    }

    private inner class AudioReader : Runnable {

        internal var isRunning = true
            private set
        private val mBuffer = ByteArray(640)

        internal fun cancel() {
            isRunning = false
        }

        override fun run() {
            var size: Int

            while (isRunning) {
                mAudioInput?.let {
                    size = it.read(mBuffer, 0, mBuffer.size)
                    if (size >= 320 && isRunning) {
                        write(mBuffer, size, 0) // Write audio samples to engine

                        // 计算录音音量，并作为事件回调出去
                        val volume = RecordVolumeUtils.calculateVolume(mBuffer, size) / RecordVolumeUtils.AUDIO_METER_MAX_DB
                        iFLYOSListener?.onEvent(iFLYOSEvent.EVENT_VOLUME_CHANGE, volume.toString())
                    }
                }
            }
        }
    }

    /**
     * 设置是否保存录音文件
     * @enable true or false
     */
    fun setSaveFIFOAudioEnabled(enable: Boolean) {
        isSaveFIFOAudio = enable

        if (enable)
            mFifoAudio = FileUtil.createFileHelper(mFIFOAudioPath)
        else
            mFifoAudio = null
    }

    /**
     * 将录音数据写入目标文件中
     */
    override fun write(audio: ByteArray, len: Int, from: Int) {
        mIvwHandler.write(audio, len)

        if ((isDirectWriteAudio && from == 1) || (!isDirectWriteAudio && from == 0)) {
            // After wakeup, the data also needs to be written to FIFO file.
            if (mIvwHandler.wakeUpState && isReadySendAudio) {
                mFifoOutStream?.let { fos ->
                    try {
                        // 若 isSaveFIFOAudio 为true时，mFifoAudio 才不为空
                        mFifoAudio?.write(audio, false)
                        fos.write(audio, 0, len)
                        fos.flush()
                    } catch (e: IOException) {
                        // 若调用了停止录音，流会被关闭，这里会捕获到 IO 异常，可以直接忽略
                        // ignore
                    }
                }
            }
        }
    }

    override fun destroy() {
        super.destroy()
        stopAudioInput()
        stopAudioSource()
        mIvwHandler.release()
        closeFifoOutStream()

        mAudioInput = null
        mReader = null
    }

    companion object {
        private const val TAG = "SpeechRecognizerHandler"

        const val FIFO_FILE_NAME = "audio.pipe"

        private const val PREF_NAME = "recognize_config"
        private const val PREF_KEY_PROFILE = "asr_profile"
        const val PREF_VALUE_NEAR_FIELD = "NEAR_FIELD"
        const val PREF_VALUE_FAR_FIELD = "FAR_FIELD"

        const val PERMISSION_DENIED = "PERMISSION_DENIED"
        const val RECORD_UNINITIALIZED = "RECORD_UNINITIALIZED"
    }
}
