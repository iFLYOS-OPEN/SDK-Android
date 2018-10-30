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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import cn.iflyos.iace.iflyos.Alerts
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.iflytek.cyber.iot.show.core.LauncherActivity
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.utils.RoundedCornersTransformation

class BodyTemplate3Fragment : BaseFragment() {
    private var titleView: TextView? = null
    private var textFieldView: TextView? = null
    private var subTextFieldView: TextView? = null
    private var skillIconView: ImageView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_body_template_3, null)
        titleView = view.findViewById(R.id.title)
        textFieldView = view.findViewById(R.id.text_field)
        subTextFieldView = view.findViewById(R.id.sub_text_field)
        skillIconView = view.findViewById(R.id.skill_icon)
        view.findViewById<ImageView>(R.id.back_icon).setOnClickListener {
            launcher?.mEngineService?.let { engineService ->
                val state = engineService.currentAlertState()
                if (state != Alerts.AlertState.STOPPED) {
                    engineService.stopAlert()
                }

                engineService.stopSpeaking()
            }
            findNavController().navigateUp()
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val arguments = arguments
        val template = arguments?.getString(LauncherActivity.EXTRA_TEMPLATE)
        if (!template.isNullOrEmpty()) {
            try {
                showTemplate(JsonParser().parse(template).asJsonObject)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showTemplate(templateJson: JsonObject) {
        val titleJson = templateJson.get("title")
        if (titleJson?.isJsonPrimitive == true)
            titleView?.text = titleJson.asString
        val textFieldJson = templateJson.get("textField")
        if (textFieldJson?.isJsonPrimitive == true)
            textFieldView?.text = textFieldJson.asString
        val subtextFieldJson = templateJson.get("subtextField")
        if (subtextFieldJson?.isJsonPrimitive == true)
            subTextFieldView?.text = templateJson.get("subtextField").asString
        val skillIcon = templateJson.get("skillIcon")
        if (skillIcon?.isJsonObject == true) {
            if (skillIcon !is JsonObject) return
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