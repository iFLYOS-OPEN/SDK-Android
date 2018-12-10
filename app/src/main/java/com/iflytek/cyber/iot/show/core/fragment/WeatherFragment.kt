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
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.iflytek.cyber.iot.show.core.LauncherActivity
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.impl.SpeechSynthesizer.SpeechSynthesizerHandler
import com.iflytek.cyber.iot.show.core.model.Constant
import com.iflytek.cyber.iot.show.core.model.Image
import com.iflytek.cyber.iot.show.core.utils.RoundedCornersTransformation

class WeatherFragment : BaseFragment() {

    private var futureDaysWeatherContainer: LinearLayout? = null
    private var currentTemperature: TextView? = null
    private var todayTemperatureRange: TextView? = null
    private var weatherTitle: TextView? = null
    private var todayWeatherIcon: ImageView? = null
    private var ivSkillIcon: ImageView? = null
    private var highContent: View? = null
    private var lowContent: View? = null
    private var dayHighTemperature: TextView? = null
    private var dayLowTemperature: TextView? = null
    private var payload: JsonObject? = null

    private var ivWeatherIcon: ImageView? = null
    private var tvWeatherQuality: TextView? = null
    private var tvDescription: TextView? = null
    private var tvTitle: TextView? = null

    private var tvFutureTitle: TextView? = null
    private var futureContent: LinearLayout? = null

    private var isFutureWeatherType: Boolean = false

    private var speechSynthesizer: SpeechSynthesizerHandler? = null

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (arguments == null) {
            return
        }

        val json = arguments!!.getString(LauncherActivity.EXTRA_TEMPLATE)
        val element = JsonParser().parse(json!!)
        payload = element.asJsonObject

        speechSynthesizer = launcher?.mEngineService?.getHandler("SpeechSynthesizer") as SpeechSynthesizerHandler
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val weatherForecastArray = payload!!.getAsJsonArray(Constant.PAYLOAD_WEATHER_FORECAST)
        val view: View
        if (weatherForecastArray != null && weatherForecastArray.size() > 0) {
            isFutureWeatherType = !(payload?.get("lowTemperature")?.asString.isNullOrEmpty()) && !(payload?.get("highTemperature")?.asString.isNullOrEmpty())
            if (isFutureWeatherType) {
                view = inflater.inflate(R.layout.fragment_weather, container, false)
                futureDaysWeatherContainer = view.findViewById(R.id.future_days_weather)
                currentTemperature = view.findViewById(R.id.current_temperature)
                highContent = view.findViewById(R.id.high_content)
                lowContent = view.findViewById(R.id.low_content)
                dayHighTemperature = view.findViewById(R.id.tv_day_high_temperature)
                dayLowTemperature = view.findViewById(R.id.tv_day_low_temperature)
                weatherTitle = view.findViewById(R.id.tv_weather_title)
                todayTemperatureRange = view.findViewById(R.id.today_temperature_range)
                todayWeatherIcon = view.findViewById(R.id.current_weather_icon)
                ivSkillIcon = view.findViewById(R.id.iv_skill_icon)
                view.findViewById<View>(R.id.iv_back).setOnClickListener { v -> Navigation.findNavController(v).navigateUp() }
            } else {
                view = inflater.inflate(R.layout.layout_future_weather, container, false)
                tvFutureTitle = view.findViewById(R.id.tv_title)
                futureContent = view.findViewById(R.id.future_days_weather)
                view.findViewById<View>(R.id.iv_back).setOnClickListener { v -> Navigation.findNavController(v).navigateUp() }
            }
        } else {
            view = inflater.inflate(R.layout.layout_weather_quality, container, false)
            view.findViewById<View>(R.id.iv_back).setOnClickListener { v -> Navigation.findNavController(v).navigateUp() }
            tvTitle = view.findViewById(R.id.tv_title)
            ivWeatherIcon = view.findViewById(R.id.iv_weather_icon)
            tvWeatherQuality = view.findViewById(R.id.tv_weather_quality)
            tvDescription = view.findViewById(R.id.tv_description)
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (payload == null) {
            Log.e(Constant.TYPE_WEATHER_TEMPLATE, "Cannot receive payload data")
            return
        }

        val weatherForecastArray = payload!!.getAsJsonArray(Constant.PAYLOAD_WEATHER_FORECAST)
        if (weatherForecastArray != null && weatherForecastArray.size() > 0) {
            if (isFutureWeatherType) {
                setupWeatherContent(weatherForecastArray)
            } else {
                setupFutureWeather(weatherForecastArray)
            }
        } else {
            setupWeatherQuality()
        }
    }

