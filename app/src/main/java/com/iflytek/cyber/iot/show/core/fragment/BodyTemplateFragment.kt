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

package com.iflytek.cyber.iot.show.core.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.app.Fragment
import android.support.v4.widget.NestedScrollView
import android.util.Log
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import cn.iflyos.iace.iflyos.MediaPlayer
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.iflytek.cyber.iot.show.core.LauncherActivity
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.impl.MediaPlayer.MediaPlayerHandler
import com.iflytek.cyber.iot.show.core.impl.SpeechSynthesizer.SpeechSynthesizerHandler
import com.iflytek.cyber.iot.show.core.utils.RoundedCornersTransformation
import com.iflytek.cyber.iot.show.core.widget.HighlightTextView

class BodyTemplateFragment : Fragment(), MediaPlayerHandler.OnMediaStateChangedListener {

    companion object {
        const val CLOSE_OFFSET = 5000L
    }

    private var template: JsonObject? = null

    private var mainTitleView: TextView? = null
    private var subTitleView: TextView? = null
    private var bodyView: HighlightTextView? = null
    private var largeImageView: ImageView? = null
    private var smallImageView: ImageView? = null
    private var scrollView: NestedScrollView? = null
    private var skillIconView: ImageView? = null

    private var largeImage = false
    private var imageUrl = ""

