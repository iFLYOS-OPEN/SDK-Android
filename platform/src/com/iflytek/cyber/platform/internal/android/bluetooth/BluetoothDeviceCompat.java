package com.iflytek.cyber.platform.internal.android.bluetooth;

import android.bluetooth.BluetoothDevice;

public class BluetoothDeviceCompat {

    public static boolean removeBond(BluetoothDevice device) {
        try {
            return (boolean) device.getClass()
                    .getMethod("removeBond")
                    .invoke(device);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
