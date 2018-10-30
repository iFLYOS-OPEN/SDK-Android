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

import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.iflytek.cyber.iot.show.core.R
import com.journeyapps.barcodescanner.BarcodeEncoder

class QRCodeFragment : Fragment() {
    private var codeView: ImageView? = null

    companion object {
        const val EXTRA_VALUE = "value"

        const val sTag = "QRCodeFragment"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_qrcode, container, false)
        codeView = view.findViewById(R.id.code_view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        codeView?.post {
            val value = arguments?.getString(EXTRA_VALUE) ?: ""
            if (!value.isEmpty()) {
                updateQRCode(value)
            }
        }
    }

    fun updateQRCode(value: String) {
        try {
            val defaultSize = resources.getDimensionPixelSize(R.dimen.default_qr_size)
            val bitmap = BarcodeEncoder().encodeBitmap(
                    value, BarcodeFormat.QR_CODE, codeView?.width ?: defaultSize, codeView?.height
                    ?: defaultSize)

            codeView?.setImageBitmap(bitmap)
        } catch (e: WriterException) {
            e.printStackTrace()
            Log.e(sTag, "二维码生成失败。")
        }

    }
}