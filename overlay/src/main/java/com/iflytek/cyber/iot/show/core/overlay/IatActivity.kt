package com.iflytek.cyber.iot.show.core.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import com.iflytek.cyber.iot.show.core.overlay.model.ActionConstant
import com.iflytek.cyber.iot.show.core.overlay.model.iFLYOSConstant
import kotlinx.android.synthetic.main.activity_iat.*

class IatActivity : AppCompatActivity() {
    companion object {
        private const val IAT_OFFSET = 3000L
        private const val TAG = "IatActivity"
    }

    private var hadFinish = false

    private var latestIat = System.currentTimeMillis()

    private var mark = System.currentTimeMillis()

    private var isResume = false

    override fun onCreate(savedInstanceState: Bundle?) {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_iat)

        recognize_view.post {
            val height = fragment_container.height

            logo.setImageResource(R.drawable.ic_voice_bar_regular_white_32dp)

            iat_text.setTextSize(TypedValue.COMPLEX_UNIT_PX, height * 0.047f)

            recognize_view.startEnterAnimation()

            cover.animate()
                    .alpha(1f)
                    .setDuration(500)
                    .setInterpolator(LinearOutSlowInInterpolator())
                    .start()

            iat_logo.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(500)
                    .setInterpolator(LinearOutSlowInInterpolator())
                    .start()
        }
        val cancelDialog = View.OnClickListener {
            finish()

            val intent = Intent(ActionConstant.ACTION_REQUEST_DIALOG_END)
            sendBroadcast(intent)
        }
        iat_logo.setOnClickListener(cancelDialog)
        cover.setOnClickListener(cancelDialog)

        val intentFilter = IntentFilter()
        intentFilter.addAction(ActionConstant.ACTION_CLIENT_DIALOG_STATE_CHANGE)
        intentFilter.addAction(ActionConstant.ACTION_CLIENT_INTER_MEDIA_TEXT)
        intentFilter.addAction(ActionConstant.ACTION_CLIENT_RENDER_PLAYER_INFO)
        intentFilter.addAction(ActionConstant.ACTION_CLIENT_RENDER_TEMPLATE)
        intentFilter.addAction(ActionConstant.ACTION_VOLUME_CHANGE)
        registerReceiver(receiver, intentFilter)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        mark = System.currentTimeMillis()

        recognize_view.releaseRunnable()

        iat_logo.scaleX = 0f
        iat_logo.scaleY = 0f
        iat_logo.post {
            iat_logo.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(500)
                    .setInterpolator(LinearOutSlowInInterpolator())
                    .start()
        }
        iat_text.text = ""
    }

    private fun generateRunnable(markTime: Long) = object : Runnable {
        override fun run() {
            if (hadFinish)
                return
            if (mark != markTime)
                return
            if (System.currentTimeMillis() >= latestIat + IAT_OFFSET) {
                Log.d(TAG, "IAT `generateRunnable()` request finish $mark")

                hadFinish = true
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        isResume = true
    }

    override fun onPause() {
        super.onPause()

        isResume = false

        recognize_view.releaseRunnable()
    }

    override fun onStop() {
        super.onStop()

        finish()
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(receiver)
    }

    override fun onBackPressed() {
        overridePendingTransition(0, 0)
        super.onBackPressed()
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!isResume)
                return
            when (intent?.action) {
                ActionConstant.ACTION_VOLUME_CHANGE -> {
                    val undefineValue = (-1).toDouble()
                    val volume = intent.getDoubleExtra("volume", undefineValue)
                    if (volume != undefineValue) {
                        recognize_view.updateVolume(volume)
                    }
                }
                ActionConstant.ACTION_CLIENT_DIALOG_STATE_CHANGE -> {
                    val lastState =
                            if (intent.getStringExtra("last_state").isNullOrEmpty()) {
                                null
                            } else {
                                iFLYOSConstant.DialogState.valueOf(intent.getStringExtra("last_state"))
                            }
                    val state =
                            if (intent.getStringExtra("state").isNullOrEmpty()) {
                                null
                            } else {
                                iFLYOSConstant.DialogState.valueOf(intent.getStringExtra("state"))
                            }

                    if (state == iFLYOSConstant.DialogState.SPEAKING) {
                        if (!hadFinish) {
                            Log.d(TAG, "SPEAKING request finish $mark")

                            hadFinish = true
                            finish()
                        }
                    }
                }
                ActionConstant.ACTION_CLIENT_RENDER_TEMPLATE,
                ActionConstant.ACTION_CLIENT_RENDER_PLAYER_INFO -> {
                    if (!hadFinish) {
                        Log.d(TAG, "Template request finish $mark")

                        hadFinish = true
                        finish()
                    }
                }
                ActionConstant.ACTION_CLIENT_INTER_MEDIA_TEXT -> {
                    val text = intent.getStringExtra("text")
                    iat_text.text = text

                    iat_text.postDelayed(generateRunnable(mark), IAT_OFFSET)
                }
            }
        }
    }
}
