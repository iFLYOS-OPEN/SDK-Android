package com.iflytek.cyber.iot.show.core.template.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.RecyclerView


class CompatDividerItemDecoration(context: Context, orientation: Int) : RecyclerView.ItemDecoration() {
    private var mDivider: Drawable? = null
    private var mOrientation: Int = 0
    private val mBounds = Rect()

    init {
        val a = context.obtainStyledAttributes(ATTRS)
        this.mDivider = a.getDrawable(0)
        if (this.mDivider == null) {
            Log.w("DividerItem", "@android:attr/listDivider was not set in the theme used for this DividerItemDecoration. Please set that attribute all call setDrawable()")
        }

        a.recycle()
        this.setOrientation(orientation)
    }

    fun setOrientation(orientation: Int) {
        if (orientation != 0 && orientation != 1) {
            throw IllegalArgumentException("Invalid orientation. It should be either HORIZONTAL or VERTICAL")
        } else {
            this.mOrientation = orientation
        }
    }

    fun setDrawable(drawable: Drawable?) {
        if (drawable == null) {
            throw IllegalArgumentException("Drawable cannot be null.")
        } else {
            this.mDivider = drawable
        }
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (parent.layoutManager != null && this.mDivider != null) {
            if (this.mOrientation == 1) {
                this.drawVertical(c, parent)
            } else {
                this.drawHorizontal(c, parent)
            }

        }
    }

    private fun drawVertical(canvas: Canvas, parent: RecyclerView) {
        canvas.save()
        val left: Int
        val right: Int
        if (parent.clipToPadding) {
            left = parent.paddingLeft
            right = parent.width - parent.paddingRight
            canvas.clipRect(left, parent.paddingTop, right, parent.height - parent.paddingBottom)
        } else {
            left = 0
            right = parent.width
        }

        val childCount = parent.childCount

        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            parent.getDecoratedBoundsWithMargins(child, this.mBounds)
            val bottom = this.mBounds.bottom + Math.round(child.translationY)
            val top = bottom - getDividerHeight(mDivider)
            this.mDivider!!.setBounds(left, top, right, bottom)
            this.mDivider!!.draw(canvas)
        }

        canvas.restore()
    }

    private fun getDividerHeight(drawable: Drawable?): Int {
        return if (drawable == null) {
            0
        } else {
            if (drawable.intrinsicHeight > 0)
                drawable.intrinsicHeight
            else
                drawable.bounds.bottom - drawable.bounds.top
        }
    }

    private fun getDividerWidth(drawable: Drawable?): Int {
        return if (drawable == null) {
            0
        } else {
            if (drawable.intrinsicWidth > 0)
                drawable.intrinsicWidth
            else
                drawable.bounds.right - drawable.bounds.left
        }
    }

    private fun drawHorizontal(canvas: Canvas, parent: RecyclerView) {
        canvas.save()
        val top: Int
        val bottom: Int
        if (parent.clipToPadding) {
            top = parent.paddingTop
            bottom = parent.height - parent.paddingBottom
            canvas.clipRect(parent.paddingLeft, top, parent.width - parent.paddingRight, bottom)
        } else {
            top = 0
            bottom = parent.height
        }

        val childCount = parent.childCount

        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            parent.layoutManager!!.getDecoratedBoundsWithMargins(child, this.mBounds)
            val right = this.mBounds.right + Math.round(child.translationX)
            val left = right - getDividerWidth(mDivider)
            this.mDivider!!.setBounds(left, top, right, bottom)
            this.mDivider!!.draw(canvas)
        }

        canvas.restore()
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        if (this.mDivider == null) {
            outRect.set(0, 0, 0, 0)
        } else {
            if (this.mOrientation == VERTICAL) {
                val dividerHeight = getDividerHeight(mDivider)
                outRect.set(0, 0, 0, dividerHeight)
            } else {
                val dividerWidth = getDividerWidth(mDivider)
                outRect.set(0, 0, dividerWidth, 0)
            }

        }
    }

    companion object {
        val HORIZONTAL = 0
        val VERTICAL = 1
        private val TAG = "DividerItem"
        private val ATTRS = intArrayOf(android.R.attr.listDivider)
    }
}