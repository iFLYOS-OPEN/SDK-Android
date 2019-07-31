package com.iflytek.cyber.iot.show.core.impl.Custom

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import cn.iflyos.sdk.android.impl.custom.CustomAgent
import cn.iflyos.sdk.android.v3.iFLYOSManager
import com.google.gson.JsonObject
import com.google.gson.JsonParser


/**
 * 开发给 ShowCore 的一个 Custom 实现，包含了一部分 ShowCore 特有的自定义技能处理
 *
 * 1. screen 设置亮度、开关显示的功能
 * 2. launcher 回到首页功能
 * 3. system 识别关机功能
 * 4. hdp 调用 HDP 直播功能
 */
class CustomHandler(val context: Context, manager: iFLYOSManager) : CustomAgent(manager) {
    var launcherControlCallback: LauncherControlCallback? = null
    var screenControlCallback: ScreenControlCallback? = null
    var powerOffCallback: PowerOffCallback? = null

    /**
     * 暂存要更新的 Custom 上下文，其中的 screen 和 hdp 字段代表启用上文描述的几个自定义功能
     */
    private var jsonContext = JsonObject()

    companion object {
        const val PKG_HDP_LIVE = "hdpfans.com"
    }

    override fun onCustomDirective(directive: String) {
        try {
            val jsonObject = JsonParser().parse(directive) as? JsonObject
            val directiveJson = jsonObject?.getAsJsonObject("directive")
            val payload = directiveJson?.getAsJsonObject("payload")
            payload?.let { json ->
                if (json.has("headerName")) {
                    val headerName = json.get("headerName").asString
                    val data = json.getAsJsonObject("data")
                    when (headerName) {
                        "screen.set_state" -> {
                            // 处理「打开/关闭显示」
                            data.get("state")?.asString?.let { state ->
                                screenControlCallback?.setState(state == "on")
                            }
                        }
                        "screen.set_brightness" -> {
                            // 处理「调高/低亮度」、「亮度调到50」等
                            data.get("brightness")?.asInt?.let {
                                screenControlCallback?.setBrightness(it)
                            }
                        }
                        "launcher.start_activity" -> {
                            // 处理「回到首页」
                            data.get("page")?.asString?.let { page ->
                                launcherControlCallback?.startActivity(page)
                            }
                        }
                        "system.power_off" -> {
                            // 处理「关机」
                            powerOffCallback?.powerOff()
                        }
                        "hdp.execute" -> {
                            // 处理 HDP 直播应用的相关指令
                            val packageName = data.get("package_name").asString
                            val className = data.get("class_name").asString
                            val extras = data.getAsJsonObject("extras")

                            try {
                                val intent = Intent()
                                intent.setClassName(packageName, className)
                                extras.keySet().map { key ->
                                    val extraValue = extras[key]
                                    extraValue?.asInt?.let { value -> intent.putExtra(key, value) }
                                    extraValue?.asString?.let { value -> intent.putExtra(key, value) }
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 更新 Screen 相关的部分上下文
     */
    fun updateScreenContext(visible: Boolean, brightness: Int) {
        val screenContext = JsonObject()
        screenContext.addProperty("version", "1.1")

        screenContext.addProperty("visible", visible)

        screenContext.addProperty("brightness", 100 * brightness / 255)

        screenContext.addProperty("type", "percent")

        jsonContext.remove("screen")
        jsonContext.add("screen", screenContext)

        updateContext(jsonContext.toString())
    }

    /**
     * 更新 HDP 技能所需的上下文信息
     */
    fun updateHdpLiveContext(isForeground: Boolean) {
        if (isForeground) {
            jsonContext.remove("hdp_state")
            jsonContext.addProperty("hdp_state", "ACTIVITY")
        } else {
            jsonContext.remove("hdp_state")
            if (checkAppInstalled(context, PKG_HDP_LIVE)) {
                jsonContext.addProperty("hdp_state", "IDLE")
            }
        }

        updateContext(jsonContext.toString())
    }

    /**
     * 检查 HDP 直播应用是否已安装
     */
    private fun checkAppInstalled(context: Context, pkgName: String): Boolean {
        if (pkgName.isEmpty()) {
            return false
        }
        var packageInfo: PackageInfo?
        try {
            packageInfo = context.packageManager.getPackageInfo(pkgName, 0)
        } catch (e: Exception) {
            packageInfo = null
            e.printStackTrace()
        }
        return packageInfo != null
    }

    interface ScreenControlCallback {
        fun setState(visible: Boolean)
        fun setBrightness(brightness: Int)
    }

    interface LauncherControlCallback {
        fun startActivity(page: String)
    }

    interface PowerOffCallback {
        fun powerOff()
    }
}