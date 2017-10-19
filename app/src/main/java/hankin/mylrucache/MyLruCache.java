package hankin.mylrucache;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.widget.ImageView;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 *
 * Created by Hankin on 2017/9/28.
 * @email hankin.huan@gmail.com
 */

public class MyLruCache {

    private static MyLruCache mMyLruCache;
    private static ExecutorService mAsynExecutor;
    /** 图片缓存容器(缩略图) */
    private static final LruCache<String, Bitmap> mBitmapBuffer = new LruCache<String, Bitmap>((int)(Runtime.getRuntime().maxMemory() / 8)){
        @Override
        protected int sizeOf(String key, Bitmap value) {//在每次存入缓存的时候调用
            return value.getAllocationByteCount();
        }
        //如果你cache的某个值需要明确释放，重写entryRemoved()方法。这个方法会在元素被put或remove时调用，源码默认是空实现的
        //当缓存快超过了容器的容量时，根据lru(最少使用算法)算法，会移除部分缓存，此时evicted为true，mBitmapBuffer.get(key)==null
        @Override
        protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
            super.entryRemoved(evicted, key, oldValue, newValue);
        }
    };

    private MyLruCache() {}
    public static synchronized MyLruCache getInstance(){
        if (mMyLruCache ==null){
            mMyLruCache = new MyLruCache();
            mAsynExecutor = Executors.newFixedThreadPool(5);
        }
        return mMyLruCache;
    }

    /** 从缓存中获取一张图片   */
    private Bitmap getBitmap4Buffer(String imagePath){
        if(!TextUtils.isEmpty(imagePath)){
            return mBitmapBuffer.get(imagePath);
        }
        return null;
    }

    /** 反射，利用系统函数创建缩略图 */
    private Bitmap createImageThumbnail(String filePath){
        if (TextUtils.isEmpty(filePath)) return null;
        Bitmap bitmap;//ThumbnailUtils.createImageThumbnail(filePath, Images.Thumbnails.MINI_KIND);
        try {
            Class<?> cls = Class.forName("android.media.ThumbnailUtils");
            Method m = cls.getMethod("createImageThumbnail", String.class, int.class);
            bitmap = (Bitmap) m.invoke(null, filePath, MediaStore.Images.Thumbnails.MINI_KIND);
        } catch (Exception e){
            e.printStackTrace();
            bitmap = null;
        }
        return bitmap;
    }

    /** 加载缩略图（异步） 只适用于本地图片 */
    private void getLocaleImgThumb(final String filePath, final OnBitmapBuffered listener){
        Bitmap bitmap = getBitmap4Buffer(filePath);
        if (bitmap == null) bitmap = getPictureThumbnail(filePath);//如果没在内存中，就从本地导入
        int ret = bitmap == null ? -1 : 0;
        if (listener!=null) listener.onBuffered(ret, bitmap);
    }

    /** 加载缩略图（同步）  */
    private Bitmap getPictureThumbnail(String filePath){
        Bitmap result = createImageThumbnail(filePath);
        if(result != null){
            synchronized(mBitmapBuffer){
                mBitmapBuffer.put(filePath, result);
            }
        }
        return result;
    }

    /** 获取网络图片 */
    private void getNetImg(final Context context, final String url, final OnBitmapBuffered listener){
        String path = MyDiskLruCache.getInstance().getCache(context, url);//先从数据库查是否在缓存中，且文件存在
        if (!TextUtils.isEmpty(path)){
            getLocaleImgThumb(path, listener);
        } else {
            path = downImage(context, url);
            Bitmap bitmap = createImageThumbnail(path);
            if (bitmap != null) {
                synchronized (mBitmapBuffer) {
                    mBitmapBuffer.put(path, bitmap);
                }
            }
            int ret = bitmap == null ? -1 : 0;
            if (listener != null) listener.onBuffered(ret, bitmap);
        }
    }

    /** 下载图片到缓存目录 */
    private String downImage(Context context, String url){
        if (context==null || TextUtils.isEmpty(url)) return null;
        try{
            OkHttpClient client = new OkHttpClient();
            client.setConnectTimeout(60, TimeUnit.SECONDS);
            client.setReadTimeout(60, TimeUnit.SECONDS);
            client.setWriteTimeout(60, TimeUnit.SECONDS);

            Request request = new Request.Builder().url(url).build();
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                InputStream is = response.body().byteStream();
                if (is != null) {
                    File file = createNewFile(context.getCacheDir().getAbsolutePath(), new Date().getTime()+"");//下载到缓存目录
                    FileOutputStream out = new FileOutputStream(file, false);
                    byte[] buf = new byte[1024];
                    int ch = -1;
                    while ((ch = is.read(buf)) != -1) {
                        out.write(buf, 0, ch);
                    }
                    out.flush();
                    out.close();
                    is.close();

                    MyDiskLruCache.getInstance().putCache(context, url, file.getAbsolutePath());//缓存路径保存到数据库
                    return file.getAbsolutePath();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /** 创建文件 */
    private File createNewFile(String filePath, String fileName) throws IOException {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) return null;
        File folderFile = new File(filePath);
        if (!folderFile.exists()){
            folderFile.mkdirs();
        }
        File file = new File(filePath, fileName);
        if (file.exists()) file.delete();
        else file.createNewFile();
        return file;
    }


    /** 显示图片，如果是本地图片先判断是否在内存，是的话直接从内存读取，否的话从本地导入内存，再显示图片，
     *           如果是网络图片先判断是否在本地，是的话走本地图片逻辑，否的话从网络下载到缓存，然后导入内存，最后显示 */
    public void displayImage(final Context context, final ImageView iv, final String path){
        iv.setImageResource(R.mipmap.img_default_icon);
        mAsynExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (path.startsWith("/")) {//本地图片
                    getLocaleImgThumb(path, new MyLruCache.OnBitmapBuffered() {
                        @Override
                        public void onBuffered(int result, final Bitmap bitmap) {
                            if (bitmap!=null) {
                                ((Activity) context).runOnUiThread(new Runnable() {
                                    @TargetApi(Build.VERSION_CODES.KITKAT)
                                    @Override
                                    public void run() {
                                        iv.setImageBitmap(bitmap);
                                    }
                                });
                            }
                        }
                    });
                } else {//网络图片
                    getNetImg(context, path, new MyLruCache.OnBitmapBuffered() {
                        @Override
                        public void onBuffered(int result, final Bitmap bitmap) {
                            if (bitmap!=null){
                                ((Activity) context).runOnUiThread(new Runnable() {
                                    @TargetApi(Build.VERSION_CODES.KITKAT)
                                    @Override
                                    public void run() {
                                        iv.setImageBitmap(bitmap);
                                    }
                                });
                            }
                        }
                    });
                }
            }
        });
    }

    public interface OnBitmapBuffered{
        void onBuffered(int result, Bitmap bitmap);
    }

}
