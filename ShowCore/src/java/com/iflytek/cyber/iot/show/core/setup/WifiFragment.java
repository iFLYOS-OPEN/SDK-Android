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

package com.iflytek.cyber.iot.show.core.setup;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.iflytek.cyber.iot.show.core.R;
import com.iflytek.cyber.iot.show.core.SetupWizardActivity;
import com.iflytek.cyber.iot.show.core.TimeService;
import com.iflytek.cyber.platform.internal.android.content.SelfBroadcastReceiver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class WifiFragment extends Fragment {

    private static final String TAG = "WifiFragment";

    private final ScanReceiver scanReceiver = new ScanReceiver();
    private final ConnectionReceiver connectionReceiver = new ConnectionReceiver();

    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private SetupWizardActivity activity;

    private WifiManager wm;

    private HashMap<String, WifiConfiguration> configs = new HashMap<>();
    private List<ScanResult> scans;

    private String connected = null;
    private String connecting = null;
    private boolean failed = false;

    private WifiAdapter adapter;
    private SwipeRefreshLayout refresher;
    private View next;
    private ImageView ivClose;
    private TextView title;
    private LinearLayout bottomBarContent;

    private boolean resetWifi;

    private OnCloseListener onCloseListener;

    private static boolean isEncrypted(ScanResult scan) {
        // [ESS]
        // [WPA2-PSK-CCMP][ESS]
        // [WPA2-PSK-CCMP][WPS][ESS]
        // [WPA-PSK-CCMP+TKIP][WPA2-PSK-CCMP+TKIP][WPS][ESS]
        return scan.capabilities.contains("WPA");
    }

    public interface OnCloseListener {
        void onClose();
    }

    public void setOnCloseListener(OnCloseListener onCloseListener) {
        this.onCloseListener = onCloseListener;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof SetupWizardActivity) {
            activity = (SetupWizardActivity) context;
        }
        wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        final Bundle args = getArguments();
        if (args != null) {
            resetWifi = args.getBoolean("RESET_WIFI");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_wifi, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        view.findViewById(R.id.back)
                .setOnClickListener(v -> activity.onBackPressed());

        next = view.findViewById(R.id.next);
        next.setOnClickListener(v -> activity.navigateTo(new PairFragment()));

        adapter = new WifiAdapter(getContext());

        final RecyclerView aps = view.findViewById(R.id.aps);
        aps.setAdapter(adapter);

        refresher = view.findViewById(R.id.refresher);
        refresher.setOnRefreshListener(() -> wm.startScan());

        bottomBarContent = view.findViewById(R.id.bottom_bar_content);
        ivClose = view.findViewById(R.id.iv_close);
        ivClose.setOnClickListener(v -> {
            if (onCloseListener != null) {
                onCloseListener.onClose();
            }
        });
        title = view.findViewById(R.id.title);

        if (resetWifi) {
            bottomBarContent.setVisibility(View.GONE);
            ivClose.setVisibility(View.VISIBLE);
            title.setText("网络设置");
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        scanReceiver.register(getContext());
        connectionReceiver.register(getContext());

        wm.setWifiEnabled(true);
        wm.startScan(); // TODO: 持续刷新
    }

    @Override
    public void onPause() {
        super.onPause();
        connectionReceiver.unregister(getContext());
        scanReceiver.unregister(getContext());
        uiHandler.removeCallbacksAndMessages(null);
    }

    private void handleScanResult() {
        configs.clear();
        for (WifiConfiguration config : wm.getConfiguredNetworks()) {
            final String ssid = config.SSID.substring(1, config.SSID.length() - 1);
            configs.put(ssid, config);

            if (config.status == WifiConfiguration.Status.CURRENT) {
                connected = ssid;
            }
        }

        next.setEnabled(connected != null);

        final HashMap<String, ScanResult> map = new HashMap<>();
        for (ScanResult o1 : wm.getScanResults()) {
            final ScanResult o2 = map.get(o1.SSID);
            if ((o2 == null || o2.level < o1.level) && o1.level != 0) {
                map.put(o1.SSID, o1);
            }
        }

        final ArrayList<ScanResult> list = new ArrayList<>(map.values());
        Collections.sort(list, (o1, o2) -> {
            final WifiConfiguration c1 = configs.get(o1.SSID);
            final WifiConfiguration c2 = configs.get(o2.SSID);

            if (c1 != null && c1.status == WifiConfiguration.Status.CURRENT) {
                return -1;
            } else if (c2 != null && c2.status == WifiConfiguration.Status.CURRENT) {
                return 1;
            }

            if (c1 != null && c2 == null) {
                return -1;
            } else if (c1 == null && c2 != null) {
                return 1;
            }

            return o2.level - o1.level;
        });

        scans = list;
        adapter.notifyDataSetChanged(); // TODO: 处理持续刷新连续动画
        refresher.setRefreshing(false);
    }

    private void handleOnItemClick(ScanResult scan) {
        if (scan.SSID.equals(connected)) {
            return;
        }

        uiHandler.postDelayed(() -> {
            connecting = scan.SSID;
            adapter.notifyDataSetChanged();
            handleConnectRequest(scan);
        }, 500);
    }

    private boolean handleOnItemLongClick(ScanResult scan) {
        final WifiConfiguration config = configs.get(scan.SSID);
        if (config == null) {
            return false;
        }

        if (config.status == WifiConfiguration.Status.CURRENT) {
            wm.disconnect();
            next.setEnabled(false);
        }

        wm.removeNetwork(config.networkId);
        wm.startScan();

        return true;
    }

    @SuppressLint("InflateParams")
    @SuppressWarnings("ConstantConditions")
    private void handleConnectRequest(ScanResult scan) {
        final WifiConfiguration config = configs.get(scan.SSID);
        if (config != null) {
            next.setEnabled(false);
            connect(config.networkId);
            return;
        }

        if (!isEncrypted(scan)) {
            next.setEnabled(false);
            connect(buildWifiConfig(scan));
            return;
        }

        final View dialogView = getLayoutInflater().inflate(
                R.layout.dialog_wifi_password, null, false);

        final EditText passwordView = dialogView.findViewById(R.id.password);

        new AlertDialog.Builder(getContext())
                .setTitle("连接到 " + scan.SSID)
                .setView(dialogView)
                .setPositiveButton("连接", (d, which) -> {
                    next.setEnabled(false);
                    final String password = passwordView.getText().toString();
                    connect(buildWifiConfig(scan, password));
                })
                .setNegativeButton("取消", (d, which) -> {
                    connecting = null;
                    failed = false;
                    adapter.notifyDataSetChanged();
                })
                .show();
    }

    private WifiConfiguration buildWifiConfig(ScanResult scan) {
        final WifiConfiguration config = new WifiConfiguration();
        config.SSID = "\"" + scan.SSID + "\"";
        config.status = WifiConfiguration.Status.ENABLED;
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        return config;
    }

    private WifiConfiguration buildWifiConfig(ScanResult scan, String password) {
        final WifiConfiguration config = new WifiConfiguration();
        config.SSID = "\"" + scan.SSID + "\"";
        config.status = WifiConfiguration.Status.ENABLED;
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        config.preSharedKey = "\"" + password + "\"";
        return config;
    }

    private void connect(WifiConfiguration config) {
        final int networkId = wm.addNetwork(config);
        Log.d(TAG, "Network ID: " + networkId);
        connect(networkId);
    }

    private void connect(int networkId) {
        if (!wm.disconnect()) {
            Log.d(TAG, "Disconnect failed");
            failed = true;
            adapter.notifyDataSetChanged();
            return;
        }

        if (!wm.enableNetwork(networkId, true)) {
            Log.d(TAG, "Enable failed");
            failed = true;
            adapter.notifyDataSetChanged();
            return;
        }

        if (!wm.reconnect()) {
            Log.d(TAG, "Reconnect failed");
            failed = true;
            adapter.notifyDataSetChanged();
            return;
        }

        Log.d(TAG, "Connecting...");
    }

    private void handleWifiConfigFailed() {
        failed = true;
        adapter.notifyDataSetChanged();
    }

    private void handleWifiConfigSucceed() {
        connecting = null;
        failed = false;
        wm.startScan();
        if (activity != null)
            activity.startService(new Intent(activity, TimeService.class));
    }

    private class WifiAdapter extends RecyclerView.Adapter<WifiAdapter.ViewHolder> {
        private final LayoutInflater inflater;

        WifiAdapter(Context context) {
            inflater = LayoutInflater.from(context);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(inflater.inflate(R.layout.item_access_point, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            final ScanResult scan = scans.get(position);
            holder.bind(scan, configs.get(scan.SSID));
        }

        @Override
        public int getItemCount() {
            return scans == null ? 0 : scans.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private final ImageView icon;
            private final TextView ssid;
            private final TextView status;

            ViewHolder(View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.icon);
                ssid = itemView.findViewById(R.id.ssid);
                status = itemView.findViewById(R.id.status);
                itemView.setOnClickListener(v -> handleOnItemClick(scans.get(getLayoutPosition())));
                itemView.setOnLongClickListener(v -> handleOnItemLongClick(scans.get(getLayoutPosition())));
            }

            void bind(ScanResult scan, WifiConfiguration config) {
                if (isEncrypted(scan)) {
                    icon.setImageResource(R.drawable.ic_signal_wifi_4_bar_lock_white_24dp);
                } else {
                    icon.setImageResource(R.drawable.ic_signal_wifi_4_bar_white_24dp);
                }

                ssid.setText(scan.SSID);

                if (scan.SSID.equals(connecting)) {
                    status.setVisibility(View.VISIBLE);
                    status.setText(failed ? "密码错误" : "正在连接…");
                } else if (config != null) {
                    status.setVisibility(View.VISIBLE);
                    status.setText(scan.SSID.equals(connected) ? "已连接" : "已保存");
                } else {
                    status.setVisibility(View.GONE);
                }
            }
        }
    }

    private class ScanReceiver extends SelfBroadcastReceiver {
        ScanReceiver() {
            super(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        }

        @Override
        protected void onReceiveAction(String action, Intent intent) {
            handleScanResult();
        }
    }

    private class ConnectionReceiver extends SelfBroadcastReceiver {

        ConnectionReceiver() {
            super(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION,
                    ConnectivityManager.CONNECTIVITY_ACTION);
        }

        @Override
        protected void onReceiveAction(String action, Intent intent) {
            switch (action) {
                case WifiManager.SUPPLICANT_STATE_CHANGED_ACTION:
                    final int error = intent.getIntExtra(
                            WifiManager.EXTRA_SUPPLICANT_ERROR, -1);

                    if (error == WifiManager.ERROR_AUTHENTICATING) {
                        Log.e(TAG, "Wi-Fi authenticate failed");
                        handleWifiConfigFailed();
                    }
                    break;
                case ConnectivityManager.CONNECTIVITY_ACTION:
                    final NetworkInfo network = intent.getParcelableExtra(
                            ConnectivityManager.EXTRA_NETWORK_INFO);
                    final NetworkInfo.DetailedState detailed = network.getDetailedState();

                    if (detailed == NetworkInfo.DetailedState.CONNECTED) {
                        Log.d(TAG, "Wi-Fi connected");
                        handleWifiConfigSucceed();
                    }
                    break;
            }
        }

    }

}
