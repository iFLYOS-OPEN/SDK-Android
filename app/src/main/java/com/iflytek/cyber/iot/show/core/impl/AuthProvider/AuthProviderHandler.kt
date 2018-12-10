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

package com.iflytek.cyber.iot.show.core.impl.AuthProvider

import android.content.Context
import android.content.SharedPreferences
import cn.iflyos.iace.iflyos.AuthProvider
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.impl.Logger.LoggerHandler
import org.json.JSONException
import org.json.JSONObject

class AuthProviderHandler(private val mContext: Context, private val mLogger: LoggerHandler) : AuthProvider() {
    private val mLwi: LoginWithIflyos?
    private var mAuthState: AuthProvider.AuthState = AuthProvider.AuthState.UNINITIALIZED
    private var mAuthToken = ""
    private val mPreferences: SharedPreferences = mContext.getSharedPreferences(
            mContext.getString(R.string.preference_file_key), Context.MODE_PRIVATE)

    val localAccessToken: String?
        get() = mPreferences.getString(mContext.getString(R.string.preference_refresh_token), "")

    init {

        // Authenticate with LWA

        mLwi = LoginWithIflyos(mLogger, mContext, mPreferences, this)
    }

    override fun getAuthToken(): String {
        if (mAuthToken == "") {
            mLogger.postWarn(sTag, "Auth token is not set")
        }
        return mAuthToken
    }

    fun login() {
        mLwi!!.login()
    }

    fun logout() {
        mLwi!!.logout()
    }

    fun logoutWithNotNotify() {
        mLwi!!.logoutWithNotNotify()
    }

    override fun getAuthState(): AuthProvider.AuthState {
        mLogger.postVerbose(sTag, String.format("Auth State Retrieved. STATE: %s", mAuthState))
        return mAuthState
    }

    fun onAuthStateChanged(authState: AuthProvider.AuthState, authError: AuthProvider.AuthError) {
        mAuthState = authState
        mLogger.postInfo(sTag, String.format("Auth State Changed. STATE: %s, ERROR: %s",
                authState, authError))
        val jsonObject = JSONObject()
        try {
            jsonObject.put("type", LoggerHandler.AUTH_LOG_STATE)
            jsonObject.put("auth_state", mAuthState.toString())
            jsonObject.put("auth_error", authError.toString())
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        mLogger.postDisplayCard(jsonObject, LoggerHandler.AUTH_LOG)
        authStateChange(authState, authError)
    }

    internal fun setAuthToken(authToken: String) {
        mAuthToken = authToken
    }

    internal fun clearAuthToken() {
        mAuthToken = ""
    }

    fun onInitialize() {
        mLwi?.onInitialize()
    }

    companion object {

        private val sTag = "AuthProvider"
    }
}
