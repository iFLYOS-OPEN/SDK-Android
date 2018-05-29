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
