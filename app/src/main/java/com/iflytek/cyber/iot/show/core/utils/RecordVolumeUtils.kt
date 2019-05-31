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

/**
 * 用于计算录音音量
 */
object RecordVolumeUtils {

    const val AUDIO_METER_MAX_DB = 20f
    @Suppress("MemberVisibilityCanBePrivate")
    const val AUDIO_METER_MIN_DB = 0f
    private var noiseLevel = 75f

    fun calculateVolume(data: ByteArray, length: Int): Float {

        val rms = calculateRms(data, length)

        if (noiseLevel < rms) {
            noiseLevel = 0.999f * noiseLevel + 0.001f * rms
        } else {
            noiseLevel = 0.95f * noiseLevel + 0.05f * rms
        }

        var db = -120f
        if (noiseLevel.toDouble() > 0.0 && (rms / noiseLevel).toDouble() > 0.000001) {
            db = 10f * Math.log10((rms / noiseLevel).toDouble()).toFloat()
        }

        return Math.min(Math.max(db, AUDIO_METER_MIN_DB), AUDIO_METER_MAX_DB)
    }

    private fun calculateRms(data: ByteArray, length: Int): Float {
        var l1: Long = 0
        var l2: Long = 0
        val k = length / 2

        var i = length
        while (i >= 2) {
            val j1 = (data[i - 1].toInt().shl(8)) + (0xff and data[i - 2].toInt())
            l1 += j1.toLong()
            l2 += (j1 * j1).toLong()
            i -= 2
        }

        return Math.sqrt(((l2 * k.toLong() - l1 * l1) / (k * k).toLong()).toDouble()).toFloat()
    }
}
