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

@file:Suppress("DEPRECATION")

package com.iflytek.cyber.iot.show.core.fragment

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.NavHostFragment
import cn.iflyos.sdk.android.v3.iFLYOSManager
import com.iflytek.cyber.iot.show.core.BuildConfig
import com.iflytek.cyber.iot.show.core.LauncherActivity
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.SelfBroadcastReceiver
import com.iflytek.cyber.iot.show.core.impl.SpeechRecognizer.SpeechRecognizerHandler
import com.iflytek.cyber.iot.show.core.model.ContentStorage
import com.iflytek.cyber.product.ota.OtaService

class AboutFragment : BaseFragment() {
    private var changeBinding = false
    private var progressDialog: ProgressDialog? = null
    private var tipsDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        receiver.register(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_about, container, false)
        view.findViewById<View>(R.id.close).setOnClickListener { NavHostFragment.findNavController(this@AboutFragment).navigateUp() }
        view.findViewById<View>(R.id.change_binding).setOnClickListener {
            val context = context
            if (context != null)
                AlertDialog.Builder(context)
                        .setTitle("更改绑定")
                        .setMessage("是否确定更改绑定")
                        .setPositiveButton(android.R.string.yes) { _, _ ->
                            changeBinding()
                        }
                        .setNegativeButton(android.R.string.no, null)
                        .show()
        }
        val spinnerValues = arrayOf(SpeechRecognizerHandler.PREF_VALUE_NEAR_FIELD,
                SpeechRecognizerHandler.PREF_VALUE_FAR_FIELD)
        val spinnerSummaries = arrayOf(getString(R.string.message_near_field),
                getString(R.string.message_far_field))

        val current = (iFLYOSManager.getInstance().speechRecognizer
                as? SpeechRecognizerHandler)?.let { speechRecognizerHandler ->
            val profile = speechRecognizerHandler.getProfile()
            val index = spinnerValues.indexOf(profile)
            if (index >= 0 && index < spinnerValues.size) {
                return@let index
            } else {
                return@let 0
            }
        } ?: 0
        view.findViewById<TextView>(R.id.tv_recognize_profile_summary)?.text =
                spinnerSummaries[current]
        view.findViewById<View>(R.id.change_recognize_profile).setOnClickListener {
            val index = (iFLYOSManager.getInstance().speechRecognizer
                    as? SpeechRecognizerHandler)?.let { speechRecognizerHandler ->
                val profile = speechRecognizerHandler.getProfile()
                val index = spinnerValues.indexOf(profile)
                if (index >= 0 && index < spinnerValues.size) {
                    return@let index
                } else {
                    return@let 0
                }
            } ?: 0
            AlertDialog.Builder(it.context)
                    .setTitle(R.string.recognize_profile)
                    .setSingleChoiceItems(spinnerSummaries, index) { dialog, position ->
                        (iFLYOSManager.getInstance().speechRecognizer
                                as? SpeechRecognizerHandler)?.setProfile(spinnerValues[position])
                        view.findViewById<TextView>(R.id.tv_recognize_profile_summary)?.text =
                                spinnerSummaries[position]
                        dialog.dismiss()
                    }
                    .show()
        }
        view.findViewById<View>(R.id.check_update).setOnClickListener {
            val startCheck = Intent(it.context, OtaService::class.java)
            startCheck.action = OtaService.ACTION_REQUEST_CHECKING
            it.context.startService(startCheck)

            progressDialog = ProgressDialog.show(it.context, "检查更新", "正在检查更新", false, false)
        }
        val textView = view.findViewById<TextView>(R.id.system_version)
        textView.text = BuildConfig.VERSION_NAME
        return view
    }

    private val receiver = object : SelfBroadcastReceiver(
            OtaService.ACTION_NEW_UPDATE_DOWNLOAD_STARTED,
            OtaService.ACTION_NEW_UPDATE_DOWNLOADED,
            OtaService.ACTION_CHECK_UPDATE_FAILED,
            OtaService.ACTION_NO_UPDATE_FOUND) {
        override fun onReceiveAction(action: String, intent: Intent) {
            when (action) {
                OtaService.ACTION_NEW_UPDATE_DOWNLOADED -> {
                    progressDialog?.dismiss()
                }
                OtaService.ACTION_NEW_UPDATE_DOWNLOAD_STARTED -> {
                    progressDialog?.dismiss()
                    progressDialog = ProgressDialog.show(context, "检查更新", "正在下载更新")
                }
                OtaService.ACTION_NO_UPDATE_FOUND -> {
                    progressDialog?.dismiss()
                    context?.let { context ->
                        tipsDialog?.dismiss()
                        tipsDialog = AlertDialog.Builder(context)
                                .setTitle("当前已是最新版本")
                                .setPositiveButton(android.R.string.yes, null)
                                .setOnDismissListener {
                                    if (it == tipsDialog) {
                                        tipsDialog = null
                                    }
                                }
                                .show()
                    }
                }
                OtaService.ACTION_CHECK_UPDATE_FAILED -> {
                    progressDialog?.dismiss()
                }
            }
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        launcher?.hideSimpleTips()

        val ivClose = view.findViewById<ImageView>(R.id.close)
        ivClose.post {
            val padding = ivClose.height * 12 / 56
            ivClose.setPadding(padding, padding, padding, padding)
        }
    }

    private fun changeBinding() {
        changeBinding = true

        ContentStorage.get().saveContent(null)

        //退出登录，销毁资源
        (activity as? LauncherActivity)?.logout()

        clearBackStack()
        NavHostFragment.findNavController(this).navigate(R.id.welcome_fragment)
    }

    private fun clearBackStack() {
        NavHostFragment.findNavController(this).popBackStack(R.id.main_fragment, true)
        NavHostFragment.findNavController(this).popBackStack(R.id.splash_fragment, true)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!changeBinding)
            launcher?.showSimpleTips()
        receiver.unregister(context)
    }
}
