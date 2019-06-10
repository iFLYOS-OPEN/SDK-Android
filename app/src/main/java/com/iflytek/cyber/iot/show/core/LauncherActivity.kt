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

package com.iflytek.cyber.iot.show.core

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.*
import android.graphics.Bitmap
import android.graphics.Color
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.*
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.KeyEvent
import android.view.View
import android.view.animation.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.core.content.PermissionChecker
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import cn.iflyos.sdk.android.impl.alerts.AlertsPlayerHandler
import cn.iflyos.sdk.android.impl.cbl.CBLAuthDelegate
import cn.iflyos.sdk.android.impl.common.iFLYOSPlayerHandler
import cn.iflyos.sdk.android.impl.externalvideoapp.ExternalVideoAppHandler
import cn.iflyos.sdk.android.impl.iflyosclient.iFLYOSClientHandler
import cn.iflyos.sdk.android.impl.mediaplayer.MediaPlayerHandler
import cn.iflyos.sdk.android.impl.template.SimpleTemplateDispatcher
import cn.iflyos.sdk.android.impl.template.TemplateRuntimeHandler
import cn.iflyos.sdk.android.impl.template.TemplateRuntimeHandler.Companion.BODY_TEMPLATE1
import cn.iflyos.sdk.android.impl.template.TemplateRuntimeHandler.Companion.BODY_TEMPLATE2
import cn.iflyos.sdk.android.impl.template.TemplateRuntimeHandler.Companion.BODY_TEMPLATE3
import cn.iflyos.sdk.android.impl.template.TemplateRuntimeHandler.Companion.LIST_TEMPLATE1
import cn.iflyos.sdk.android.impl.template.TemplateRuntimeHandler.Companion.OPTION_TEMPLATE1
import cn.iflyos.sdk.android.impl.template.TemplateRuntimeHandler.Companion.OPTION_TEMPLATE2
import cn.iflyos.sdk.android.impl.template.TemplateRuntimeHandler.Companion.OPTION_TEMPLATE3
import cn.iflyos.sdk.android.impl.template.TemplateRuntimeHandler.Companion.WEATHER_TEMPLATE
import cn.iflyos.sdk.android.v3.constant.ExternalVideoAppConstant
import cn.iflyos.sdk.android.v3.constant.iFLYOSEvent
import cn.iflyos.sdk.android.v3.iFLYOSManager
import cn.iflyos.sdk.android.v3.iface.Alerts
import cn.iflyos.sdk.android.v3.iface.MediaPlayer
import cn.iflyos.sdk.android.v3.iface.PlatformInterface
import cn.iflyos.sdk.android.v3.iface.iFLYOSClient
import cn.iflyos.sdk.android.v3.iface.iFLYOSClient.AuthState
import cn.iflyos.sdk.android.v3.ipc.iFLYOSInterface
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.iflytek.cyber.iot.show.core.fragment.*
import com.iflytek.cyber.iot.show.core.impl.ExternalVideoApp.AppStateObserver
import com.iflytek.cyber.iot.show.core.impl.ExternalVideoApp.ExternalVideoAppDirectiveHandler
import com.iflytek.cyber.iot.show.core.impl.SpeechRecognizer.SpeechRecognizerHandler
import com.iflytek.cyber.iot.show.core.model.ActionConstant
import com.iflytek.cyber.iot.show.core.model.ContentStorage
import com.iflytek.cyber.iot.show.core.model.TemplateContent
import com.iflytek.cyber.iot.show.core.retrofit.SSLSocketFactoryCompat
import com.iflytek.cyber.iot.show.core.template.*
import com.iflytek.cyber.iot.show.core.utils.ConnectivityUtils
import com.iflytek.cyber.iot.show.core.utils.ToneManager
import com.iflytek.cyber.iot.show.core.widget.RecognizeWaveView
import jp.wasabeef.blurry.Blurry
import kotlinx.android.synthetic.main.activity_launcher.*
import java.lang.ref.SoftReference
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.X509TrustManager
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.math.roundToInt

/**
 * 主入口，且 Manifest 中声明为 Launcher，以达到在智能音箱中可以开机启动的目的
 */
class LauncherActivity : AppCompatActivity(), TemplateFragment.TemplateCallback {

    private var ivBlur: ImageView? = null // 唤醒时的模糊背景
    private var tvTipsTitle: TextView? = null // 唤醒界面的提示标题，引用对象仅做动画
    private var tvTipsSimple: TextView? = null // 主界面的提示语
    private val tipsSet = HashSet<TextView?>() // 唤醒界面的提示语，引用对象仅做动画
    private var tvIat: TextView? = null // 唤醒界面的转写结果
    private var ivIatLogo: ImageView? = null // 唤醒界面的左下角 Logo，点击可结束识别
    private var ivLogo: ImageView? = null // 主界面的左下角 Logo，点击可开始识别
    private var recognizeWaveView: RecognizeWaveView? = null // 唤醒界面的波形动画
    private val finishedSourceIdMap = HashMap<String, String>() // 已经播放结束的资源

    private var manager: iFLYOSManager? = null // iFLYOS 管理器，单例

    private var latestIat = System.currentTimeMillis() // 最后一次转写结果的到达时间
    private var authUserCode: String? = null // 授权的用户码
    private var isNetworkLost = false // 标识 ivs 连接是否断开，WiFi 是否断开

    private var networkCallback: ConnectivityManager.NetworkCallback? = null // 网络状态更改回调
    private val clearCardHandler = ClearCardHandler() // 清除卡片的计时 Handler

    private val observerListenerList = HashSet<ObserverListener>() // 订阅监听器集合

    private val longPressHandler = LongPressHandler(this) // 长按计时器，用于执行音量键长按处理
    private var isNetworkAvailable: Boolean = false // 网络可用性标识符
        get() {
            return if (Build.VERSION.SDK_INT < 21) {
                ConnectivityUtils.isNetworkAvailable(this)
            } else {
                field
            }
        }
    private val checkNetworkHandler: Handler // 用于定时检查网络或 iFLYOS 是否可用

    private var currentTips = -1 // 记录当前的提示语 id
    private var currentTipsAnimator: Animator? = null // 提示语动画
    private var requestUsageDialog: AlertDialog? = null // 请求查看使用情况对话框

    private var mStatusType = StatusType.NORMAL // 当前底部状态栏显示类型
    private var mStatusHidden = false // 标识底部状态栏是否被隐藏
    private val networkErrorClickListener = View.OnClickListener {
        // 网络出错时点击底栏的操作
        val navController = findNavController(R.id.fragment)
        val currentDestinationId = navController.currentDestination?.id
        if (mStatusHidden || (currentDestinationId != R.id.main_fragment &&
                        currentDestinationId != R.id.player_fragment)) {
            return@OnClickListener
        }
        val arguments = Bundle()
        arguments.putBoolean("RESET_WIFI", true)
        navController.navigate(R.id.action_main_to_wifi_fragment, arguments)
    }
    private val authorizeErrorClickListener = View.OnClickListener {
        // 授权过期、被解绑点击底栏的操作
        val navController = findNavController(R.id.fragment)
        val currentDestinationId = navController.currentDestination?.id
        if (mStatusHidden || (currentDestinationId != R.id.main_fragment &&
                        currentDestinationId != R.id.player_fragment)) {
            return@OnClickListener
        }

        val arguments = Bundle()
        arguments.putBoolean("shouldShowTips", true)
        navController.navigate(R.id.action_main_to_pair_fragment, arguments)
    }

    private val tapToTalkClickListener = View.OnClickListener {
        // 开始识别
        if (mStatusHidden) {
            return@OnClickListener
        }

        manager?.dlgStart()

        handleVoiceStart()
    }

    // 闹钟状态改变监听
    private val onAlertStateChangedListener = object : AlertsPlayerHandler.OnAlertStateChangedListener {
        override fun onAlertStateChanged(alertToken: String, state: Alerts.AlertState, reason: String) {
            val intent = Intent(ActionConstant.ACTION_CLIENT_ALERT_STATE_CHANGE)
            intent.putExtra("alert_token", alertToken)
            intent.putExtra("state", state.toString())
            intent.putExtra("reason", reason)
            sendBroadcast(intent)

            when (state) {
                Alerts.AlertState.STARTED -> {
                    if (currentTips != TIPS_ALERT) {
                        mStatusType = StatusType.ALERT
                        updateTips(TIPS_ALERT)
                    }
                }
                Alerts.AlertState.STOPPED,  // 被本地停止
                Alerts.AlertState.SNOOZED,  // 打盹
                Alerts.AlertState.COMPLETED // 闹钟播放完了
                -> {
                    val current = getCurrentFragment()
                    if (current is BodyTemplateFragment || current is BodyTemplate3Fragment) {
                        clearCardHandler.startCount(Runnable {
                            val delayCurrent = getCurrentFragment()
                            if (current == delayCurrent) {
                                requestClearCard()
                            }
                        }, TimeUnit.SECONDS.toMillis(3))
                    }
                }
                else -> {
                    // ignore
                }
            }
        }
    }

    private val appStateObserver = AppStateObserver.getInstance(this) // 应用状态管理

    // 处理外置播放器指令
    private val extVideoAppDirectiveHandler = ExternalVideoAppDirectiveHandler()

