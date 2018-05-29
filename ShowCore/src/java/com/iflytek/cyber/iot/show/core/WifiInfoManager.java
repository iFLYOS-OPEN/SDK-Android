package com.iflytek.cyber.iot.show.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class WifiInfoManager {

    private static final WifiInfoManager sManager = new WifiInfoManager();

    private List<NetworkStateListener> listeners = new ArrayList<>();
    private List<WifiRssiListener> rssiListeners = new ArrayList<>();

    private Handler uiHandler = new Handler(Looper.getMainLooper());

    public static WifiInfoManager getManager() {
        return sManager;
    }

    @Nullable
    public WifiInfo getWifiInfo(Context context) {
        final WifiManager manager = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        if (manager != null) {
            return manager.getConnectionInfo();
        } else {
            return null;
        }
    }

    public int getWifiSignalLevel(Context context) {
        final WifiInfo wifiInfo = getWifiInfo(context);
        if (wifiInfo != null) {
            return WifiManager.calculateSignalLevel(wifiInfo.getRssi(), 5);
        } else {
            return 0;
        }
    }

    public boolean isNetworkAvailable(Context context) {
        final ConnectivityManager manager = (ConnectivityManager) context.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager != null) {
            final NetworkInfo networkInfo = manager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isAvailable();
        } else {
            return false;
        }
    }

    public void registerNetworkCallback(Context context, @NonNull NetworkStateListener networkStateListener) {
        listeners.add(networkStateListener);
        final ConnectivityManager manager = (ConnectivityManager) context.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager != null) {
            final NetworkRequest request = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build();
            manager.registerNetworkCallback(request, networkCallback);
        }
    }

    public void unregisterNetworkCallback(Context context) {
        final ConnectivityManager manager = (ConnectivityManager) context.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager != null) {
            manager.unregisterNetworkCallback(networkCallback);
        }
    }

    public void registerWifiRssiCallback(Context context, WifiRssiListener listener) {
        rssiListeners.add(listener);
        context.registerReceiver(receiver, new IntentFilter(WifiManager.RSSI_CHANGED_ACTION));
    }

    public void unregisterWifiRssiCallback(Context context) {
        if (receiver != null) {
            context.unregisterReceiver(receiver);
        }
        receiver = null;
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            for (WifiRssiListener listener : rssiListeners) {
                listener.onChange();
            }
        }
    };

    private ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            super.onAvailable(network);
            uiHandler.post(() -> {
                for (NetworkStateListener listener : listeners) {
                    listener.onAvailable(network);
                }
            });
        }

        @Override
        public void onLost(Network network) {
            super.onLost(network);
            uiHandler.post(() -> {
                for (NetworkStateListener listener : listeners) {
                    listener.onLost(network);
                }
            });
        }
    };

    public interface NetworkStateListener {
        void onAvailable(Network network);
        void onLost(Network network);
    }

    public interface WifiRssiListener {
        void onChange();
    }
}
