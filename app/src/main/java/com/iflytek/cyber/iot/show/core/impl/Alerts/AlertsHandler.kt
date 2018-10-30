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

package com.iflytek.cyber.iot.show.core.impl.Alerts

import android.content.Context
import cn.iflyos.iace.iflyos.Alerts
import cn.iflyos.iace.iflyos.MediaPlayer
import cn.iflyos.iace.iflyos.Speaker
import com.iflytek.cyber.iot.show.core.impl.Logger.LoggerHandler
import com.iflytek.cyber.iot.show.core.impl.MediaPlayer.MediaPlayerHandler

class AlertsHandler(private val mContext: Context,
                    private val mLogger: LoggerHandler,
                    mediaPlayer: MediaPlayer,
                    speaker: Speaker) : Alerts(mediaPlayer, speaker) {
    var currentState: Alerts.AlertState? = Alerts.AlertState.STOPPED

    constructor(context: Context,
                logger: LoggerHandler,
                mediaPlayer: MediaPlayerHandler) : this(context, logger, mediaPlayer, mediaPlayer.speaker)

    override fun alertStateChanged(alertToken: String?,
                                   state: Alerts.AlertState?,
                                   reason: String?) {
        mLogger.postInfo(sTag, String.format("Alert State Changed. STATE: %s, REASON: %s",
                state, reason))
        currentState = state
    }

    fun onLocalStop() {
        mLogger.postInfo(sTag, "Stopping active alert")
        super.localStop()
    }

    fun onRemoveAllAlerts() {
        mLogger.postInfo(sTag, "Removing all pending alerts from storage")
        super.removeAllAlerts()
    }

    companion object {

        private const val sTag = "Alerts"
    }
}
