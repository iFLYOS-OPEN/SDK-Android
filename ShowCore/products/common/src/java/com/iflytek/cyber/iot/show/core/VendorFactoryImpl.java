package com.iflytek.cyber.iot.show.core;

import android.content.Context;

import com.iflytek.cyber.platform.Recorder;

class VendorFactoryImpl extends VendorFactory {

    VendorFactoryImpl(Context context) {
        super(context);
    }

    @Override
    Recorder createRecorder(Recorder.AudioListener listener) {
        return new AndroidRecorder(context, listener);
    }

}
