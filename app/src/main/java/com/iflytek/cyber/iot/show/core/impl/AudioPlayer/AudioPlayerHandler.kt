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

package com.iflytek.cyber.iot.show.core.impl.AudioPlayer

import com.iflytek.cyber.iot.show.core.impl.MediaPlayer.MediaPlayerHandler

import cn.iflyos.iace.iflyos.AudioPlayer
import cn.iflyos.iace.iflyos.MediaPlayer
import cn.iflyos.iace.iflyos.Speaker

class AudioPlayerHandler(mediaPlayer: MediaPlayer, speaker: Speaker) : AudioPlayer(mediaPlayer, speaker) {

    constructor(mediaPlayer: MediaPlayerHandler) : this(mediaPlayer, mediaPlayer.speaker)
}
