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

package com.iflytek.cyber.iot.show.core.template

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.iflytek.cyber.iot.show.core.template.utils.RoundedCornersTransformation
import com.iflytek.cyber.iot.show.core.template.widget.HighlightTextView

class BodyTemplateFragment : TemplateFragment() {

    companion object {
        private const val TAG = "BodyTemplate"
    }

    private var template: JsonObject? = null

    private var mainTitleView: TextView? = null
    private var subTitleView: TextView? = null
    private var bodyView: HighlightTextView? = null
    private var largeImageView: ImageView? = null
    private var smallImageView: ImageView? = null
    private var scrollView: NestedScrollView? = null
    private var skillIconView: ImageView? = null
    private var backIconView: ImageView? = null

    private var largeImage = false
    private var imageUrl = ""

    private var updatingPosition = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_body_template, container, false)
        backIconView = view.findViewById(R.id.back_icon)
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

        backIconView?.setOnClickListener {
            onBackPressed(this, template.toString())
        }

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

        val onTouchListener = View.OnTouchListener { _, event ->
            if (event.action == KeyEvent.ACTION_DOWN || event.action == KeyEvent.ACTION_UP) {
                if (updatingPosition) {
                    updatingPosition = false
                    bodyView?.stopAnimation()
                }

                onScrollableBodyTouched(this, template.toString())
            }
            false
        }
        view.setOnTouchListener(onTouchListener)
        scrollView?.setOnTouchListener(onTouchListener)

        val argument = arguments
        argument?.let {
            template = JsonParser().parse(argument.getString(EXTRA_TEMPLATE)).asJsonObject

            bodyView?.post {
                val height = view.height

                backIconView?.let { imageView ->
                    val padding = imageView.height * 12 / 56
                    imageView.setPadding(padding, padding, padding, padding)
                }

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
            }
        }
    }

    fun onPlayStarted() {
        updatingPosition = true
        activity?.runOnUiThread {
            scrollView?.smoothScrollTo(0, 0)
            bodyView?.let { bodyView ->
                if (!(bodyView.text.isNullOrEmpty()) && bodyView.lineCount > 0) {
                    bodyView.startAnimation()
                } else {
                    bodyView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                        override fun onGlobalLayout() {
                            if (!(bodyView.text.isNullOrEmpty()) && bodyView.lineCount > 0) {
                                bodyView.startAnimation()
                                bodyView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                            }
                        }
                    })
                }
            }
        }
    }

    fun onPlayFinished() {
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
        }
    }

    override fun getTemplatePayload(): String {
        template ?: let {
            return it.toString()
        }
        return super.getTemplatePayload()
    }

    fun updatePosition(position: Long) {
        bodyView?.updatePosition(position)
    }

    private fun showTemplate(template: JsonObject) {
        (template.get("title") as? JsonObject)?.let { title ->
            (title.get("mainTitle") as? JsonPrimitive)?.let {
                mainTitleView?.text = it.asString
            }
            (title.get("subTitle") as? JsonPrimitive)?.let {
                val text = it.asString.trim()
                Log.d("BodyTemplate", "subTitle: $text")
                subTitleView?.text = text
                subTitleView?.visibility = if (text.isEmpty()) View.GONE else View.VISIBLE
            } ?: run {
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