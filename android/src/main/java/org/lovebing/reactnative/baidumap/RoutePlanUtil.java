package org.lovebing.reactnative.baidumap;

import java.util.List;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.model.LatLng;
import org.lovebing.reactnative.baidumap.overlayutil.BikingRouteOverlay;
import org.lovebing.reactnative.baidumap.overlayutil.DrivingRouteOverlay;
import org.lovebing.reactnative.baidumap.overlayutil.MassTransitRouteOverlay;
import org.lovebing.reactnative.baidumap.overlayutil.OverlayManager;
import org.lovebing.reactnative.baidumap.overlayutil.TransitRouteOverlay;
import org.lovebing.reactnative.baidumap.overlayutil.WalkingRouteOverlay;
import com.baidu.mapapi.search.core.RouteLine;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.route.BikingRouteLine;
import com.baidu.mapapi.search.route.BikingRoutePlanOption;
import com.baidu.mapapi.search.route.BikingRouteResult;
import com.baidu.mapapi.search.route.DrivingRouteLine;
import com.baidu.mapapi.search.route.DrivingRoutePlanOption;
import com.baidu.mapapi.search.route.DrivingRoutePlanOption.DrivingPolicy;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.IndoorRouteResult;
import com.baidu.mapapi.search.route.MassTransitRouteLine;
import com.baidu.mapapi.search.route.MassTransitRoutePlanOption;
import com.baidu.mapapi.search.route.MassTransitRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.PlanNode;
import com.baidu.mapapi.search.route.RoutePlanSearch; //路径规划搜索接口
import com.baidu.mapapi.search.route.TransitRouteLine;
import com.baidu.mapapi.search.route.TransitRoutePlanOption;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRouteLine;
import com.baidu.mapapi.search.route.WalkingRouteLine.WalkingStep;
import com.baidu.mapapi.search.route.WalkingRoutePlanOption;
import com.baidu.mapapi.search.route.WalkingRouteResult;

import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.search.route.DrivingRouteLine.DrivingStep;
import com.baidu.mapapi.map.Polyline;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import android.util.Log;

import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.uimanager.ThemedReactContext;
import java.util.ArrayList;

public class RoutePlanUtil implements OnGetRoutePlanResultListener {

    BaiduMap mBaidumap = null;

    // 搜索相关
    RoutePlanSearch mSearch = null; // 搜索模块，也可去掉地图模块独立使用       
    WalkingRouteResult nowResultwalk = null;
    BikingRouteResult nowResultbike = null;
    TransitRouteResult nowResultransit = null;
    DrivingRouteResult nowResultdrive = null;
    MassTransitRouteResult nowResultmass = null;

    private ThemedReactContext mReactContext;

    RouteLine route = null;
    OverlayManager routeOverlay = null;

    private TextView popupText = null; // 泡泡view

    boolean useDefaultIcon = false;

    int nowSearchType = -1; // 当前进行的检索，供判断浏览节点时结果使用。

    int walkingIndex = 0;
    int walkingLength = 0;
    //步行 路段所经过的地理坐标集合,（为支持多点步行，转为画折线需要该集合)
    List<LatLng> walkingWayPoints = new ArrayList<LatLng>();

    /**
     * 初始化搜索模块
     * @return
     */
    protected RoutePlanSearch getSearch() {
        if (mSearch != null) {
            mSearch.destroy();
        }
        // 初始化搜索模块，注册事件监听
        mSearch = RoutePlanSearch.newInstance(); //获取RoutePlan检索实例
        mSearch.setOnGetRoutePlanResultListener(this); //设置路线检索监听者

        return mSearch;
    }

    /***
     * 发起路线规划搜索示例
     * 
     */
    public void searchButtonProcess(final MapView mapView, final ReadableArray planNodeList,
            final ThemedReactContext context) {

        walkingWayPoints.clear();
        walkingLength = 0;
        walkingIndex = 0;

        mSearch = getSearch();
        mReactContext = context;
        // 重置浏览节点的路线数据
        route = null;
        //mapView.getMap().clear();

        mBaidumap = mapView.getMap();
        mBaidumap.clear();

        //drivingPlanSearch(mSearch,planNodeList);

        walkingPlanSearch(mSearch, planNodeList);
    }

