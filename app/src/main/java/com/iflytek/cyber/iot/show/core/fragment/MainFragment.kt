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

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.support.v7.widget.AppCompatTextView
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageSwitcher
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import cn.iflyos.iace.iflyos.IflyosClient
import cn.iflyos.iace.iflyos.MediaPlayer
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.iflytek.cyber.iot.show.core.BuildConfig
import com.iflytek.cyber.iot.show.core.LauncherActivity
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.impl.Logger.LogEntry
import com.iflytek.cyber.iot.show.core.impl.Logger.LoggerHandler
import com.iflytek.cyber.iot.show.core.impl.PlaybackController.PlaybackControllerHandler
import com.iflytek.cyber.iot.show.core.model.Content
import com.iflytek.cyber.iot.show.core.model.ContentStorage
import com.iflytek.cyber.iot.show.core.model.TemplateContent
import com.iflytek.cyber.iot.show.core.retrofit.SSLSocketFactoryCompat
import com.iflytek.cyber.iot.show.core.utils.GpsUtils
import com.iflytek.cyber.iot.show.core.utils.RoundedCornersTransformation
import com.iflytek.cyber.iot.show.core.weather.Weather
import com.iflytek.cyber.iot.show.core.weather.WeatherApi
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.ref.SoftReference
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.*
import javax.net.ssl.X509TrustManager

class MainFragment : BaseFragment(), AMapLocationListener, Observer, PlaybackControllerHandler.PlaybackCallback {

    private var contentView: View? = null
    private var tvWeather: TextView? = null
    private var tvTemperature: TextView? = null
    private var clock: AppCompatTextView? = null
    private var date: AppCompatTextView? = null
    private var ivIndex: ImageSwitcher? = null
    private var ivWeather: ImageView? = null
    private var ivMusic: ImageView? = null
    private var musicForeground: View? = null
    private var ivSettings: ImageView? = null
    private var ivPlaying: ImageView? = null

    private var mLocationClient: AMapLocationClient? = null
    private var mLocationOption: AMapLocationClientOption? = null
    private var weatherApi: WeatherApi? = null

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var settingsFragment: SettingsFragment

    private var lastLogType: Int = -1000

    companion object {
        const val CHANGE_WALLPAPER_TIME = 30 * 60 * 1000
        const val CHANGE_WEATHER_TIME = 3L * 60 * 60 * 1000
        const val sTag = "MainFragment"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        if (contentView == null) {
            contentView = inflater.inflate(R.layout.fragment_main, container, false)
            tvWeather = contentView?.findViewById(R.id.tv_weather_desc)
            ivIndex = contentView?.findViewById(R.id.iv_index)
            clock = contentView?.findViewById(R.id.clock)
            date = contentView?.findViewById(R.id.date)
            ivWeather = contentView?.findViewById(R.id.img_weather)
            tvTemperature = contentView?.findViewById(R.id.tv_temperature)
            ivMusic = contentView?.findViewById(R.id.music)
            musicForeground = contentView?.findViewById(R.id.music_foreground)
            ivSettings = contentView?.findViewById(R.id.settings)
            ivPlaying = contentView?.findViewById(R.id.iv_playing)

            ivMusic?.setOnClickListener {
                findNavController().navigate(R.id.action_to_player_fragment)
            }

            setupView()

            setupRetrofit()

            setupLocation()

            settingsFragment = SettingsFragment()
        }
        return contentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        launcher?.let {
            val playbackController = it.mEngineService?.getPlaybackController()
            playbackController?.setPlaybackCallback(this)
        }
    }

    override fun onPositionUpdated(position: Long) {
    }

    override fun onPlaybackStateChanged(state: MediaPlayer.MediaState?) {
        if (state == MediaPlayer.MediaState.PLAYING) {
            ContentStorage.get().isMusicPlaying = true
        } else if (state == MediaPlayer.MediaState.STOPPED) {
            ContentStorage.get().isMusicPlaying = false
        }
        updatePlayState()
    }