    private var isResume = false // 主界面是否可见标识

    init {
        // 保证检查网络的 handler 在子线程中运行
        val handlerThread = HandlerThread("checkNetwork")
        handlerThread.start()
        checkNetworkHandler = Handler(handlerThread.looper)
    }

    companion object {
        const val EXTRA_TEMPLATE = "template"

        private const val sTag = "LauncherActivity"

        private const val CHECK_IVS_OFFSET = 60 * 1000L // 检查 iFLYOS 可用性的时间间隔

        private const val REQUEST_PERMISSION_CODE = 1001

        // 各个提示语 id
        const val TIPS_DEFAULT = R.array.default_tips
        const val TIPS_WEATHER = R.array.weather_tips
        const val TIPS_PLAYER = R.array.player_tips
        const val TIPS_OPTION = R.array.option_tips
        const val TIPS_ALERT = R.array.alert_tips
    }

    enum class StatusType { // 底部状态栏状态类型
        AUTHORIZE_ERROR,
        NETWORK_ERROR,
        NORMAL,
        PLAYER,
        WEATHER,
        OPTION,
        ALERT,
        RETRYING,
        SERVER_ERROR,
        TIMEOUT_ERROR,
        UNKNOWN_ERROR,
        UNSUPPORTED_DEVICE_ERROR,
    }

    fun addObserver(listener: ObserverListener) {
        observerListenerList.add(listener)
    }

    fun deleteObserver(listener: ObserverListener) {
        observerListenerList.remove(listener)
    }

    /**
     * 根据 mStatusType 更新底部状态栏状态
     */
    private fun updateBottomBar() {
        Log.d(sTag, "Status Type : $mStatusType")
        when (mStatusType) {
            StatusType.NORMAL -> {
                if (currentTips != TIPS_DEFAULT)
                    updateTips(TIPS_DEFAULT)
                tvTipsSimple?.setOnClickListener(tapToTalkClickListener)
                ivLogo?.setOnClickListener(tapToTalkClickListener)
                ivLogo?.setImageResource(R.drawable.ic_voice_bar_regular_white_32dp)
            }
            StatusType.PLAYER -> {
                if (currentTips != TIPS_PLAYER)
                    updateTips(TIPS_PLAYER)
                tvTipsSimple?.setOnClickListener(tapToTalkClickListener)
                ivLogo?.setOnClickListener(tapToTalkClickListener)
                ivLogo?.setImageResource(R.drawable.ic_voice_bar_regular_white_32dp)
            }
            StatusType.WEATHER -> {
                if (currentTips != TIPS_WEATHER)
                    updateTips(TIPS_WEATHER)
                tvTipsSimple?.setOnClickListener(tapToTalkClickListener)
                ivLogo?.setOnClickListener(tapToTalkClickListener)
                ivLogo?.setImageResource(R.drawable.ic_voice_bar_regular_white_32dp)
            }
            StatusType.ALERT -> {
                if (currentTips != TIPS_ALERT)
                    updateTips(TIPS_ALERT)
                tvTipsSimple?.setOnClickListener(tapToTalkClickListener)
                ivLogo?.setOnClickListener(tapToTalkClickListener)
                ivLogo?.setImageResource(R.drawable.ic_voice_bar_regular_white_32dp)
            }
            StatusType.OPTION -> {
                if (currentTips != TIPS_OPTION)
                    updateTips(TIPS_OPTION)
                tvTipsSimple?.setOnClickListener(tapToTalkClickListener)
                ivLogo?.setOnClickListener(tapToTalkClickListener)
                ivLogo?.setImageResource(R.drawable.ic_voice_bar_regular_white_32dp)
            }
            StatusType.AUTHORIZE_ERROR -> {
                currentTips = R.string.authorize_error
                tvTipsSimple?.setText(R.string.authorize_error)
                tvTipsSimple?.setOnClickListener(authorizeErrorClickListener)
                ivLogo?.setOnClickListener(authorizeErrorClickListener)
                ivLogo?.setImageResource(R.drawable.ic_voice_bar_exception_white_32dp)
            }
            StatusType.SERVER_ERROR -> {
                currentTips = R.string.unknown_error
                tvTipsSimple?.setText(R.string.unknown_error)
                tvTipsSimple?.setOnClickListener(null)
                ivLogo?.setImageResource(R.drawable.ic_voice_bar_exception_white_32dp)
                ivLogo?.setOnClickListener(null)
            }
            StatusType.NETWORK_ERROR -> {
                currentTips = R.string.network_error
                tvTipsSimple?.setText(R.string.network_error)
                tvTipsSimple?.setOnClickListener(networkErrorClickListener)
                ivLogo?.setOnClickListener(networkErrorClickListener)
                ivLogo?.setImageResource(R.drawable.ic_voice_bar_exception_white_32dp)
            }
            StatusType.UNKNOWN_ERROR -> {
                currentTips = R.string.unknown_error
                tvTipsSimple?.setText(R.string.unknown_error)
                tvTipsSimple?.setOnClickListener(null)
                ivLogo?.setImageResource(R.drawable.ic_voice_bar_exception_white_32dp)
                ivLogo?.setOnClickListener(null)
            }
            StatusType.RETRYING -> {
                currentTips = R.string.retry_connecting
                tvTipsSimple?.setText(R.string.retry_connecting)
                tvTipsSimple?.setOnClickListener(null)
                ivLogo?.setImageResource(R.drawable.ic_voice_bar_exception_white_32dp)
                ivLogo?.setOnClickListener(null)
            }
            StatusType.UNSUPPORTED_DEVICE_ERROR -> {
                currentTips = R.string.unsupported_device_error
                tvTipsSimple?.setText(R.string.unsupported_device_error)
                tvTipsSimple?.setOnClickListener(null)
                ivLogo?.setImageResource(R.drawable.ic_voice_bar_exception_white_32dp)
                ivLogo?.setOnClickListener(null)
            }
            else -> {

            }
        }
        (findViewById<View>(R.id.error_next)?.parent as? View)?.run {
            post {
                requestLayout()
            }
        }

        if (!mStatusHidden) {
            val normalStatusTypes = arrayOf(
                    StatusType.NORMAL,
                    StatusType.ALERT,
                    StatusType.PLAYER,
                    StatusType.OPTION,
                    StatusType.WEATHER
            )
            if (mStatusType in normalStatusTypes && !isNetworkLost) {
                hideErrorBar()
            } else {
                Log.w(sTag, "show Error bar: $mStatusType")
                showErrorBar()
            }
        }
    }

    /**
     * 设置当前状态栏状态，用于在 Fragment 中操作此 Activity
     */
    fun setStatusType(statusType: StatusType) {
        if (!isNetworkLost) {
            mStatusType = statusType
        }
        updateBottomBar()
    }

    fun getStatusType() = mStatusType

    /**
     * 在指定的提示语数组中随机抓取一个提示语
     *
     * 在 RES/value/string.xml 中可查看所有的样例提示语数组
     */
    private fun getTips(arrayResId: Int): String? {
        val array = resources.getStringArray(arrayResId)
        return if (array.isEmpty()) {
            null
        } else {
            array[(Math.random() * (array.size - 1)).roundToInt()]
        }
    }

