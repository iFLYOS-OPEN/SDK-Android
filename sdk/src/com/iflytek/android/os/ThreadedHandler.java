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

package com.iflytek.android.os;

import android.os.Handler;
import android.os.HandlerThread;

public class ThreadedHandler extends Handler {

    private final HandlerThread thread;

    public ThreadedHandler(String name) {
        this(createThread(name));
    }

    private ThreadedHandler(HandlerThread thread) {
        super(thread.getLooper());
        this.thread = thread;
    }

    private static HandlerThread createThread(String name) {
        final HandlerThread thread = new HandlerThread(name);
        thread.start();
        return thread;
    }

    public void quit() {
        removeCallbacksAndMessages(null);
        thread.quit();
    }

}
