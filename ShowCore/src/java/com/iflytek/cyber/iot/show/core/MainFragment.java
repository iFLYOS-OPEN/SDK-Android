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

package com.iflytek.cyber.iot.show.core;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Network;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.iflytek.cyber.iot.show.core.setup.WifiActivity;
import com.iflytek.cyber.iot.show.core.weather.Weather;
import com.iflytek.cyber.iot.show.core.weather.WeatherApi;
import com.iflytek.cyber.platform.internal.retrofit2.SimpleCallback;
import com.iflytek.cyber.resolver.templateruntime.GlideApp;

import java.lang.ref.SoftReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import okhttp3.OkHttpClient;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainFragment extends Fragment implements AMapLocationListener {

    private TextView clock;
    private TextView date;
    private ImageView ivWeather;
    private TextView tvWeather;
    private TextView tvTemperature;
    private ImageView ivNetwork;

    private WeatherApi weatherApi;

    private Handler handler = new Handler(Looper.getMainLooper());

    private Context context;

    private SettingsFragment settingsFragment;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        settingsFragment = new SettingsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        clock = view.findViewById(R.id.clock);
        date = view.findViewById(R.id.date);
        tvWeather = view.findViewById(R.id.tv_weather_desc);
        tvTemperature = view.findViewById(R.id.tv_temperature);
        ivWeather = view.findViewById(R.id.img_weather);
        ivNetwork = view.findViewById(R.id.network);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        updateCalendar(calendar);

        timerHandler.sendEmptyMessageDelayed(0, 1000);

        if (!GpsUtils.checkGpsEnable(context)) {
            GpsUtils.requestGps(context);
        }

        view.findViewById(R.id.settings).setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 23) {
                if (Settings.System.canWrite(getContext())) {
                    if (getFragmentManager() != null)
                        settingsFragment.show(getFragmentManager(), "Settings");
                } else {
                    new AlertDialog.Builder(getContext())
                            .setTitle("提示")
                            .setMessage("使用设置需要允许 修改系统设置 权限")
                            .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                                intent.setData(Uri.parse("package:com.iflytek.cyber.iot.show.core"));
                                startActivity(intent);
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                }
            } else if (getFragmentManager() != null)
                settingsFragment.show(getFragmentManager(), "Settings");
        });

        ivNetwork.setOnClickListener(v -> {
            startActivity(new Intent(context, WifiActivity.class));
        });
    }

    private void setupRetrofit() {
        final Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();

        final OkHttpClient client = new OkHttpClient.Builder()
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .client(client)
                .baseUrl("https://homeweb.iflyos.cn")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        weatherApi = retrofit.create(WeatherApi.class);
    }

    public AMapLocationClient mLocationClient;
    public AMapLocationClientOption mLocationOption = null;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setupRetrofit();

        mLocationClient = new AMapLocationClient(getContext());
        mLocationOption = new AMapLocationClientOption();
        mLocationClient.setLocationListener(this);
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        mLocationOption.setOnceLocation(true);
        mLocationClient.setLocationOption(mLocationOption);

        mLocationClient.startLocation();

        final WifiInfoManager manager = WifiInfoManager.getManager();

        manager.registerNetworkCallback(context,
                new WifiInfoManager.NetworkStateListener() {
                    @Override
                    public void onAvailable(Network network) {
                        ivNetwork.setClickable(false);
                    }

                    @Override
                    public void onLost(Network network) {
                        ivNetwork.setClickable(true);
                        ivNetwork.setImageResource(R.drawable.ic_baseline_wifi_error_24px);
                    }
                });

        updateNetworkRssi();
        manager.registerWifiRssiCallback(context, this::updateNetworkRssi);
    }

    private void updateNetworkRssi() {
        int level = WifiInfoManager.getManager().getWifiSignalLevel(context);
        if (level == 1) {
            ivNetwork.setImageResource(R.drawable.ic_signal_wifi_1_bar_black_24dp);
        } else if (level == 2) {
            ivNetwork.setImageResource(R.drawable.ic_signal_wifi_2_bar_black_24dp);
        } else if (level == 3) {
            ivNetwork.setImageResource(R.drawable.ic_signal_wifi_3_bar_black_24dp);
        } else if (level == 4) {
            ivNetwork.setImageResource(R.drawable.ic_signal_wifi_4_bar_white_24dp);
        } else {
            ivNetwork.setImageResource(R.drawable.ic_baseline_wifi_error_24px);
        }
    }

    private void updateCalendar(Calendar calendar) {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        clock.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));

        SimpleDateFormat format = new SimpleDateFormat("MM'月'dd'日' E", Locale.getDefault());
        date.setText(format.format(calendar.getTime()));
    }

    private void loadWeather(AMapLocation location) {
        final Context context = getContext();
        if (context == null) {
            return;
        }

        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String token = pref.getString("access_token", "");

        String authorization = "Bearer " + token;
        String currentLocation = String.format(Locale.CHINESE, "%.2f", location.getLongitude())
                + "," + String.format(Locale.CHINESE, "%.2f", location.getLatitude());
        weatherApi.getWeather(authorization, currentLocation)
                .enqueue(new SimpleCallback<Weather>() {
                    @Override
                    public void onSuccess(Weather body, Response<Weather> response) {
                        if (response.isSuccessful()) {
                            final Weather weather = response.body();
                            if (weather != null) {
                                updateUi(weather);
                            }
                        }
                    }

                    @Override
                    public void onHttpFailure(int code, JsonObject body, Response<Weather> response) {

                    }

                    @Override
                    public void onNetworkFailure(Throwable t) {
                        t.printStackTrace();
                    }
                });
    }

    private void updateUi(Weather weather) {
        final Context context = getContext();
        if (context != null) {
            GlideApp.with(context)
                    .load(weather.icon)
                    .centerCrop()
                    .into(ivWeather);
            tvTemperature.setText(String.format("%s ℃", weather.temperature));
            tvWeather.setText(weather.lifestyle);
        }
        startUpdateWeatherLoop();
    }

    private void startUpdateWeatherLoop() {
        handler.postDelayed(() -> mLocationClient.startLocation(), 3 * 60 * 60 * 1000);
    }

    private TimerHandler timerHandler = new TimerHandler(this);

    private static class TimerHandler extends Handler {
        private SoftReference<MainFragment> reference;

        TimerHandler(MainFragment mainFragment) {
            reference = new SoftReference<>(mainFragment);
        }

        @Override
        public void handleMessage(Message msg) {
            MainFragment fragment = reference.get();
            if (fragment != null && !fragment.isDetached()) {
                fragment.updateCalendar(Calendar.getInstance());
                sendEmptyMessageDelayed(0, 1000);
            }
        }
    }

    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (amapLocation != null) {
            if (amapLocation.getErrorCode() == 0) {
                loadWeather(amapLocation);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mLocationClient.stopLocation();
    }
}
