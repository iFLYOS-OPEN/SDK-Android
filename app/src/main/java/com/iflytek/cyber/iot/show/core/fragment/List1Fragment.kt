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

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.iflytek.cyber.iot.show.core.LauncherActivity
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.impl.Logger.LogEntry
import com.iflytek.cyber.iot.show.core.impl.Logger.LoggerHandler
import com.iflytek.cyber.iot.show.core.model.Constant
import com.iflytek.cyber.iot.show.core.model.Image
import com.iflytek.cyber.iot.show.core.model.ListItem
import com.iflytek.cyber.iot.show.core.utils.InsetDividerDecoration
import com.iflytek.cyber.iot.show.core.utils.RoundedCornersTransformation
import java.util.*

class List1Fragment : BaseFragment(), Observer {

    private var payload: JsonObject? = null
    private var alreadyNavigateUp = false
    private var dialogIdle = false

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (arguments == null) {
            return
        }

        val json = arguments?.getString(LauncherActivity.EXTRA_TEMPLATE)
        payload = JsonParser().parse(json).asJsonObject
        launcher?.addObserver(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<ImageView>(R.id.iv_back).setOnClickListener {
            alreadyNavigateUp = true
            if (!dialogIdle) {
                exit()
            }
        }
        val skillIcon = view.findViewById<ImageView>(R.id.iv_skill_icon)
        try {
            val jsonObject = payload?.get("skillIcon")
            if (jsonObject is JsonObject && context != null) {
                val img = Gson().fromJson(jsonObject, Image::class.java)
                if (img.sources != null && img.sources.size > 0) {
                    setCover(img, skillIcon)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val subTitle = view.findViewById<TextView>(R.id.tv_subtitle)
        val mainTitle = view.findViewById<TextView>(R.id.tv_mainTitle)

        val title = payload?.getAsJsonObject(Constant.PAYLOAD_TITLE)
        subTitle.text = title?.get(Constant.PAYLOAD_SUB_TITLE)?.asString
        mainTitle.text = title?.get(Constant.PAYLOAD_MAIN_TITLE)?.asString

        val listItems = ArrayList<ListItem>()
        val array = payload?.getAsJsonArray("listItems")
        if (array != null) {
            for (element in array) {
                val left = element.asJsonObject.get("leftTextField").asString
                val right = element.asJsonObject.get("rightTextField").asString
                val item = ListItem(left, right)
                listItems.add(item)
            }
        }

        val list = view.findViewById<RecyclerView>(R.id.list)
        list.layoutManager = object : LinearLayoutManager(context) {
            override fun checkLayoutParams(lp: RecyclerView.LayoutParams?): Boolean {
                lp?.height = (height * 0.25f).toInt()
                return true
            }
        }
        list.addItemDecoration(InsetDividerDecoration(List1Adapter.ListHolder::class.java,
                resources.getDimensionPixelOffset(R.dimen.line_height),
                0,
                Color.parseColor("#1effffff")))
        val adapter = List1Adapter(listItems)
        list.adapter = adapter
    }

    private fun setCover(image: Image, icon: ImageView) {
        icon.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                Glide.with(context!!)
                        .load(image.sources[0].url)
                        .apply(RequestOptions
                                .placeholderOf(R.drawable.cover_default)
                                .transform(RoundedCornersTransformation(
                                        icon.height / 12,
                                        0)))
                        .into(icon)
                icon.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        launcher?.deleteObserver(this)
    }

    private fun exit() {
        try {
            findNavController().navigateUp()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun update(o: Observable?, arg: Any?) {
        if (o !is LoggerHandler.LoggerObservable) {
            return
        }

        if (arg !is LogEntry) {
            return
        }

        if (arg.type == LoggerHandler.DIALOG_STATE) {
            try {
                val template = arg.json.getJSONObject("template")
                val state = template.getString("state")
                when (state) {
                    "IDLE" -> {
                        dialogIdle = true
                        if (!alreadyNavigateUp) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                exit()
                            }, 500)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}