    private fun setupWeatherContent(weatherForecastArray: JsonArray) {
        try {
            val layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT)
            layoutParams.weight = 1f
            futureDaysWeatherContainer!!.gravity = Gravity.CENTER
            for (i in 0 until weatherForecastArray.size()) {
                futureDaysWeatherContainer!!.addView(
                        generateWeatherItem(weatherForecastArray.get(i).asJsonObject),
                        layoutParams)
            }

            val title = payload!!.getAsJsonObject(Constant.PAYLOAD_TITLE)
            val subTitle = title.get(Constant.PAYLOAD_SUB_TITLE).asString
            val mainTitle = title.get(Constant.PAYLOAD_MAIN_TITLE).asString
            weatherTitle?.text = String.format("%s  %s", subTitle, mainTitle)

            val currentWeatherIcon = payload!!.getAsJsonObject(Constant.PAYLOAD_CURRENT_WEATHER_ICON)
            if (!currentWeatherIcon.isJsonNull && context != null) {
                val img = Gson().fromJson(currentWeatherIcon, Image::class.java)
                if (img.sources != null && img.sources.size > 0) {
                    Glide.with(context!!)
                            .load(img.sources[0].url)
                            .into(todayWeatherIcon!!)
                }
            }

            val lowTemperature = payload!!.get(Constant.PAYLOAD_LOW_TEMPERATURE).asString
            val highTemperature = payload!!.get(Constant.PAYLOAD_HIGH_TEMPERATURE).asString

            //是否是未来的某一天
            val isSomeDay = (payload?.get("currentWeather")?.asString.isNullOrEmpty())
            if (isSomeDay) {
                highContent?.visibility = View.VISIBLE
                lowContent?.visibility = View.VISIBLE
                currentTemperature?.visibility = View.GONE
                todayTemperatureRange?.visibility = View.GONE

                dayHighTemperature?.text = highTemperature
                dayLowTemperature?.text = lowTemperature
            } else {
                highContent?.visibility = View.GONE
                lowContent?.visibility = View.GONE
                currentTemperature?.visibility = View.VISIBLE
                todayTemperatureRange?.visibility = View.VISIBLE

                currentTemperature?.text = payload!!.get(Constant.PAYLOAD_CURRENT_WEATHER).asString
                todayTemperatureRange?.text = String.format("%s ~ %s", lowTemperature, highTemperature)
            }

            val skillIcon = payload!!.getAsJsonObject("skillIcon")
            if (!skillIcon.isJsonNull && context != null) {
                val img = Gson().fromJson(skillIcon, Image::class.java)
                if (img.sources != null && img.sources.size > 0) {
                    setLogo(img)
                }
            }
        } catch (e: Exception) {
            Log.e(Constant.TYPE_WEATHER_TEMPLATE, "Cannot analyze payload data")
            e.printStackTrace()
        }

    }

    private fun setLogo(img: Image) {
        ivSkillIcon?.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val context = context ?: return
                ivSkillIcon?.let {
                    Glide.with(context)
                            .load(img.sources[0].url)
                            .apply(RequestOptions
                                    .placeholderOf(R.drawable.cover_default)
                                    .transform(RoundedCornersTransformation(
                                            it.height / 4,
                                            0)))
                            .into(it)
                    it.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
        })
    }

    private fun generateWeatherItem(weatherForecast: JsonObject): View {
        val view = LayoutInflater.from(context).inflate(R.layout.item_future_weather, null)
        val ivFutureIcon = view.findViewById<ImageView>(R.id.iv_future_icon)
        val image = weatherForecast.get(Constant.PAYLOAD_IMAGE).asJsonObject
        if (!image.isJsonNull && context != null) {
            val img = Gson().fromJson(image, Image::class.java)
            if (img.sources != null && img.sources.size > 0) {
                Glide.with(context!!)
                        .load(img.sources[0].url)
                        .into(ivFutureIcon)
            }
        }
        val tvFutureDay = view.findViewById<TextView>(R.id.tv_future_day)
        tvFutureDay.text = weatherForecast.get(Constant.PAYLOAD_DAY).asString
        val tvFutureTemp = view.findViewById<TextView>(R.id.tv_future_temp)
        val lowValue = weatherForecast.get(Constant.PAYLOAD_LOW_TEMPERATURE).asString
        val highValue = weatherForecast.get(Constant.PAYLOAD_HIGH_TEMPERATURE).asString
        tvFutureTemp.text = String.format("%s ~ %s", lowValue, highValue)
        return view
    }

    private fun setupWeatherQuality() {
        try {
            val title = payload!!.getAsJsonObject(Constant.PAYLOAD_TITLE)
            val subTitle = title.get(Constant.PAYLOAD_SUB_TITLE).asString
            val mainTitle = title.get(Constant.PAYLOAD_MAIN_TITLE).asString
            tvTitle!!.text = String.format("%s  %s", subTitle, mainTitle)
            val description = payload!!.get("description").asString
            tvDescription!!.text = description
            val currentWeatherIcon = payload!!.getAsJsonObject(Constant.PAYLOAD_CURRENT_WEATHER_ICON)
            if (!currentWeatherIcon.isJsonNull && context != null) {
                val img = Gson().fromJson(currentWeatherIcon, Image::class.java)
                if (img.sources != null && img.sources.size > 0) {
                    Glide.with(context!!)
                            .load(img.sources[0].url)
                            .into(ivWeatherIcon!!)
                }
            }
            val currentWeather = payload!!.get(Constant.PAYLOAD_CURRENT_WEATHER).asString
            tvWeatherQuality!!.text = currentWeather
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun setupFutureWeather(weatherForecastArray: JsonArray) {
        try {
            val title = payload!!.getAsJsonObject(Constant.PAYLOAD_TITLE)
            tvFutureTitle!!.text = String.format("%s %s", title.get(Constant.PAYLOAD_SUB_TITLE).asString,
                    title.get(Constant.PAYLOAD_MAIN_TITLE).asString)

            val layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT)
            layoutParams.weight = 1f
            futureContent!!.gravity = Gravity.CENTER
            for (i in 0 until weatherForecastArray.size()) {
                futureContent!!.addView(
                        generateFutureWeatherItem(weatherForecastArray.get(i).asJsonObject),
                        layoutParams)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun generateFutureWeatherItem(weatherForecast: JsonObject): View {
        val view = LayoutInflater.from(context).inflate(R.layout.item_weather_forcast, null)
        val tvWeek = view.findViewById<TextView>(R.id.tv_week)
        val tvTime = view.findViewById<TextView>(R.id.tv_time)
        val icon = view.findViewById<ImageView>(R.id.iv_future_icon)
        val high = view.findViewById<TextView>(R.id.tv_high_temp)
        val low = view.findViewById<TextView>(R.id.tv_low_temp)

        tvWeek.text = weatherForecast.get(Constant.PAYLOAD_DAY).asString
        tvTime.text = weatherForecast.get(Constant.PAYLOAD_DATE).asString
        high.text = weatherForecast.get(Constant.PAYLOAD_HIGH_TEMPERATURE).asString
        low.text = weatherForecast.get(Constant.PAYLOAD_LOW_TEMPERATURE).asString

        val image = weatherForecast.get(Constant.PAYLOAD_IMAGE).asJsonObject
        if (!image.isJsonNull && context != null) {
            val img = Gson().fromJson(image, Image::class.java)
            if (img.sources != null && img.sources.size > 0) {
                Glide.with(context!!)
                        .load(img.sources[0].url)
                        .into(icon)
            }
        }
        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        speechSynthesizer?.mediaPlayer?.stop()
    }
}
