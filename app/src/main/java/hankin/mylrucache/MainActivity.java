package hankin.mylrucache;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.ArrayList;

/**
 *
 * Created by Hankin on 2017/10/2.
 * @email hankin.huan@gmail.com
 */

public class MainActivity extends Activity {

    private GridView mGridView;
    private MyAdapter mAdapter;
    private ArrayList<String> mList = new ArrayList<>();
    private String urls[] = new String[]{
            "http://imgsrc.baidu.com/imgad/pic/item/0824ab18972bd407ff79ebde71899e510fb30952.jpg",
            "http://img.taopic.com/uploads/allimg/140112/235108-14011223015249.jpg",
            "http://pic.35pic.com/normal/07/18/70/11451465_211522532127_2.jpg",
            "http://img.sccnn.com/bimg/338/27062.jpg",
            "http://img.sccnn.com/bimg/338/27062.jpg",
            "http://pic2.ooopic.com/12/22/94/37bOOOPICc7_1024.jpg",
            "http://pic2.ooopic.com/12/22/94/37bOOOPICc7_1024.jpg",
            "http://img02.tooopen.com/images/20140307/sy_56348856275.jpg",
            "http://www.3dmgame.com/uploads/allimg/140914/156-1409140RU1.jpg",
            "http://pic51.nipic.com/file/20141023/2531170_115622554000_2.jpg",
            "http://pic.58pic.com/58pic/17/69/38/5579fe1861e96_1024.jpg",
            "http://img2.imgtn.bdimg.com/it/u=3894062614,1839770030&fm=26&gp=0.jpg",
            "http://pic.58pic.com/58pic/15/21/52/82E58PICgsM_1024.jpg",
            "http://pic36.nipic.com/20131203/3655282_202314394372_2.jpg",
            "http://pic3.nipic.com/20090613/2538177_163100003_2.jpg"
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mGridView = findViewById(R.id.gv_main);
        mAdapter = new MyAdapter();
        mGridView.setAdapter(mAdapter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1234);
        } else {
            select();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {//请求权限结果回调
        switch (requestCode){
            case 1234:
                if (grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    select();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

    private void select(){
        int count = 0;
        Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media.DATA, MediaStore.Images.Media.MIME_TYPE},
                MediaStore.Images.Media.MIME_TYPE+"=?",
                new String[]{"image/jpeg"},
                MediaStore.Images.Media.DATE_ADDED+" desc");
        if (cursor!=null){
            while (cursor.moveToNext()){
                mList.add(cursor.getString(0));
                mList.add(urls[count]);
                count++;
                if (count>urls.length-1) break;
            }
            mAdapter.notifyDataSetChanged();
            cursor.close();
        }
    }

    class MyAdapter extends BaseAdapter{
        @Override
        public int getCount() {
            return mList.size();
        }
        @Override
        public Object getItem(int i) {
            return mList.get(i);
        }
        @Override
        public long getItemId(int i) {
            return i;
        }
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder holder;
            if (view==null){
                view = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_main, null);
                holder = new ViewHolder(view);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }
            int width = getResources().getDisplayMetrics().widthPixels/2;
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) holder.iv.getLayoutParams();
            if (lp==null) lp = new LinearLayout.LayoutParams(width, width);
            lp.width = width;
            lp.height = width;
            holder.iv.setLayoutParams(lp);
            MyLruCache.getInstance().displayImage(MainActivity.this, holder.iv, mList.get(i));
            return view;
        }
    }
    class ViewHolder{
        ImageView iv;
        ViewHolder(View view){
            iv = view.findViewById(R.id.iv_item_main);
        }
    }

}
