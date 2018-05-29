package com.iflytek.cyber.resolver.templateruntime.fragment.weather;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.view.ContextThemeWrapper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.iflytek.cyber.resolver.templateruntime.Constant;
import com.iflytek.cyber.resolver.templateruntime.GlideApp;
import com.iflytek.cyber.resolver.templateruntime.R;
import com.iflytek.cyber.resolver.templateruntime.model.Image;

public class WeatherFragment extends Fragment {
    private LinearLayout futureDaysWeatherContainer;
    private TextView currentTemperature;
    private TextView todayTemperatureRange;
    private TextView today;
    private TextView position;
    private ImageView todayWeatherIcon;
    private JsonObject payload;

    public static WeatherFragment generate(JsonObject payload) {
        WeatherFragment weatherFragment = new WeatherFragment();
        weatherFragment.payload = payload;
        return weatherFragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_weather, container, false);
        futureDaysWeatherContainer = view.findViewById(R.id.future_days_weather);
        currentTemperature = view.findViewById(R.id.current_temperature);
        position = view.findViewById(R.id.position);
        today = view.findViewById(R.id.today);
        todayTemperatureRange = view.findViewById(R.id.today_temperature_range);
        todayWeatherIcon = view.findViewById(R.id.current_weather_icon);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (payload == null) {
            Log.e(Constant.TYPE_WEATHER_TEMPLATE, "Cannot receive payload data");
            return;
        }
        try {
            JsonArray weatherForecastArray = payload.getAsJsonArray(Constant.PAYLOAD_WEATHER_FORECAST);
            LinearLayout.LayoutParams layoutParams =
                    new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.weight = 1;
            layoutParams.leftMargin = getResources().getDimensionPixelSize(R.dimen.weatherItemHorizontalMargin);
            layoutParams.rightMargin = getResources().getDimensionPixelSize(R.dimen.weatherItemHorizontalMargin);
            layoutParams.topMargin = getResources().getDimensionPixelSize(R.dimen.weatherItemVerticalMargin);
            layoutParams.bottomMargin = getResources().getDimensionPixelSize(R.dimen.weatherItemVerticalMargin);
            int padding = getResources().getDimensionPixelSize(R.dimen.weatherContainerPadding);
            futureDaysWeatherContainer.setPadding(padding, 0, padding, 0);
            futureDaysWeatherContainer.setGravity(Gravity.CENTER);
            for (int i = 0; i < weatherForecastArray.size(); i++) {
                futureDaysWeatherContainer.addView(
                        generateWeatherItem(weatherForecastArray.get(i).getAsJsonObject()),
                        layoutParams);
            }
            currentTemperature.setText(payload.get(Constant.PAYLOAD_CURRENT_WEATHER).getAsString());
            JsonObject title = payload.getAsJsonObject(Constant.PAYLOAD_TITLE);
            today.setText(title.get(Constant.PAYLOAD_SUB_TITLE).getAsString());
            position.setText(title.get(Constant.PAYLOAD_MAIN_TITLE).getAsString());
            JsonObject currentWeatherIcon = payload.getAsJsonObject(Constant.PAYLOAD_CURRENT_WEATHER_ICON);
            if (!currentWeatherIcon.isJsonNull()) {
                Image img = new Gson().fromJson(currentWeatherIcon, Image.class);
                if (img.sources != null && img.sources.size() > 0) {
                    GlideApp.with(getContext())
                            .load(img.sources.get(0).url)
                            .into(todayWeatherIcon);
                }
            }
            JsonObject lowTemperature = payload.getAsJsonObject(Constant.PAYLOAD_LOW_TEMPERATURE);
            String lowValue = lowTemperature.get(Constant.PAYLOAD_VALUE).getAsString();
            JsonObject highTemperature = payload.getAsJsonObject(Constant.PAYLOAD_HIGH_TEMPERATURE);
            String highValue = highTemperature.get(Constant.PAYLOAD_VALUE).getAsString();
            todayTemperatureRange.setText(lowValue + " ~ " + highValue);
        } catch (Exception e) {
            Log.e(Constant.TYPE_WEATHER_TEMPLATE, "Cannot analyze payload data");
            e.printStackTrace();
        }
    }

    private View generateWeatherItem(JsonObject weatherForecast) {
        LinearLayout linearLayout = new LinearLayout(new ContextThemeWrapper(getContext(), R.style.WeatherTheme));
        int verticalPadding = getResources().getDimensionPixelSize(R.dimen.weatherItemVerticalPadding);
        int horizontalPadding = getResources().getDimensionPixelSize(R.dimen.weatherItemHorizontalPadding);
        linearLayout.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
        linearLayout.setGravity(Gravity.CENTER);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        ImageView imageView = new ImageView(new ContextThemeWrapper(getContext(), R.style.WeatherTheme));
        JsonObject image = weatherForecast.get(Constant.PAYLOAD_IMAGE).getAsJsonObject();
        if (!image.isJsonNull()) {
            Image img = new Gson().fromJson(image, Image.class);
            if (img.sources != null && img.sources.size() > 0) {
                GlideApp.with(getContext())
                        .load(img.sources.get(0).url)
                        .into(imageView);
            }
        }
        int weatherIconSize = getResources().getDimensionPixelSize(R.dimen.weatherIconSize);
        linearLayout.addView(imageView, new LinearLayout.LayoutParams(weatherIconSize, weatherIconSize));
        TextView weekday = new TextView(new ContextThemeWrapper(getContext(), R.style.WeatherTheme));
        weekday.setText(weatherForecast.get(Constant.PAYLOAD_DATE).getAsString());
        TextViewCompat.setTextAppearance(weekday, R.style.TextAppearance_AppCompat_Small);
        weekday.setTextSize(20);
        weekday.setTextColor(ContextCompat.getColor(getContext(), R.color.textColorDarkPrimary));
        linearLayout.addView(weekday, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView temperatureRange = new TextView(new ContextThemeWrapper(getContext(), R.style.WeatherTheme));
        TextViewCompat.setTextAppearance(temperatureRange, R.style.TextAppearance_AppCompat_Body1);
        temperatureRange.setTextColor(ContextCompat.getColor(getContext(), R.color.textColorDarkSecondary));
        temperatureRange.setTextSize(20);
        String lowValue = weatherForecast.get(Constant.PAYLOAD_LOW_TEMPERATURE).getAsString();
        String highValue = weatherForecast.get(Constant.PAYLOAD_HIGH_TEMPERATURE).getAsString();
        temperatureRange.setText(lowValue + " ~ " + highValue);
        linearLayout.addView(temperatureRange, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return linearLayout;
    }
}