    /**
     * 步行线路查询
     */
    private void walkingPlanSearch(final RoutePlanSearch mSearch, final ReadableArray planNodeList) {

        //添加Marker
        walkingLength = planNodeList.size();
        for (int i = 0; i < planNodeList.size(); i++) {
            ReadableMap nodeMap = planNodeList.getMap(i);
            int resourceId = 0;
            if (i == 0) {
                resourceId = R.drawable.icon_st;
            } else if (i == planNodeList.size() - 1) {
                resourceId = R.drawable.icon_en;
            } else {
                resourceId = R.drawable.icon_gcoding;
            }
            BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromResource(resourceId);
            LatLng position = new LatLng(nodeMap.getDouble("latitude"), nodeMap.getDouble("longitude"));
            OverlayOptions overlayOptions = new MarkerOptions().icon(bitmapDescriptor).position(position)
                    .title(nodeMap.getString("title"));

            mBaidumap.addOverlay(overlayOptions);
        }

        //步行路线规划
        for (int i = 0; i < planNodeList.size() - 1; i++) {

            walkingIndex = i;

            ReadableMap stMap = planNodeList.getMap(i);
            ReadableMap enMap = planNodeList.getMap(i + 1);
            // 设置起终点信息，
            LatLng stLatLng = new LatLng(stMap.getDouble("latitude"), stMap.getDouble("longitude"));
            LatLng enLatLng = new LatLng(enMap.getDouble("latitude"), enMap.getDouble("longitude"));
            PlanNode stNode = PlanNode.withLocation(stLatLng);
            PlanNode enNode = PlanNode.withLocation(enLatLng);

            mSearch.walkingSearch((new WalkingRoutePlanOption()).from(stNode).to(enNode)); //步行路线规划参数    
        }

    }

    /***
     * 驾车线路查询
     */
    private void drivingPlanSearch(final RoutePlanSearch mSearch, final ReadableArray planNodeList) {

        ReadableMap stMap = planNodeList.getMap(0);
        ReadableMap enMap = planNodeList.getMap(planNodeList.size() - 1);

        // 设置起终点信息，
        LatLng stLatLng = new LatLng(stMap.getDouble("latitude"), stMap.getDouble("longitude"));
        LatLng enLatLng = new LatLng(enMap.getDouble("latitude"), enMap.getDouble("longitude"));
        PlanNode stNode = PlanNode.withLocation(stLatLng);
        PlanNode enNode = PlanNode.withLocation(enLatLng);

        //途径点
        List passbyNode_list = new ArrayList();
        for (int i = 1; i < planNodeList.size() - 1; i++) {
            LatLng latlng = new LatLng(planNodeList.getMap(i).getDouble("latitude"),
                    planNodeList.getMap(i).getDouble("longitude"));
            PlanNode passby = PlanNode.withLocation(latlng);//途径点
            passbyNode_list.add(passby);
        }

        mSearch.drivingSearch((new DrivingRoutePlanOption()) //发起驾车路线规划
                .from(stNode) //驾车路线规划参数 from 设置起点 
                .to(enNode) //to 设置终点
                .passBy(passbyNode_list) //passBy 设置途径点
                .policy(DrivingPolicy.ECAR_TIME_FIRST)); //驾乘检索策略常量：时间优先
    }

