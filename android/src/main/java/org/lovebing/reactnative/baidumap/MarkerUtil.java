package org.lovebing.reactnative.baidumap;

import org.lovebing.reactnative.baidumap.ImageLoader.LoaderCallBack;
import android.util.Log;
import android.widget.Button;

import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.facebook.react.bridge.ReadableMap;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;

import android.widget.ImageView;
import android.widget.Toast;
import android.text.TextUtils;
import android.os.Environment;

import java.util.List;

import android.graphics.Canvas;  
import android.graphics.Color;  
import android.graphics.Paint;  
import android.graphics.Rect; 
import android.graphics.RectF; 
import android.graphics.PorterDuffXfermode; 
import android.graphics.PorterDuff;  

/**
 * Created by lovebing on Sept 28, 2016.
 */
public class MarkerUtil {

    public static void updateMaker(Marker maker, ReadableMap option) {
        LatLng position = getLatLngFromOption(option);
        maker.setPosition(position);
        maker.setTitle(option.getString("title"));
    }

    public static void addMarker(final MapView mapView,final ReadableMap option, final Context context,
            final List<Marker> markers, final Integer index) {

        //得到可用的图片  
        if (!TextUtils.isEmpty(option.getString("imgpath"))) {

            ImageLoader.loadImage(option.getString("photoId"), option.getString("imgpath"), new LoaderCallBack() {

                @Override
                public void onLoadOver(Bitmap bitmap) {

                  
                    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View view = inflater.inflate(R.layout.custom_headpoint, null);
                    ImageView imageView = (ImageView) view.findViewById(R.id.headimage);

                    imageView.setImageBitmap(bitmap);
                    //BitmapDescriptor bitmapDesc = BitmapDescriptorFactory.fromBitmap(bitmap);
                    BitmapDescriptor bitmapDesc = BitmapDescriptorFactory.fromView(view);

                    if (bitmapDesc == null) {
                        bitmapDesc = BitmapDescriptorFactory.fromResource(R.mipmap.icon_gcoding);
                    }

                    LatLng position = getLatLngFromOption(option);
                    OverlayOptions overlayOptions = new MarkerOptions().icon(bitmapDesc).position(position)
                            .title(option.getString("title"));

                    mapView.getMap().addOverlay(overlayOptions);
                   // markers.add(index, marker);
                }

                @Override
                public void onError(Exception e) {
                    e.printStackTrace();
                    Toast.makeText(context, "error : " + e.getMessage(), Toast.LENGTH_SHORT).show();

                }

                @Override
                public void onDecodeFile(Options options) {

                }

            });

        } else {
            BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromResource(R.mipmap.icon_gcoding);
            LatLng position = getLatLngFromOption(option);
            OverlayOptions overlayOptions = new MarkerOptions().icon(bitmapDescriptor).position(position)
                    .title(option.getString("title"));

           mapView.getMap().addOverlay(overlayOptions);
           // markers.add(index, marker);
        }

        // BitmapDescriptor bitmap = BitmapDescriptorFactory.fromPath(
        //         "F:\\ReactNativeProject\\ReactNativeF\\android\\app\\build\\intermediates\\res\\merged\\debug\\mipmap-hdpi-v4\\icon_gcoding.png");        
    }

    private static LatLng getLatLngFromOption(ReadableMap option) {
        double latitude = option.getDouble("latitude");
        double longitude = option.getDouble("longitude");
        return new LatLng(latitude, longitude);

    }

}
