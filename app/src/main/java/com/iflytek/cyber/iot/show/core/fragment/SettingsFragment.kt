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

import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.support.v4.app.DialogFragment
import android.support.v7.widget.AppCompatTextView
import android.text.TextUtils
import android.util.TypedValue
import android.view.*
import android.widget.ImageView
import android.widget.SeekBar
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.iflytek.cyber.iot.show.core.EngineService
import com.iflytek.cyber.iot.show.core.LauncherActivity
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.impl.MediaPlayer.MediaPlayerHandler
import com.iflytek.cyber.iot.show.core.impl.MediaPlayer.MediaPlayerHandler.SpeakerHandler.*
import com.iflytek.cyber.iot.show.core.utils.WifiInfoManager

class SettingsFragment : DialogFragment(), View.OnClickListener {
    private var seekBarBrightness: SeekBar? = null
    private var seekBarVolume: SeekBar? = null
    private var ivVolume: ImageView? = null
    private var ivBrightness: ImageView? = null
    private var ivNetwork: ImageView? = null
    private var wifiName: AppCompatTextView? = null

    private var launcher: LauncherActivity? = null

    private var mute = false

    private val volumeBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {

        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            val intent = Intent(context, EngineService::class.java)
            intent.action = EngineService.ACTION_UPDATE_VOLUME
            intent.putExtra(EXTRA_VOLUME, seekBar.progress.toByte())
            context?.startService(intent)
        }
    }

    override fun dismiss() {
        dialog?.let {
            if (it.isShowing)
                super.dismiss()
        }
    }

    private val brightnessBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            val max = seekBar.max
            when {
                progress > max * 0.677 ->
                    ivBrightness?.setImageResource(R.drawable.ic_brightness_high_white_24dp)
                progress > max * 0.333 ->
                    ivBrightness?.setImageResource(R.drawable.ic_brightness_medium_white_24dp)
                else ->
                    ivBrightness?.setImageResource(R.drawable.ic_brightness_low_white_24dp)
            }
            if (!fromUser)
                return
            val activity = activity ?: return
            val contentResolver = activity.contentResolver
            try {
                if (Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                    Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                }
            } catch (e: Settings.SettingNotFoundException) {
                e.printStackTrace()
            }

            Settings.System.putInt(contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS, progress)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {

        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {

        }
    }

    private val volumeChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent == null || TextUtils.isEmpty(intent.action))
                return
            when (intent.action) {
                ACTION_VOLUME_CHANGED -> {
                    val volume = intent.getByteExtra(EXTRA_VOLUME, 50)
                    seekBarVolume?.progress = volume.toInt()
                }
                ACTION_MUTE_CHANGED -> {
                    mute = intent.getBooleanExtra(MediaPlayerHandler.SpeakerHandler.EXTRA_MUTE, false)
                    if (mute) {
                        ivVolume?.setImageResource(R.drawable.ic_volume_off_white_24dp)
                    } else {
                        ivVolume?.setImageResource(R.drawable.ic_volume_up_white_24dp)
                    }
                }
            }
        }
    }

    private val brightnessObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean, uri: Uri) {
            val context = context ?: return
            val contentResolver = context.contentResolver
            if (BRIGHTNESS_MODE_URI == uri) {
                var mode = 0
                try {
                    mode = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE)
                } catch (e: Settings.SettingNotFoundException) {
                    e.printStackTrace()
                }

                if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                    ivBrightness?.setImageResource(R.drawable.ic_brightness_auto_white_24dp)
                } else {
                    var currentBrightness = 0
                    val max = seekBarBrightness?.max ?: 0
                    try {
                        currentBrightness = Settings.System.getInt(contentResolver,
                                Settings.System.SCREEN_BRIGHTNESS)
                    } catch (e: Settings.SettingNotFoundException) {
                        e.printStackTrace()
                    }

                    when {
                        currentBrightness > max * 0.677 ->
                            ivBrightness?.setImageResource(R.drawable.ic_brightness_high_white_24dp)
                        currentBrightness > max * 0.333 ->
                            ivBrightness?.setImageResource(R.drawable.ic_brightness_medium_white_24dp)
                        else ->
                            ivBrightness?.setImageResource(R.drawable.ic_brightness_low_white_24dp)
                    }
                }
            } else if (BRIGHTNESS_URI == uri) {
                var currentBrightness = 0
                try {
                    currentBrightness = Settings.System.getInt(contentResolver,
                            Settings.System.SCREEN_BRIGHTNESS)
                } catch (e: Settings.SettingNotFoundException) {
                    e.printStackTrace()
                }

                seekBarBrightness?.progress = currentBrightness
            }
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is LauncherActivity) {
            launcher = context
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.layout_settings, container, false)
        seekBarBrightness = view.findViewById(R.id.brightness_bar)
        seekBarBrightness?.max = 255
        seekBarBrightness?.setOnSeekBarChangeListener(brightnessBarChangeListener)
        seekBarVolume = view.findViewById(R.id.volume_bar)
        seekBarVolume!!.setOnSeekBarChangeListener(volumeBarChangeListener)
        ivVolume = view.findViewById(R.id.iv_volume)
        ivBrightness = view.findViewById(R.id.iv_brightness)
        ivVolume?.setOnClickListener(this)
        ivBrightness?.setOnClickListener(this)
        ivNetwork = view.findViewById(R.id.network)
        wifiName = view.findViewById(R.id.wifi_name)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val wifiContent = view.findViewById<View>(R.id.wifi_content)
        wifiContent.setOnClickListener {
            dismiss()
            val navOptions = NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_top)
                    .setPopExitAnim(R.anim.slide_page_pop_exit)
                    .build()
            val arguments = Bundle()
            arguments.putBoolean("RESET_WIFI", true)
            NavHostFragment.findNavController(this@SettingsFragment)
                    .navigate(R.id.wifi_fragment, arguments, navOptions)
        }
        view.findViewById<View>(R.id.about).setOnClickListener {
            dismiss()
            val navOptions = NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_top)
                    .setPopExitAnim(R.anim.slide_page_pop_exit)
                    .build()
            NavHostFragment.findNavController(this@SettingsFragment)
                    .navigate(R.id.about_fragment, null, navOptions)
        }
        view.findViewById<View>(R.id.empty_content).setOnClickListener { this@SettingsFragment.dismiss() }
        seekBarBrightness?.progressDrawable?.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
        seekBarBrightness?.thumb?.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
        seekBarVolume?.progressDrawable?.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
        seekBarVolume?.thumb?.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
        seekBarVolume?.max = 100
        launcher?.mEngineService?.let { engineService ->
            val volume = engineService.speakerVolume()
            mute = engineService.speakerMute() == true

            seekBarVolume?.progress = volume?.toInt() ?: 50
            ivVolume?.setImageResource(
                    if (mute)
                        R.drawable.ic_volume_off_white_24dp
                    else
                        R.drawable.ic_volume_up_white_24dp)
        }
        view.post {
            val height = view.height

            wifiName?.setTextSize(TypedValue.COMPLEX_UNIT_PX, height * .033f)
            wifiName?.lineHeight = (height * .047).toInt()

            val about = view.findViewById<AppCompatTextView>(R.id.tv_about)
            about?.setTextSize(TypedValue.COMPLEX_UNIT_PX, height * .033f)
            about?.lineHeight = (height * .047).toInt()
        }
        super.onViewCreated(view, savedInstanceState)

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        WifiInfoManager.getManager().registerWifiRssiCallback(context) { this@SettingsFragment.updateNetworkRssi() }
    }

    private fun updateWifiName() {
        val context = context ?: return
        val info = WifiInfoManager.getManager().getWifiInfo(context)
        if (info != null) {
            val ssid = info.ssid
            val name = ssid.substring(1, ssid.length - 1)
            wifiName!!.text = name
        }
    }

    private fun updateNetworkRssi() {
        val context = context ?: return
        updateWifiName()
        val level = WifiInfoManager.getManager().getWifiSignalLevel(context)
        when (level) {
            0 -> ivNetwork?.setImageResource(R.drawable.ic_signal_wifi_0_bar_white_24dp)
            1 -> ivNetwork?.setImageResource(R.drawable.ic_signal_wifi_1_bar_white_24dp)
            2 -> ivNetwork?.setImageResource(R.drawable.ic_signal_wifi_2_bar_white_24dp)
            3 -> ivNetwork?.setImageResource(R.drawable.ic_signal_wifi_3_bar_white_24dp)
            4 -> ivNetwork?.setImageResource(R.drawable.ic_signal_wifi_4_bar_white_24dp)
            else -> ivNetwork?.setImageResource(R.drawable.ic_baseline_wifi_error_24px)
        }
    }

    override fun onResume() {
        super.onResume()

        updateWifiName()

        if (null != dialog && null != dialog.window) {
            dialog.window!!.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar

                    or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar

                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }

        val context = context ?: return

        try {
            val contentResolver = context.contentResolver
            val currentBrightness = Settings.System.getInt(context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS)
            seekBarBrightness?.progress = currentBrightness

            val maxBrightness = seekBarBrightness?.max ?: 0
            when {
                currentBrightness > maxBrightness * 0.677 ->
                    ivBrightness?.setImageResource(R.drawable.ic_brightness_high_white_24dp)
                currentBrightness > maxBrightness * 0.333 ->
                    ivBrightness?.setImageResource(R.drawable.ic_brightness_medium_white_24dp)
                else ->
                    ivBrightness?.setImageResource(R.drawable.ic_brightness_low_white_24dp)
            }
            var mode = 0
            try {
                mode = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE)
            } catch (e: Settings.SettingNotFoundException) {
                e.printStackTrace()
            }

            if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                seekBarBrightness?.isEnabled = false
                ivBrightness?.setImageResource(R.drawable.ic_brightness_auto_white_24dp)
            } else {
                seekBarBrightness?.isEnabled = true
            }

        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
        }

    }

    override fun onStart() {
        super.onStart()

        val dialog = dialog
        if (dialog != null) {
            if (dialog.window != null) {
                val window = dialog.window
                window!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                window.setGravity(Gravity.TOP)
                window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                window.setWindowAnimations(R.style.SettingsAnimation)
            }
        }

        val activity = activity ?: return
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_VOLUME_CHANGED)
        intentFilter.addAction(ACTION_MUTE_CHANGED)
        activity.registerReceiver(volumeChangeReceiver, intentFilter)

        try {
            val cr = activity.contentResolver
            cr.registerContentObserver(BRIGHTNESS_MODE_URI, false, brightnessObserver)
            cr.registerContentObserver(BRIGHTNESS_URI, false, brightnessObserver)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun onDestroy() {
        super.onDestroy()

        val activity = activity ?: return

        activity.unregisterReceiver(volumeChangeReceiver)

        activity.contentResolver.unregisterContentObserver(brightnessObserver)
    }

    override fun onClick(v: View) {
        val context = context ?: return
        when (v.id) {
            R.id.iv_volume -> {
                val intent = Intent(context, EngineService::class.java)
                intent.action = EngineService.ACTION_UPDATE_MUTE
                intent.putExtra(EngineService.EXTRA_MUTE, !mute)
                context.startService(intent)
            }
            R.id.iv_brightness -> {
                val contentResolver = context.contentResolver
                var mode = 0
                try {
                    mode = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE)
                } catch (e: Settings.SettingNotFoundException) {
                    e.printStackTrace()
                }

                if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                    Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                    val currentBrightness = seekBarBrightness?.progress ?: 0

                    val maxBrightness = seekBarBrightness?.max ?: 0
                    when {
                        currentBrightness > maxBrightness * 0.677 ->
                            ivBrightness?.setImageResource(R.drawable.ic_brightness_high_white_24dp)
                        currentBrightness > maxBrightness * 0.333 ->
                            ivBrightness?.setImageResource(R.drawable.ic_brightness_medium_white_24dp)
                        else -> ivBrightness?.setImageResource(R.drawable.ic_brightness_low_white_24dp)
                    }
                    seekBarBrightness?.isEnabled = true
                } else {
                    Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                            Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)
                    ivBrightness?.setImageResource(R.drawable.ic_brightness_auto_white_24dp)
                    seekBarBrightness?.isEnabled = false
                }
            }
        }
    }

    companion object {

        private val BRIGHTNESS_MODE_URI = Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE)
        private val BRIGHTNESS_URI = Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS)
        private const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
    }
}
