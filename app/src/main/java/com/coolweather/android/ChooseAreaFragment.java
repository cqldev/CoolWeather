package com.coolweather.android;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.coolweather.android.db.City;
import com.coolweather.android.db.County;
import com.coolweather.android.db.Province;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by Sky on 2017/6/9.
 */

public class ChooseAreaFragment extends Fragment {

    private static final String TAG = "ChooseAreaFragment";

    private static final int LEVEL_PROVINCE = 0;

    private static final int LEVEL_CITY = 1;

    private static final int LEVEL_COUNTY = 2;

    private TextView title;

    private Button back;

    private ListView listView;

    private ProgressDialog progressDialog;

    private ArrayAdapter<String> adapter;

    private List<String> dataList = new ArrayList<>();

    /**
     * 所有省列表
     */
    private List<Province> provincesList;

    /**
     * 选中省的所有市列表
     */
    private List<City> cityList;

    /**
     * 选中市的所有县列表
     */
    private List<County> countyList;

    /**
     * 当前选中的省
     */
    private Province selProvince;

    /**
     * 当前选中的市
     */
    private City selCity;

    /**
     * 当前选中的县
     */
    private County selCounty;

    /**
     * 当前所在的省市区级别
     */
    private int curLevel;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);



        title = (TextView) view.findViewById(R.id.tile);
        back = (Button) view.findViewById(R.id.back);
        listView = (ListView) view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (curLevel == LEVEL_PROVINCE) {
                    selProvince = provincesList.get(position);
                    queryCities();
                } else if (curLevel == LEVEL_CITY) {
                    selCity = cityList.get(position);
                    queryCounties();
                } else if (curLevel == LEVEL_COUNTY) {
                    String weatherId = countyList.get(position).getWeatherId();
                    if (getActivity() instanceof MainActivity) {
                        Intent intent = new Intent(getActivity(), WeatherActivity.class);
                        intent.putExtra("weather_id", weatherId);
                        startActivity(intent);
                        getActivity().finish();
                    } else if (getActivity() instanceof WeatherActivity) {
                        WeatherActivity weatherActivity = (WeatherActivity) getActivity();
                        weatherActivity.drawerLayout.closeDrawers();
                        weatherActivity.swipeRefresh.setRefreshing(true);
                        weatherActivity.setWeatherId(weatherId);
                        weatherActivity.requestWeather();
                    }
                }
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (curLevel == LEVEL_CITY) {
                    queryProvince();
                } else if (curLevel == LEVEL_COUNTY) {
                    queryCities();
                }
            }
        });
        queryProvince();
    }

    /**
     * 查询所有省的数据，优先查询数据库，若没有则从服务端获取
     */
    public void queryProvince() {
        title.setText(R.string.china);
        back.setVisibility(View.GONE);
        provincesList = DataSupport.findAll(Province.class);
        if (provincesList.size() > 0) {
            dataList.clear();
            for (Province province : provincesList) {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            curLevel = LEVEL_PROVINCE;
        } else {
            queryServer("http://guolin.tech/api/china", "province");
        }
    }

    /**
     * 查询当前省的所有城市，优先查询数据库，若没有则从服务器获取
     */
    public void queryCities() {
        title.setText(selProvince.getProvinceName());
        back.setVisibility(View.VISIBLE);
        cityList = DataSupport.where("provinceid = ?", String.valueOf(selProvince.getProvinceCode())).find(City.class);
        if (cityList.size() > 0) {
            dataList.clear();
            for (City city : cityList) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            curLevel = LEVEL_CITY;
        } else {
            queryServer("http://guolin.tech/api/china/" + selProvince.getProvinceCode(), "city");
        }
    }

    public void queryCounties() {
        title.setText(selCity.getCityName());
        back.setVisibility(View.VISIBLE);
        countyList = DataSupport.where("cityid = ?", String.valueOf(selCity.getCityCode())).find(County.class);
        if (countyList.size() > 0) {
            dataList.clear();
            for (County county : countyList) {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            curLevel = LEVEL_COUNTY;
        } else {
            queryServer("http://guolin.tech/api/china/" + selCity.getProvinceId() + "/" + selCity.getCityCode(), "county");
        }
    }

    public void queryServer(String address, final String type) {

        showProgressDialog();
        HttpUtil.sendHttpRequest(address, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String resBody = response.body().string();
                boolean result = false;
                if ("province".equals(type)) {
                    result = Utility.handleProvinceResponse(resBody);
                } else if ("city".equals(type)) {
                    result = Utility.handleCityResponse(resBody, selProvince.getProvinceCode());
                } else if ("county".equals(type)) {
                    result = Utility.handleCountyResponse(resBody, selCity.getCityCode());
                }

                if (result) {
                    dismissProgressDialog();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if ("province".equals(type)) {
                                queryProvince();
                            } else if ("city".equals(type)) {
                                queryCities();
                            } else if ("county".equals(type)) {
                                queryCounties();
                            }
                        }
                    });
                }

            }

            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dismissProgressDialog();
                        Toast.makeText(getActivity(), R.string.loadFailed, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    public void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载中...");
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setCancelable(true);
        }
        progressDialog.show();
    }

    public void dismissProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

}