    /**
     * 根据提示语 id 更新当前底部状态栏显示的提示语
     */
    fun updateTips(arrayResId: Int) {
        currentTips = arrayResId

        val tips = getTips(arrayResId)

        Log.d(sTag, "UpdateTips: $tips")

        currentTipsAnimator?.cancel()

        // 若底部状态栏正在隐藏状态，则直接设置文本，否则进行透明度变换动画
        if (mStatusHidden) {
            tvTipsSimple?.text = tips
        } else {
            val animator = ValueAnimator.ofFloat(0f, 1f)
            animator.addUpdateListener {
                val value = it.animatedValue as Float
                if (value < 0.5f) {
                    tvTipsSimple?.alpha = 1 - value * 2
                } else {
                    if (tvTipsSimple?.text.toString() != tips.toString()) {
                        tvTipsSimple?.text = tips
                    }
                    tvTipsSimple?.alpha = value * 2 - 1
                }
            }
            animator.interpolator = LinearInterpolator()
            animator.duration = 1000
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    super.onAnimationEnd(animation)
                    if (animation == currentTipsAnimator)
                        currentTipsAnimator = null
                }

                override fun onAnimationCancel(animation: Animator?) {
                    super.onAnimationCancel(animation)
                    if (animation == currentTipsAnimator)
                        currentTipsAnimator = null
                }
            })
            animator.start()
        }
    }

    /**
     * 根据 id 和 template 数据显示 TemplateRuntime
     *
     * @param id fragment 在 /RES/navigation/navi.xml 中定义的 id
     * @param template 下发的指令的 payload
     */
    private fun showTemplate(id: Int, template: String) {
        handleVoiceEnd()
        val nav = findNavController(R.id.fragment)
        when (nav.currentDestination?.id) {
            R.id.main_fragment, R.id.player_fragment -> {
                templateToFragment(nav, id, template)
            }
            else -> {
                findViewById<View>(R.id.fragment).postDelayed({
                    templateToFragment(nav, id, template)
                }, 300)
            }
        }
    }

    /**
     * 清除正在显示的 TemplateRuntime
     */
    private fun clearTemplate(nav: NavController) {
        nav.popBackStack(R.id.body_template_3_fragment, true)
        nav.popBackStack(R.id.body_template_fragment, true)
        nav.popBackStack(R.id.list_fragment, true)
        nav.popBackStack(R.id.weather_fragment, true)
        nav.popBackStack(R.id.option_template_fragment, true)
    }

    /**
     * 使用 NavController 显示指定的 fragment（TemplateRuntime）
     */
    private fun templateToFragment(nav: NavController, id: Int, template: String) {
        clearTemplate(nav)

        val bundle = Bundle()
        bundle.putString(EXTRA_TEMPLATE, template)

        val navOptions = NavOptions.Builder()
                .setEnterAnim(R.anim.slide_in_top)
                .setExitAnim(android.R.anim.fade_out)
                .setPopExitAnim(R.anim.slide_page_pop_exit)
                .setPopEnterAnim(android.R.anim.fade_in)
                .build()
        nav.navigate(id, bundle, navOptions)
    }

    // 检查网络 Runnable
    private val checkNetworkRunnable = object : Runnable {
        override fun run() {
            checkNetworkHandler.postDelayed(this, CHECK_IVS_OFFSET)

            ConnectivityUtils.checkIvsAvailable(onSuccess = {
                if (!isNetworkAvailable) {
                    isNetworkAvailable = true
                    isNetworkLost = false

                    (manager?.getHandler(PlatformInterface.SpecialHandler.IFLYOSCLIENT.value())
                            as? iFLYOSClientHandler)?.requestReconnect()
                }
            }, onFailed = { _, responseCode ->
                if (responseCode == -1) {
                    // 网络失去连接
                    if (isNetworkAvailable) {
                        isNetworkAvailable = false
                        isNetworkLost = true

                        mStatusType = StatusType.NETWORK_ERROR
                        runOnUiThread {
                            handleVoiceEnd()
                            updateBottomBar()
                        }
                    }
                }
            })
        }
    }

    // 执行唤醒开始的动画
    private fun handleVoiceStart() {
        runOnUiThread {
            hideSimpleTips()
            showIatPage()
        }
    }

    // 隐藏错误栏
    private fun hideErrorBar() {
        error_bar.animate().cancel()
        error_bar.animate()
                .alpha(0f)
                .setDuration(200)
                .setInterpolator(FastOutSlowInInterpolator())
                .start()
        error_divider.animate().cancel()
        error_divider.animate()
                .alpha(0f)
                .setDuration(200)
                .setInterpolator(FastOutSlowInInterpolator())
                .start()
        error_next.animate().cancel()
        error_next.animate()
                .alpha(0f)
                .setDuration(200)
                .setInterpolator(FastOutSlowInInterpolator())
                .start()
    }

    // 显示错误栏
    private fun showErrorBar() {
        error_bar.animate().cancel()
        error_bar.animate()
                .alpha(1f)
                .setDuration(250)
                .setInterpolator(FastOutSlowInInterpolator())
                .start()
        error_divider.animate().cancel()
        error_divider.animate()
                .alpha(1f)
                .setDuration(250)
                .setInterpolator(FastOutSlowInInterpolator())
                .start()
        error_next.animate().cancel()
        error_next.animate()
                .alpha(1f)
                .setDuration(200)
                .setInterpolator(FastOutSlowInInterpolator())
                .start()
    }

    // 隐藏主界面提示语
    fun hideSimpleTips() {
        mStatusHidden = true
        hideErrorBar()
        ivLogo?.run {
            animate().scaleX(0f)
                    .scaleY(0f)
                    .setDuration(150)
                    .start()
        }
        ivLogo?.isClickable = false

        tvTipsSimple?.run {
            animate().alpha(0f).setDuration(150).start()
        }
    }

    // 显示唤醒转写页面
    private fun showIatPage() {
        recognizeWaveView?.startEnterAnimation()
        ivBlur?.run {
            animate().alpha(1f).setDuration(200).start()
        }
        val animationSet = AnimationSet(true)
        val translation = TranslateAnimation(((ivBlur?.width
                ?: 0) / 8).toFloat(), 0f, 0f, 0f)
        animationSet.addAnimation(translation)
        val alphaAnimation = AlphaAnimation(0f, 1f)
        animationSet.addAnimation(alphaAnimation)
        animationSet.interpolator = FastOutSlowInInterpolator()
        animationSet.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {
            }

            override fun onAnimationStart(animation: Animation?) {
                tvTipsTitle?.alpha = 1f
                tipsSet.map {
                    it?.alpha = 1f
                }
            }

            override fun onAnimationEnd(animation: Animation?) {
                tvTipsTitle?.alpha = 1f
                tipsSet.map {
                    it?.alpha = 1f
                }
            }

        })
        animationSet.startOffset = 100
        animationSet.duration = 300

        tvTipsTitle?.startAnimation(animationSet)
        tipsSet.map {
            it?.startAnimation(animationSet)
        }
        ivIatLogo?.isClickable = true
        ivIatLogo?.run {
            val scaleAnimation = ScaleAnimation(0f, 1f, 0f, 1f, (width / 2).toFloat(), (height / 2).toFloat())
            scaleAnimation.duration = 200
            scaleAnimation.startOffset = 150
            scaleAnimation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationRepeat(animation: Animation?) {

                }

                override fun onAnimationEnd(animation: Animation?) {
                    scaleX = 1f
                    scaleY = 1f
                }

                override fun onAnimationStart(animation: Animation?) {

                }

            })
            startAnimation(scaleAnimation)
        }
        tvIat?.run {
            text = ""
            animate().alpha(1f).setStartDelay(150).setDuration(200).start()
        }
    }

    // 检查是否需要显示主界面提示语
    fun checkIfResetState() {
        if (ivBlur?.alpha == 1f) {
            // 正在转写、识别
        } else {
            showSimpleTips()
        }
    }

    // 显示主界面提示语
    fun showSimpleTips() {
        mStatusHidden = false
        updateBottomBar()
        ivLogo?.run {
            animate().scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .setStartDelay(150)
                    .start()
        }
        ivLogo?.isClickable = true

        tvTipsSimple?.run {
            animate().alpha(1f).setStartDelay(150).setDuration(200).start()
        }
    }

    // 隐藏唤醒转写页面
    private fun hideIatPage() {
        recognizeWaveView?.startQuitAnimation()
        ivBlur?.run {
            animate().alpha(0f).setStartDelay(100).setDuration(400).start()
        }
        val animationSet = AnimationSet(true)
        val translation = TranslateAnimation(0f, -((ivBlur?.width
                ?: 0) / 8).toFloat(), 0f, 0f)
        animationSet.addAnimation(translation)
        val alphaAnimation = AlphaAnimation(1f, 0f)
        animationSet.addAnimation(alphaAnimation)
        animationSet.duration = 400
        animationSet.interpolator = FastOutSlowInInterpolator()
        animationSet.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {
            }

            override fun onAnimationStart(animation: Animation?) {
            }

            override fun onAnimationEnd(animation: Animation?) {
                tvTipsTitle?.alpha = 0f
                tipsSet.map {
                    it?.alpha = 0f
                }
            }

        })

        tvTipsTitle?.startAnimation(animationSet)
        tipsSet.map {
            it?.startAnimation(animationSet)
        }

        ivIatLogo?.isClickable = false
        ivIatLogo?.run {
            val scaleAnimation = ScaleAnimation(1f, 0f, 1f, 0f, (width / 2).toFloat(), (height / 2).toFloat())
            scaleAnimation.duration = 150
            scaleAnimation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationRepeat(animation: Animation?) {

                }

                override fun onAnimationEnd(animation: Animation?) {
                    scaleX = 0f
                    scaleY = 0f
                }

                override fun onAnimationStart(animation: Animation?) {

                }

            })
            startAnimation(scaleAnimation)
        }

        tvIat?.run {
            animate().alpha(0f).setDuration(150).start()
        }
    }

    // 执行唤醒结束动画
    private fun handleVoiceEnd() {
        Log.d(sTag, "handleVoiceEnd")
        val navController = findNavController(R.id.fragment)
        when (navController.currentDestination?.id) {
            R.id.wifi_fragment, R.id.about_fragment -> {

            }
            else -> {
                runOnUiThread {
                    if (ivBlur?.alpha != 0f) {
                        hideIatPage()
                        showSimpleTips()
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        setImmersiveFlags()
    }

    private fun setImmersiveFlags() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    // 监听应用安装、卸载广播，用于更新 Context
    private val pkgChangedReceiver = object : SelfBroadcastReceiver(Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REMOVED) {
        override fun onReceiveAction(action: String, intent: Intent) {
            when (action) {
                Intent.ACTION_PACKAGE_ADDED,
                Intent.ACTION_PACKAGE_REPLACED,
                Intent.ACTION_PACKAGE_REMOVED -> {
                    // 更新外置播放器 Context
                    val packages = packageManager.getInstalledPackages(0)
                    val installedPkgName = HashSet<String>()
                    if (!packages.isNullOrEmpty()) {
                        packages.map {
                            if (it.packageName == ExternalVideoAppConstant.PKG_IQIYI_TV
                                    || it.packageName == ExternalVideoAppConstant.PKG_IQIYI_SPEAKER) {
                                installedPkgName.add(it.packageName)
                            }
                        }
                    }

                    extVideoAppDirectiveHandler.resetAppStates(installedPkgName.toTypedArray())
                }
            }
        }
    }

    // 监听外部发送广播对程序进行控制
    private val iflyosReceiver = object : SelfBroadcastReceiver(ActionConstant.ACTION_STOP_SPEAKING,
            ActionConstant.ACTION_TEMPLATE_RUNTIME_SELECT_ELEMENT,
            ActionConstant.ACTION_REQUEST_CLEAR_CARD,
            ActionConstant.ACTION_REQUEST_STOP_CURRENT_ALERT,
            ActionConstant.ACTION_REQUEST_DIALOG_END,
            ActionConstant.ACTION_PLAY_WAKE_SOUND) {
        override fun onReceiveAction(action: String, intent: Intent) {
            when (action) {
                ActionConstant.ACTION_STOP_SPEAKING -> {
                    // 停止播放正在播放的 TTS
                    val speakPlayerHandler = manager?.getHandler(iFLYOSManager.SPEAKER_HANDLER) as? iFLYOSPlayerHandler
                    (speakPlayerHandler?.mediaPlayer as? MediaPlayerHandler)
                            ?.stop(false)
                }
                ActionConstant.ACTION_PLAY_WAKE_SOUND -> {
                    // keep volume as 1f, Speaker volume should apply to system volume
                    ToneManager[this@LauncherActivity].play(ToneManager.TONE_WAKE, 1f)
                }
                ActionConstant.ACTION_TEMPLATE_RUNTIME_SELECT_ELEMENT -> {
                    // 应用外显示可选列表时，选中时应发送该广播
                    val token = intent.getStringExtra("token")
                    val selectedItemToken = intent.getStringExtra("selectedItemToken")
                    if (token.isNullOrEmpty() || selectedItemToken.isNullOrEmpty()) {
                        Log.e(sTag, "ACTION_TEMPLATE_RUNTIME_SELECT_ELEMENT handle failed," +
                                " wrong argument {token: $token, selectedItemToken: $selectedItemToken}")
                    } else {
                        (manager?.getHandler(PlatformInterface.SpecialHandler.TEMPLATETUNTIME.value())
                                as? TemplateRuntimeHandler)?.selectElement(token, selectedItemToken)
                    }
                }
                ActionConstant.ACTION_REQUEST_CLEAR_CARD -> {
                    (manager?.getHandler(PlatformInterface.SpecialHandler.TEMPLATETUNTIME.value())
                            as? TemplateRuntimeHandler)?.requestClearCard()
                }
                ActionConstant.ACTION_REQUEST_STOP_CURRENT_ALERT -> {
                    // 请求停止正在响铃的闹钟
                    val alertsPlayerHandler = manager?.getHandler(iFLYOSManager.ALERT_HANDLER)
                            as? AlertsPlayerHandler
                    if (alertsPlayerHandler?.mediaPlayer?.isPlaying() == true) {
                        alertsPlayerHandler.localStop()
                    }
                }
                ActionConstant.ACTION_REQUEST_DIALOG_END -> {
                    // 请求结束唤醒
                    Log.d(sTag, "receive ${ActionConstant.ACTION_REQUEST_DIALOG_END}")
                    manager?.sendMsg(iFLYOSInterface.IFLYOS_DO_STOP_AUDIO_FOREGROUND_ACTIVITY, "")
                    manager?.dlgEnd()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(sTag, "onCreate")
        setContentView(R.layout.activity_launcher)

        // check SSL
        if (Build.VERSION.SDK_INT < 21)
            try {
                val trustAllCert =
                        object : X509TrustManager {
                            @SuppressLint("TrustAllX509TrustManager")
                            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                            }

                            @SuppressLint("TrustAllX509TrustManager")
                            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                            }

                            override fun getAcceptedIssuers(): Array<X509Certificate> {
                                return emptyArray()
                            }

                        }
                val sslSocketFactory = SSLSocketFactoryCompat(trustAllCert)
                HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketFactory)
            } catch (e: Exception) {
                e.printStackTrace()
            }

        setupView()

        setImmersiveFlags()

        hideIatPage()

        //init new ivs sdk
        if (iFLYOSManager.currentInstance() == null) {
            manager = iFLYOSManager.getInstance()

            manager?.init(applicationContext, BuildConfig.CLIENT_ID, "${cacheDir.path}/iflyos/config.json", iflyosListener, generateAdditionalParams())
        } else {
            manager = iFLYOSManager.currentInstance()

            manager?.updateListener(iflyosListener)
        }

        // 注册 App 前台状态改变监听
        appStateObserver.addOnStateChangeListener(extVideoAppDirectiveHandler)

        isNetworkAvailable = ConnectivityUtils.isNetworkAvailable(this)

        iflyosReceiver.register(this)

        pkgChangedReceiver.getFilter().addDataScheme("package")
        pkgChangedReceiver.register(this)

        // api 21 以上该广播过时
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT < 21) {
            val intentFilter = IntentFilter()
            intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
            registerReceiver(connectStateReceiver, intentFilter)
        } else {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE)
            if (connectivityManager is ConnectivityManager) {
                networkCallback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network?) {
                        super.onAvailable(network)
                        isNetworkAvailable = true
                        val handler = manager?.getHandler(
                                PlatformInterface.SpecialHandler.IFLYOSCLIENT.value()) as? iFLYOSClientHandler
                        // 网络可用时，若发现 iFLYOS 断开连接，则尝试重连
                        when (handler?.connectionStatus) {
                            iFLYOSClient.ConnectionStatus.CONNECTED -> {

                            }
                            iFLYOSClient.ConnectionStatus.DISCONNECTED -> {
                                tvTipsSimple?.post {
                                    handler.requestReconnect()

                                    mStatusType = StatusType.RETRYING
                                    runOnUiThread {
                                        updateBottomBar()
                                    }
                                }
                            }
                            iFLYOSClient.ConnectionStatus.PENDING -> {
                                tvTipsSimple?.post {
                                    mStatusType = StatusType.RETRYING
                                    runOnUiThread {
                                        updateBottomBar()
                                    }
                                }
                            }
                        }
                    }

                    override fun onLost(network: Network?) {
                        super.onLost(network)
                        isNetworkAvailable = false
                        val networkAvailable = ConnectivityUtils.isNetworkAvailable(this@LauncherActivity)
                        mStatusType = if (networkAvailable) {
                            StatusType.NORMAL
                        } else {
                            StatusType.NETWORK_ERROR
                        }
                        runOnUiThread {
                            handleVoiceEnd()
                            updateBottomBar()
                        }
                    }
                }
                val request = NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
                connectivityManager.registerNetworkCallback(request, networkCallback)
            }
        }

        if (PermissionChecker.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PermissionChecker.PERMISSION_GRANTED && PermissionChecker.checkSelfPermission(
                        this, Manifest.permission.READ_PHONE_STATE)
                == PermissionChecker.PERMISSION_GRANTED && PermissionChecker.checkSelfPermission(
                        this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PermissionChecker.PERMISSION_GRANTED) {
            initializedPage()
        }

        // try to init ToneManager when onCreate
        ToneManager[this] // just create new instance

        manager?.setOnRefreshListener(object : CBLAuthDelegate.OnRefreshListener {
            override fun onRefresh(authState: AuthState) {
                if (authState == AuthState.INVALID_REFRESH_TOKEN) {
                    if (NavHostFragment.findNavController(fragment).currentDestination?.id == R.id.splash_fragment) {
                        hideSimpleTips()
                        NavHostFragment.findNavController(fragment).navigate(R.id.action_to_welcome_fragment)
                    }
                }
            }
        })
    }

    private fun generateAdditionalParams(): JsonObject {
        // 配置上报自定义端能力
        val additionalParams = JsonObject()
        val customCap = JsonObject()
        // 增加外部视频播放器能力(实验性)
        // 目前支持：爱奇艺tv版，爱奇艺语音版
        customCap.addProperty("ExternalVideoApp", "1.0")
        additionalParams.add("customCap", customCap)

        // 自定义唤醒词可参考以下代码，将唤醒词资源路径传入即可，详细内容请参阅文档相关章节描述
//        val customWakeUp = JsonObject()
//        customWakeUp.addProperty("wakeup_res_path", "path")
//        additionalParams.add("ivw", customWakeUp)

        return additionalParams
    }

    private fun initializedPage() {
        if (manager?.isConnectedIvs == true) {
            manager?.initializeIvs()
        } else {
            if (NavHostFragment.findNavController(fragment).currentDestination?.id == R.id.splash_fragment) {
                hideSimpleTips()
                NavHostFragment.findNavController(fragment).navigate(R.id.action_to_welcome_fragment)
            }
        }
    }

    private fun setupView() {
        ivBlur = findViewById(R.id.blur_recognize_background)
        tvTipsTitle = findViewById(R.id.recognize_tips_title)
        tvTipsSimple = findViewById(R.id.tips_simple)
        tvIat = findViewById(R.id.iat_text)
        ivIatLogo = findViewById(R.id.iat_logo)
        ivLogo = findViewById(R.id.logo)
        recognizeWaveView = findViewById(R.id.recognize_view)
        tipsSet.add(findViewById(R.id.recognize_tips_1))
        tipsSet.add(findViewById(R.id.recognize_tips_2))
        tipsSet.add(findViewById(R.id.recognize_tips_3))

        ivLogo?.setOnClickListener {
            manager?.dlgStart()
        }
        ivIatLogo?.setOnClickListener {
            handleVoiceEnd()
            manager?.sendMsg(iFLYOSInterface.IFLYOS_DO_STOP_AUDIO_FOREGROUND_ACTIVITY, "")
            manager?.dlgEnd()
        }

        ivBlur?.post {
            val height = fragment_container.height

            tvTipsTitle?.setTextSize(TypedValue.COMPLEX_UNIT_PX, height * 0.047f)
            ivLogo?.setImageResource(R.drawable.ic_voice_bar_regular_white_32dp)
            tvTipsSimple?.setTextSize(TypedValue.COMPLEX_UNIT_PX, height * 0.047f)
            tipsSet.map {
                it?.setTextSize(TypedValue.COMPLEX_UNIT_PX, height * 0.067f)
            }

            tvIat?.setTextSize(TypedValue.COMPLEX_UNIT_PX, height * 0.047f)
        }
    }

    override fun onResume() {
        super.onResume()
        if (PermissionChecker.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PermissionChecker.PERMISSION_GRANTED || PermissionChecker.checkSelfPermission(
                        this, Manifest.permission.READ_PHONE_STATE)
                != PermissionChecker.PERMISSION_GRANTED || PermissionChecker.checkSelfPermission(
                        this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PermissionChecker.PERMISSION_GRANTED) {
            // 申请 RECORD_AUDIO 权限用以语音识别
            // 申请 READ_PHONE_STATE 权限用以标识设备 id，
            //      若不需要也可参考 Settings.Secure.ANDROID_ID，或随机生成。保证唯一性即可。
            // 请求 ACCESS_COARSE_LOCATION 权限用于请求天气预报，若不需要亦可移除。
            requestPermission()
        } else {
            manager?.updateParams(generateAdditionalParams())
        }

        try {
            val result = appStateObserver.start()
            if (!result) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (requestUsageDialog == null) {
                        requestUsageDialog = AlertDialog.Builder(this)
                                .setTitle("请允许ShowCore读取应用使用情况")
                                .setMessage("ShowCore 需要读取应用使用情况，来管理外部视频应用相关操作")
                                .setPositiveButton(android.R.string.yes) { _, _ ->
                                    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                }
                                .setNegativeButton(android.R.string.no) { _, _ ->
                                    finish()
                                }
                                .setOnDismissListener {
                                    requestUsageDialog = null
                                }
                                .show()
                    }
                } else {
                    Toast.makeText(this, "ShowCore无法获取前台应用状态变化", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.d(sTag, "AppStateObserver started.")

                val packages = packageManager.getInstalledPackages(0)
                val installedPkgName = HashSet<String>()
                if (!packages.isNullOrEmpty()) {
                    packages.map {
                        if (it.packageName == ExternalVideoAppConstant.PKG_IQIYI_TV
                                || it.packageName == ExternalVideoAppConstant.PKG_IQIYI_SPEAKER) {
                            installedPkgName.add(it.packageName)
                        }
                    }
                }

                extVideoAppDirectiveHandler.resetAppStates(installedPkgName.toTypedArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        isResume = true

        val current = getCurrentFragment()
        if (current !is AboutFragment && current !is WifiFragment
                && current !is WelcomeFragment && current !is SplashFragment
                && current !is PairFragment) {
            showSimpleTips()
        }
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.READ_PHONE_STATE),
                REQUEST_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (permissions[0] == Manifest.permission.RECORD_AUDIO &&
                    grantResults[0] == PermissionChecker.PERMISSION_GRANTED) {
                //没给权限前会炸，允许后重新启动录音
                (manager?.speechRecognizer as? SpeechRecognizerHandler)?.restartRecording()
            }

            if (grantResults[0] == PermissionChecker.PERMISSION_GRANTED &&
                    grantResults[1] == PermissionChecker.PERMISSION_GRANTED &&
                    grantResults[2] == PermissionChecker.PERMISSION_GRANTED) {
                initializedPage()
            } else {
                // 忽略未授予权限，onResume 中会处理没有权限的情况，智能音箱集成时应预先允许权限
            }
        }
    }

    private val simpleTemplateDispatcher = object : SimpleTemplateDispatcher() {
        override fun onTemplateDispatched(template: String, type: String?) {
            if (isResume) {
                // 根据不同的 type 显示对应的 TemplateRuntime
                when (type) {
                    BODY_TEMPLATE1, BODY_TEMPLATE2 -> {
                        showTemplate(R.id.body_template_fragment, template)
                    }
                    BODY_TEMPLATE3 -> {
                        showTemplate(R.id.body_template_3_fragment, template)
                    }
                    WEATHER_TEMPLATE -> {
                        showTemplate(R.id.weather_fragment, template)
                    }
                    LIST_TEMPLATE1 -> {
                        showTemplate(R.id.list_fragment, template)
                    }
                    OPTION_TEMPLATE1, OPTION_TEMPLATE2, OPTION_TEMPLATE3 -> {
                        showTemplate(R.id.option_template_fragment, template)
                    }
                    else -> {
                        Log.d(sTag, "type: $type, template: $template")
                    }
                }
            } else {
                try {
                    val intent = Intent()
                    val componentName = ComponentName("$packageName.overlay", "$packageName.overlay.TemplateActivity")
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.component = componentName
                    intent.putExtra("template", template)
                    val tips = when (type) {
                        OPTION_TEMPLATE1, OPTION_TEMPLATE2, OPTION_TEMPLATE3 -> {
                            getTips(TIPS_OPTION)
                        }
                        WEATHER_TEMPLATE -> {
                            getTips(TIPS_WEATHER)
                        }
                        else -> {
                            val alertsPlayerHandler = manager?.getHandler(iFLYOSManager.ALERT_HANDLER)
                                    as? AlertsPlayerHandler
                            if (alertsPlayerHandler?.mediaPlayer?.isPlaying() == true) {
                                getTips(TIPS_ALERT)
                            } else {
                                getTips(TIPS_DEFAULT)
                            }
                        }
                    }
                    if (!tips.isNullOrEmpty()) {
                        intent.putExtra("tips", tips)
                    }
                    applicationContext.startActivity(intent)
                    overridePendingTransition(0, 0)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val broadcast = Intent(ActionConstant.ACTION_CLIENT_RENDER_TEMPLATE)
            broadcast.putExtra("template", template)
            sendBroadcast(broadcast)
        }

        override fun onPlayerInfoDispatched(template: String) {
            super.onPlayerInfoDispatched(template)

            val templateContent = Gson().fromJson(template, TemplateContent::class.java)
            ContentStorage.get().saveContent(templateContent.content)

            val broadcast = Intent(ActionConstant.ACTION_CLIENT_RENDER_PLAYER_INFO)
            broadcast.putExtra("template", template)
            sendBroadcast(broadcast)
        }

        override fun clearTemplate() {
            super.clearTemplate()

            val broadcast = Intent(ActionConstant.ACTION_CLIENT_CLEAR_TEMPLATE)
            sendBroadcast(broadcast)

            val controller = NavHostFragment.findNavController(fragment)
            when (controller.currentDestination?.id) {
                R.id.main_fragment, R.id.welcome_fragment -> {
                    // ignore
                }
                else -> {
                    clearTemplate(findNavController(R.id.fragment))
                }
            }
        }

        override fun clearPlayerInfo() {
            super.clearPlayerInfo()

            val broadcast = Intent(ActionConstant.ACTION_CLIENT_CLEAR_TEMPLATE)
            sendBroadcast(broadcast)

            val controller = NavHostFragment.findNavController(fragment)
            when (controller.currentDestination?.id) {
                R.id.main_fragment, R.id.welcome_fragment -> {
                    // ignore
                }
                else -> {
                    clearTemplate(findNavController(R.id.fragment))
                }
            }
        }
    }

    private fun requestClearCard() {
        Log.d(sTag, "requestClearTemplate")
        (manager?.getHandler(PlatformInterface.SpecialHandler.TEMPLATETUNTIME.value())
                as? TemplateRuntimeHandler)?.requestClearCard()
    }

    override fun onPause() {
        super.onPause()

        isResume = false

        getCurrentFragment()?.let {
            // 若界面置于后台，则 TemplateRuntime 界面应被关闭
            if (it is TemplateFragment) {
                onBackPressed(it, it.getTemplatePayload())
            }
        }

        hideSimpleTips()
    }

    private var iflyosListener = object : iFLYOSManager.iFLYOSListener {
        override fun onEvent(evt: iFLYOSEvent, msg: String) {
            runOnUiThread {
                // 向已注册的订阅分发时间
                //      订阅: addObserver
                //      取消: deleteObserver
                observerListenerList.map {
                    try {
                        it.update(evt, msg)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            val params = iFLYOSInterface.unpackParams(msg)

            when (evt) {
                iFLYOSEvent.EVENT_CLIENT_AUTH_STATE_CHANGE -> {
                    // 授权状态改变
                    val authState = params[1]
                    runOnUiThread {
                        onAuthStateChange(authState, params)
                    }
                }
                iFLYOSEvent.EVENT_ENGINE_RAW -> {
                    // 引擎事件透传回调
                    if (TextUtils.equals(params[0], iFLYOSInterface.IFLYOS_EVT_MEDIAPLAYER_CREATE)) {
                        manager?.registerAllHandler(iFLYOSInterface.unpackParams(params[1]))

                        val speakPlayerHandler = manager?.getHandler(iFLYOSManager.SPEAKER_HANDLER) as? iFLYOSPlayerHandler
                        (speakPlayerHandler?.mediaPlayer as? MediaPlayerHandler)
                                ?.addOnMediaStateChangedListener(mediaStateChangedListener)

                        val audioPlayerHandler = manager?.getHandler(iFLYOSManager.AUDIO_HANDLER) as? iFLYOSPlayerHandler
                        (audioPlayerHandler?.mediaPlayer as? MediaPlayerHandler)
                                ?.addOnMediaStateChangedListener(mediaStateChangedListener)

                        val alertsPlayerHandler = manager?.getHandler(iFLYOSManager.ALERT_HANDLER) as? AlertsPlayerHandler
                        alertsPlayerHandler?.addOnAlertStateChangedListener(onAlertStateChangedListener)
                    }
                }
                iFLYOSEvent.EVENT_CLIENT_INTER_MEDIA_TEXT -> {
                    // 转写结果回调
                    latestIat = System.currentTimeMillis()
                    tvIat?.text = params[0]

                    val broadcast = Intent(ActionConstant.ACTION_CLIENT_INTER_MEDIA_TEXT)
                    broadcast.putExtra("text", msg)
                    sendBroadcast(broadcast)
                }
                iFLYOSEvent.EVENT_CLIENT_DIALOG_STATE_CHANGE -> {
                    // 会话状态变更
                    val dialogState = iFLYOSClient.DialogState.valueOf(params[1])
                    Log.d(sTag, "Dialog state change: $dialogState")
                    if (dialogState == iFLYOSClient.DialogState.END ||
                            dialogState == iFLYOSClient.DialogState.SPEAKING) {
                        handleVoiceEnd()
                    } else if (dialogState == iFLYOSClient.DialogState.EXPECT) {
                        if (isResume) {
                            handleVoiceStart()
                        } else {
                            try {
                                val intent = Intent()
                                val componentName = ComponentName("$packageName.overlay",
                                        "$packageName.overlay.IatActivity")
                                intent.action = "$packageName.overlay.action.START"
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                intent.component = componentName
                                applicationContext.startActivity(intent)
                                overridePendingTransition(0, 0)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        sendBroadcast(Intent(ActionConstant.ACTION_PLAY_WAKE_SOUND))
                    }

                    Log.d(sTag, "send ${ActionConstant.ACTION_CLIENT_DIALOG_STATE_CHANGE}: ${params[1]}")

                    val broadcast = Intent(ActionConstant.ACTION_CLIENT_DIALOG_STATE_CHANGE)
                    broadcast.putExtra("last_state", params[0])
                    broadcast.putExtra("state", params[1])
                    sendBroadcast(broadcast)
                }
                iFLYOSEvent.EVENT_SPEECH_RECOGNIZER_WAKEUP -> {
                    // 唤醒回调
                    try {
                        val nav = findNavController(R.id.fragment)
                        when (nav.currentDestination?.id) {
                            R.id.about_fragment, R.id.wifi_fragment -> {
                                nav.navigateUp()
                            }
                        }
                    } catch (e: IllegalStateException) {
                        e.printStackTrace()
                    }

                    if (isResume) {
                        handleVoiceStart()
                    } else {
                        try {
                            val intent = Intent()
                            val componentName = ComponentName("$packageName.overlay", "$packageName.overlay.IatActivity")
                            intent.action = "$packageName.overlay.action.START"
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            intent.component = componentName
                            applicationContext.startActivity(intent)
                            overridePendingTransition(0, 0)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    sendBroadcast(Intent(ActionConstant.ACTION_PLAY_WAKE_SOUND))
                }
                iFLYOSEvent.EVENT_REVOKE_AUTHORIZATION -> {
                    // 授权失效，更新界面
                    mStatusType = StatusType.AUTHORIZE_ERROR
                    updateBottomBar()
                }
                iFLYOSEvent.EVENT_VOLUME_CHANGE -> {
                    // 录音音量变化，可根据音量变化展示界面动效
                    val volume = msg.toDouble()
                    recognizeWaveView?.updateVolume(volume)

                    val broadcast = Intent(ActionConstant.ACTION_VOLUME_CHANGE)
                    broadcast.putExtra("volume", volume)
                    sendBroadcast(broadcast)
                }
                iFLYOSEvent.EVENT_CONNECTION_STATUS_CHANGED -> {
                    // iFLYOS 连接状态改变
                    if (params[0] == iFLYOSClient.ConnectionStatus.CONNECTED.name) {
                        isNetworkLost = false
                        isNetworkAvailable = true

                        mStatusType = StatusType.NORMAL
                        updateBottomBar()

                        (manager?.getHandler(PlatformInterface.SpecialHandler.TEMPLATETUNTIME.value())
                                as? TemplateRuntimeHandler)?.setTimerEnable(false)

                        // 更新外置播放器 Context
                        val packages = packageManager.getInstalledPackages(0)
                        val installedPkgName = HashSet<String>()
                        if (!packages.isNullOrEmpty()) {
                            packages.map {
                                if (it.packageName == ExternalVideoAppConstant.PKG_IQIYI_TV
                                        || it.packageName == ExternalVideoAppConstant.PKG_IQIYI_SPEAKER) {
                                    installedPkgName.add(it.packageName)
                                }
                            }
                        }

                        extVideoAppDirectiveHandler.resetAppStates(installedPkgName.toTypedArray())
                    } else if (params[0] == iFLYOSClient.ConnectionStatus.DISCONNECTED.name) {
                        isNetworkLost = true
                        val navController = findNavController(R.id.fragment)
                        if (navController.currentDestination?.id != R.id.main_fragment) {
                            when (navController.currentDestination?.id) {
                                R.id.wifi_fragment, R.id.about_fragment -> {
                                    navController.navigateUp()
                                }
                            }
                        }
                        runOnUiThread {
                            mStatusType = StatusType.NETWORK_ERROR
                            updateBottomBar()
                        }
                    } else if (params[0] == iFLYOSClient.ConnectionStatus.PENDING.name) {
                        if (params[1] == iFLYOSClient.ConnectionChangedReason.SERVER_SIDE_DISCONNECT.name) {
                            isNetworkLost = true
                            val navController = findNavController(R.id.fragment)
                            if (navController.currentDestination?.id != R.id.main_fragment) {
                                when (navController.currentDestination?.id) {
                                    R.id.wifi_fragment, R.id.about_fragment -> {
                                        navController.navigateUp()
                                    }
                                }
                            }
                            runOnUiThread {
                                mStatusType = StatusType.NETWORK_ERROR
                                updateBottomBar()
                            }
                        }
                    }
                }
                iFLYOSEvent.EVENT_AUTH_REFRESHED -> {
                    // Token 已更新
                    val speechRecognizerHandler = SpeechRecognizerHandler(this@LauncherActivity, this)
                    speechRecognizerHandler.wakeUpHandler = {
                        // 定义是否可被开始识别
                        val normalStatus = arrayOf(
                                StatusType.AUTHORIZE_ERROR,
                                StatusType.NETWORK_ERROR,
                                StatusType.SERVER_ERROR,
                                StatusType.TIMEOUT_ERROR,
                                StatusType.UNSUPPORTED_DEVICE_ERROR,
                                StatusType.UNKNOWN_ERROR
                        )
                        // 若不处于以上错误状态，则可以开始识别
                        mStatusType !in normalStatus
                    }
                    manager?.startIFlyOS(speechRecognizerHandler)

                    // 注册 TemplateRuntime 分发回调
                    (manager?.getHandler(PlatformInterface.SpecialHandler.TEMPLATETUNTIME.value())
                            as? TemplateRuntimeHandler)
                            ?.registerTemplateDispatchedListener(simpleTemplateDispatcher)

                    // 注册外置视频播放器指令处理
                    val extVideoAppHandler = manager?.getHandler(PlatformInterface.SpecialHandler.EXTERNALVIDEOAPP.value())
                    if (extVideoAppHandler is ExternalVideoAppHandler) {
                        extVideoAppDirectiveHandler.init(this@LauncherActivity)
                        extVideoAppHandler.registerDirectiveHandler(extVideoAppDirectiveHandler)
                    }

                    checkNetworkHandler.removeCallbacksAndMessages(null)
                    checkNetworkHandler.postDelayed(checkNetworkRunnable, CHECK_IVS_OFFSET)
                }
                iFLYOSEvent.EVENT_CREATE_AUDIO_RECORD_FAILED -> {
                    // 创建录音失败
                    when (msg) {
                        SpeechRecognizerHandler.PERMISSION_DENIED -> {
                            // 无法获得录音权限
                            // code of requesting permission had been written in other place
                        }
                        SpeechRecognizerHandler.RECORD_UNINITIALIZED -> {
                            AlertDialog.Builder(this@LauncherActivity)
                                    .setTitle(R.string.recording_is_occupied)
                                    .setMessage(R.string.recording_is_occupied_message)
                                    .setPositiveButton(android.R.string.yes, null)
                                    .setOnDismissListener { finish() }
                                    .show()
                        }
                        else -> {
                            // ignore
                        }
                    }
                }
                else -> {
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.d(sTag, "onNewIntent")
    }

    override fun onStart(fragment: TemplateFragment) {
    }

    override fun onStop(fragment: TemplateFragment) {
    }

    override fun onResume(fragment: TemplateFragment) {
        clearCardHandler.clearCount()

        if (fragment is BodyTemplateFragment) {
            val speakPlayerHandler = manager?.getHandler(iFLYOSManager.SPEAKER_HANDLER)
            if (speakPlayerHandler is iFLYOSPlayerHandler) {
                if (speakPlayerHandler.mediaPlayer?.isPlaying() == true) {
                    fragment.onPlayStarted()
                }
            }
        }
        val alertsPlayerHandler = manager?.getHandler(iFLYOSManager.ALERT_HANDLER)
                as? AlertsPlayerHandler

        when (fragment) {
            is WeatherFragment -> {
                mStatusType = StatusType.WEATHER
                updateTips(TIPS_WEATHER)
            }
            is OptionTemplateFragment -> {
                mStatusType = StatusType.OPTION
                updateTips(TIPS_OPTION)
            }
            else -> {
                if (alertsPlayerHandler?.mediaPlayer?.isPlaying() == true) {
                    if (currentTips != TIPS_ALERT) {
                        mStatusType = StatusType.ALERT
                        updateTips(TIPS_ALERT)
                    }
                } else {
                    if (currentTips != TIPS_DEFAULT) {
                        mStatusType = StatusType.NORMAL
                        updateTips(TIPS_DEFAULT)
                    }
                }
            }
        }
    }

    override fun onPause(fragment: TemplateFragment) {
    }

    override fun onSelectElement(fragment: TemplateFragment, token: String, selectedItemToken: String) {
        // 可选列表模板中，点击某一项的回调
        val broadcast = Intent(ActionConstant.ACTION_TEMPLATE_RUNTIME_SELECT_ELEMENT)
        broadcast.putExtra("token", token)
        broadcast.putExtra("selectedItemToken", selectedItemToken)
        sendBroadcast(broadcast)
    }

    override fun onBackPressed(fragment: TemplateFragment, template: String) {
        Log.d(sTag, "onBackPressed: $fragment")

        requestClearCard()

        // 若TTS正在播放，则停止
        val speakPlayerHandler = manager?.getHandler(iFLYOSManager.SPEAKER_HANDLER) as? iFLYOSPlayerHandler
        (speakPlayerHandler?.mediaPlayer as? MediaPlayerHandler)?.let { player ->
            if (player.isPlaying()) {
                player.stop(false)
            }
        }

        // 若闹钟在响，则停止
        val alertsPlayerHandler = manager?.getHandler(iFLYOSManager.ALERT_HANDLER) as? AlertsPlayerHandler
        (alertsPlayerHandler?.mediaPlayer as? MediaPlayerHandler)?.let { player ->
            if (player.isPlaying()) {
                manager?.executeCommand(iFLYOSManager.Command.CMD_ALERT_STOP)
            }
        }
    }

    override fun onScrollableBodyStopped(fragment: TemplateFragment, template: String) {
        // 若TTS正在播放，则停止
        val speakPlayerHandler = manager?.getHandler(iFLYOSManager.SPEAKER_HANDLER) as? iFLYOSPlayerHandler
        (speakPlayerHandler?.mediaPlayer as? MediaPlayerHandler)?.let { player ->
            if (player.isPlaying()) {
                player.stop(false)
            }
        }

        // 开始关闭倒计时
        clearCardHandler.startCount(Runnable {
            val current = getCurrentFragment()
            if (current == fragment) {
                requestClearCard()
            }
        }, TimeUnit.SECONDS.toMillis(5))
    }

    private val mediaStateChangedListener = object : MediaPlayerHandler.OnMediaStateChangedListener {
        override fun onMediaStateChanged(playerName: String, sourceId: String, state: MediaPlayer.MediaState) {
            val broadcast = Intent(ActionConstant.ACTION_CLIENT_MEDIA_STATE_CHANGE)
            broadcast.putExtra("player_name", playerName)
            broadcast.putExtra("state", state.toString())
            broadcast.putExtra("source_id", sourceId)
            sendBroadcast(broadcast)

            if (playerName == iFLYOSManager.SPEAKER_HANDLER) {
                Log.d(sTag, "onMediaStateChanged($playerName, $state)")
                if (state == MediaPlayer.MediaState.FINISH
                        || state == MediaPlayer.MediaState.STOPPED) {
                    finishedSourceIdMap[playerName]?.let { last ->
                        try {
                            val value = last.toInt()
                            val newValue = sourceId.toInt()
                            if (newValue > value) {
                                finishedSourceIdMap[playerName] = sourceId
                            } else {
                                return
                            }
                        } catch (e: NumberFormatException) {
                            e.printStackTrace()
                        }
                    } ?: run {
                        finishedSourceIdMap[playerName] = sourceId
                    }

                    val current = getCurrentFragment()
                    if (current is BodyTemplateFragment) {
                        // BodyTemplateFragment 中有类似歌词滚动效果，通知播放结束
                        current.onPlayFinished()
                    }
                    if (current is TemplateFragment) {
                        // TTS 播放结束时，若 Template 正在显示，计时关闭 Template
                        if (current !is OptionTemplateFragment) {
                            clearCardHandler.startCount(Runnable {
                                val delayCurrent = getCurrentFragment()
                                if (current == delayCurrent) {
                                    requestClearCard()
                                }
                            }, TimeUnit.SECONDS.toMillis(5))
                        } else {
                            clearCardHandler.startCount(Runnable {
                                val delayCurrent = getCurrentFragment()
                                if (current == delayCurrent) {
                                    requestClearCard()
                                }
                            }, TimeUnit.MINUTES.toMillis(2))
                        }
                    }

                    if (!isResume) {
                        // 通知 overlay 模块触发关闭占位界面（TTS 播放时会有占位空白界面用以占据焦点）
                        clearCardHandler.postDelayed({
                            try {
                                val intent = Intent()
                                intent.action = "$packageName.overlay.action.CLOSE"
                                sendBroadcast(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, 1500)
                    }
                } else if (state == MediaPlayer.MediaState.PLAYING) {
                    val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment)
                            as? NavHostFragment
                    val current = navHostFragment?.childFragmentManager?.fragments?.get(0)
                    if (current is BodyTemplateFragment) {
                        current.onPlayStarted()
                    }

                    if (!isResume) {
                        // 通知 overlay 模块触发打开占位界面（TTS 播放时会有占位空白界面用以占据焦点）
                        try {
                            val intent = Intent()
                            val componentName = ComponentName("$packageName.overlay",
                                    "$packageName.overlay.TemplateActivity")
                            intent.action = "$packageName.overlay.action.BLANK"
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            intent.component = componentName
                            applicationContext.startActivity(intent)
                            overridePendingTransition(0, 0)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } else if (playerName == iFLYOSManager.AUDIO_HANDLER) {
                if (state == MediaPlayer.MediaState.BUFFERING) {
                    // AudioPlayer 准备播放时，外部视频 App 应结束焦点占用
                    (manager?.getHandler(PlatformInterface.SpecialHandler.EXTERNALVIDEOAPP.value())
                            as? ExternalVideoAppHandler)?.let { handler ->
                        Log.d(sTag, "try to release ext video app.")
                        handler.requestReleaseChannel()
                    }
                }
            }
        }

        override fun onMediaError(playerName: String, mediaError: MediaPlayer.MediaError, message: String) {

        }

        override fun onPositionUpdated(playerName: String, position: Long) {
            // 播放进度更新
            val broadcast = Intent(ActionConstant.ACTION_CLIENT_MEDIA_POSITION_UPDATED)
            broadcast.putExtra("player_name", playerName)
            broadcast.putExtra("position", position)
            sendBroadcast(broadcast)

            if (playerName == "SpeakMediaPlayer") {
                val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment)
                        as? NavHostFragment
                val current = navHostFragment?.childFragmentManager?.fragments?.get(0)
                if (current is BodyTemplateFragment) {
                    // BodyTemplateFragment 中有类似歌词滚动效果，更新时间以更新滚动距离
                    current.updatePosition(position)
                }
            }
        }
    }

    fun getCurrentFragment(): Fragment? {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment)
                as? NavHostFragment
        return navHostFragment?.childFragmentManager?.fragments?.get(0)
    }

    // 监听授权状态改变
    private fun onAuthStateChange(authState: String, params: List<String>) {
        when (authState) {
            AuthState.OK.name -> {
                isNetworkLost = false
                val controller = NavHostFragment.findNavController(fragment)
                if (controller.currentDestination?.id == R.id.splash_fragment) {
                    controller.navigate(R.id.action_to_main_fragment)
                }
                mStatusType = StatusType.NORMAL
                updateBottomBar()
            }
            AuthState.REQUIRED.name -> {
                isNetworkLost = true
                if (params.size > 2) {
                    authUserCode = params[2]
                }
                if (NavHostFragment.findNavController(fragment).currentDestination?.id == R.id.splash_fragment) {
                    NavHostFragment.findNavController(fragment).navigate(R.id.action_to_welcome_fragment)
                }
            }
            AuthState.TOKEN_EXPIRED.name, AuthState.UNINITIALIZED.name -> {
                isNetworkLost = true
                mStatusType = StatusType.AUTHORIZE_ERROR
                updateBottomBar()
            }
        }
    }

    override fun onBackPressed() {
        val controller = NavHostFragment.findNavController(fragment)
        when (controller.currentDestination?.id) {
            R.id.main_fragment, R.id.welcome_fragment -> {
                finish()
            }
            else -> {
                if (getCurrentFragment() is TemplateFragment) {
                    requestClearCard()

                    // 若TTS正在播放，则停止
                    val speakPlayerHandler = manager?.getHandler(iFLYOSManager.SPEAKER_HANDLER) as? iFLYOSPlayerHandler
                    (speakPlayerHandler?.mediaPlayer as? MediaPlayerHandler)?.let { player ->
                        if (player.isPlaying()) {
                            player.stop(false)
                        }
                    }

                    // 若闹钟在响，则停止
                    val alertsPlayerHandler = manager?.getHandler(iFLYOSManager.ALERT_HANDLER) as? AlertsPlayerHandler
                    (alertsPlayerHandler?.mediaPlayer as? MediaPlayerHandler)?.let { player ->
                        if (player.isPlaying()) {
                            manager?.executeCommand(iFLYOSManager.Command.CMD_ALERT_STOP)
                        }
                    }
                } else {
                    super.onBackPressed()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        manager?.destroy()

        ToneManager[this].destroy()

        iflyosReceiver.unregister(this)

        pkgChangedReceiver.unregister(this)

        if (Build.VERSION.SDK_INT < 21)
            unregisterReceiver(connectStateReceiver)
        else {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE)
            if (connectivityManager is ConnectivityManager) {
                connectivityManager.unregisterNetworkCallback(networkCallback)
            }
        }

        val speakPlayerHandler = manager?.getHandler(iFLYOSManager.SPEAKER_HANDLER) as? iFLYOSPlayerHandler
        (speakPlayerHandler?.mediaPlayer as? MediaPlayerHandler)
                ?.removeOnMediaStateChangedListener(mediaStateChangedListener)

        val audioPlayerHandler = manager?.getHandler(iFLYOSManager.AUDIO_HANDLER) as? iFLYOSPlayerHandler
        (audioPlayerHandler?.mediaPlayer as? MediaPlayerHandler)
                ?.removeOnMediaStateChangedListener(mediaStateChangedListener)

        val alertsPlayerHandler = manager?.getHandler(iFLYOSManager.ALERT_HANDLER) as? AlertsPlayerHandler
        alertsPlayerHandler?.removeOnAlertStateChangedListener(onAlertStateChangedListener)

        appStateObserver.removeOnStateChangeListener(extVideoAppDirectiveHandler)
    }

    // 更新唤醒界面的模糊背景
    fun updateBlurImage(bitmap: Bitmap?) {
        bitmap?.let {
            Blurry.with(this).radius(75)
                    .color(Color.parseColor("#80000000"))
                    .from(bitmap).into(ivBlur)
        }
    }

    /**
     * iFLYOS 退出登录
     */
    fun logout() {
        manager?.run {
            logoutIvs()
            stopIFLYOS()
        }

        checkNetworkHandler.removeCallbacksAndMessages(null)
    }

    private val connectStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            @Suppress("DEPRECATION")
            when (intent?.action) {
                ConnectivityManager.CONNECTIVITY_ACTION -> {
                    // api 21 以上应使用 networkCallback
                    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

                    if (connectivityManager.activeNetworkInfo.isConnected) {
                        // 若网络恢复可用，触发 iFLYOS 重连
                        val handler = manager?.getHandler(
                                PlatformInterface.SpecialHandler.IFLYOSCLIENT.value()) as? iFLYOSClientHandler
                        if (handler?.connectionStatus == iFLYOSClient.ConnectionStatus.DISCONNECTED ||
                                handler?.connectionStatus == iFLYOSClient.ConnectionStatus.PENDING) {
                            tvTipsSimple?.post {
                                mStatusType = StatusType.RETRYING
                                runOnUiThread {
                                    updateBottomBar()
                                }
                            }
                        }
                    } else {
                        // 更新底部状态栏
                        mStatusType = StatusType.NETWORK_ERROR
                        handleVoiceEnd()
                        updateBottomBar()
                    }
                }
            }
        }
    }

    // 监听音量键按下
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            longPressHandler.sendEmptyMessage(LongPressHandler.VOLUME_DOWN)
            return true
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            longPressHandler.sendEmptyMessage(LongPressHandler.VOLUME_UP)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    // 监听音量键松开
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {
                longPressHandler.sendEmptyMessage(LongPressHandler.VOLUME_RELEASE)
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    private class ClearCardHandler : Handler() {
        private var flag = -1

        fun startCount(runnable: Runnable, delay: Long) {
            Log.d("ClearCardHandler", "startCount")
            flag = UUID.randomUUID().hashCode()
            val msg = Message.obtain()
            msg.what = 1
            msg.arg1 = flag
            msg.obj = runnable
            sendMessageDelayed(msg, delay)
        }

        fun clearCount() {
            Log.d("ClearCardHandler", "clearCount")
            flag = -1
            removeCallbacksAndMessages(null)
        }

        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            when (msg?.what) {
                1 -> {
                    if (flag == msg.arg1) {
                        Log.d("ClearCardHandler", "handleMessage")
                        (msg.obj as? Runnable)?.run()
                    }
                }
            }
        }
    }

    private class LongPressHandler(activity: LauncherActivity) : Handler() {
        val softReference = SoftReference<LauncherActivity>(activity)
        private var isRelease = true

        companion object {
            const val VOLUME_UP = 1
            const val VOLUME_DOWN = 0
            const val VOLUME_RELEASE = 2
            private const val VOLUME_LONG_PRESS = 3

            private const val DELAY_FIRST = 650L // 第一次按下需要延迟，后面才相当于长按
            private const val DELAY_INTERVAL = 200L // 进入长按状态每两次调整音量的时间间隔
        }

        /**
         * 执行减音量
         */
        private fun volumeDown() {
            softReference.get()?.let { activity ->
                val audioManager = activity.getSystemService(Context.AUDIO_SERVICE)
                        as? AudioManager ?: return
                val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val min = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC)
                } else {
                    0
                }
                val step = 100 / (max - min)
                (activity.manager?.getHandler(iFLYOSManager.AUDIO_HANDLER)
                        as? iFLYOSPlayerHandler)?.let { player ->
                    activity.manager?.executeSetVolume(Math.max(0, player.speaker.getVolume() - step))
                }
            }
        }

        /**
         * 执行加音量
         */
        private fun volumeUp() {
            softReference.get()?.let { activity ->
                val audioManager = activity.getSystemService(Context.AUDIO_SERVICE)
                        as? AudioManager ?: return
                val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val min = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC)
                } else {
                    0
                }
                val step = 100 / (max - min)
                (activity.manager?.getHandler(iFLYOSManager.AUDIO_HANDLER)
                        as? iFLYOSPlayerHandler)?.let { player ->
                    activity.manager?.executeSetVolume(Math.min(100, player.speaker.getVolume() + step))
                }
            }
        }

        override fun handleMessage(msg: Message?) {
            val activity = softReference.get()
            activity?.let {
                when (msg?.what) {
                    VOLUME_DOWN -> {
                        isRelease = false
                        volumeDown()

                        val message = Message.obtain()
                        message.what = VOLUME_LONG_PRESS
                        message.arg1 = VOLUME_DOWN
                        sendMessageDelayed(message, DELAY_FIRST)
                    }
                    VOLUME_UP -> {
                        isRelease = false
                        volumeUp()

                        val message = Message.obtain()
                        message.what = VOLUME_LONG_PRESS
                        message.arg1 = VOLUME_UP
                        sendMessageDelayed(message, DELAY_FIRST)
                    }
                    VOLUME_RELEASE -> {
                        isRelease = true
                    }
                    VOLUME_LONG_PRESS -> {
                        if (!isRelease) {
                            when (msg.arg1) {
                                VOLUME_DOWN -> {
                                    volumeDown()
                                }
                                VOLUME_UP -> {
                                    volumeUp()
                                }
                                else -> {

                                }
                            }
                            val message = Message.obtain(msg)
                            sendMessageDelayed(message, DELAY_INTERVAL)
                        } else {
                            // touch released
                        }
                    }
                    else -> {
                        // ignore
                    }
                }
            } ?: run {
                Log.e(sTag, "LongPressHandler activity is null")
            }
        }
    }

}
