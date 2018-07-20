package com.iflytek.cyber.resolver.speaker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.text.TextUtils;

/**
 * Receive action while volume changing by physical button
 */
class VolumeChangeReceiver extends BroadcastReceiver {

    static final String STREAM_MUTE_CHANGED_ACTION = "android.media.STREAM_MUTE_CHANGED_ACTION";
    static final String VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION";

    private static final String EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE";

    private final SpeakerResolver resolver;

    VolumeChangeReceiver(SpeakerResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || TextUtils.isEmpty(intent.getAction())) {
            return;
        }

        switch (intent.getAction()) {
            case VOLUME_CHANGED_ACTION: {
                int type = intent.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, -1);
                if (type == AudioManager.STREAM_MUSIC) {
                    resolver.onVolumeChanged();
                }
            }
            break;
            case STREAM_MUTE_CHANGED_ACTION: {
                int type = intent.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, -1);
                if (type == AudioManager.STREAM_MUSIC) {
                    resolver.onMuteChanged();
                }
            }
            break;
        }
    }

}
