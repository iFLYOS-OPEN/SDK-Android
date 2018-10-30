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

package com.iflytek.cyber.iot.show.core.impl.IflyosClient;

import android.content.Context;

import com.iflytek.cyber.iot.show.core.impl.Logger.LoggerHandler;
import com.iflytek.cyber.iot.show.core.impl.SpeechRecognizer.SpeechRecognizerHandler;

import org.json.JSONException;
import org.json.JSONObject;

import cn.iflyos.iace.iflyos.IflyosClient;

public class IflyosClientHandler extends IflyosClient {

    private static final String sTag = "IflyosClient";

    private final Context mContext;
    private final LoggerHandler mLogger;
    private SpeechRecognizerHandler mRecognizer;
    //    private TextView mConnectionText, mAuthText, mDialogText;
    private ConnectionStatus mConnectionStatus = ConnectionStatus.DISCONNECTED;

    public IflyosClientHandler(Context context, LoggerHandler logger) {
        mContext = context;
        mLogger = logger;

        setupGUI();
    }

    public SpeechRecognizerHandler setRecognizer(SpeechRecognizerHandler recognizer) {
        mRecognizer = recognizer;
        return recognizer;
    }

    @Override
    public void dialogStateChanged(final DialogState state) {
        mLogger.postInfo(sTag, "Dialog State Changed. STATE: " + state);
        if (state == null) {
            return;
        }
        JSONObject template = new JSONObject();
        try {
            template.put("state", state.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mLogger.postDisplayCard(template, LoggerHandler.DIALOG_STATE);
//        mActivity.runOnUiThread( new Runnable() {
//            @Override
//            public void run() {
//                mDialogText.setText( state != null ? state.toString() : "" );
//            }
//        });
    }

    @Override
    public void authStateChanged(final AuthState state, final AuthError error) {
        if (error == AuthError.NO_ERROR) {
            mLogger.postInfo(sTag, "Auth State Changed. STATE: " + state);
            if (state == AuthState.REFRESHED) {
                mRecognizer.enableWakewordDetection();
            }
        } else {
            mLogger.postWarn(sTag, String.format("Auth State Changed. STATE: %s, ERROR: %s",
                    state, error));
        }
//        mActivity.runOnUiThread( new Runnable() {
//            @Override
//            public void run() {
//                mAuthText.setText( state != null ? state.toString() : "" );
//            }
//        });
    }

    @Override
    public void connectionStatusChanged(final ConnectionStatus status,
                                        final ConnectionChangedReason reason) {
        mConnectionStatus = status;
        mLogger.postInfo(sTag, String.format("Connection Status Changed. STATUS: %s, REASON: %s",
                status, reason));
        JSONObject template = new JSONObject();
        try {
            template.put("status", status.toString());
            template.put("reason", reason.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mLogger.postDisplayCard(template, LoggerHandler.CONNECTION_STATE);
    }

    @Override
    public void OnIntermediaText(final String dialogRequestId, final String text) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("text", text);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mLogger.postDisplayCard(jsonObject, LoggerHandler.IAT_LOG);
    }

    @Override
    public void OnCustomDirective(final String directive) {
        mLogger.postInfo(sTag, String.format("custom directive: %s", directive));
    }

    public ConnectionStatus getConnectionStatus() {
        return mConnectionStatus;
    }

    private void setupGUI() {
//        mConnectionText = mActivity.findViewById( R.id.connectionState ) ;
//        mAuthText = mActivity.findViewById( R.id.authState );
//        mDialogText = mActivity.findViewById( R.id.dialogState );
//
//        mActivity.runOnUiThread( new Runnable() {
//            @Override
//            public void run() {
//                mConnectionText.setText( ConnectionStatus.DISCONNECTED.toString() );
//                mAuthText.setText( AuthState.UNINITIALIZED.toString() );
//                mDialogText.setText( DialogState.IDLE.toString() );
//            }
//        });
    }
}
