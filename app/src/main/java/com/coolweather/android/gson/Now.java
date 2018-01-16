package com.coolweather.android.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Sky on 2017/6/13.
 */

public class Now {

    @SerializedName("tmp")
    public String temprature;

    @SerializedName("cond")
    public More more;

    public class More{

        @SerializedName("txt")
        public String info;
    }
}
