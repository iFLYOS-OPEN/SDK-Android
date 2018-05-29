package com.iflytek.cyber.platform.internal.android.app;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public abstract class StickyService extends Service {

    public void onStartCommand(Intent intent) {
    }

    @Override
    public final int onStartCommand(Intent intent, int flags, int startId) {
        onStartCommand(intent);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
