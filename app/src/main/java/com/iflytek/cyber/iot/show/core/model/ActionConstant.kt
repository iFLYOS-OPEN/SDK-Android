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


package com.iflytek.cyber.iot.show.core.model

object ActionConstant {
    private const val ACTION_PREFIX = "com.iflytek.cyber.iot.show.core.action"
    const val ACTION_VOLUME_CHANGE = "$ACTION_PREFIX.VOLUME_CHANGE"
    const val ACTION_CLIENT_DIALOG_STATE_CHANGE = "$ACTION_PREFIX.CLIENT_DIALOG_STATE_CHANGE"
    const val ACTION_CLIENT_MEDIA_STATE_CHANGE = "$ACTION_PREFIX.CLIENT_MEDIA_STATE_CHANGE"
    const val ACTION_CLIENT_ALERT_STATE_CHANGE = "$ACTION_PREFIX.CLIENT_ALERT_STATE_CHANGE"
    const val ACTION_CLIENT_MEDIA_POSITION_UPDATED = "$ACTION_PREFIX.CLIENT_MEDIA_POSITION_UPDATED"
    const val ACTION_CLIENT_INTER_MEDIA_TEXT = "$ACTION_PREFIX.CLIENT_INTER_MEDIA_TEXT"
    const val ACTION_CLIENT_RENDER_TEMPLATE = "$ACTION_PREFIX.CLIENT_RENDER_TEMPLATE"
    const val ACTION_CLIENT_RENDER_PLAYER_INFO = "$ACTION_PREFIX.CLIENT_RENDER_PLAYER_INFO"
    const val ACTION_CLIENT_CLEAR_TEMPLATE = "$ACTION_PREFIX.CLIENT_CLEAR_TEMPLATE"

    const val ACTION_STOP_SPEAKING = "$ACTION_PREFIX.STOP_SPEAKING"
    const val ACTION_REQUEST_CLEAR_CARD = "$ACTION_PREFIX.REQUEST_CLEAR_CARD"
    const val ACTION_REQUEST_STOP_CURRENT_ALERT = "$ACTION_PREFIX.REQUEST_STOP_CURRENT_ALERT"
    const val ACTION_REQUEST_DIALOG_END = "$ACTION_PREFIX.REQUEST_DIALOG_END"
    const val ACTION_PLAY_WAKE_SOUND = "$ACTION_PREFIX.PLAY_WAKE_SOUND"
    const val ACTION_TEMPLATE_RUNTIME_SELECT_ELEMENT = "$ACTION_PREFIX.TEMPLATE_RUNTIME_SELECT_ELEMENT"
}