    /**
     * 
     * 步行线路查询结果
     */
    @Override
    public void onGetWalkingRouteResult(WalkingRouteResult result) {

        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(mReactContext, "抱歉，未找到结果", Toast.LENGTH_SHORT).show();
        }
        if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {

            return;
        }
        if (result.error == SearchResult.ERRORNO.NO_ERROR) {

            //=================================//
            //两点间步行线路 Demo代码  
            // 直接显示
            /*
            route = result.getRouteLines().get(0);
            WalkingRouteOverlay overlay = new MyWalkingRouteOverlay(mBaidumap);
            mBaidumap.setOnMarkerClickListener(overlay);
            routeOverlay = overlay;
            overlay.setData(result.getRouteLines().get(0)); //设置数据，
            overlay.addToMap();     //将线路画在地图上
            overlay.zoomToSpan();   //缩放操作，显示整个线路
            */
            //===================================//

            //多点步行，收集 路段所经过的地理坐标
            final ArrayList<OverlayOptions> list = new ArrayList<OverlayOptions>();
            PolylineOptions object = new PolylineOptions();
            List<LatLng> arg0 = new ArrayList<LatLng>();
            List<WalkingStep> allStep = result.getRouteLines().get(0).getAllStep();

            for (int i = 0; i < allStep.size(); i++) {
                WalkingStep walkingStep = allStep.get(i);
                if(walkingStep.getWayPoints().size()>0){
                    arg0.addAll(walkingStep.getWayPoints());
                }
            }

            object.color(Color.GREEN).width(12).points(arg0);
                //.dottedLine(true)
                //.customTexture(BitmapDescriptorFactory.fromResource(R.drawable.icon_road_green_arrow));
            list.add(object);
            OverlayManager overlayManager = new OverlayManager(mBaidumap) {
                @Override
                public boolean onPolylineClick(Polyline arg0) {
                    return false;
                }

                @Override
                public boolean onMarkerClick(Marker arg0) {
                    return false;
                }

                @Override
                public List<OverlayOptions> getOverlayOptions() {
                    return list;
                }
            };
            overlayManager.addToMap();
            //overlayManager.zoomToSpan();

        } else {
            Toast.makeText(mReactContext, "walkingroute结果数<0", Toast.LENGTH_SHORT).show();
            return;
        }

    }

    public List<BitmapDescriptor> getCustomTextureList() {
        ArrayList<BitmapDescriptor> list = new ArrayList<BitmapDescriptor>();
        list.add(BitmapDescriptorFactory.fromResource(R.drawable.icon_road_green_arrow));
        return list;
    }

    @Override
    public void onGetTransitRouteResult(TransitRouteResult result) {
    }

    @Override
    public void onGetMassTransitRouteResult(MassTransitRouteResult result) {
    }

    @Override
    public void onGetBikingRouteResult(BikingRouteResult result) {

    }

    @Override
    public void onGetIndoorRouteResult(IndoorRouteResult indoorRouteResult) {

    }

    /***
     * 驾车线路查询结果
     */
    @Override
    public void onGetDrivingRouteResult(DrivingRouteResult result) {
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(mReactContext, "抱歉，未找到结果", Toast.LENGTH_SHORT).show();
        }
        if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
            // 起终点或途经点地址有岐义，通过以下接口获取建议查询信息
            // result.getSuggestAddrInfo()
            return;
        }
        if (result.error == SearchResult.ERRORNO.NO_ERROR) {
            //nodeIndex = -1;

            //驾车线路 Demo版
            // route = result.getRouteLines().get(0);
            // DrivingRouteOverlay overlay = new DrivingRouteOverlay(mBaidumap);
            // routeOverlay = overlay;
            // mBaidumap.setOnMarkerClickListener(overlay);
            // overlay.setData(result.getRouteLines().get(0));
            // overlay.addToMap();
            // overlay.zoomToSpan();

            drivingLineWithoutIcon(result.getRouteLines().get(0)); //折线无转弯节点版
        }
    }

    /***
     * 驾车线路 改为 画折线
     */
    private void drivingLineWithoutIcon(DrivingRouteLine drivingRouteLine) {
        final ArrayList<OverlayOptions> list = new ArrayList<OverlayOptions>();
        PolylineOptions object = new PolylineOptions();
        List<LatLng> arg0 = new ArrayList<LatLng>();
        List<DrivingStep> allStep = drivingRouteLine.getAllStep();
        for (int i = 0; i < allStep.size(); i++) {
            DrivingStep drivingStep = allStep.get(i);
            List<LatLng> wayPoints = drivingStep.getWayPoints();
            arg0.addAll(wayPoints);
        }
        object.points(arg0);

        list.add(object);
        OverlayManager overlayManager = new OverlayManager(mBaidumap) {
            @Override
            public boolean onPolylineClick(Polyline arg0) {
                return false;
            }

            @Override
            public boolean onMarkerClick(Marker arg0) {
                return false;
            }

            @Override
            public List<OverlayOptions> getOverlayOptions() {
                return list;
            }
        };
        overlayManager.addToMap();
        //overlayManager.zoomToSpan();
    }

    /***
    * 步行线路 改为 画折线
    */
    private void walkingLineWithPoints(List<LatLng> walkingWayPoints) {

        final ArrayList<OverlayOptions> list = new ArrayList<OverlayOptions>();
        PolylineOptions object = new PolylineOptions();

        object.points(walkingWayPoints);

        list.add(object);
        OverlayManager overlayManager = new OverlayManager(mBaidumap) {
            @Override
            public boolean onPolylineClick(Polyline arg0) {
                return false;
            }

            @Override
            public boolean onMarkerClick(Marker arg0) {
                return false;
            }

            @Override
            public List<OverlayOptions> getOverlayOptions() {
                return list;
            }
        };
        overlayManager.addToMap();
        
    }

    /**
     * 定制RouteOverly
     */
    private class MyWalkingRouteOverlay extends WalkingRouteOverlay {

        public MyWalkingRouteOverlay(BaiduMap baiduMap) {
            super(baiduMap);
        }

        @Override
        public BitmapDescriptor getStartMarker() {

            return null;
        }

        @Override
        public BitmapDescriptor getTerminalMarker() {

            return null;
        }

        @Override
        public boolean onRouteNodeClick(int i) {
            return false;
        }

    }

}
