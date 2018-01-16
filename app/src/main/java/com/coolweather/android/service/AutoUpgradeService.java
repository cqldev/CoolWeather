package com.coolweather.android.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.coolweather.android.R;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AutoUpgradeService extends Service {
    public AutoUpgradeService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(0, getNotification());  //id = 0 时下拉栏中不显示该通知

        updateWeatherInfo();
        updateBingPic();

        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        long delay = 20 * 1000;
        long triggerAtTime = delay + SystemClock.elapsedRealtime();
        Intent intent1 = new Intent(this, AutoUpgradeService.class);
        PendingIntent pi = PendingIntent.getService(this, 0, intent1, 0);
        manager.cancel(pi);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);

        return super.onStartCommand(intent, flags, startId);
    }

    public void updateWeatherInfo() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherInfo = preferences.getString("weather", null);
        if (weatherInfo != null) {
            Weather weather = Utility.handleWeatherResponse(weatherInfo);
            String weatherId = weather.basic.weatherId;
            String url = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=c2183ec155e14fdeaa2737d1c7934b6f";
            HttpUtil.sendHttpRequest(url, new Callback() {
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String weatherResponse = response.body().string();
                    Weather weather = Utility.handleWeatherResponse(weatherResponse);
                    if (weather != null && "ok".equals(weather.status)) {
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("weather", weatherResponse);
                    }
                }

                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }
            });
        }

    }

    public void updateBingPic() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String bingPicUrl = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendHttpRequest(bingPicUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("bing_pic", response.body().string());
            }
        });

    }

    public Notification getNotification(){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentTitle("this is weather service")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));

        return builder.build();
    }


}
