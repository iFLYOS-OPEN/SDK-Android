package com.iflytek.cyber.product.ota

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import android.util.Log
import com.google.gson.Gson
import com.jaredrummler.apkparser.ApkParser
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import okio.ByteString.Companion.toByteString
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import kotlin.collections.HashSet

/**
 * 一个简易的检查更新服务
 */
class OtaService : Service() {
    companion object {
        const val ACTION_REQUEST_CHECKING = "com.iflytek.cyber.product.ota.action.REQUEST_CHECKING"
        const val ACTION_START_SERVICE = "com.iflytek.cyber.product.ota.action.START_SERVICE"
        const val ACTION_NEW_UPDATE_DOWNLOADED = "com.iflytek.cyber.product.ota.action.NEW_UPDATE_DOWNLOADED"
        const val ACTION_NO_UPDATE_FOUND = "com.iflytek.cyber.product.ota.action.NO_UPDATE_FOUND"
        const val ACTION_CHECK_UPDATE_FAILED = "com.iflytek.cyber.product.ota.action.CHECK_UPDATE_FAILED"
        const val ACTION_NEW_UPDATE_DOWNLOAD_STARTED = "com.iflytek.cyber.product.ota.action.NEW_UPDATE_DOWNLOAD_STARTED"
        private const val TAG = "OtaService"
        private const val OTA_URL = "https://ota.iflyos.cn"
        private const val PREF_NAME = "com.iflytek.cyber.product.ota.pref"
        private const val PREF_VERSION_CODE_MAP = "version_code_map"
        private const val PREF_PID = "pid"

        const val EXTRA_DEVICE_ID = "device_id"
        const val EXTRA_CLIENT_ID = "client_id"
        const val EXTRA_OTA_SECRET = "ota_secret"
        const val EXTRA_DOWNLOAD_PATH = "download_path"
        const val EXTRA_VERSION = "version"
        const val EXTRA_PACKAGE_NAME = "package_name"

        const val EXTRA_PATH = "path"

        const val CHECK_INTERVAL = 24 * 3600 * 1000 // 每两次自动检查更新的时间间隔
    }

    private lateinit var otaApi: OtaApi
    private lateinit var client: OkHttpClient
    private lateinit var retrofit: Retrofit
    private lateinit var pref: SharedPreferences

    private var deviceId: String? = null
    private var clientId: String? = null
    private var clientSecret: String? = null
    private var downloadPath: String? = null
    private var versionCode: Int? = null
    private var otaPackageName: String? = null

    private var currentDownloadRequest: Request? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        val clientBuilder = OkHttpClient.Builder()

        if (BuildConfig.DEBUG) {
            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BODY
            clientBuilder.addInterceptor(interceptor)
        }

        clientBuilder.addInterceptor { chain ->
            val timestamp = System.currentTimeMillis() / 1000
            val nonce = (Math.random() * 10000).toInt().toString()

            val signature = sha1(String.format(Locale.US, "%s:%s:%s:%s:%s",
                    clientId, deviceId, timestamp, nonce, clientSecret))

            Log.d(TAG, "Using deviceId: $deviceId, clientId: $clientId, clientSecret: $clientSecret")
            val request = chain.request()
                    .newBuilder()
                    .addHeader("X-Client-ID", clientId.toString())
                    .addHeader("X-Device-ID", deviceId.toString())
                    .addHeader("X-Timestamp", timestamp.toString())
                    .addHeader("X-Nonce", nonce)
                    .addHeader("X-Signature", signature)
                    .build()

            chain.proceed(request)
        }

        client = clientBuilder.build()
        retrofit = Retrofit.Builder()
                .client(client)
                .baseUrl(OTA_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        otaApi = retrofit.create(OtaApi::class.java)

        pref = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    private fun sha1(data: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-1")
            digest.update(data.toByteArray())
            ByteBuffer.wrap(digest.digest()).toByteString().hex()
        } catch (e: NoSuchAlgorithmException) {
            ""
        }

    }