    private fun setupView() {
        val runnable = object : Runnable {
            override fun run() {
                val resources = intArrayOf(R.drawable.bg_index1,
                        R.drawable.bg_index2, R.drawable.bg_index3,
                        R.drawable.bg_index4, R.drawable.bg_index5,
                        R.drawable.bg_index6, R.drawable.bg_index7)
                var i = (Math.random() * (resources.size - 1)).toInt()
                var tag = -1
                if (ivIndex?.tag != null) {
                    tag = ivIndex?.tag as Int
                }
                var tryCount = 0
                while (i == tag && tryCount < 100) { // trying 100 times is enough
                    i = (Math.random() * (resources.size - 1)).toInt()
                    tryCount = tryCount.plus(1)
                }
                context?.let {
                    val bitmap = BitmapFactory.decodeResource(this@MainFragment.resources, resources[i])
                    val activity = activity
                    if (activity is LauncherActivity) {
                        activity.updateBlurImage(bitmap)
                    }
                }
                ivIndex?.setImageResource(resources[i])
                ivIndex?.tag = i
                ivIndex?.postDelayed(this, CHANGE_WALLPAPER_TIME.toLong())
            }
        }
        ivIndex?.setInAnimation(context, android.R.anim.fade_in)
        ivIndex?.setOutAnimation(context, android.R.anim.fade_out)
        ivIndex?.setFactory {
            val iv = ImageView(context)
            iv.scaleType = ImageView.ScaleType.CENTER_CROP
            iv
        }
        runnable.run()
        tvWeather?.post {
            val viewGroup = tvWeather?.parent as View
            tvWeather?.setTextSize(TypedValue.COMPLEX_UNIT_PX, viewGroup.height * 0.04f)
            clock?.setTextSize(TypedValue.COMPLEX_UNIT_PX, viewGroup.height * 0.133f)

            val gradientDrawable = GradientDrawable()
            gradientDrawable.cornerRadius = (musicForeground?.height ?: 0) / 6f
            gradientDrawable.setStroke(Math.max(1f, (musicForeground?.height
                    ?: 0) / 48f).toInt(), Color.parseColor("#d8d8d8"))
            gradientDrawable.setSize(musicForeground?.width ?: 0, musicForeground?.height ?: 0)
            musicForeground?.background = gradientDrawable

            updateCalendar(Calendar.getInstance())
        }

        val timerHandler = TimerHandler(this)
        timerHandler.sendEmptyMessageDelayed(0, 1000)

        ivMusic?.let {
            val corner = it.height / 6
            Glide.with(it)
                    .load(R.drawable.cover_tiny)
                    .apply(RequestOptions()
                            .transform(RoundedCornersTransformation(corner, 0)))
                    .into(it)
        }

        ivSettings?.setOnClickListener {
            if (Build.VERSION.SDK_INT >= 23) {
                if (Settings.System.canWrite(context)) {
                    if (fragmentManager != null)
                        settingsFragment.show(fragmentManager, "Settings")
                } else {
                    AlertDialog.Builder(context)
                            .setTitle("提示")
                            .setMessage("使用设置需要允许 修改系统设置 权限")
                            .setPositiveButton(android.R.string.yes) { _, _ ->
                                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                                intent.data = Uri.parse("package:com.iflytek.cyber.iot.show.core")
                                startActivity(intent)
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .show()
                }
            } else if (fragmentManager != null)
                settingsFragment.show(fragmentManager, "Settings")
        }

        // Location for weather
        if (context != null && !GpsUtils.checkGpsEnable(context!!)) {
            GpsUtils.requestGps(context!!)
        }
    }

    private val locationRunnable = Runnable {
        mLocationClient?.startLocation()
    }

    private fun setupLocation() {
        mLocationClient = AMapLocationClient(context)
        mLocationOption = AMapLocationClientOption()
        mLocationClient?.setLocationListener(this)
        mLocationOption?.locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
        mLocationOption?.isOnceLocation = true
        mLocationClient?.setLocationOption(mLocationOption)
        mLocationClient?.startLocation()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        (activity as? LauncherActivity)?.addObserver(this)
    }

    override fun onDetach() {
        super.onDetach()

        (activity as? LauncherActivity)?.deleteObserver(this)
    }

    private fun setupRetrofit() {
        val gson = GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create()
        val clientBuilder = OkHttpClient.Builder()
        if (Build.VERSION.SDK_INT < 21) {
            val trustAllCert =
                    object : X509TrustManager {
                        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                        }

                        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                        }

                        override fun getAcceptedIssuers(): Array<X509Certificate> {
                            return emptyArray()
                        }

                    }
            val sslSocketFactory = SSLSocketFactoryCompat(trustAllCert)
            clientBuilder.sslSocketFactory(sslSocketFactory, trustAllCert)
        }
        val client = clientBuilder.build()

        val retrofit = Retrofit.Builder()
                .client(client)
                .baseUrl("https://home.iflyos.cn")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()

