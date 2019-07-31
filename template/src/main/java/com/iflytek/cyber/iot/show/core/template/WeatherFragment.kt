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
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.gson.*
import com.iflytek.cyber.iot.show.core.template.model.Constant
import com.iflytek.cyber.iot.show.core.template.model.Image
import com.iflytek.cyber.iot.show.core.template.utils.RoundedCornersTransformation

class WeatherFragment : TemplateFragment() {

    private var futureDaysWeatherContainer: LinearLayout? = null
    private var currentTemperature: TextView? = null
    private var todayTemperatureRange: TextView? = null
    private var weatherTitle: TextView? = null
    private var todayWeatherIcon: ImageView? = null
    private var ivSkillIcon: ImageView? = null
    private var ivBackIcon: ImageView? = null
    private var highContent: View? = null
    private var lowContent: View? = null
    private var dayHighTemperature: TextView? = null
    private var dayLowTemperature: TextView? = null
    private var payload: JsonObject? = null
    private var scrollView: HorizontalScrollView? = null

    private var ivWeatherIcon: ImageView? = null
    private var tvWeatherQuality: TextView? = null
    private var tvDescription: TextView? = null
    private var tvTitle: TextView? = null

    private var tvFutureTitle: TextView? = null
    private var futureContent: LinearLayout? = null

    private var isFutureWeatherType: Boolean = false

    private var alreadyNavigateUp = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val arguments = arguments ?: return

