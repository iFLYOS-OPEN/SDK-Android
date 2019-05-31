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

package com.iflytek.cyber.iot.show.core.overlay.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.support.v7.widget.AppCompatTextView
import android.util.AttributeSet
import android.util.Log


class HighlightTextView
@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : AppCompatTextView(context, attrs, defStyleAttr) {
    private val coverPaint: Paint = Paint()

    private var animationCoverTop = 0f

    private val timestampArray = ArrayList<Long>()

    var onHighlightChangeListener: OnHighlightChangeListener? = null

    private var animationStopped = false

    companion object {
        private const val sTag = "HighlightTextView"
        private const val ALPHA_COVER = 84
    }

    init {
        coverPaint.color = Color.BLACK
        coverPaint.alpha = ALPHA_COVER

    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (!animationStopped) {
            val height = measuredHeight
            val width = measuredWidth

            // 高亮上部遮罩
            if (animationCoverTop > 0)
                canvas?.drawRect(0f, 0f, width.toFloat(), animationCoverTop, coverPaint)

            // 高亮下部遮罩
            canvas?.drawRect(0f, animationCoverTop + lineHeight, width.toFloat(), (lineHeight * lineCount).toFloat(), coverPaint)
        }
    }

    fun startAnimation() {
        timestampArray.clear()
        var durationCount = 0L
        timestampArray.add(0) // 第一行一定从零开始
        for (i in 0 until lineCount) {
            val lineStart = layout.getLineStart(i)
            val lineEnd = layout.getLineEnd(i)
            if (lineStart < text.length && lineEnd <= text.length) {
                val lineText = text.substring(lineStart, lineEnd)
                durationCount += textToDuration(lineText)
                timestampArray.add(durationCount)
            } else {
                timestampArray.add(0)
            }
        }
        Log.d(sTag, "array: ${timestampArray.toArray()?.contentToString()}")
    }

    fun stopAnimation() {
        animationCoverTop = 0f
        animationStopped = true
        invalidate()
    }

    fun updatePosition(position: Long) {
        for (index in 0 until timestampArray.size) {
            val value = timestampArray[index]
            if (position < value) {
                val target = Math.max(0, index - 1) // 未达到下一行的时间点
                if (lineHeight * target != animationCoverTop.toInt()) {
                    animationCoverTop = (lineHeight * target).toFloat()
                    invalidate()
                    onHighlightChangeListener?.onHighlightChange(this, target, animationCoverTop.toInt())
                }
                return
            }
        }
    }

    private fun textToDuration(text: String): Long {
        val pureText = text.replace(Regex("[^\u4E00-\u9FA5]"), "")
        val symbol = Regex("[\uff0c|\u3001|\uff1f|\uff01|\u3002|\uff1a|\uff1b|\u007e|\u2026]")
        val sum = symbol.findAll(text).count()
        return pureText.length * 230L + sum * 50
    }

    interface OnHighlightChangeListener {
        fun onHighlightChange(view: HighlightTextView, line: Int, offset: Int)
    }
}