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

package com.iflytek.cyber.platform;

import android.os.SystemClock;

import com.google.gson.JsonObject;

import java.util.HashMap;

import static com.iflytek.cyber.CyberDelegate.CHANNEL_VISUAL;

public class FocusManager {

    private final HashMap<String, Channel> channels = new HashMap<>();

    FocusManager() {
    }

    public void activate(String channel, String intf) {
        final Channel c = new Channel();
        c.intf = intf;
        channels.put(channel, c);
    }

    public void deactivate(String channel, String intf) {
        final Channel c = channels.get(channel);
        if (c == null) {
            return;
        }

        if (!intf.equals(c.intf)) {
            return;
        }

        c.active = false;
        c.lastActive = SystemClock.elapsedRealtime();
    }

    public JsonObject get(String channel) {
        final Channel c = channels.get(channel);
        if (c == null) {
            return null;
        }

        final JsonObject item = new JsonObject();
        item.addProperty("interface", c.intf);

        if (!CHANNEL_VISUAL.equals(channel)) {
            item.addProperty("idleTimeInMilliseconds",
                    c.active ? 0 : SystemClock.elapsedRealtime() - c.lastActive);
        }

        return item;
    }

    private class Channel {
        private String intf;
        private boolean active = false;
        private long lastActive = SystemClock.elapsedRealtime();
    }

}
