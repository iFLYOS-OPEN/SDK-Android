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

package com.iflytek.cyber.iot.show.core.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.util.SparseIntArray
import androidx.annotation.RawRes
import com.iflytek.cyber.iot.show.core.R

/**
 * 用于播放简短的提示音
 */
class ToneManager private constructor(context: Context) {

    private val soundPool: SoundPool = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        SoundPool.Builder()
                .setMaxStreams(3)
                .setAudioAttributes(AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build())
                .build()
    } else {
        @Suppress("DEPRECATION") // Android L 之前未被标为过时
        SoundPool(3, AudioManager.STREAM_MUSIC, 0)
    }

    private val tones = SparseIntArray()

    init {
        // 加载对应的提示音
        load(context, TONE_WAKE, R.raw.wake_up_sound)
        load(context, TONE_VOLUME, R.raw.plastic_soda_pop)
        // 若要新增自定义的提示音，可新建一个新的、值不同于 WAKE 和 VOLUME 的 TONE_CUSTOM (命名可自定义)
        // 然后像下面这样调用加载提示音，加载完后若要播放只需使用 play(TONE_CUSTOM, volume)
        // load(context, TONE_CUSTOM, R.raw.custom_tone)
    }

    private fun load(context: Context, tone: Int, @RawRes resId: Int) {
        tones.put(tone, soundPool.load(context, resId, 1))
    }

    fun play(tone: Int, volume: Float): Boolean {
        val sound = tones.get(tone, -1)
        if (sound == -1) {
            return false
        }

        return soundPool.play(sound, volume, volume, 1, 0, 1.0f) != 0
    }

    fun destroy() {
        soundPool.release()
        toneManager = null
    }

    companion object {

        const val TONE_WAKE = 1
        const val TONE_VOLUME = 2

        private var toneManager: ToneManager? = null

        operator fun get(context: Context): ToneManager {
            val current = toneManager
            return if (current == null) {
                val newToneManager = ToneManager(context)
                toneManager = newToneManager
                newToneManager
            } else
                current
        }
    }
}
