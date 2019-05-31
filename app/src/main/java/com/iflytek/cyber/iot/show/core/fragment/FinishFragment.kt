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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.iflytek.cyber.iot.show.core.R

import java.util.concurrent.TimeUnit

import androidx.navigation.fragment.NavHostFragment

class FinishFragment : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_finish, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.postDelayed({
            clearBackStack()
            NavHostFragment.findNavController(this@FinishFragment).navigate(R.id.main_fragment)
        }, TimeUnit.SECONDS.toMillis(3))

        launcher?.hideSimpleTips()
    }

    private fun clearBackStack() {
        NavHostFragment.findNavController(this).popBackStack(R.id.pair_fragment, true)
        NavHostFragment.findNavController(this).popBackStack(R.id.wifi_fragment, true)
        NavHostFragment.findNavController(this).popBackStack(R.id.welcome_fragment, true)
        NavHostFragment.findNavController(this).popBackStack(R.id.splash_fragment, true)
    }
}
