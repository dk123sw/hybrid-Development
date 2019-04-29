package com.example.jingbin.webviewstudy;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import com.example.jingbin.webviewstudy.utils.Tools;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class CreateFileActivity extends AppCompatActivity implements View.OnClickListener{
    private static final int FILE_PERMISSION = 101;
    private static final int PRIVATE_CODE = 1315;//开启GPS权限
    private static final int BAIDU_READ_PHONE_STATE = 100;//定位权限请求
    static final String[] LOCATIONGPS = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_PHONE_STATE};
    private TextView textView;

    private String filePath;
    private String fileName;

    public static void openIntent(Context mContext, String mTitle) {
        Intent intent = new Intent(mContext, CreateFileActivity.class);
        intent.putExtra("mTitle", mTitle == null ? "加载中..." : mTitle);
        mContext.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_file);
        filePath = Environment.getExternalStorageDirectory() + "/a_myDocument";
        initView();
        getLocation();
    }

    private void initView() {
        findViewById(R.id.create_file).setOnClickListener(this);
        findViewById(R.id.display_location).setOnClickListener(this);
        textView = findViewById(R.id.display_location);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.create_file:
                if (Build.VERSION.SDK_INT >= 23) {
                    if ((ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED
                            && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PERMISSION_GRANTED )){
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, FILE_PERMISSION);
                    }else {
                        createFile();
                    }
                }else {
                    createFile();
                }
                break;
//            case R.id.display_location:
//                break;
        }
    }

    private void createFile() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");
        Date date = new Date();
        fileName = simpleDateFormat.format(date);
        Tools.makeRootDirectory(filePath);
        Tools.makeRootDirectory(filePath+"/"+fileName);
    }

    /**
     * android 6.0 以上需要动态申请权限
     */
    private void initPermission() {
        String[] permissions = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        ArrayList<String> toApplyList = new ArrayList<String>();

        for (String perm : permissions) {
            if (PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, perm)) {
                toApplyList.add(perm);
                // 进入到这里代表没有权限.
                Toast.makeText(this, "没有充足的权限", Toast.LENGTH_LONG).show();
            }
        }
        String[] tmpList = new String[toApplyList.size()];
        if (!toApplyList.isEmpty()) {
            ActivityCompat.requestPermissions(this, toApplyList.toArray(tmpList), FILE_PERMISSION);
        }
    }

//    /**
//     * 检测GPS、位置权限是否开启
//     */
//    public void showGPSContacts() {
//        LocationManager lm = (LocationManager) this.getSystemService(this.LOCATION_SERVICE);
//        boolean ok = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
//        if (ok) {//开了定位服务
//            if (Build.VERSION.SDK_INT >= 23) { //判断是否为android6.0系统版本，如果是，需要动态添加权限
//                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
//                        != PERMISSION_GRANTED) {// 没有权限，申请权限。
//                    ActivityCompat.requestPermissions(this, LOCATIONGPS,
//                            BAIDU_READ_PHONE_STATE);
//                } else {
//                    getLocation();//getLocation为定位方法
//                }
//            } else {
//                getLocation();//getLocation为定位方法
//            }
//        } else {
//            Toast.makeText(this, "系统检测到未开启GPS定位服务,请开启", Toast.LENGTH_SHORT).show();
//            Intent intent = new Intent();
//            intent.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//            startActivityForResult(intent, PRIVATE_CODE);
//        }
//        getLocation();//getLocation为定位方法
//    }

    /**
     * 获取具体位置的经纬度
     */
    private void getLocation() {
        // 获取位置管理服务
        LocationManager locationManager;
        String serviceName = Context.LOCATION_SERVICE;
        locationManager = (LocationManager) this.getSystemService(serviceName);
        // 查找到服务信息
        Criteria criteria = new Criteria();
//        criteria.setAccuracy(Criteria.ACCURACY_FINE); // 高精度
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.POWER_LOW); // 低功耗
        String provider = locationManager.getBestProvider(criteria, true); // 获取GPS信息
        /**这段代码不需要深究，是locationManager.getLastKnownLocation(provider)自动生成的，不加会出错**/
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PERMISSION_GRANTED) {
            return;
        }
        Location location = locationManager.getLastKnownLocation(provider); // 通过GPS获取位置
        updateLocation(location);
    }

    /**
     * 获取到当前位置的经纬度
     * @param location
     */
    private void updateLocation(Location location) {
        if (location != null) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            textView.setText("维度：" + latitude + "\n经度" + longitude);
//            Toast.makeText(this, "维度：" + latitude + "\n经度" + longitude, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "无法获取到位置信息", Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case FILE_PERMISSION:
                if(grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                    createFile();
                } else {
                    Toast.makeText(this, "缺少权限", Toast.LENGTH_LONG).show();
                }
                break;
//            case BAIDU_READ_PHONE_STATE:
//                //如果用户取消，permissions可能为null.
//                if (grantResults[0] == PERMISSION_GRANTED && grantResults.length > 0) {  //有权限
//                    // 获取到权限，作相应处理
//                    getLocation();
//                } else {
//                    showGPSContacts();
//                }
//                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
