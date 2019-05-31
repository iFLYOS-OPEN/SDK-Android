/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iflytek.cyber.iot.show.core.template.utils

import android.graphics.Canvas
import android.graphics.Paint
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.RecyclerView

/**
 * A decoration which draws a horizontal divider between [RecyclerView.ViewHolder]s of a given
 * type; with a left inset.
 */
class InsetDividerDecoration(private val dividedClass: Class<*>,
                             private val height: Int,
                             private val inset: Int,
                             @ColorInt dividerColor: Int) : RecyclerView.ItemDecoration() {
    private val paint: Paint = Paint()

    init {
        paint.color = dividerColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = height.toFloat()
    }

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val childCount = parent.childCount
        val lm = parent.layoutManager
        val lines = FloatArray(childCount * 4)
        var hasDividers = false

        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            val viewHolder = parent.getChildViewHolder(child)

            if (viewHolder.javaClass == dividedClass) {
                lines[i * 4] = (inset + lm!!.getDecoratedLeft(child)).toFloat()
                lines[i * 4 + 2] = lm.getDecoratedRight(child).toFloat()
                val y = lm.getDecoratedBottom(child) + child.translationY.toInt() - height
                lines[i * 4 + 1] = y.toFloat()
                lines[i * 4 + 3] = y.toFloat()
                hasDividers = true
            }
        }
        if (hasDividers) {
            canvas.drawLines(lines, paint)
        }
    }
}
