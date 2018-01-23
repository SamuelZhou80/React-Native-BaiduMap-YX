package org.lovebing.reactnative.baidumap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import android.os.Environment;
import java.net.URL;
import java.nio.channels.FileLock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;

/**
 * 图片数据加载类
 * @author chn 2016年3月4日 15:08:10
 *
 */
abstract public class ImageLoader {

    public static final String SDCARD_DIR = Environment.getExternalStorageDirectory().getPath();

    private static ExecutorService mExecutor = Executors.newFixedThreadPool(5);
    private static final String cachePath = SDCARD_DIR + "/rncrm/headicon/";

    /**
     * 加载图片
     * @param photoId
     * @param url
     * @param callback
     */
    public static void loadImage(final String photoId, final String url, final LoaderCallBack callback) {

        final Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.obj != null) {
                    callback.onLoadOver((Bitmap) msg.obj);
                }
            }
        };
        mExecutor.execute(new Runnable() {

            @Override
            public void run() {
                // 目录不存在则先创建
                File dirFile = new File(cachePath);
                if (!dirFile.exists()) {
                    dirFile.mkdirs();
                }
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                String sPath = cachePath + photoId ;
                Bitmap bitmap;
                // 缓存对象查找
                bitmap = loadFromCache(sPath, callback);
                if (bitmap != null) {
                    Message newmsg = handler.obtainMessage(0, bitmap);
                    handler.sendMessage(newmsg);
                    return;
                }

                try {
                    // 没有就下载
                    InputStream is = loadFromNet(url);
                    // 写入缓存
                    writeToCache(sPath, is);

                    is.close();

                } catch (Exception e) {
                    callback.onError(e);
                }

                try {
                    // //预处理 用于获取图片原图大小
                    Options options = new Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(sPath, options);
                    // //获取需求规格的图片
                    // callback.onDecodeFile(options);
                    // options.inJustDecodeBounds = false;

                    // //如果宽高超过屏幕宽进行特殊处理
                    // int mSize=HardWare.getScreenWidth();
                    // if(options.outWidth>mSize&&options.inSampleSize==1){
                    //     int  heightScale= Math.round(options.outHeight/mSize); 
                    //     int  widthScale= Math.round(options.outWidth/mSize);
                    //     options.inSampleSize=heightScale>widthScale?heightScale:widthScale;
                    // }

                    options.inJustDecodeBounds = false;
                    options.inSampleSize = getFitInSampleSize(35, 35, options);

                    bitmap = BitmapFactory.decodeFile(sPath, options);
                } catch (Exception e) {
                    e.printStackTrace();
                    
                }

                if (bitmap != null) {
                    Message newmsg = handler.obtainMessage(0, bitmap);
                    handler.sendMessage(newmsg);
                }
            }
        });

    }

    private static Bitmap loadFromCache(String path, LoaderCallBack callback) {
        File file;
        Bitmap bitmap = null;
        // 缓存对象查找
        try {
            file = new File(path);
            //预处理 用于获取图片原图数据
            Options options = new Options();
            options.inJustDecodeBounds = true;
            bitmap = BitmapFactory.decodeFile(file.getPath(), options);

            // //获取需求规格的图片
            // callback.onDecodeFile(options);
            // options.inPreferredConfig = Config.RGB_565;
            // options.inJustDecodeBounds = false;
            // //如果宽高超过屏幕宽进行特殊处理
            // int mSize=HardWare.getScreenWidth();
            // if(options.outWidth>mSize&&options.inSampleSize==1){
            //     int  heightScale= Math.round(options.outHeight/mSize); 
            //     int  widthScale= Math.round(options.outWidth/mSize);
            //     options.inSampleSize=heightScale>widthScale?heightScale:widthScale;
            // }

            options.inPreferredConfig = Config.RGB_565;
            options.inJustDecodeBounds = false;
            options.inSampleSize = getFitInSampleSize(44, 44, options);
            bitmap = BitmapFactory.decodeFile(file.getPath(), options);
            if (bitmap != null) {
                return bitmap;
            } else {
                // 可能错误的文件删除
                new File(path).delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int getFitInSampleSize(int reqWidth, int reqHeight, Options options) {
        int inSampleSize = 1;
        if (options.outWidth > reqWidth || options.outHeight > reqHeight) {
            int widthRatio = Math.round((float) options.outWidth / (float) reqWidth);
            int heightRatio = Math.round((float) options.outHeight / (float) reqHeight);
            inSampleSize = Math.min(widthRatio, heightRatio);
        }
        return inSampleSize;
    }

    /**
     * 根据url从网络获取图片
     * @param urlString
     * @return
     * @throws MalformedURLException 
     */
    private static InputStream loadFromNet(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        conn.setConnectTimeout(6 * 1000);
        conn.setDoInput(true);
        conn.connect();
        // 将得到的数据转化成InputStream
        return conn.getInputStream();
    }

    /**
     * 写图片文件
     * @param path
     * @param is
     * @throws Exception
     */
    private static void writeToCache(String path, InputStream is) throws Exception {
        FileLock fl = null;
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(path);
            fl = fos.getChannel().tryLock();
            if (fl != null && fl.isValid()) {
                byte[] buffer = new byte[1024];
                int len1 = 0;
                while ((len1 = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, len1);
                }
                fl.release();
                fos.close();
            }

        } catch (Exception e) {
            throw e;
        } finally {
            if (fl != null && fl.isValid()) {
                try {
                    fl.release();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                is.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    public static interface LoaderCallBack {

        /**
         * 要求获取的图片的转换规格
         * @return
         */
        void onDecodeFile(Options options);

        /**
         * 图片获取完毕
         * @param bitmap
         */
        void onLoadOver(Bitmap bitmap);

        /**
         * 发生错误
         * @param e
         */
        void onError(Exception e);
    }
}