        weatherApi = retrofit.create<WeatherApi>(WeatherApi::class.java)
    }

    private fun updateCalendar(calendar: Calendar) {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        clock?.text = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)

        val format = SimpleDateFormat("MM'月'dd'日' E", Locale.CHINA)
        date?.text = format.format(calendar.time)
    }

    override fun onLocationChanged(location: AMapLocation?) {
        if (location?.errorCode == 0) {
            loadWeather(location)

            ivWeather?.postDelayed(locationRunnable, CHANGE_WEATHER_TIME)
        }
    }

    private fun loadWeather(location: AMapLocation) {
        val activity = activity ?: return
        if (activity is LauncherActivity) {
            val token = activity.mEngineService?.getAuthToken()

            if (!token.isNullOrEmpty()) {
                val authorization = "Bearer " + token!!
                val currentLocation = (String.format(Locale.CHINESE, "%.2f", location.longitude)
                        + "," + String.format(Locale.CHINESE, "%.2f", location.latitude))
                weatherApi?.getWeather(authorization, currentLocation)
                        ?.enqueue(object : Callback<Weather> {
                            override fun onFailure(call: Call<Weather>, t: Throwable) {

                            }

                            override fun onResponse(call: Call<Weather>, response: Response<Weather>) {
                                if (response.isSuccessful) {
                                    val weather = response.body()
                                    if (weather != null) {
                                        updateUi(weather)
                                    }
                                } else
                                    Log.e(sTag, "Get weather reqeust failed: ${response.code()}")
                            }
                        })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updatePlayState()
        updateMusicCover(ContentStorage.get().currentContent)
        launcher?.showSimpleTips()
    }

    private fun updatePlayState() {
        if (ContentStorage.get().isMusicPlaying) {
            ivPlaying?.setBackgroundResource(R.drawable.ic_equalizer)
            val animationDrawable = ivPlaying?.background as AnimationDrawable
            animationDrawable.start()
        } else {
            if (ivPlaying?.background is AnimationDrawable) {
                (ivPlaying?.background as AnimationDrawable).stop()
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    ivPlaying?.setBackgroundResource(R.drawable.ic_equalizer1_white_48dp)
                }
            }
        }
    }

    private fun updateUi(weather: Weather) {
        val context = context
        if (context != null) {
            ivWeather?.let {
                Glide.with(context)
                        .load(weather.icon)
                        .apply(RequestOptions().centerCrop())
                        .into(it)
            }
            tvTemperature?.text = String.format("%s ℃", weather.temperature)
            tvWeather?.text = weather.lifestyle
        }
        startUpdateWeatherLoop()
    }

    override fun update(o: Observable?, arg: Any?) {
        if (o is LoggerHandler.LoggerObservable) {
            if (arg is LogEntry) {
                val type = arg.type
                val json = arg.json
                when (type) {
                    LoggerHandler.RENDER_PLAYER_INFO -> {
                        val content = Gson().fromJson<TemplateContent>(arg.json.toString(), TemplateContent::class.java)
                        ContentStorage.get().saveContent(content.template)
                        ivMusic?.post {
                            updateMusicCover(content.template)
                        }
                    }
                    LoggerHandler.CONNECTION_STATE -> {
                        val template = json.optJSONObject("template")
                        val status = template.optString("status")
                        when (status) {
                            IflyosClient.ConnectionStatus.CONNECTED.toString() -> {
                                locationRunnable.run()
                            }
                        }
                    }
                    LoggerHandler.DIALOG_STATE -> {
                        val state = json.optJSONObject("template").optString("state")
                        when (state) {
                            IflyosClient.DialogState.LISTENING.toString() -> {
                                settingsFragment.dismiss()
                            }
                        }
                    }
                }
                if (type != LoggerHandler.RECORD_VOLUME) {
                    lastLogType = type
                }
            }
        }
    }

    private fun updateMusicCover(content: Content?) {
        if (ivMusic != null && launcher != null && content?.art?.sources?.isEmpty() != true) {
            ivMusic?.let {
                val url = content?.art?.sources?.get(0)?.url
                if (url.isNullOrEmpty()) {
                    val corner = it.height / 6
                    Glide.with(it)
                            .load(R.drawable.cover_tiny)
                            .apply(RequestOptions()
                                    .transform(RoundedCornersTransformation(corner, 0)))
                            .into(it)
                } else {
                    Glide.with(launcher!!)
                            .asBitmap()
                            .load(url)
                            .apply(RequestOptions()
                                    .placeholder(it.drawable)
                                    .error(R.drawable.cover_tiny)
                                    .transform(RoundedCornersTransformation(
                                            it.height / 6,
                                            0)))
                            .listener(object : RequestListener<Bitmap> {
                                override fun onLoadFailed(e: GlideException?,
                                                          model: Any,
                                                          target: Target<Bitmap>,
                                                          isFirstResource: Boolean): Boolean {
                                    return false
                                }

                                override fun onResourceReady(resource: Bitmap,
                                                             model: Any,
                                                             target: Target<Bitmap>,
                                                             dataSource: DataSource,
                                                             isFirstResource: Boolean): Boolean {
                                    return false
                                }
                            })
                            .into(it)
                }
            }
        } else {
            ivMusic?.let {
                val corner = it.height / 6
                Glide.with(it)
                        .load(R.drawable.cover_tiny)
                        .apply(RequestOptions()
                                .transform(RoundedCornersTransformation(corner, 0)))
                        .into(it)
            }
        }
    }


    private fun startUpdateWeatherLoop() {
        handler.postDelayed({ mLocationClient?.startLocation() }, (3 * 60 * 60 * 1000).toLong())
    }

    private class TimerHandler internal constructor(
            mainFragment: MainFragment,
            private val reference: SoftReference<MainFragment> =
                    SoftReference(mainFragment)) : Handler() {

        override fun handleMessage(msg: Message) {
            val fragment = reference.get()
            if (fragment != null && !fragment.isDetached) {
                fragment.updateCalendar(Calendar.getInstance())
                sendEmptyMessageDelayed(0, 1000)
            }
        }
    }

}