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
