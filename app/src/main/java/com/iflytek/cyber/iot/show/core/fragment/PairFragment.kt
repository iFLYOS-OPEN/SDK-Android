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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import cn.iflyos.sdk.android.v3.constant.iFLYOSEvent
import cn.iflyos.sdk.android.v3.iFLYOSManager
import cn.iflyos.sdk.android.v3.iface.iFLYOSClient
import cn.iflyos.sdk.android.v3.ipc.iFLYOSInterface
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.iflytek.cyber.iot.show.core.LauncherActivity
import com.iflytek.cyber.iot.show.core.ObserverListener
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.TimeService
import com.journeyapps.barcodescanner.BarcodeEncoder

/**
 * 用于显示授权二维码
 */
class PairFragment : BaseFragment(), ObserverListener {

    private var activity: LauncherActivity? = null

    private var verificationUri: String? = null
    private var userCode: String? = null

    private var loading: View? = null
    private var failed: View? = null
    private var tips: View? = null
    private var retry: View? = null

    private var code: ImageView? = null

    private var error: TextView? = null

    private var shouldShowTips: Boolean = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as LauncherActivity?

        shouldShowTips = arguments?.getBoolean("shouldShowTips", false) == true
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        container?.removeAllViews()
        return inflater.inflate(R.layout.fragment_pair, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        launcher?.hideSimpleTips()

        view.findViewById<ImageView>(R.id.back)?.let { ivBack ->
            ivBack.setOnClickListener {
                Log.d(TAG, "onClick back")
                it.setOnClickListener(null)
                findNavController().navigateUp()
            }
            ivBack.post {
                val padding = ivBack.height * 12 / 56
                ivBack.setPadding(padding, padding, padding, padding)
                ivBack.setImageResource(R.drawable.ic_previous_white_32dp)
            }
        }

        loading = view.findViewById(R.id.loading)
        failed = view.findViewById(R.id.failed)
        tips = view.findViewById(R.id.tv_tips)

        code = view.findViewById(R.id.code)

        error = view.findViewById(R.id.error)
        retry = view.findViewById(R.id.retry)

        view.findViewById<View>(R.id.retry)
                .setOnClickListener { this@PairFragment.pair() }

        activity?.addObserver(this)

        iFLYOSManager.getInstance().stopIFLYOS()
    }

    override fun onResume() {
        super.onResume()
        activity?.startService(Intent(activity, TimeService::class.java))
        pair()
    }

    private fun pair() {
        failed?.visibility = View.INVISIBLE
        code?.visibility = View.INVISIBLE
        loading?.visibility = View.VISIBLE
        tips?.visibility = View.VISIBLE
        retry?.visibility = View.INVISIBLE

        val manager = iFLYOSManager.getInstance()

        // 请求取消前面的轮询任务
        manager.cancelTask()

        // 请求登录，等待 update() 中获取用于生成二维码的数据
        manager.loginIvs()
    }

    private fun showError(message: String) {
        Log.w(TAG, "showError($message)")
        error?.text = message
        loading?.visibility = View.INVISIBLE
        code?.visibility = View.INVISIBLE
        tips?.visibility = View.INVISIBLE
        failed?.visibility = View.VISIBLE
        retry?.visibility = View.VISIBLE
    }

    private fun navigateFinish() {
        if (null != getActivity()) {
            Navigation.findNavController(getActivity()!!, R.id.fragment).navigate(R.id.action_to_finish_fragment)
        }
    }

    private fun generateCode() {
        if (userCode.isNullOrEmpty() || verificationUri.isNullOrEmpty()) {
            return
        }

        val url = "$verificationUri?user_code=$userCode"

        code?.let { imageView ->
            val size = imageView.height
            try {
                val bitmap = BarcodeEncoder().encodeBitmap(
                        url, BarcodeFormat.QR_CODE, size, size)

                code?.setImageBitmap(bitmap)

                failed?.visibility = View.INVISIBLE
                loading?.visibility = View.INVISIBLE
                code?.visibility = View.VISIBLE
            } catch (e: WriterException) {
                e.printStackTrace()
                showError("二维码生成失败")
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        iFLYOSManager.getInstance().cancelTask()
        if (launcher != null && shouldShowTips) {
            launcher?.showSimpleTips()
        }
    }

    override fun update(event: iFLYOSEvent, arg: String) {
        val params = iFLYOSInterface.unpackParams(arg)
        if (event === iFLYOSEvent.EVENT_CLIENT_AUTH_STATE_CHANGE) {
            val authState = params[1]
            if (authState == iFLYOSClient.AuthState.OK.name) {
                navigateFinish()
            }
        } else if (event === iFLYOSEvent.EVENT_CBL_CODE) {
            this.verificationUri = params[0]
            this.userCode = params[1]
            generateCode()
        } else if (event == iFLYOSEvent.EVENT_CBL_CODE_EXPIRED) {
            showError("二维码已过期")
        }
    }

    companion object {
        private const val TAG = "PairFragment"
    }
}
