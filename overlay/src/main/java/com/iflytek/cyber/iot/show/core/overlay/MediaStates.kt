package com.iflytek.cyber.iot.show.core.overlay

import com.iflytek.cyber.iot.show.core.overlay.model.iFLYOSConstant

object MediaStates {
    private val mediaStates = HashMap<String, iFLYOSConstant.MediaState>()
    private val listeners = HashSet<OnMediaStateChangedListener>()

    fun setState(playerName: String, sourceId: String, state: iFLYOSConstant.MediaState) {
        listeners.map { it.onMediaStateChanged(playerName, sourceId, mediaStates[playerName], state) }
        mediaStates[playerName] = state
    }

    fun addOnMediaStateChangedListener(listener: OnMediaStateChangedListener) {
        listeners.add(listener)
    }

    fun removeOnMediaStateChangedListener(listener: OnMediaStateChangedListener) {
        listeners.remove(listener)
    }

    fun updatePosition(playerName: String, position: Long) {
        listeners.map { it.onPositionUpdated(playerName, position) }
    }

    fun getState(playerName: String) = mediaStates[playerName]

    interface OnMediaStateChangedListener {
        fun onMediaStateChanged(playerName: String, sourceId: String, preState: iFLYOSConstant.MediaState?, newState: iFLYOSConstant.MediaState)
        fun onPositionUpdated(playerName: String, position: Long)
    }
}