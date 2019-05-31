package com.iflytek.cyber.iot.show.core.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.iflytek.cyber.iot.show.core.overlay.model.ActionConstant
import com.iflytek.cyber.iot.show.core.overlay.model.iFLYOSConstant.AlertState
import com.iflytek.cyber.iot.show.core.overlay.model.iFLYOSConstant.MediaState
import com.iflytek.cyber.iot.show.core.template.*
import com.iflytek.cyber.iot.show.core.template.model.Constant
import kotlinx.android.synthetic.main.activity_template.*
import org.json.JSONObject
import java.util.*
import java.util.concurrent.TimeUnit

class TemplateActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "TemplateActivity"

        const val ACTION_CLOSE = "com.iflytek.cyber.iot.show.core.overlay.action.CLOSE"
        const val ACTION_BLANK = "com.iflytek.cyber.iot.show.core.overlay.action.BLANK"

        const val EXTRA_TEMPLATE = "template"
        const val EXTRA_TIPS = "tips"

        // 避免前一个未显示完又要显示新的 Template 时，Handler 会被重新创建
        private val clearCardHandler = ClearCardHandler()
    }

    private var currentFragment: TemplateFragment? = null
    private val finishedSourceIdMap = HashMap<String, String>()

    private var isBlank = true

    private val receiver = object : iFLYOSReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            super.onReceive(context, intent)
            if (isBlank)
                return
            when (intent.action) {
                ActionConstant.ACTION_CLIENT_CLEAR_TEMPLATE -> {
                    finish()
                }
                ActionConstant.ACTION_CLIENT_ALERT_STATE_CHANGE -> {
                    val alertToken = intent.getStringExtra("alert_token")
                    val stateStr = intent.getStringExtra("state")
                    val reason = intent.getStringExtra("reason")
                    if (!stateStr.isNullOrEmpty()) {
                        onAlertStateChanged(alertToken,
                                AlertState.valueOf(stateStr), reason)
                    }
                }
            }
        }
    }

    private val closeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!isBlank)
                return
            when (intent?.action) {
                ACTION_CLOSE -> {
                    finish()
                }
            }
        }
    }

    private fun onAlertStateChanged(alertToken: String, state: AlertState, reason: String) {
        when (state) {
            AlertState.STOPPED,  // 被本地停止
            AlertState.SNOOZED,  // 打盹
            AlertState.COMPLETED // 闹钟播放完了
            -> {
                val current = currentFragment
                if (current is BodyTemplateFragment || current is BodyTemplate3Fragment) {
                    Log.d(TAG, "闹钟计时")
                    clearCardHandler.startCount(Runnable {
                        val delayCurrent = currentFragment
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

    override fun onCreate(savedInstanceState: Bundle?) {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate")

        if (intent.action == ACTION_BLANK) {
            if (isBlank) {
                setContentView(View(this))
            }
        } else {
            isBlank = false
            setContentView(R.layout.activity_template)

            val tips = intent.getStringExtra(EXTRA_TIPS)
            if (!tips.isNullOrEmpty())
                tips_simple.text = tips

            val template = intent.getStringExtra(EXTRA_TEMPLATE)
            handleTemplate(template)

            MediaStates.addOnMediaStateChangedListener(onMediaStateChangedListener)
        }

        val intentFilter = IntentFilter()
        intentFilter.addAction(ActionConstant.ACTION_CLIENT_MEDIA_POSITION_UPDATED)
        intentFilter.addAction(ActionConstant.ACTION_CLIENT_MEDIA_STATE_CHANGE)
        intentFilter.addAction(ActionConstant.ACTION_CLIENT_ALERT_STATE_CHANGE)
        intentFilter.addAction(ActionConstant.ACTION_CLIENT_DIALOG_STATE_CHANGE)
        intentFilter.addAction(ActionConstant.ACTION_CLIENT_INTER_MEDIA_TEXT)
        intentFilter.addAction(ActionConstant.ACTION_CLIENT_CLEAR_TEMPLATE)
        registerReceiver(receiver, intentFilter)

        val closeFilter = IntentFilter()
        closeFilter.addAction(ACTION_CLOSE)
        registerReceiver(closeReceiver, closeFilter)
    }

    private val onMediaStateChangedListener = object : MediaStates.OnMediaStateChangedListener {
        override fun onMediaStateChanged(playerName: String, sourceId: String, preState: MediaState?, newState: MediaState) {
            Log.v(TAG, "onMediaStateChanged($playerName, $sourceId, $preState, $newState)")
            if (playerName == "SpeakMediaPlayer") {
                val bodyTemplateFragment = currentFragment as? BodyTemplateFragment
                if (newState == MediaState.PLAYING) {
                    bodyTemplateFragment?.onPlayStarted()
                } else if (newState == MediaState.STOPPED
                        || newState == MediaState.FINISH) {
                    bodyTemplateFragment?.onPlayFinished()

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

                    val current = currentFragment
                    if (current is TemplateFragment) {
                        if (current !is OptionTemplateFragment) {
                            clearCardHandler.startCount(Runnable {
                                if (isDestroyed)
                                    return@Runnable
                                val delayCurrent = currentFragment
                                if (current == delayCurrent) {
                                    requestClearCard()
                                }
                            }, TimeUnit.SECONDS.toMillis(5))
                        } else {
                            clearCardHandler.startCount(Runnable {
                                if (isDestroyed)
                                    return@Runnable
                                val delayCurrent = currentFragment
                                if (current == delayCurrent) {
                                    requestClearCard()
                                }
                            }, TimeUnit.MINUTES.toMillis(2))
                        }
                    }
                }
            }
        }

        override fun onPositionUpdated(playerName: String, position: Long) {
            val bodyTemplateFragment = currentFragment as? BodyTemplateFragment
            bodyTemplateFragment?.updatePosition(position)
        }
    }

    private fun handleTemplate(template: String) {
        val templateJson = JSONObject(template)
        try {
            val type = templateJson.optString(Constant.PAYLOAD_TYPE)

            val templateFragment: TemplateFragment? = when (type) {
                Constant.TYPE_BODY_TEMPLATE_1, Constant.TYPE_BODY_TEMPLATE_2 -> {
                    BodyTemplateFragment()
                }
                Constant.TYPE_BODY_TEMPLATE_3 -> {
                    BodyTemplate3Fragment()
                }
                Constant.TYPE_WEATHER_TEMPLATE -> {
                    WeatherFragment()
                }
                Constant.TYPE_LIST_TEMPLATE_1 -> {
                    List1Fragment()
                }
                Constant.TYPE_OPTION_TEMPLATE_1,
                Constant.TYPE_OPTION_TEMPLATE_2,
                Constant.TYPE_OPTION_TEMPLATE_3 -> {
                    OptionTemplateFragment()
                }
                else -> {
                    null
                }
            }
            if (templateFragment != null) {
                currentFragment = templateFragment

                val arguments = Bundle()
                arguments.putString(TemplateFragment.EXTRA_TEMPLATE, template)
                templateFragment.arguments = arguments

                supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment, templateFragment)
                        .commit()

                currentFragment?.registerCallback(templateCallback)
            } else {
                Log.e("TemplateActivity", "Template fragment is null")
                Toast.makeText(this, "无法识别的 template 参数", Toast.LENGTH_SHORT).show()
                finish()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "无法识别的 template 参数", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        clearCardHandler.clearCount()

        intent ?: return

        if (intent.action == ACTION_BLANK) {
            if (isBlank)
                setContentView(View(this))
        } else {
            if (isBlank) {
                isBlank = false
                setContentView(R.layout.activity_template)
            }

            val tips = intent.getStringExtra(EXTRA_TIPS)
            if (!tips.isNullOrEmpty())
                tips_simple.text = tips

            val template = intent.getStringExtra(EXTRA_TEMPLATE)

            currentFragment?.unregisterCallback(templateCallback)

            handleTemplate(template)
        }
    }

    private fun requestStopSpeaking() {
        sendBroadcast(Intent(ActionConstant.ACTION_STOP_SPEAKING))
    }

    private fun requestStopCurrentAlert() {
        val intent = Intent(ActionConstant.ACTION_REQUEST_STOP_CURRENT_ALERT)
        sendBroadcast(intent)
    }

    private fun requestClearCard() {
        val intent = Intent(ActionConstant.ACTION_REQUEST_CLEAR_CARD)
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(receiver)

        unregisterReceiver(closeReceiver)

        MediaStates.removeOnMediaStateChangedListener(onMediaStateChangedListener)
    }

    private val templateCallback = object : TemplateFragment.SimpleTemplateCallback() {
        override fun onResume(fragment: TemplateFragment) {
            super.onResume(fragment)
            if (fragment is BodyTemplateFragment) {
                fragment.onPlayStarted()
            }
            showSimpleTips()
        }

        override fun onBackPressed(fragment: TemplateFragment, template: String) {
            requestStopSpeaking()

            requestClearCard()

            requestStopCurrentAlert()
        }

        override fun onScrollableBodyStopped(fragment: TemplateFragment, template: String) {
            requestStopSpeaking()

            requestStopCurrentAlert()

            clearCardHandler.startCount(Runnable {
                if (currentFragment == fragment) {
                    requestClearCard()
                }
            }, TimeUnit.SECONDS.toMillis(5))
        }

        override fun onSelectElement(fragment: TemplateFragment, token: String, selectedItemToken: String) {
            val broadcast = Intent(ActionConstant.ACTION_TEMPLATE_RUNTIME_SELECT_ELEMENT)
            broadcast.putExtra("token", token)
            broadcast.putExtra("selectedItemToken", selectedItemToken)
            sendBroadcast(broadcast)
        }
    }

    fun showSimpleTips() {
        logo.run {
            if (scaleX == 0f && scaleY == 0f) {
                val scaleAnimation = ScaleAnimation(0f, 1f, 0f, 1f, (width / 2).toFloat(), (height / 2).toFloat())
                scaleAnimation.duration = 200
                scaleAnimation.startOffset = 150
                scaleAnimation.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {
                        scaleX = 1f
                        scaleY = 1f
                    }

                    override fun onAnimationRepeat(animation: Animation?) {

                    }

                    override fun onAnimationEnd(animation: Animation?) {
                        scaleX = 1f
                        scaleY = 1f
                    }


                })
                startAnimation(scaleAnimation)
            }
        }
        logo.isClickable = true

        tips_simple.run {
            animate().alpha(1f).setStartDelay(150).setDuration(200).start()
        }
    }

    override fun onResume() {
        super.onResume()

        if (!isBlank) {
            logo.animate().scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .setStartDelay(150)
                    .start()
            tips_simple.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .setStartDelay(150)
                    .start()
        }
    }

    override fun onPause() {
        super.onPause()

        if (!isBlank) {
            logo.animate()
                    .scaleX(0f)
                    .scaleY(0f)
                    .setDuration(150)
                    .start()
            tips_simple.animate()
                    .alpha(0f)
                    .setDuration(150)
                    .start()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()

        if (!isBlank) {
            requestStopSpeaking()

            requestStopCurrentAlert()

            requestClearCard()
        }
    }

    private class ClearCardHandler : Handler() {
        init {
            Log.v("ClearCardHandler", "${this.hashCode()} create new handler")
        }

        private var flag = -1

        fun startCount(runnable: Runnable, delay: Long) {
            Log.v("ClearCardHandler", "${this.hashCode()} startCount")
            flag = UUID.randomUUID().hashCode()
            val msg = Message.obtain()
            msg.what = 1
            msg.arg1 = flag
            msg.obj = runnable
            sendMessageDelayed(msg, delay)
        }

        fun clearCount() {
            Log.v("ClearCardHandler", "${this.hashCode()} clearCount")
            flag = -1
            removeCallbacksAndMessages(null)
        }

        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            when (msg?.what) {
                1 -> {
                    if (flag == msg.arg1) {
                        Log.v("ClearCardHandler", "${this.hashCode()} handleMessage: $flag")
                        (msg.obj as? Runnable)?.run()
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        requestClearCard()
        currentFragment?.unregisterCallback(templateCallback)
    }
}