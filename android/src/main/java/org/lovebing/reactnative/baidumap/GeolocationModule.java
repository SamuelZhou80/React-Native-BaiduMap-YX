package org.lovebing.reactnative.baidumap;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeOption;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.baidu.mapapi.utils.CoordinateConverter;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;

/**
 * Created by lovebing on 2016/10/28.
 */
public class GeolocationModule extends BaseModule
        implements BDLocationListener, OnGetGeoCoderResultListener {

    private LocationClient locationClient;
    private static GeoCoder geoCoder;
    private String mPrevLocTime;
    private BDLocation mCurLocation;

    public GeolocationModule(ReactApplicationContext reactContext) {
        super(reactContext);
        context = reactContext;
    }

    public String getName() {
        return "BaiduGeolocationModule";
    }

    private void initLocationClient() {
        LocationClientOption option = new LocationClientOption();
        option.setCoorType("gcj02");
        option.setIsNeedAddress(true);
        option.setIsNeedAltitude(true);
        option.setIsNeedLocationDescribe(true);
        option.setIsNeedLocationPoiList(true);
        option.setOpenGps(true);
        option.setScanSpan(10000); // 设置定位间隔为10秒钟
        locationClient = new LocationClient(context.getApplicationContext());
        locationClient.setLocOption(option);
        locationClient.registerLocationListener(this);
        locationClient.start();
    }

    /**
     *
     * @return
     */
    protected GeoCoder getGeoCoder() {
        if(geoCoder != null) {
            geoCoder.destroy();
        }
        geoCoder = GeoCoder.newInstance();
        geoCoder.setOnGetGeoCodeResultListener(this);
        return geoCoder;
    }

    /**
     *
     * @param sourceLatLng
     * @return
     */
    protected LatLng getBaiduCoorFromGPSCoor(LatLng sourceLatLng) {
        CoordinateConverter converter = new CoordinateConverter();
        converter.from(CoordinateConverter.CoordType.GPS);
        converter.coord(sourceLatLng);
        LatLng desLatLng = converter.convert();
        return desLatLng;
    }

    @ReactMethod
    public void startLocation(Promise promise) {
        if (locationClient == null) {
            initLocationClient();
        } else if (!locationClient.isStarted()) {
            locationClient.start();
        }
        promise.resolve(1);
    }

    @ReactMethod
    public void stopLocation(Promise promise) {
        if (locationClient != null) {
            locationClient.stop();
        }
        promise.resolve(1);
    }

    @ReactMethod
    public void getCurrentPosition(Promise promise) {
        if (locationClient == null) {
            initLocationClient();
        } else if (!locationClient.isStarted()) {
            locationClient.start();
        }

        WritableMap params = Arguments.createMap();
        if (mCurLocation != null) {
            int loactionStatus = 0;
            int locType = mCurLocation.getLocType();
            if (locType == BDLocation.TypeGpsLocation) {
                loactionStatus = 1; // GPS定位
            } else if (locType == BDLocation.TypeNetWorkLocation) {
                loactionStatus = 2; // 网络定位
            } else if (locType >= 162 && locType <= 167) {
                loactionStatus = locType; // "服务端定位失败"
            } else if (locType > 500 && locType <= 700) {
                loactionStatus = locType; // "key验证失败"
            }
            params.putInt("locationType", loactionStatus);
            params.putDouble("latitude", mCurLocation.getLatitude());
            params.putDouble("longitude", mCurLocation.getLongitude());
            params.putDouble("direction", mCurLocation.getDirection());
            params.putDouble("altitude", mCurLocation.getAltitude());
            params.putDouble("radius", mCurLocation.getRadius());
            params.putString("address", mCurLocation.getAddrStr());
            params.putString("countryCode", mCurLocation.getCountryCode());
            params.putString("country", mCurLocation.getCountry());
            params.putString("province", mCurLocation.getProvince());
            params.putString("cityCode", mCurLocation.getCityCode());
            params.putString("city", mCurLocation.getCity());
            params.putString("district", mCurLocation.getDistrict());
            params.putString("street", mCurLocation.getStreet());
            params.putString("streetNumber", mCurLocation.getStreetNumber());
            params.putString("buildingId", mCurLocation.getBuildingID());
            params.putString("buildingName", mCurLocation.getBuildingName());
            params.putString("locationTime", mCurLocation.getTime());
            params.putString("locationdesc", mCurLocation.getLocationDescribe()); // 位置描述信息
            if (mCurLocation.getPoiList() != null && mCurLocation.getPoiList().size() > 0) {
                params.putString("poi", mCurLocation.getPoiList().get(0).getName());
            } else {
                params.putString("poi", "no poi");
            }
            promise.resolve(params);
        } else {
            params.putInt("locationType", 0);
            params.putDouble("latitude", 0);
            params.putDouble("longitude", 0);
            params.putString("address", "");
            params.putString("locationdesc", ""); // 位置描述信息
            params.putString("poi", "no poi");
            promise.resolve(params);
        }
    }

    @ReactMethod
    public void geocode(String city, String addr) {
        getGeoCoder().geocode(new GeoCodeOption()
                .city(city).address(addr));
    }

    @ReactMethod
    public void reverseGeoCode(double lat, double lng) {
        getGeoCoder().reverseGeoCode(new ReverseGeoCodeOption()
                .location(new LatLng(lat, lng)));
    }

    @ReactMethod
    public void reverseGeoCodeGPS(double lat, double lng) {
        getGeoCoder().reverseGeoCode(new ReverseGeoCodeOption()
                .location(getBaiduCoorFromGPSCoor(new LatLng(lat, lng))));
    }

    @Override
    public void onReceiveLocation(BDLocation bdLocation) {
        if (bdLocation != null) {
            // 当本次未定位时, 判断上次定位时间在一分钟以内的取上次定位数据
            int locType = bdLocation.getLocType();
            String time = bdLocation.getTime();
            if (locType != BDLocation.TypeGpsLocation && locType != BDLocation.TypeNetWorkLocation) {
                if (mPrevLocTime == null || getDateTimeDiffer(time, mPrevLocTime) > 60) {
                    mCurLocation = bdLocation;
                    mPrevLocTime = time;
                }
            } else {
                mCurLocation = bdLocation;
                mPrevLocTime = time;
            }
        }
    }

    @Override
    public void onGetGeoCodeResult(GeoCodeResult result) {
        WritableMap params = Arguments.createMap();
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            params.putInt("errcode", -1);
        }
        else {
            params.putDouble("latitude",  result.getLocation().latitude);
            params.putDouble("longitude",  result.getLocation().longitude);
        }
        sendEvent("onGetGeoCodeResult", params);
    }

    @Override
    public void onGetReverseGeoCodeResult(ReverseGeoCodeResult result) {
        WritableMap params = Arguments.createMap();
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            params.putInt("errcode", -1);
        }
        else {
            ReverseGeoCodeResult.AddressComponent addressComponent = result.getAddressDetail();
            params.putString("address", result.getAddress());
            params.putString("province", addressComponent.province);
            params.putString("city", addressComponent.city);
            params.putString("district", addressComponent.district);
            params.putString("street", addressComponent.street);
            params.putString("streetNumber", addressComponent.streetNumber);
        }
        sendEvent("onGetReverseGeoCodeResult", params);
    }
    
    public void onConnectHotSpotMessage(String s, int i){

    }
    
    /**
     * 获取两个输入时间的差值(YYYY-MM-dd HH:mm:ss格式输入)
     * 
     * @return 单位s
     */
    public long getDateTimeDiffer(String startTime, String endTime) {
        SimpleDateFormat dfs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date begin;
        Date end;
        long between = 0;
        try {
            begin = dfs.parse(startTime);
            end = dfs.parse(endTime);
            between = (end.getTime() - begin.getTime()) / 1000;// 除以1000是为了转换成秒
        } catch (ParseException e) {
            e.printStackTrace();
            return 100000;
        }

        return Math.abs(between);
    }
}
