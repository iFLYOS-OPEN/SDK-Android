/*
 * Copyright (C) 2019 iFLYTEK CO.,LTD.
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

package com.iflytek.cyber.iot.show.core.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.iflytek.cyber.iot.show.core.LauncherActivity
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.SelfBroadcastReceiver
import com.iflytek.cyber.iot.show.core.TimeService
import java.util.*

class WifiFragment : BaseFragment() {

    private val scanReceiver = ScanReceiver()
    private val connectionReceiver = ConnectionReceiver()

    private val uiHandler = Handler(Looper.getMainLooper())

    private var activity: LauncherActivity? = null

    private var wm: WifiManager? = null

    private val configs = HashMap<String, WifiConfiguration>()
    private var scans: List<ScanResult>? = null

    private var connected: String? = null
    private var connecting: String? = null
    private var failed = false

    private var adapter: WifiAdapter? = null
    private var refresher: SwipeRefreshLayout? = null
    private var next: Button? = null
    private var ivClose: ImageView? = null
    private var title: TextView? = null

    private var resetWifi: Boolean = false

    private var warningAlert: AlertDialog? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is LauncherActivity) {
            activity = context
        }
        wm = context?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val args = arguments
        if (args != null) {
            resetWifi = args.getBoolean("RESET_WIFI")
        }
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        container?.removeAllViews()
        val view = inflater.inflate(R.layout.fragment_wifi, container, false)
        next = view.findViewById(R.id.next)
        refresher = view.findViewById(R.id.refresher)
        ivClose = view.findViewById(R.id.iv_close)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.back)
                .setOnClickListener { activity?.onBackPressed() }

        next?.post {
            next?.setTextSize(TypedValue.COMPLEX_UNIT_PX, (next?.height ?: 0) * 0.47f)
            next?.setText(R.string.next_step)
        }
        next?.setOnClickListener { v -> Navigation.findNavController(v).navigate(R.id.action_to_pair_fragment) }

        adapter = WifiAdapter(view.context)

        val aps = view.findViewById<RecyclerView>(R.id.aps)
        aps.adapter = adapter

        refresher?.setColorSchemeColors(ContextCompat.getColor(view.context, R.color.setup_primary))
        refresher?.isRefreshing = true
        refresher?.setOnRefreshListener {
            @Suppress("DEPRECATION")
            wm?.startScan()
        }

        ivClose?.setOnClickListener { v -> Navigation.findNavController(v).navigateUp() }
        title = view.findViewById(R.id.title)

        if (resetWifi) {
            view.findViewById<View>(R.id.back)?.visibility = View.GONE
            view.findViewById<View>(R.id.wifi_bottom_divider)?.visibility = View.GONE
            next?.visibility = View.GONE
            ivClose?.visibility = View.VISIBLE
            ivClose?.post {
                ivClose?.let { imageView ->
                    val padding = imageView.height * 12 / 56
                    imageView.setPadding(padding, padding, padding, padding)
                }
            }
            title?.text = "网络设置"

            val layoutParams = title?.layoutParams as ConstraintLayout.LayoutParams
            layoutParams.startToStart = R.id.title_start_margin
            title?.layoutParams = layoutParams

            val refreshLayoutParams = refresher?.layoutParams as ConstraintLayout.LayoutParams
            refreshLayoutParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            refresher?.layoutParams = refreshLayoutParams
        } else {
            view.findViewById<ImageView>(R.id.back)?.let { ivBack ->
                ivBack.setOnClickListener {
                    it.setOnClickListener(null)
                    findNavController().navigateUp()
                }
                ivBack.post {
                    val padding = ivBack.height * 12 / 56
                    ivBack.setPadding(padding, padding, padding, padding)
                    ivBack.setImageResource(R.drawable.ic_previous_white_32dp)
                }
            }
        }

        if (null != activity) {
            activity?.hideSimpleTips()
        }

        if (null != activity) {
            if (ActivityCompat.checkSelfPermission(activity!!, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PermissionChecker.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_CODE)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_CODE && grantResults.isNotEmpty()
                && grantResults[0] == PermissionChecker.PERMISSION_GRANTED &&
                permissions.isNotEmpty() && permissions[0] == Manifest.permission.ACCESS_FINE_LOCATION) {
            wm?.startScan()
        } else {
            findNavController().navigateUp()
        }
    }

    override fun onResume() {
        super.onResume()

        scanReceiver.register(context)
        connectionReceiver.register(context)

        @Suppress("DEPRECATION")
        if (wm?.isWifiEnabled == true) {
            wm?.startScan() // TODO: 持续刷新

            handleScanResult()
        } else {
            val result = wm?.setWifiEnabled(true)
            Log.d(TAG, "开启WiFi结果: $result")
            if (result == true) {
                wm?.startScan() // TODO: 持续刷新

                handleScanResult()
            } else {
                context?.let { context ->
                    warningAlert = AlertDialog.Builder(context)
                            .setTitle("无法配置网络")
                            .setMessage("需要打开WiFi以配置网络")
                            .setPositiveButton(android.R.string.yes, null)
                            .setOnDismissListener {
                                if (!isDetached)
                                    findNavController().navigateUp()
                            }
                            .show()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        connectionReceiver.unregister(context)
        scanReceiver.unregister(context)
        uiHandler.removeCallbacksAndMessages(null)
        warningAlert?.dismiss()
    }

    override fun onDestroy() {
        super.onDestroy()

        if (null != activity && resetWifi) {
            activity?.checkIfResetState()
        }
    }

    private fun handleScanResult() {
        configs.clear()
        wm?.let {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                for (config in it.configuredNetworks) {
                    val ssid = config.SSID.substring(1, config.SSID.length - 1)
                    configs[ssid] = config

                    if (config.status == WifiConfiguration.Status.CURRENT) {
                        connected = ssid
                    }
                }
            } else {
                val connManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
                if (networkInfo.isConnected) {
                    val wifiInfo = it.connectionInfo
                    val ssid = wifiInfo.ssid
                    connected = ssid.substring(1, ssid.length - 1)
                }
            }

            val map = HashMap<String, ScanResult>()
            for (o1 in it.scanResults) {
                val o2 = map[o1.SSID]
                if ((o2 == null || o2.level < o1.level) && o1.level != 0) {
                    map[o1.SSID] = o1
                }
            }
            val list = ArrayList(map.values)
            Collections.sort(list, Comparator { o1, o2 ->
                val c1 = configs[o1.SSID]
                val c2 = configs[o2.SSID]

                if (c1 != null && c1.status == WifiConfiguration.Status.CURRENT) {
                    return@Comparator -1
                } else if (c2 != null && c2.status == WifiConfiguration.Status.CURRENT) {
                    return@Comparator 1
                }

                if (c1 != null && c2 == null) {
                    return@Comparator -1
                } else if (c1 == null && c2 != null) {
                    return@Comparator 1
                }

                o2.level - o1.level
            })
            scans = list
            adapter?.notifyDataSetChanged() // TODO: 处理持续刷新连续动画

            next?.isEnabled = connected != null && !TextUtils.equals(connected, "<unknown ssid>")

            refresher?.isRefreshing = false
        }
    }

    private fun handleOnItemClick(scan: ScanResult) {
        if (scan.SSID == connected) {
            return
        }

        uiHandler.postDelayed({
            connecting = scan.SSID
            adapter?.notifyDataSetChanged()
            this@WifiFragment.handleConnectRequest(scan)
        }, 500)
    }

    private fun handleOnItemLongClick(scan: ScanResult): Boolean {
        val config = configs[scan.SSID] ?: return false

        if (config.status == WifiConfiguration.Status.CURRENT) {
            wm?.disconnect()
            next?.isEnabled = false
        }

        wm?.removeNetwork(config.networkId)
        @Suppress("DEPRECATION")
        wm?.startScan()

        return true
    }

    @SuppressLint("InflateParams")
    private fun handleConnectRequest(scan: ScanResult) {
        val config = configs[scan.SSID]
        if (config != null) {
            next?.isEnabled = false
            connect(config.networkId)
            return
        }

        if (!isEncrypted(scan)) {
            next?.isEnabled = false
            connect(buildWifiConfig(scan))
            return
        }

        val dialogView = layoutInflater.inflate(
                R.layout.dialog_wifi_password, null, false)

        val passwordView = dialogView.findViewById<EditText>(R.id.password)

        AlertDialog.Builder(context!!)
                .setTitle("连接到 " + scan.SSID)
                .setView(dialogView)
                .setPositiveButton("连接") { _, _ ->
                    next?.isEnabled = false
                    val password = passwordView.text.toString()
                    this@WifiFragment.connect(this@WifiFragment.buildWifiConfig(scan, password))
                }
                .setNegativeButton("取消") { _, _ ->
                    connecting = null
                    failed = false
                    adapter?.notifyDataSetChanged()
                }
                .show()
    }

    private fun buildWifiConfig(scan: ScanResult): WifiConfiguration {
        val config = WifiConfiguration()
        config.SSID = "\"" + scan.SSID + "\""
        config.status = WifiConfiguration.Status.ENABLED
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
        return config
    }

    private fun buildWifiConfig(scan: ScanResult, password: String): WifiConfiguration {
        val config = WifiConfiguration()
        config.SSID = "\"" + scan.SSID + "\""
        config.status = WifiConfiguration.Status.ENABLED
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
        config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN)
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)
        @Suppress("DEPRECATION")
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
        config.preSharedKey = "\"" + password + "\""
        return config
    }

    private fun connect(config: WifiConfiguration) {
        val networkId = wm?.addNetwork(config)
        Log.d(TAG, "Network ID: $networkId")
        networkId?.let {
            connect(networkId)
        }
    }

    private fun connect(networkId: Int) {
        if (wm?.disconnect() != true) {
            Log.d(TAG, "Disconnect failed")
            failed = true
            adapter?.notifyDataSetChanged()
            return
        }

        if (wm?.enableNetwork(networkId, true) != true) {
            Log.d(TAG, "Enable failed")
            failed = true
            adapter?.notifyDataSetChanged()
            return
        }

        if (wm?.reconnect() != true) {
            Log.d(TAG, "Reconnect failed")
            failed = true
            adapter?.notifyDataSetChanged()
            return
        }

        Log.d(TAG, "Connecting...")
    }

    private fun handleWifiConfigFailed() {
        failed = true
        adapter?.notifyDataSetChanged()
    }

    private fun handleWifiConfigSucceed() {
        connecting = null
        failed = false
        @Suppress("DEPRECATION")
        wm?.startScan()
        if (activity != null) {
            activity?.startService(Intent(activity, TimeService::class.java))
        }
    }

    private inner class WifiAdapter internal constructor(context: Context) : RecyclerView.Adapter<WifiAdapter.ViewHolder>() {
        private val inflater: LayoutInflater = LayoutInflater.from(context)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(inflater.inflate(R.layout.item_access_point, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val scan = scans!![position]
            holder.bind(scan, configs[scan.SSID])
        }

        override fun getItemCount(): Int {
            return scans?.size ?: 0
        }

        internal inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val icon: ImageView = itemView.findViewById(R.id.icon)
            private val ssid: TextView = itemView.findViewById(R.id.ssid)
            private val status: TextView = itemView.findViewById(R.id.status)

            init {
                itemView.setOnClickListener { handleOnItemClick(scans!![this@ViewHolder.layoutPosition]) }
                itemView.setOnLongClickListener { handleOnItemLongClick(scans!![this@ViewHolder.layoutPosition]) }
            }

            fun bind(scan: ScanResult, config: WifiConfiguration?) {
                if (isEncrypted(scan)) {
                    icon.setImageResource(R.drawable.ic_signal_wifi_4_bar_lock_white_24dp)
                } else {
                    icon.setImageResource(R.drawable.ic_signal_wifi_4_bar_white_24dp)
                }

                ssid.text = scan.SSID

                when {
                    scan.SSID == connecting -> {
                        status.visibility = View.VISIBLE
                        status.text = if (failed) "密码错误" else "正在连接…"
                    }
                    scan.SSID == connected -> {
                        status.visibility = View.VISIBLE
                        status.text = "已连接"
                    }
                    config != null -> {
                        status.visibility = View.VISIBLE
                        status.text = "已保存"
                    }
                    else -> status.visibility = View.GONE
                }
            }
        }
    }

    private inner class ScanReceiver internal constructor() : SelfBroadcastReceiver(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {

        override fun onReceiveAction(action: String, intent: Intent) {
            handleScanResult()
        }
    }

    @Suppress("DEPRECATION")
    private inner class ConnectionReceiver internal constructor() :
            SelfBroadcastReceiver(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION, ConnectivityManager.CONNECTIVITY_ACTION) {

        override fun onReceiveAction(action: String, intent: Intent) {
            when (action) {
                WifiManager.SUPPLICANT_STATE_CHANGED_ACTION -> {
                    val error = intent.getIntExtra(
                            WifiManager.EXTRA_SUPPLICANT_ERROR, -1)

                    if (error == WifiManager.ERROR_AUTHENTICATING) {
                        Log.e(TAG, "Wi-Fi authenticate failed")
                        handleWifiConfigFailed()
                    }
                }
                ConnectivityManager.CONNECTIVITY_ACTION -> {
                    val network = intent.getParcelableExtra<NetworkInfo>(
                            ConnectivityManager.EXTRA_NETWORK_INFO)
                    val detailed = network.detailedState

                    if (detailed == NetworkInfo.DetailedState.CONNECTED) {
                        Log.d(TAG, "Wi-Fi connected")
                        handleWifiConfigSucceed()
                    }
                }
            }
        }

    }

    companion object {

        private const val TAG = "WifiFragment"
        private const val REQUEST_LOCATION_CODE = 10423

        private fun isEncrypted(scan: ScanResult): Boolean {
            // [ESS]
            // [WPA2-PSK-CCMP][ESS]
            // [WPA2-PSK-CCMP][WPS][ESS]
            // [WPA-PSK-CCMP+TKIP][WPA2-PSK-CCMP+TKIP][WPS][ESS]
            return scan.capabilities.contains("WPA")
        }
    }

}
