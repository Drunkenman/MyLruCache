package hankin.mylrucache;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import java.io.File;

/**
 *
 * Created by Hankin on 2017/9/29.
 * @email hankin.huan@gmail.com
 */

public class MyDiskLruCache {

    private static MyDiskLruCache mMyDiskLruCache;

    private MyDiskLruCache(){}
    public static synchronized MyDiskLruCache getInstance(){
        if (mMyDiskLruCache==null){
            mMyDiskLruCache = new MyDiskLruCache();
        }
        return mMyDiskLruCache;
    }

    /** 保存网络图片的缓存路径 */
    public void putCache(Context context, String url, String path){
        if (context==null || TextUtils.isEmpty(url)) return;;
        SQLiteDatabase db = new DBHelper(context).getWritableDatabase();
        Cursor cursor = db.query(Constants.TABLENAME, new String[]{Constants.PATH}, Constants.URL+"=?",
                new String[]{url}, null, null, null, null);
        if (cursor!=null){
            ContentValues values = new ContentValues();
            if (cursor.getCount()>0){
                values.put(Constants.PATH, path);
                db.update(Constants.TABLENAME, values, Constants.URL+"=?", new String[]{url});
            } else {
                values.put(Constants.URL, url);
                values.put(Constants.PATH, path);
                db.insert(Constants.TABLENAME, null, values);
            }
            cursor.close();
        }
        db.close();
    }

    /** 获取网络图片的缓存路径，如果路劲文件不存在则删除数据 */
    public String getCache(Context context, String url){
        if (context==null || TextUtils.isEmpty(url)) return null;
        String path = null;
        SQLiteDatabase db = new DBHelper(context).getWritableDatabase();
        Cursor cursor = db.query(Constants.TABLENAME, new String[]{Constants.PATH}, Constants.URL+"=?",
                new String[]{url}, null, null, null, null);
        if (cursor!=null){
            if (cursor.getCount()>0){
                cursor.moveToNext();
                if (new File(cursor.getString(0)).exists()){
                    path = cursor.getString(0);
                } else {
                    db.delete(Constants.TABLENAME, Constants.URL+"=?", new String[]{url});
                }
            }
            cursor.close();
        }
        db.close();
        return path;
    }

    private class Constants {
        static final String DBNAME = "hankin_disklru.db";
        static final String TABLENAME = "disklru";
        static final String ID = "_id";
        static final String URL = "url";
        static final String PATH = "path";
    }
    private class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context context) {
            super(context, Constants.DBNAME, null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String sql = "create table if not exists "+ Constants.TABLENAME+"("+ Constants.ID+" integer primary key autoincrement, "
                    + Constants.URL+" text not null unique, "+ Constants.PATH+" text not null)";
            db.execSQL(sql);
            sql = "create index ind_url on "+ Constants.TABLENAME+"("+ Constants.URL+")";
            db.execSQL(sql);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            String sql = "drop table if exists "+ Constants.TABLENAME;
            db.execSQL(sql);
        }

    }

}
