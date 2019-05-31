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

package com.iflytek.cyber.iot.show.core.widget

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import com.iflytek.cyber.iot.show.core.R

class RecognizeWaveView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {
    private val wave1: ImageView = ImageView(context)
    private val wave2: ImageView = ImageView(context)
    private val wave3: ImageView = ImageView(context)
    private val wave4: ImageView = ImageView(context)

    private val fastOutSlowIn = FastOutSlowInInterpolator()
    private val linearOutSlowIn = LinearOutSlowInInterpolator()

    private var shouldAnimVolume = false
    private var volumeUpdateHandler: Handler = Handler()
    private var targetVolume = 0.0
    private var oldVolume = 0.0

    companion object {
        private const val sTag = "RecognizeWaveView"

        private const val MIN_SCALE_Y = 0.25f
        private const val MIN_SCALE_X = 0.6f

        private const val ENTER_DURATION = 333L // ms
        private const val ENTER_OFFSET = 83L // ms

        private const val ENTER_MAX_SCALE_X = 1f
        private const val ENTER_MAX_SCALE_Y = 0.3f

        private const val VOLUME_CHANGED_MAX_SCALE_Y = 1f
        private const val VOLUME_CHANGED_MAX_SCALE_X = 1f

        private const val VOLUME_CHANGED_FULL_DURATION = 180L
        private const val VOLUME_CHANGED_MIN_DURATION = 135L
        private const val VOLUME_CHANGED_OFFSET = 83L
        private const val VOLUME_CHANGED_MAGIC_NUMBER = 0.3
    }

    private val updateVolumeRunnable = object : Runnable {
        override fun run() {
            if (shouldAnimVolume) {
                val offset = targetVolume - oldVolume
                val value =
                        when {
                            offset > VOLUME_CHANGED_MAGIC_NUMBER ->
                                Math.min(oldVolume + VOLUME_CHANGED_MAGIC_NUMBER, 1.0)
                            offset < -VOLUME_CHANGED_MAGIC_NUMBER ->
                                Math.max(oldVolume - VOLUME_CHANGED_MAGIC_NUMBER, 0.0)
                            else -> targetVolume
                        }
                oldVolume = targetVolume

                shouldAnimVolume = false
                updateWaveVolume(wave1, value, offset, Runnable {
                    shouldAnimVolume = true
                })
                postDelayed({ updateWaveVolume(wave3, value, offset) }, VOLUME_CHANGED_OFFSET)
                postDelayed({ updateWaveVolume(wave2, value, offset) }, VOLUME_CHANGED_OFFSET * 2)
                postDelayed({ updateWaveVolume(wave4, value, offset) }, VOLUME_CHANGED_OFFSET * 3)
            }
            volumeUpdateHandler.postDelayed(this, 1000L / 60) // 60fps
        }
    }

