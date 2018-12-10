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
import android.os.Build
import cn.iflyos.iace.iflyos.AuthProvider
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.impl.Logger.LoggerHandler
import com.iflytek.cyber.iot.show.core.opus.Tls12SocketFactory
import okhttp3.OkHttpClient
import okhttp3.OkUrlFactory
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.*
import java.util.concurrent.Executors

internal class LoginWithIVSCBL(private val mContext: Context,
                               private val mPreferences: SharedPreferences,
                               private val mLogger: LoggerHandler,
                               private val mAuthProvider: AuthProviderHandler) : Observable() {
    private val mExecutor = Executors.newSingleThreadExecutor()
    private val mClientId: String? = mPreferences.getString(mContext.getString(R.string.preference_client_id), "")
    private val mClientSecret: String? = mPreferences.getString(mContext.getString(R.string.preference_client_secret), "")
    private val mProductID: String? = mPreferences.getString(mContext.getString(R.string.preference_product_id), "")
    private val mProductDSN: String? = mPreferences.getString(mContext.getString(R.string.preference_product_dsn), "")

    private val mTimer = Timer()
    private var mAuthorizationTimerTask: TimerTask? = null
    private var mRefreshTimerTask: TimerTask? = null
    private val mOkHttpClient: OkHttpClient = Tls12SocketFactory.enableTls12OnPreLollipop(OkHttpClient.Builder()).build()
    @Suppress("DEPRECATION")
    private val mOkHttpFactory: OkUrlFactory = OkUrlFactory(mOkHttpClient)

    @Throws(IOException::class)
    private fun getConn(url: URL): HttpURLConnection {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mOkHttpFactory.open(url)
        } else url.openConnection() as HttpURLConnection
    }

    fun login() {
        mLogger.postInfo(sTag, "Attempting to authenticate")
        requestDeviceAuthorization()
    }

    private fun requestDeviceAuthorization() {
        mExecutor.submit(requestDeviceAuthorizationTask())
    }

    private inner class requestDeviceAuthorizationTask : Runnable {
        override fun run() {
            try {
                if (mClientId != "") {
                    val scopeData = JSONObject()
                    val productInstanceAttributes = JSONObject()

                    productInstanceAttributes.put("device_id", mProductDSN)
                    scopeData.put("user_ivs_all", productInstanceAttributes)

                    val urlParameters = ("response_type=device_code"
                            + "&client_id=" + mClientId
                            + "&scope=user_ivs_all"
                            + "&scope_data=" + URLEncoder.encode(scopeData.toString(), "utf8"))

                    var con: HttpURLConnection? = null
                    var os: DataOutputStream? = null
                    var response: InputStream? = null

                    try {
                        con = getConn(URL(sAuthRequestUrl))
                        con.requestMethod = "POST"

                        con.setRequestProperty("Host", "auth.iflyos.cn")
                        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                        con.doOutput = true
                        os = DataOutputStream(con.outputStream)
                        os.writeBytes(urlParameters)
                        os.flush()

                        val responseCode = con.responseCode
                        mLogger.postInfo(sTag, String.format("response code:%d", responseCode))
                        if (responseCode == sResponseOk) response = con.inputStream

                    } catch (e: IOException) {
                        mLogger.postError(sTag, e)
                    } finally {
                        con?.disconnect()
                        if (os != null) {
                            try {
                                os.flush()
                                os.close()
                            } catch (e: IOException) {
                                mLogger.postWarn(sTag, "Cannot close resource. Error: " + e.message)
                            }

                        }
                    }

                    val responseJSON = getResponseJSON(response)
                    if (responseJSON != null) {
                        val uri = responseJSON.getString("verification_uri")
                        val code = responseJSON.getString("user_code")

                        // Log card
                        val renderJSON = JSONObject()
                        renderJSON.put("verification_uri", uri)
                        renderJSON.put("user_code", code)
                        renderJSON.put("type", LoggerHandler.AUTH_LOG_URL)
                        mLogger.postDisplayCard(renderJSON, LoggerHandler.CBL_CODE)

                        // Log response
                        mLogger.postInfo(sTag,
                                String.format("Verification URI with user code: %s?user_code=%s",
                                        uri, code))

                        requestDeviceToken(responseJSON)

                    } else
                        mLogger.postError(sTag, "Error requesting device authorization")

                } else
                    mLogger.postWarn(sTag, "Cannot authenticate. Invalid client ID")

            } catch (e: Exception) {
                mLogger.postError(sTag, e)
            }

        }
    }

    private fun requestDeviceToken(response: JSONObject) {
        try {
            val deviceCode = response.getString("device_code")
            val userCode = response.getString("user_code")
            val expirySeconds = response.getString("expires_in")
            val urlParameters = ("grant_type=urn:ietf:params:oauth:grant-type:device_code"
                    + "&device_code=" + deviceCode
                    + "&client_id=" + mClientId)

            mTimer.schedule(object : TimerTask() {
                var i = Integer.parseInt(expirySeconds) / sPollInterval

                override fun run() {
                    if (i > 0) {
                        var con: HttpURLConnection? = null
                        var os: DataOutputStream? = null
                        var `in`: BufferedReader? = null
                        try {
                            val obj = URL(sTokenRequestUrl)
                            con = getConn(obj)

                            con.requestMethod = "POST"
                            con.setRequestProperty("Host", "auth.iflyos.cn")
                            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                            con.doOutput = true

                            os = DataOutputStream(con.outputStream)
                            os.writeBytes(urlParameters)

                            val responseCode = con.responseCode
                            if (responseCode == sResponseOk) {
                                this.cancel()
                                `in` = BufferedReader(
                                        InputStreamReader(con.inputStream))
                                val responseSb = StringBuilder()

                                var inputLine = `in`.readLine()
                                while (inputLine != null) {
                                    responseSb.append(inputLine)
                                    inputLine = `in`.readLine()
                                }

                                val responseJSON = JSONObject(responseSb.toString())
                                val accessToken = responseJSON.getString("access_token")
                                val refreshToken = responseJSON.getString("refresh_token")
                                val expiresInSeconds = responseJSON.getString("expires_in")

                                // Write refresh token to shared preferences
                                val editor = mPreferences.edit()
                                editor.putString(mContext.getString(R.string.preference_refresh_token), refreshToken)
                                editor.apply()

                                // Refresh access token automatically before expiry
                                startRefreshTimer(java.lang.Long.parseLong(expiresInSeconds), refreshToken)

                                // Inform AuthProvider of refreshed state
                                mLogger.postVerbose(sTag,
                                        "Refreshing Auth State with token: $accessToken")
                                mAuthProvider.authToken = accessToken
                                mAuthProvider.onAuthStateChanged(AuthProvider.AuthState.REFRESHED,
                                        AuthProvider.AuthError.NO_ERROR)
                                setChanged()
                                notifyObservers("logged in")
                            }

                        } catch (e: Exception) {
                            this.cancel()
                            mLogger.postError(sTag, e)
                            return
                        } finally {
                            con?.disconnect()
                            if (os != null) {
                                try {
                                    os.flush()
                                    os.close()
                                } catch (e: IOException) {
                                    mLogger.postWarn(sTag, "Cannot close resource. Error: " + e.message)
                                }

                            }
                            if (`in` != null) {
                                try {
                                    `in`.close()
                                } catch (e: IOException) {
                                    mLogger.postWarn(sTag, "Cannot close resource. Error: " + e.message)
                                }

                            }
                        }
                        i--

                    } else { // User didn't authorize with code before it expired
                        this.cancel()
                        // Prompt to attempt authorization again
                        val expiredMessage = "The code has expired. Retry to generate a new code."
                        try {
                            // Log code expired card
                            val renderJSON = JSONObject()
                            renderJSON.put("message", expiredMessage)
                            mLogger.postDisplayCard(renderJSON, LoggerHandler.CBL_CODE_EXPIRED)
                        } catch (e: JSONException) {
                            mLogger.postError(sTag, "JSON Error: " + e.message)
                            return
                        }

                        mLogger.postWarn(sTag, expiredMessage)
                    }
                }
            }, 0, (sPollInterval * 1000).toLong())
        } catch (e: Exception) {
            mLogger.postError(sTag, "Error requesting device token. Error: " + e.message)
        }

    }

    private fun refreshAuthToken(refreshToken: String) {
        mExecutor.submit(RefreshAuthTokenTask(refreshToken))
    }

    private inner class RefreshAuthTokenTask internal constructor(refreshToken: String) : Runnable {
        internal var mRefreshToken = ""

        init {
            mRefreshToken = refreshToken
        }

        override fun run() {
            if (mRefreshToken != ""
                    && mClientId != "" && mClientSecret != "") {

                val urlParameters = ("grant_type=refresh_token"
                        + "&refresh_token=" + mRefreshToken
                        + "&client_id=" + mClientId
                        + "&client_secret=" + mClientSecret)
                var con: HttpURLConnection? = null
                var os: DataOutputStream? = null
                var response: InputStream? = null

                try {
                    val obj = URL(sTokenRequestUrl)
                    con = getConn(obj)
                    con.requestMethod = "POST"

                    con.doOutput = true
                    os = DataOutputStream(con.outputStream)
                    os.writeBytes(urlParameters)

                    val responseCode = con.responseCode
                    if (responseCode == sResponseOk) response = con.inputStream

                } catch (e: IOException) {
                    mLogger.postError(sTag, e)
                } finally {
                    con?.disconnect()
                    if (os != null) {
                        try {
                            os.flush()
                            os.close()
                        } catch (e: IOException) {
                            mLogger.postWarn(sTag, "Cannot close resource. Error: " + e.message)
                        }

                    }
                }

                val responseJSON = getResponseJSON(response)

                if (responseJSON != null) {
                    try {
                        val expiresInSeconds = responseJSON.getString("expires_in")
                        val accessToken = responseJSON.getString("access_token")

                        // Refresh access token automatically before expiry
                        startRefreshTimer(java.lang.Long.parseLong(expiresInSeconds), mRefreshToken)

                        // Inform Auth Provider of refreshed state
                        mLogger.postVerbose(sTag,
                                "Refreshing Auth State with token: $accessToken")
                        mAuthProvider.authToken = accessToken
                        mAuthProvider.onAuthStateChanged(AuthProvider.AuthState.REFRESHED,
                                AuthProvider.AuthError.NO_ERROR)
                        setChanged()
                        notifyObservers("logged in")

                    } catch (e: JSONException) {
                        mLogger.postError(sTag, "Error refreshing auth token. Error: " + e.message)
                    }

                } else {
                    mAuthProvider.clearAuthToken()
                    mAuthProvider.onAuthStateChanged(AuthProvider.AuthState.UNINITIALIZED,
                            AuthProvider.AuthError.AUTHORIZATION_FAILED)
                    mLogger.postError(sTag, "Error refreshing auth token")
                }

            } else
                mLogger.postWarn(sTag, String.format(
                        "Invalid Auth Parameters, clientID: %s, clientSecret: %s, refreshToken: %s",
                        mClientId, mClientSecret, mRefreshToken))
        }
    }

    private fun startRefreshTimer(delaySeconds: Long?, refreshToken: String) {
        mTimer.schedule(object : TimerTask() {
            override fun run() {
                refreshAuthToken(refreshToken)
            }
        }, delaySeconds!! * 1000 - sRefreshAccessTokenTime)
    }

    fun logout() {
        mLogger.postInfo(sTag, "Attempting to un-authenticate")

        // stop refresh timer task
        if (mRefreshTimerTask != null) mRefreshTimerTask!!.cancel()

        // Clear refresh token in preferences
        val editor = mPreferences.edit()
        editor.putString(mContext.getString(R.string.preference_refresh_token), "")
        editor.putString(mContext.getString(R.string.preference_login_method), "")
        editor.apply()

        // Notify AuthProvider of unauthenticated state
        mAuthProvider.clearAuthToken()
        mAuthProvider.onAuthStateChanged(AuthProvider.AuthState.UNINITIALIZED,
                AuthProvider.AuthError.NO_ERROR)

        // Notify observers to update GUI
        setChanged()
        notifyObservers("logged out")
    }

    fun logoutWithNotNotify() {
        mLogger.postInfo(sTag, "Attempting to un-authenticate")

        // stop refresh timer task
        if (mRefreshTimerTask != null) mRefreshTimerTask!!.cancel()

        // Clear refresh token in preferences
        val editor = mPreferences.edit()
        editor.putString(mContext.getString(R.string.preference_refresh_token), "")
        editor.putString(mContext.getString(R.string.preference_login_method), "")
        editor.apply()

        // Notify AuthProvider of unauthenticated state
        mAuthProvider.clearAuthToken()
    }

    fun onInitialize() {
        val refreshToken = mPreferences.getString(mContext.getString(R.string.preference_refresh_token), null)
                ?: return
        if (!refreshToken.isEmpty()) refreshAuthToken(refreshToken)
    }

    private fun getResponseJSON(inStream: InputStream?): JSONObject? {
        if (inStream != null) {

            val response = StringBuilder()

            try {
                BufferedReader(InputStreamReader(inStream)).use { `in` ->

                    var inputLine = `in`.readLine()
                    while (inputLine != null) {
                        response.append(inputLine)
                        inputLine = `in`.readLine()
                    }
                    return JSONObject(response.toString())
                }
            } catch (e: Exception) {
                mLogger.postError(sTag, e)
            } finally {
                try {
                    inStream.close()
                } catch (e: IOException) {
                    mLogger.postWarn(sTag, "Cannot close resource. Error: " + e.message)
                }

            }
        }
        return null
    }

    fun cancelPendingAuthorization() {
        if (mAuthorizationTimerTask != null) {
            mAuthorizationTimerTask!!.cancel()
        }
    }

    companion object {

        private const val sTag = "CBL"

        private const val sResponseOk = 200

        // Refresh access token 2 minutes before it expires
        private const val sRefreshAccessTokenTime = 120000

        // Poll every 10 seconds when requesting device token
        private const val sPollInterval = 10

        // Display card type
        private const val CBL_CODE_EXPIRED = 0
        private const val CBL_CODE = 1

        // CBL auth endpoint URLs
        private const val sBaseEndpointUrl = "https://auth.iflyos.cn/oauth/ivs/"
        private const val sAuthRequestUrl = LoginWithIVSCBL.sBaseEndpointUrl + "device_code"
        private const val sTokenRequestUrl = LoginWithIVSCBL.sBaseEndpointUrl + "token"

    }
}
