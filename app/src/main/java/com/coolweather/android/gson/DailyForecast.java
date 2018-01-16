package com.coolweather.android.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Sky on 2017/6/13.
 */

public class DailyForecast {

    public String date;

    @SerializedName("tmp")
    public Temprature temprature;

    @SerializedName("cond")
    public More more;

    public class Temprature{
        public String max;

        public String min;
    }

    public class More{

        @SerializedName("txt_d")
        public String info;
    }
}