        val json = arguments.getString(EXTRA_TEMPLATE)
        val element = JsonParser().parse(json!!)
        payload = element.asJsonObject
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val weatherForecastArray = payload?.getAsJsonArray(Constant.PAYLOAD_WEATHER_FORECAST)
        val view: View
        if (weatherForecastArray != null && weatherForecastArray.size() > 0) {
            isFutureWeatherType = !(payload?.get("lowTemperature")?.asString.isNullOrEmpty()) && !(payload?.get("highTemperature")?.asString.isNullOrEmpty())
            if (isFutureWeatherType) {
                view = inflater.inflate(R.layout.fragment_weather, container, false)
                scrollView = view.findViewById(R.id.scroll_future_days_weather)
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
            } else {
                view = inflater.inflate(R.layout.layout_future_weather, container, false)
                scrollView = view.findViewById(R.id.scroll_future_days_weather)
                tvFutureTitle = view.findViewById(R.id.tv_title)
                futureContent = view.findViewById(R.id.future_days_weather)
            }
        } else {
            view = inflater.inflate(R.layout.layout_weather_quality, container, false)
            tvTitle = view.findViewById(R.id.tv_title)
            ivWeatherIcon = view.findViewById(R.id.iv_weather_icon)
            tvWeatherQuality = view.findViewById(R.id.tv_weather_quality)
            tvDescription = view.findViewById(R.id.tv_description)
        }
        ivBackIcon = view.findViewById(R.id.iv_back)
        ivBackIcon?.setOnClickListener { _ -> navigateUp() }
        return view
    }

    private fun navigateUp() {
        alreadyNavigateUp = true

        onBackPressed(this, payload.toString())
    }

    override fun getTemplatePayload(): String {
        payload ?: let {
            return it.toString()
        }
        return super.getTemplatePayload()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (payload == null) {
            Log.e(Constant.TYPE_WEATHER_TEMPLATE, "Cannot receive payload data")
            return
        }

        ivBackIcon?.post {
            ivBackIcon?.run {
                val padding = height * 12 / 56
                setPadding(padding, padding, padding, padding)
            }
        }

        view.post {
            (payload?.get(Constant.PAYLOAD_WEATHER_FORECAST) as? JsonArray)
                    ?.let { weatherForecastArray ->
                        if (weatherForecastArray.size() > 0) {
                            if (isFutureWeatherType) {
                                setupWeatherContent(weatherForecastArray)
                            } else {
                                setupFutureWeather(weatherForecastArray)
                            }
                        } else {
                            setupWeatherQuality()
                        }
                    } ?: run {
                setupWeatherQuality()
            }

        }

        view.setOnTouchListener { _, event ->
            if (event.action == KeyEvent.ACTION_DOWN || event.action == KeyEvent.ACTION_UP) {
                onScrollableBodyTouched(this, payload.toString())
            }
            false
        }
        scrollView?.setOnTouchListener { _, event ->
            if (event.action == KeyEvent.ACTION_DOWN || event.action == KeyEvent.ACTION_UP) {
                onScrollableBodyTouched(this, payload.toString())
            }
            false
        }
    }

    private fun setupWeatherContent(weatherForecastArray: JsonArray) {
        try {
            val arraySize = weatherForecastArray.size()

            val padding: Int = ((view?.width ?: 0) * .047).toInt()
            futureDaysWeatherContainer?.setPadding(padding, 0, padding, 0)

            val layoutParams = LinearLayout.LayoutParams(
                    ((scrollView?.width
                            ?: 0) - padding * 2) / Math.min(6, arraySize),
                    LinearLayout.LayoutParams.WRAP_CONTENT)
            futureDaysWeatherContainer?.gravity = Gravity.CENTER
            for (i in 0 until weatherForecastArray.size()) {
                futureDaysWeatherContainer?.addView(
                        generateWeatherItem(weatherForecastArray.get(i).asJsonObject),
                        layoutParams)
            }

            (payload?.get(Constant.PAYLOAD_TITLE) as? JsonObject)?.let { title ->
                val subTitle = (title.get(Constant.PAYLOAD_SUB_TITLE) as? JsonPrimitive)?.asString
                val mainTitle = (title.get(Constant.PAYLOAD_MAIN_TITLE) as? JsonPrimitive)?.asString
                weatherTitle?.text = String.format("%s  %s", subTitle, mainTitle)
            }

            val currentWeatherIcon = payload!!.getAsJsonObject(Constant.PAYLOAD_CURRENT_WEATHER_ICON)
            if (!currentWeatherIcon.isJsonNull && context != null) {
                val img = Gson().fromJson(currentWeatherIcon, Image::class.java)
                if (img?.sources?.isNotEmpty() == true) {
                    Glide.with(context!!)
                            .load(img.sources[0].url)
                            .into(todayWeatherIcon!!)
                }
            }

            val lowTemperature = (payload?.get(Constant.PAYLOAD_LOW_TEMPERATURE) as? JsonPrimitive)?.asString
            val highTemperature = (payload?.get(Constant.PAYLOAD_HIGH_TEMPERATURE) as? JsonPrimitive)?.asString

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

            val skillIcon = payload?.get("skillIcon")
            if (skillIcon?.isJsonObject == true) {
                val img = Gson().fromJson(skillIcon, Image::class.java)
                if (img?.sources?.isNotEmpty() == true) {
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
        (weatherForecast.get(Constant.PAYLOAD_IMAGE) as? JsonObject)?.let { image ->
            val context = context ?: return@let
            val img = Gson().fromJson(image, Image::class.java)
            if (img?.sources?.isNotEmpty() == true) {
                Glide.with(context)
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
            val subTitle = (title.get(Constant.PAYLOAD_SUB_TITLE) as? JsonPrimitive)?.asString
            val mainTitle = (title.get(Constant.PAYLOAD_MAIN_TITLE) as? JsonPrimitive)?.asString
            tvTitle?.text = String.format("%s  %s", subTitle, mainTitle)
            val description = (payload?.get("description") as? JsonPrimitive)?.asString
            tvDescription?.text = description
            (payload?.get(Constant.PAYLOAD_CURRENT_WEATHER_ICON) as? JsonObject)
                    ?.let { currentWeatherIcon ->
                        val context = context ?: return@let
                        val img = Gson().fromJson(currentWeatherIcon, Image::class.java)
                        if (img?.sources?.isNotEmpty() == true) {
                            Glide.with(context)
                                    .load(img.sources[0].url)
                                    .into(ivWeatherIcon!!)
                        }
                    }
            val currentWeather = (payload?.get(Constant.PAYLOAD_CURRENT_WEATHER) as? JsonPrimitive)?.asString
            tvWeatherQuality?.text = currentWeather
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun setupFutureWeather(weatherForecastArray: JsonArray) {
        try {
            (payload?.get(Constant.PAYLOAD_TITLE) as? JsonObject)?.let { title ->
                tvFutureTitle?.text = String.format("%s %s",
                        (title.get(Constant.PAYLOAD_SUB_TITLE) as? JsonPrimitive)?.asString,
                        (title.get(Constant.PAYLOAD_MAIN_TITLE) as? JsonPrimitive)?.asString)
            }
            val padding: Int = ((view?.width ?: 0) * .047).toInt()
            futureContent?.setPadding(padding, 0, padding, 0)

            val arraySize = weatherForecastArray.size()
            val layoutParams = LinearLayout.LayoutParams(
                    ((scrollView?.width
                            ?: 0) - padding * 2) / Math.min(6, arraySize),
                    LinearLayout.LayoutParams.WRAP_CONTENT)
            futureContent?.gravity = Gravity.CENTER
            for (i in 0 until weatherForecastArray.size()) {
                futureContent?.addView(
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

        (weatherForecast.get(Constant.PAYLOAD_IMAGE) as? JsonObject)?.let { image ->
            val context = context ?: return@let
            val img = Gson().fromJson(image, Image::class.java)
            if (img?.sources?.isNotEmpty() == true) {
                Glide.with(context)
                        .load(img.sources[0].url)
                        .into(icon)
            }
        }
        return view
    }
}
