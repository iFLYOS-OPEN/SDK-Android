package com.iflytek.cyber.iot.show.core;


import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;

public class GpsUtils {

    private static void openGpsSettings(Context context) {
        context.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    }

    public static boolean checkGpsEnable(Context context) {
        final LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return manager != null && manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public static void requestGps(final Context context) {
        new AlertDialog.Builder(context)
                .setPositiveButton("开启", (dialogInterface, i) -> openGpsSettings(context))
                .setNegativeButton("取消", null)
                .setMessage("开启位置信息，获取更准确的天气信息")
                .show();
    }
}
