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

import com.google.gson.JsonObject;

public interface CyberHandler {

    void setEndpoint(String endpoint);

    void stopCapture();

    void expectSpeech(JsonObject initiator);

    void onRecognizeStart(String dialogRequestId);

    void onRecognizeDirective(String dialogRequestId, JsonObject directive);

    void onRecognizeFailure(String dialogRequestId, Throwable e);

    void onRecognizeEnd(String dialogRequestId);

    void onDialogResolved();

    void onEventDirective(JsonObject directive);

    void onEventFailure(Throwable e);

    void onEventEnd();

}
