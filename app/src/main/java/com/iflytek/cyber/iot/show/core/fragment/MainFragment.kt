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
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageSwitcher
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.navigation.fragment.NavHostFragment.findNavController
import androidx.navigation.fragment.findNavController
import cn.iflyos.sdk.android.impl.common.iFLYOSPlayerHandler
import cn.iflyos.sdk.android.impl.mediaplayer.MediaPlayerHandler
import cn.iflyos.sdk.android.impl.template.SimpleTemplateDispatcher
import cn.iflyos.sdk.android.impl.template.TemplateRuntimeHandler
import cn.iflyos.sdk.android.v3.constant.iFLYOSEvent
import cn.iflyos.sdk.android.v3.iFLYOSManager
import cn.iflyos.sdk.android.v3.iface.MediaPlayer
import cn.iflyos.sdk.android.v3.iface.PlatformInterface
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
import com.iflytek.cyber.iot.show.core.ObserverListener
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.model.Content
import com.iflytek.cyber.iot.show.core.model.ContentStorage
import com.iflytek.cyber.iot.show.core.model.TemplateContent
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
import java.text.SimpleDateFormat
import java.util.*

/**
 * 主界面显示 Fragment
 */
class MainFragment : BaseFragment(), AMapLocationListener, ObserverListener,
        MediaPlayerHandler.OnMediaStateChangedListener {

    private var contentView: View? = null // 主界面 View
    private var tvWeather: TextView? = null // 天气文本
    private var tvTemperature: TextView? = null // 温度文本
    private var clock: AppCompatTextView? = null // 时钟文本
    private var date: AppCompatTextView? = null // 日期文本
    private var ivIndex: ImageSwitcher? = null // 壁纸切换器
    private var ivWeather: ImageView? = null // 天气图标
    private var ivMusic: ImageView? = null // 音乐缩略图
    private var musicForeground: View? = null // 用于显示正在播放动效时的封面遮罩
    private var ivSettings: ImageView? = null // 设置图标
    private var ivPlaying: ImageView? = null // 音乐缩略图前的波浪控件，用于标识是否正在播放

    private var mLocationClient: AMapLocationClient? = null // 高德定位对象
    private var mLocationOption: AMapLocationClientOption? = null // 高德定位参数对象
    private var weatherApi: WeatherApi? = null // 天气 API

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var settingsFragment: SettingsFragment // 设置 Fragment

    private var playerHandler: MediaPlayerHandler? = null // 用于引用在 SDK 中使用的 AudioPlayer

    companion object {
        const val CHANGE_WALLPAPER_TIME = 30 * 60 * 1000 // 更换壁纸时间间隔
        const val CHANGE_WEATHER_TIME = 3L * 60 * 60 * 1000 // 检查天气预报时间间隔
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

        val manager = iFLYOSManager.getInstance()
        val iFLYOSPlayerHandler = manager.getHandler("AudioMediaPlayer") as? iFLYOSPlayerHandler
        playerHandler = iFLYOSPlayerHandler?.mediaPlayer as? MediaPlayerHandler
        playerHandler?.addOnMediaStateChangedListener(this)
    }

    private val simpleTemplateDispatcher = object : SimpleTemplateDispatcher() {
        override fun onPlayerInfoDispatched(template: String) {
            val templateContent = Gson().fromJson(template, TemplateContent::class.java)
            val prevTemplate = ContentStorage.get().template
            ContentStorage.get().saveContent(templateContent.content)
            if (prevTemplate == null || prevTemplate.audioItemId != templateContent.audioItemId) {
                updateMusicCover(templateContent.content)
                ContentStorage.get().saveTemplate(templateContent)
            }
        }
    }

    override fun onMediaStateChanged(playerName: String, sourceId: String, state: MediaPlayer.MediaState) {
        if (state == MediaPlayer.MediaState.PLAYING) {
            ContentStorage.get().isMusicPlaying = true
        } else if (state == MediaPlayer.MediaState.STOPPED) {
            ContentStorage.get().isMusicPlaying = false
        }
        updatePlayState()
    }

    override fun onMediaError(playerName: String, mediaError: MediaPlayer.MediaError, message: String) {
        Log.e("MainFragment", "play media error: $mediaError   message is: $message")
    }

    override fun onPositionUpdated(playerName: String, position: Long) {

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
            iv.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT)
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
                    fragmentManager?.let { fragmentManager ->
                        settingsFragment.show(fragmentManager, "Settings")
                    }
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
            } else {
                fragmentManager?.let { fragmentManager ->
                    settingsFragment.show(fragmentManager, "Settings")
                }
            }
        }

        // Location for weather
        if (context != null && !GpsUtils.checkGpsEnable(context!!)) {
            GpsUtils.requestGps(context!!)
        }
    }

    private val locationRunnable = Runnable {
        mLocationClient?.startLocation()
    }

    /**
     * 初始化高德定位
     */
    private fun setupLocation() {
        mLocationClient = AMapLocationClient(context)
        mLocationOption = AMapLocationClientOption()
        mLocationClient?.setLocationListener(this)
        mLocationOption?.locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
        mLocationOption?.isOnceLocation = true
        mLocationClient?.setLocationOption(mLocationOption)
        mLocationClient?.startLocation()
    }

    fun startLocation() {
        mLocationClient?.startLocation()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        (activity as? LauncherActivity)?.addObserver(this)
    }

    override fun onDetach() {
        super.onDetach()

        (activity as? LauncherActivity)?.deleteObserver(this)
        playerHandler?.removeOnMediaStateChangedListener(this)
    }

    private fun setupRetrofit() {
        val gson = GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create()
        val clientBuilder = OkHttpClient.Builder()
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
            val token = iFLYOSManager.getInstance().authToken

            if (!token.isNullOrEmpty()) {
                val authorization = "Bearer $token"
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
                                } else {
                                    Log.e(sTag, "Get weather request failed: ${response.code()}")
                                }
                            }
                        })
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val handler = iFLYOSManager.getInstance().getHandler(PlatformInterface.SpecialHandler.TEMPLATETUNTIME.value())
        if (handler is TemplateRuntimeHandler) {
            handler.registerTemplateDispatchedListener(simpleTemplateDispatcher)
        }
    }

    override fun onResume() {
        super.onResume()
        updatePlayState()
        updateMusicCover(ContentStorage.get().currentContent)
        launcher?.run {
            val statusType = getStatusType()
            val needResetType = arrayOf(
                    LauncherActivity.StatusType.PLAYER,
                    LauncherActivity.StatusType.ALERT,
                    LauncherActivity.StatusType.WEATHER,
                    LauncherActivity.StatusType.OPTION
            )
            if (statusType in needResetType)
                setStatusType(LauncherActivity.StatusType.NORMAL)
            checkIfResetState()
        }
    }

    override fun onStop() {
        super.onStop()
        val handler = iFLYOSManager.getInstance().getHandler(PlatformInterface.SpecialHandler.TEMPLATETUNTIME.value())
        if (handler is TemplateRuntimeHandler) {
            handler.unregisterDispatchedListener(simpleTemplateDispatcher)
        }
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

    override fun update(event: iFLYOSEvent, arg: String) {
        if (event == iFLYOSEvent.EVENT_SPEECH_RECOGNIZER_WAKEUP) {
            settingsFragment.dismiss()
        }
    }

    private fun updateMusicCover(content: Content?) {
        val ivMusic = ivMusic ?: return
        val launcher = launcher ?: return
        if (content?.art?.sources?.isEmpty() != true) {
            ivMusic.let {
                val url = content?.art?.sources?.get(0)?.url
                if (url.isNullOrEmpty()) {
                    val corner = it.height / 6
                    Glide.with(it)
                            .load(R.drawable.cover_tiny)
                            .apply(RequestOptions()
                                    .transform(RoundedCornersTransformation(corner, 0)))
                            .into(it)
                } else {
                    Glide.with(launcher)
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
            ivMusic.let {
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