    init {
        wave1.setImageResource(R.drawable.vo_light_a)
        wave2.setImageResource(R.drawable.vo_light_b)
        wave3.setImageResource(R.drawable.vo_light_c)
        wave4.setImageResource(R.drawable.vo_light_d)

        post {
            wave1.scaleX = 0f
            wave1.scaleY = 0f
            val layoutParams1 = wave1.layoutParams as LayoutParams
            layoutParams1.width = width / 2
            layoutParams1.height = (layoutParams1.width * 0.547f).toInt()
            layoutParams1.gravity = Gravity.BOTTOM
            wave1.layoutParams = layoutParams1

            wave2.scaleX = 0f
            wave2.scaleY = 0f
            val layoutParams2 = wave2.layoutParams as LayoutParams
            layoutParams2.width = width / 2
            layoutParams2.height = (layoutParams1.width * 0.547f).toInt()
            layoutParams2.leftMargin = width / 12
            layoutParams2.gravity = Gravity.BOTTOM
            wave2.layoutParams = layoutParams2

            wave3.scaleX = 0f
            wave3.scaleY = 0f
            val layoutParams3 = wave3.layoutParams as LayoutParams
            layoutParams3.width = width / 2
            layoutParams3.height = (layoutParams1.width * 0.547f).toInt()
            layoutParams3.rightMargin = width / 12
            layoutParams3.gravity = Gravity.END or Gravity.BOTTOM
            wave3.layoutParams = layoutParams3

            wave4.scaleX = 0f
            wave4.scaleY = 0f
            val layoutParams4 = wave4.layoutParams as LayoutParams
            layoutParams4.width = width / 2
            layoutParams4.height = (layoutParams1.width * 0.547f).toInt()
            layoutParams4.gravity = Gravity.END or Gravity.BOTTOM
            wave4.layoutParams = layoutParams4
        }

        wave1.alpha = 0f
        wave2.alpha = 0f
        wave3.alpha = 0f
        wave4.alpha = 0f

        addView(wave1, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(wave2, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(wave3, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(wave4, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        volumeUpdateHandler.post(updateVolumeRunnable)
    }

    private fun initPivot() {
        wave1.pivotX = 0f
        wave1.pivotY = wave1.height.toFloat()

        wave2.pivotX = (wave2.width / 2).toFloat()
        wave2.pivotY = wave2.height.toFloat()

        wave3.pivotX = (wave3.width / 2).toFloat()
        wave3.pivotY = wave3.height.toFloat()

        wave4.pivotX = wave4.width.toFloat()
        wave4.pivotY = wave4.height.toFloat()
    }

    fun startEnterAnimation() {
        shouldAnimVolume = false

        initPivot()

        val animation = ValueAnimator.ofFloat(0f, (ENTER_DURATION + ENTER_OFFSET * 3).toFloat())
        animation.duration = ENTER_DURATION + ENTER_OFFSET * 3
        animation.addUpdateListener(enterAnimUpdate)
        animation.addListener(object : Animator.AnimatorListener {
            override fun onAnimationEnd(animation: Animator?) {
                shouldAnimVolume = true
            }

            override fun onAnimationRepeat(animation: Animator?) {
            }

            override fun onAnimationCancel(animation: Animator?) {
                shouldAnimVolume = true
            }

            override fun onAnimationStart(animation: Animator?) {
            }

        })
        animation.start()
    }

    fun updateVolume(volume: Double) {
        targetVolume = Math.min(1.0, volume / 0.6)
    }

    private fun updateWaveVolume(waveView: View, value: Double, offset: Double) {
        updateWaveVolume(waveView, value, offset, null)
    }

    private fun updateWaveVolume(waveView: View, value: Double, offset: Double, endRunnable: Runnable?) {
        val animator = waveView.animate()
                .scaleX((MIN_SCALE_X + value * (VOLUME_CHANGED_MAX_SCALE_X - MIN_SCALE_X)).toFloat())
                .scaleY((MIN_SCALE_Y + value * (VOLUME_CHANGED_MAX_SCALE_Y - MIN_SCALE_Y)).toFloat())
                .setDuration(Math.max((VOLUME_CHANGED_FULL_DURATION *
                        Math.abs(offset / VOLUME_CHANGED_MAGIC_NUMBER)).toLong(), VOLUME_CHANGED_MIN_DURATION))
        endRunnable?.let {
            animator.withEndAction(it)
        }
        animator.start()
    }

    // enter with 582ms, per light pay 333ms, and 83ms delay
    // order by: 1 -> 3 -> 2 -> 4
    private val enterAnimUpdate = ValueAnimator.AnimatorUpdateListener {
        val value = it.animatedValue as Float

        val wave1Value = Math.min(value, ENTER_DURATION.toFloat())
        waveEnterAnimate(wave1, wave1Value)

        val wave3Value = Math.max(0f, Math.min(value - ENTER_OFFSET, ENTER_DURATION.toFloat()))
        waveEnterAnimate(wave3, wave3Value)

        val wave2Value = Math.max(0f, Math.min(value - ENTER_OFFSET * 2, ENTER_DURATION.toFloat()))
        waveEnterAnimate(wave2, wave2Value)

        val wave4Value = Math.max(0f, Math.min(value - ENTER_OFFSET * 3, ENTER_DURATION.toFloat()))
        waveEnterAnimate(wave4, wave4Value)
    }

    private fun waveEnterAnimate(waveView: View, value: Float) {
        if (value < ENTER_DURATION / 2) {
            val percent = value / ENTER_DURATION * 2

            val realPercent = linearOutSlowIn.getInterpolation(percent)

            waveView.scaleX = realPercent * ENTER_MAX_SCALE_X
            waveView.scaleY = realPercent * ENTER_MAX_SCALE_Y
            waveView.alpha = realPercent
        } else {
            val percent = (value - ENTER_DURATION / 2) / ENTER_DURATION * 2

            val realPercent = 1 - fastOutSlowIn.getInterpolation(percent)

            waveView.scaleX = realPercent * (ENTER_MAX_SCALE_X - MIN_SCALE_X) + MIN_SCALE_X
            waveView.scaleY = realPercent * (ENTER_MAX_SCALE_Y - MIN_SCALE_Y) + MIN_SCALE_Y
        }
    }

    fun startQuitAnimation() {
        oldVolume = 0.0
        shouldAnimVolume = false
        wave1.animate().cancel()
        wave2.animate().cancel()
        wave3.animate().cancel()
        wave4.animate().cancel()
        wave1.animate().alpha(0f).scaleY(0f).scaleX(0f).setDuration(200).start()
        wave2.animate().alpha(0f).scaleY(0f).scaleX(0f).setDuration(200).start()
        wave3.animate().alpha(0f).scaleY(0f).scaleX(0f).setDuration(200).start()
        wave4.animate().alpha(0f).scaleY(0f).scaleX(0f).setDuration(200).start()
    }

}
