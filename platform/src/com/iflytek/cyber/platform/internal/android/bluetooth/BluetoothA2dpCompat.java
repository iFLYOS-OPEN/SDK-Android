package com.iflytek.cyber.platform.internal.android.bluetooth;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;

public class BluetoothA2dpCompat {

    public static boolean connect(BluetoothA2dp a2dp, BluetoothDevice device) {
        try {
            return (boolean) a2dp.getClass()
                    .getMethod("connect", BluetoothDevice.class)
                    .invoke(a2dp, device);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
