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

package com.iflytek.cyber.resolver.speaker;

import android.media.AudioManager;
import android.os.Build;

class AudioManagerCompat {

    static int getStreamVolumePercent(AudioManager am, int streamType) {
        final float max = am.getStreamMaxVolume(streamType);
        final float volume = am.getStreamVolume(streamType);
        return (int) ((volume / max) * 100f);
    }

    static void setStreamVolumePercent(AudioManager am, int streamType, int volume) {
        final int max = am.getStreamMaxVolume(streamType);
        final int index = (int) ((float) max * ((float) volume / 100f));
        am.setStreamVolume(streamType, Math.min(Math.max(0, index), max), 0);
    }

    static void adjustStreamVolumePercent(AudioManager am, int streamType, int volume) {
        final int current = getStreamVolumePercent(am, streamType);
        setStreamVolumePercent(am, streamType, current + volume);
    }

    static boolean isStreamMute(AudioManager am, int streamType) {
        if (Build.VERSION.SDK_INT >= 23) {
            return am.isStreamMute(streamType);
        } else {
            return am.getStreamVolume(streamType) == 0;
        }
    }

    static void setStreamMute(AudioManager am, int streamType, boolean state) {
        if (Build.VERSION.SDK_INT < 23) {
            am.setStreamMute(streamType, state);
        } else {
            am.adjustStreamVolume(streamType, state
                    ? AudioManager.ADJUST_MUTE
                    : AudioManager.ADJUST_UNMUTE, 0);
        }
    }

}