    private fun downloadFile(id: Long, url: String) {
        val filePath = "$downloadPath/${sha1(url)}.apk"
        val file = File(filePath)
        var ifNeedNewFile: Boolean
        if (file.exists()) {
            try {
                val apkParser = ApkParser.create(file)
                if (apkParser.apkMeta.versionCode >= versionCode!!
                        && apkParser.apkMeta.packageName == otaPackageName) {
                    // 判断本地下载的 apk 版本已经比已安装的版本新
                    ifNeedNewFile = false

                    // 发送已经下载好新的 apk 的广播
                    val intent = Intent(ACTION_NEW_UPDATE_DOWNLOADED)
                    intent.putExtra(EXTRA_PATH, file.path)
                    sendBroadcast(intent)
                } else {
                    ifNeedNewFile = true
                }
                apkParser.apkMeta.let {
                    val versionCodeAndId = VersionCodeAndId(
                            it.versionCode.toInt(), id)
                    val versionCodeMap = getPrefVersionCodeMap()
                    if (versionCodeMap == null) {
                        val newMap = VersionCodeMap(setOf(versionCodeAndId))
                        savePrefVersionCodeMap(newMap)
                    } else {
                        val set = HashSet<VersionCodeAndId>()
                        set.addAll(versionCodeMap.set)
                        set.add(versionCodeAndId)
                        val newVersionCodeMap = VersionCodeMap(set)
                        savePrefVersionCodeMap(newVersionCodeMap)
                    }
                }
            } catch (e: ApkParser.InvalidApkException) {
                // 如果文件下载未完成就被打断，就会导致无法解析
                e.printStackTrace()
                ifNeedNewFile = true
            }
        } else {
            ifNeedNewFile = true
        }
        if (ifNeedNewFile) {
            file.createNewFile()
            sendBroadcast(Intent(ACTION_NEW_UPDATE_DOWNLOAD_STARTED))
            val request = Request.Builder().url(url).build()
            currentDownloadRequest = request
            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    e.printStackTrace()
                    sendBroadcast(Intent(ACTION_CHECK_UPDATE_FAILED))
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    response.body()?.let { responseBody ->
                        val inputStream = responseBody.byteStream()
                        val outputStream = FileOutputStream(file)

                        val bytes = ByteArray(1024)

                        var len = 0
                        while (len != -1 && currentDownloadRequest == request) {
                            len = inputStream.read(bytes)
                            if (len != -1)
                                outputStream.write(bytes, 0, len)
                        }
                        outputStream.flush()

                        inputStream.close()
                        outputStream.close()


                        // 判断是否被一个新的下载请求抢了
                        if (currentDownloadRequest == request) {
                            try {
                                val apkParser = ApkParser.create(file)
                                apkParser.apkMeta.let {
                                    val versionCodeAndId = VersionCodeAndId(
                                            it.versionCode.toInt(), id)
                                    val versionCodeMap = getPrefVersionCodeMap()
                                    if (versionCodeMap == null) {
                                        val newMap = VersionCodeMap(setOf(versionCodeAndId))
                                        savePrefVersionCodeMap(newMap)
                                    } else {
                                        val set = HashSet<VersionCodeAndId>()
                                        set.addAll(versionCodeMap.set)
                                        set.add(versionCodeAndId)
                                        val newVersionCodeMap = VersionCodeMap(set)
                                        savePrefVersionCodeMap(newVersionCodeMap)
                                    }
                                }
                                // 发送已经下载好新的 apk 的广播
                                val intent = Intent(ACTION_NEW_UPDATE_DOWNLOADED)
                                intent.putExtra(EXTRA_PATH, file.path)
                                sendBroadcast(intent)
                            } catch (e: ApkParser.InvalidApkException) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            })
        }
    }

