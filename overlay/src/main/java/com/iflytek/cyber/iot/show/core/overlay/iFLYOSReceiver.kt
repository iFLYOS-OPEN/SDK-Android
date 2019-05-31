package com.iflytek.cyber.iot.show.core.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.iflytek.cyber.iot.show.core.overlay.model.ActionConstant
import com.iflytek.cyber.iot.show.core.overlay.model.iFLYOSConstant

@Suppress("ClassName")
open class iFLYOSReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ActionConstant.ACTION_CLIENT_MEDIA_STATE_CHANGE -> {
                val state = intent.getStringExtra("state")
                val playerName = intent.getStringExtra("player_name")
                val sourceId = intent.getStringExtra("source_id")
                val stateEnum =
                        try {
                            iFLYOSConstant.MediaState.pickValueOf(state)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }
                if (stateEnum != null && !sourceId.isNullOrEmpty())
                    MediaStates.setState(playerName, sourceId, stateEnum)
            }
            ActionConstant.ACTION_CLIENT_MEDIA_POSITION_UPDATED -> {
                val playerName = intent.getStringExtra("player_name")
                val position = intent.getLongExtra("position", -1L)
                if (position != -1L) {
                    MediaStates.updatePosition(playerName, position)
                }
            }
        }
    }
}