    private var updatingPosition = false
    private var lastTouchTime = 0L
    private var mSpeechSynthesizerHandler: SpeechSynthesizerHandler? = null
    private var mMediaPlayer: MediaPlayerHandler? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_body_template, container, false)
        view.findViewById<View>(R.id.back_icon).setOnClickListener {
            if (updatingPosition) {
                mSpeechSynthesizerHandler?.mediaPlayer?.stop()
            }
            findNavController().navigateUp()
        }
        mainTitleView = view?.findViewById(R.id.main_title)
        subTitleView = view?.findViewById(R.id.sub_title)
        bodyView = view?.findViewById(R.id.body)
        largeImageView = view?.findViewById(R.id.large_image)
        smallImageView = view?.findViewById(R.id.small_image)
        scrollView = view?.findViewById(R.id.body_scroll_view)
        skillIconView = view?.findViewById(R.id.skill_icon)
        return view
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bodyView?.onHighlightChangeListener = object : HighlightTextView.OnHighlightChangeListener {
            override fun onHighlightChange(view: HighlightTextView, line: Int, offset: Int) {
                if (largeImage) {
                    scrollView?.smoothScrollTo(0, offset)
                } else {
                    if (offset > (scrollView?.height ?: 0) / 2) {
                        if (scrollView?.scrollY != view.height) {
                            scrollView?.smoothScrollTo(0, offset - (scrollView?.height ?: 0) / 2)
                        }
                    }
                }
            }
        }
        scrollView?.setOnTouchListener { _, event ->
            if (event.action == KeyEvent.ACTION_DOWN || event.action == KeyEvent.ACTION_DOWN) {
                updatingPosition = false
                bodyView?.stopAnimation()
                lastTouchTime = System.currentTimeMillis()
                scrollView?.postDelayed(endRunnable, CLOSE_OFFSET)
                val mediaPlayer = mSpeechSynthesizerHandler?.mediaPlayer
                mediaPlayer?.stop()
            }
            false
        }
        val argument = arguments
        argument?.let {
            template = JsonParser().parse(argument.getString(LauncherActivity.EXTRA_TEMPLATE)).asJsonObject

            bodyView?.post {
                val height = view.height

                mainTitleView?.setTextSize(TypedValue.COMPLEX_UNIT_PX, height * 0.037f)
                subTitleView?.setTextSize(TypedValue.COMPLEX_UNIT_PX, height * 0.027f)
                bodyView?.setTextSize(TypedValue.COMPLEX_UNIT_PX, height * 0.067f)
                bodyView?.lineHeight = (height * 0.103f).toInt()
                val layoutParams = bodyView?.layoutParams as LinearLayout.LayoutParams
                layoutParams.topMargin = (height * 0.04f).toInt()
                bodyView?.layoutParams = layoutParams

                template?.let { template ->
                    showTemplate(template)
                }

                bodyView?.post {
                    val mediaPlayer = mSpeechSynthesizerHandler?.mediaPlayer
                    if (mediaPlayer is MediaPlayerHandler) {
                        mMediaPlayer = mediaPlayer
                        mMediaPlayer?.addOnMediaStateChangedListener(this)
                        if (mMediaPlayer?.isPlaying == true) {
                            onPlayStarted()
                        }
                    }
                }
            }
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        val activity = activity
        if (activity is LauncherActivity) {
            val handler = activity.getHandler("SpeechSynthesizer")
            if (handler is SpeechSynthesizerHandler)
                mSpeechSynthesizerHandler = handler
        }
    }

    override fun onDetach() {
        super.onDetach()

        val activity = activity
        if (activity is LauncherActivity) {
            mMediaPlayer?.removeOnMediaStateChangedListener(this)
            mMediaPlayer = null
            mSpeechSynthesizerHandler = null
        }
    }

    private fun onPlayStarted() {
        updatingPosition = true
        activity?.runOnUiThread {
            scrollView?.smoothScrollTo(0, 0)
            bodyView?.startAnimation()
            positionUpdateRunnable.run()
        }
    }

    private fun onPlayFinished() {
        if (updatingPosition) {
            updatingPosition = false
            bodyView?.post {
                bodyView?.stopAnimation()
                if (largeImage) {
                    scrollView?.smoothScrollTo(0, largeImageView?.height
                            ?: 0)
                } else {
                    scrollView?.smoothScrollTo(0, 0)
                }
            }
            bodyView?.postDelayed(endRunnable, CLOSE_OFFSET)
        }
    }

    override fun onMediaStateChanged(state: MediaPlayer.MediaState) {
        when (state) {
            MediaPlayer.MediaState.PLAYING -> {
                if (isVisible)
                    onPlayStarted()
            }
            MediaPlayer.MediaState.STOPPED -> {
                if (isVisible)
                    onPlayFinished()
            }
            else -> {
            }
        }
    }

    private val endRunnable = Runnable {
        if (isDetached)
            return@Runnable
        if (lastTouchTime + CLOSE_OFFSET <= System.currentTimeMillis()) {
            if (fragmentManager != null)
                findNavController().navigateUp()
        }
    }

    private val positionUpdateRunnable = object : Runnable {
        override fun run() {
            mMediaPlayer?.let { player ->
                val position = player.position

                if (updatingPosition) {
                    bodyView?.updatePosition(position)
                    bodyView?.postDelayed(this, 100)
                }
            }
        }
    }

    private fun showTemplate(template: JsonObject) {
        if (template.has("title")) {
            val title = template.get("title").asJsonObject
            if (title.has("mainTitle"))
                mainTitleView?.text = title?.get("mainTitle")?.asString
            if (title.has("subTitle") && !title?.get("subTitle")?.asString.isNullOrEmpty())
                subTitleView?.text = title?.get("subTitle")?.asString
            else {
                subTitleView?.visibility = View.GONE
            }
        }
        if (template.has("image")) {
            val imageStructure = template.get("image").asJsonObject
            val imageArray = imageStructure.getAsJsonArray("sources")
            if (imageArray.size() > 0) {
                val image = imageArray[0].asJsonObject
                val size = image.get("size").asString
                when (size) {
                    "X-LARGE", "LARGE" -> {
                        largeImage = true
                    }
                    "MEDIUM", "SMALL", "X-SMALL" -> {
                        largeImage = false
                    }
                }
                imageUrl = image.get("url").asString
            }
        }

        if (template.has("textField")) {
            val text = template.get("textField")?.asString
            if (text.isNullOrEmpty()) {
                val layoutParams = largeImageView?.layoutParams
                layoutParams?.height = scrollView?.height ?: 0
                largeImageView?.layoutParams = layoutParams
            } else {
                val layoutParams = largeImageView?.layoutParams
                layoutParams?.height = (scrollView?.height ?: 0) -
                        (bodyView?.lineHeight
                                ?: 0) - (bodyView?.layoutParams as LinearLayout.LayoutParams).topMargin
                largeImageView?.layoutParams = layoutParams
            }
            if (text.isNullOrEmpty()) {
                bodyView?.visibility = View.GONE
            } else {
                bodyView?.visibility = View.VISIBLE
                bodyView?.text = text
            }
        } else {
            bodyView?.visibility = View.GONE
        }

        if (imageUrl.isEmpty()) {
            smallImageView?.visibility = View.GONE
            largeImageView?.visibility = View.GONE
        } else {
            if (largeImage) {
                smallImageView?.visibility = View.GONE
                largeImageView?.visibility = View.VISIBLE
                largeImageView?.let {
                    Glide.with(it)
                            .load(imageUrl)
                            .into(it)
                }
            } else {
                smallImageView?.visibility = View.VISIBLE
                largeImageView?.visibility = View.GONE
                smallImageView?.let {
                    Glide.with(it)
                            .load(imageUrl)
                            .into(it)
                }
            }
        }

        val skillIcon = template.get("skillIcon")
        if (skillIcon is JsonObject) {
            val imageArray = skillIcon.get("resources")
            if (imageArray is JsonArray && imageArray.size() > 0) {
                val image = imageArray[0]
                val url = image.asJsonObject.get("url").asString
                skillIconView?.let {
                    Glide.with(it)
                            .load(url)
                            .apply(RequestOptions()
                                    .transform(RoundedCornersTransformation(it.height / 4, 0)))
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .into(it)
                }
            }
        }
    }
}