package com.coolweather.android.gson;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by Sky on 2017/6/13.
 */

public class Weather {

    public AQI aqi;

    public Basic basic;

    @SerializedName("daily_forecast")
    public List<DailyForecast> dailyForecastList;

//    @SerializedName("hourly_forecast")
//    public List<HourForecast> hourForecastList;

    public Suggestion suggestion;

    public Now now;

    public String status;

}
