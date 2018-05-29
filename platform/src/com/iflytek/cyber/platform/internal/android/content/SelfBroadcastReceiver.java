package com.iflytek.cyber.platform.internal.android.content;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;

public abstract class SelfBroadcastReceiver extends BroadcastReceiver {

    private final IntentFilter filter = new IntentFilter();

    private boolean registered = false;

    public SelfBroadcastReceiver(String... actions) {
        for (String action : actions) {
            filter.addAction(action);
        }
    }

    protected abstract void onReceiveAction(String action, Intent intent);

    @Override
    public final void onReceive(Context context, Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (!TextUtils.isEmpty(action)) {
                onReceiveAction(action, intent);
            }
        }
    }

    public void register(Context context) {
        if (!registered) {
            context.registerReceiver(this, filter);
            registered = true;
        }
    }

    public void unregister(Context context) {
        if (registered) {
            registered = false;
            context.unregisterReceiver(this);
        }
    }

}