    /**
     * 开始检查更新
     */
    private fun startChecking() {
        otaApi.getPackages().enqueue(object : Callback<List<PackageEntity>> {
            override fun onFailure(call: Call<List<PackageEntity>>, t: Throwable) {
                t.printStackTrace()
                sendBroadcast(Intent(ACTION_CHECK_UPDATE_FAILED))
            }

            override fun onResponse(call: Call<List<PackageEntity>>, response: Response<List<PackageEntity>>) {
                if (response.isSuccessful) {
                    val body = response.body()

                    var needSendBroadcast = false

                    val savedPid = pref.getLong(PREF_PID, -1L)
                    if (savedPid == -1L) {
                        needSendBroadcast = true
                    } else {
                        // 本地会保存已经上报过的版本 id
                        // 服务端 id 自增，故检查到服务端返回的版本比较新时才需要开始下载
                        body?.map {
                            if (it.id > savedPid) {
                                needSendBroadcast = true
                            }
                        }
                    }

                    if (needSendBroadcast) {
                        // find the latest package
                        var latestPackage: PackageEntity? = null
                        var currentMax = 0L
                        body?.map {
                            if (currentMax < it.id) {
                                latestPackage = it
                                currentMax = it.id
                            }
                        }
                        latestPackage?.let { packageEntity ->
                            // 开始下载
                            downloadFile(packageEntity.id, packageEntity.url)
                        } ?: run {
                            sendBroadcast(Intent(ACTION_NO_UPDATE_FOUND))
                        }
                    } else {
                        sendBroadcast(Intent(ACTION_NO_UPDATE_FOUND))
                    }
                }
            }
        })
    }

    private fun savePrefVersionCodeMap(versionCodeMap: VersionCodeMap) {
        val json = Gson().toJson(versionCodeMap)
        pref.edit().putString(PREF_VERSION_CODE_MAP, json).apply()
    }

    private fun getPrefVersionCodeMap(): VersionCodeMap? {
        if (pref.contains(PREF_VERSION_CODE_MAP)) {
            val value = pref.getString(PREF_VERSION_CODE_MAP, null)
            return if (value.isNullOrEmpty()) {
                null
            } else {
                Gson().fromJson(value, VersionCodeMap::class.java)
            }
        }
        return null
    }

    private fun reportUpdated(pid: Long) {
        otaApi.putPackages(ReportEntity(listOf(pid))).enqueue(object : Callback<Void> {
            override fun onFailure(call: Call<Void>, t: Throwable) {
                t.printStackTrace()
            }

            override fun onResponse(call: Call<Void>, response: Response<Void>) {

            }
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_REQUEST_CHECKING -> {
                startChecking()

                // 在指定时间间隔后继续检查更新
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val serviceIntent = Intent(this, OtaService::class.java)
                serviceIntent.action = ACTION_REQUEST_CHECKING
                val pendingIntent = PendingIntent.getService(this, 1000, serviceIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT)
                alarmManager.set(AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + CHECK_INTERVAL, pendingIntent)
            }
            ACTION_START_SERVICE -> {
                clientId = intent.getStringExtra(EXTRA_CLIENT_ID)
                deviceId = intent.getStringExtra(EXTRA_DEVICE_ID)
                clientSecret = intent.getStringExtra(EXTRA_OTA_SECRET)
                downloadPath = intent.getStringExtra(EXTRA_DOWNLOAD_PATH)
                versionCode = intent.getIntExtra(EXTRA_VERSION, -1)
                otaPackageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)

                var needCheckNow = true
                getPrefVersionCodeMap()?.let {
                    // 本地会保存 version code 和服务端返回 id 的对应关系
                    // 若检查到当前 version code 的版本对应的 id 本地未曾上报过，则上报该 id
                    // 上报 id 后，服务端会标记此设备已升级到此版本，小于此版本的包信息将不会再返回
                    it.set.map { versionCodeAndId ->
                        if (versionCodeAndId.versionCode == versionCode) {
                            val pid = versionCodeAndId.pid
                            val savePid = pref.getLong(PREF_PID, -1L)
                            if (savePid != pid) {
                                pref.edit().putLong(PREF_PID, pid).apply()
                                needCheckNow = false
                                reportUpdated(pid)
                            }
                        }
                    }
                } ?: run {
                    needCheckNow = true
                }

                if (needCheckNow)
                    startChecking()

                // 在指定时间间隔后继续检查更新
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val serviceIntent = Intent(this, OtaService::class.java)
                serviceIntent.action = ACTION_REQUEST_CHECKING
                val pendingIntent = PendingIntent.getService(this, 1000, serviceIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT)
                alarmManager.set(AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + CHECK_INTERVAL, pendingIntent)
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }
}