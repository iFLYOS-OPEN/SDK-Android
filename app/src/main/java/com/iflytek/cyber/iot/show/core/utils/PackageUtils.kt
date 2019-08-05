package com.iflytek.cyber.iot.show.core.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.iflytek.cyber.iot.show.core.BuildConfig
import com.jaredrummler.apkparser.ApkParser
import java.io.File
import kotlin.Exception

object PackageUtils {
    fun checkIfLatest(context: Context, filePath: String?): Boolean {
        filePath ?: return false

        try {
            val apkParser = ApkParser.create(filePath)

            val meta = apkParser.apkMeta

            if (meta.packageName != context.packageName)
                return false

            return BuildConfig.VERSION_CODE <= meta.versionCode
        } catch (e: Exception) {
            return false
        }
    }

    fun notifyInstallApk(context: Context, filePath: String) {
        try {
            val install = Intent(Intent.ACTION_VIEW)
            install.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            val apkFile = File(filePath)
            val uri = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                Uri.fromFile(apkFile)
            } else {
                install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                FileProvider.getUriForFile(context, "${context.packageName}.fileProvider", apkFile)
            }
            install.setDataAndType(uri, "application/vnd.android.package-archive")
            context.applicationContext.startActivity(install)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
