package com.iflytek.cyber.iot.show.core.weather;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface WeatherApi {

    @GET("/api/weather")
    Call<Weather> getWeather(@Header("Authorization") String authorization, @Query("location") String location);
}
