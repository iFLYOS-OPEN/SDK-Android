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

import android.os.Handler;
import android.os.Looper;
import android.util.ArrayMap;
import android.util.Log;

import com.iflytek.android.io.CloseableUtil;
import com.iflytek.cyber.CyberDelegate;

import java.util.concurrent.TimeUnit;

import okio.BufferedSource;

public class AttachmentManager {

    private static final String TAG = "AttachmentManager";

    private static final long ATTACHMENT_LIFETIME = TimeUnit.SECONDS.toMillis(30);

    private final Object lock = new Object();

    private final Handler cleanupHandler = new Handler(Looper.getMainLooper());
    private final ArrayMap<String, AttachmentRequest> attachments = new ArrayMap<>();

    public void query(String cid, CyberDelegate.AttachmentCallback callback) {
        synchronized (lock) {
            AttachmentRequest request = attachments.get(cid);
            if (request == null) {
                request = new AttachmentRequest(cid);
                attachments.put(cid, request);
            }

            request.requested(callback);
        }
    }

    public void ready(String cid, BufferedSource source) {
        synchronized (lock) {
            AttachmentRequest request = attachments.get(cid);
            if (request == null) {
                request = new AttachmentRequest(cid);
                attachments.put(cid, request);
            }

            request.available(source);
        }
    }

    public void destroy() {
        synchronized (lock) {
            for (AttachmentRequest request : attachments.values()) {
                request.cancel();
            }
        }

        cleanupHandler.removeCallbacksAndMessages(null);
        attachments.clear();
    }

    private class AttachmentRequest {

        private final String cid;
        private final Runnable onTimeout;

        private CyberDelegate.AttachmentCallback callback = null;
        private BufferedSource stream = null;

        private AttachmentRequest(String cid) {
            this.cid = cid;
            this.onTimeout = this::cancel;

            cleanupHandler.postDelayed(this.onTimeout, ATTACHMENT_LIFETIME);
        }

        private void requested(CyberDelegate.AttachmentCallback callback) {
            Log.d(TAG, "Requested " + this);
            this.callback = callback;
            ready();
        }

        private void available(BufferedSource stream) {
            Log.d(TAG, "Available " + this);
            this.stream = stream;
            ready();
        }

        private void ready() {
            if (callback == null || stream == null) {
                return;
            }

            callback.onAttachment(cid, stream);

            cleanup();
        }

        private void cancel() {
            Log.d(TAG, "Canceling " + this);
            if (callback != null) {
                callback.onNotFound(cid);
            }

            CloseableUtil.safeClose(stream);
            cleanup();
        }

        private void cleanup() {
            Log.d(TAG, "Cleaning up " + this);
            cleanupHandler.removeCallbacks(this.onTimeout);
            synchronized (lock) {
                attachments.remove(cid);
            }
        }

        @Override
        public String toString() {
            return String.format("Attachment[cid=%s]", cid);
        }

    }

}
