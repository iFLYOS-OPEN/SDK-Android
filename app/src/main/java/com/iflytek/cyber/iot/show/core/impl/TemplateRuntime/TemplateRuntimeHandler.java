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

package com.iflytek.cyber.iot.show.core.impl.TemplateRuntime;

import android.support.annotation.Nullable;
import android.util.Log;

import com.iflytek.cyber.iot.show.core.impl.Logger.LoggerHandler;
import com.iflytek.cyber.iot.show.core.impl.PlaybackController.PlaybackControllerHandler;

import org.json.JSONException;
import org.json.JSONObject;

import cn.iflyos.iace.iflyos.TemplateRuntime;

public class TemplateRuntimeHandler extends TemplateRuntime {

    private static final String sTag = "TemplateRuntime";

    private final LoggerHandler mLogger;
    private final PlaybackControllerHandler mPlaybackController;
    private String mCurrentAudioItemId;

    public TemplateRuntimeHandler(LoggerHandler logger,
                                  @Nullable PlaybackControllerHandler playbackController) {
        mLogger = logger;
        mPlaybackController = playbackController;
    }

    @Override
    public void renderTemplate(String payload) {
        try {
            // Log payload
            JSONObject template = new JSONObject(payload);
            mLogger.postInfo(sTag, template.toString(4));

            // Log card
            String type = template.getString("type");
            Log.w("Logger", template.toString());
            switch (type) {
                case "BodyTemplate1":
                    mLogger.postDisplayCard(template, LoggerHandler.BODY_TEMPLATE1);
                    break;
                case "BodyTemplate2":
                    mLogger.postDisplayCard(template, LoggerHandler.BODY_TEMPLATE2);
                    break;
                case "BodyTemplate3":
                    mLogger.postDisplayCard(template, LoggerHandler.BODY_TEMPLATE3);
                    break;
                case "ListTemplate1":
                    mLogger.postDisplayCard(template, LoggerHandler.LIST_TEMPLATE1);
                    break;
                case "WeatherTemplate":
                    mLogger.postDisplayCard(template, LoggerHandler.WEATHER_TEMPLATE);
                    break;
                case "LocalSearchListTemplate1":
                    mLogger.postDisplayCard(template, LoggerHandler.LOCAL_SEARCH_LIST_TEMPLATE1);
                    break;
                default:
                    break;
            }
        } catch (JSONException e) {
            mLogger.postError(sTag, e.getMessage());
        }
    }

    @Override
    public void renderPlayerInfo(String payload) {

        try {
            JSONObject playerInfo = new JSONObject(payload);
            String audioItemId = playerInfo.getString("audioItemId");

            // Update playback controller buttons and player info labels
           /* if (mPlaybackController != null) {
                JSONArray controls = playerInfo.getJSONArray("controls");
                for (int j = 0; j < controls.length(); j++) {
                    JSONObject control = controls.getJSONObject(j);
                    if (control.getString("type").equals("BUTTON")) {
                        final boolean enabled = control.getBoolean("enabled");
                        final String name = control.getString("name");
                        mPlaybackController.updateControlButton(name, enabled);
                    }
                }

                JSONObject content = playerInfo.getJSONObject("content");
                String title = content.has("title") ? content.getString("title") : "";
                String artist = content.has("titleSubtext1") ? content.getString("titleSubtext1") : "";
                JSONObject provider = content.getJSONObject("provider");
                String name = provider.has("name") ? provider.getString("name") : "";
                mPlaybackController.setPlayerInfo(title, artist, name);
            }*/

            // Log only if audio item has changed
            if (!audioItemId.equals(mCurrentAudioItemId)) {
                mCurrentAudioItemId = audioItemId;

                // Log payload
                mLogger.postInfo(sTag, playerInfo.toString(4));

                // Log card
                JSONObject content = playerInfo.getJSONObject("content");
                mLogger.postDisplayCard(content, LoggerHandler.RENDER_PLAYER_INFO);

            }
        } catch (JSONException e) {
            mLogger.postError(sTag, e.getMessage());
        }
    }

    @Override
    public void clearTemplate() {
        // Handle dismissing display card here
        mLogger.postInfo(sTag, "handle clearTemplate()");
    }

    @Override
    public void clearPlayerInfo() {
        // Handle clearing player info here
        mLogger.postInfo(sTag, "handle clearPlayerInfo()");
        if (mPlaybackController != null) mPlaybackController.setPlayerInfo("", "", "");
    }
}
