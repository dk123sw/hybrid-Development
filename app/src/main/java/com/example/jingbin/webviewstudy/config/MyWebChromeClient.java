package com.example.jingbin.webviewstudy.config;

import android.Manifest;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Build;
import android.os.Environment;
import android.os.Parcelable;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Toast;

import com.example.jingbin.webviewstudy.R;
import com.example.jingbin.webviewstudy.WebViewActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static android.app.Activity.RESULT_OK;

/**
 * Created by jingbin on 2016/11/17.
 * - 播放网络视频配置
 * - 上传图片(兼容)
 */
public class MyWebChromeClient extends WebChromeClient {

    private ValueCallback<Uri> mUploadMessage;
    private ValueCallback<Uri[]> mUploadMessageForAndroid5;
    public static int FILECHOOSER_RESULTCODE = 1;
    public static int FILECHOOSER_RESULTCODE_FOR_ANDROID_5 = 2;

    private View mXProgressVideo;
    private WebViewActivity mActivity;
    private IWebPageView mIWebPageView;
    private View mXCustomView;
    private CustomViewCallback mXCustomViewCallback;
    private Uri imageUri;
    public MyWebChromeClient(IWebPageView mIWebPageView) {
        this.mIWebPageView = mIWebPageView;
        this.mActivity = (WebViewActivity) mIWebPageView;
    }

    /**
     * 播放网络视频时全屏会被调用的方法
     */
    @Override
    public void onShowCustomView(View view, CustomViewCallback callback) {
        mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        mIWebPageView.hindWebView();
        // 如果一个视图已经存在，那么立刻终止并新建一个
        if (mXCustomView != null) {
            callback.onCustomViewHidden();
            return;
        }

        mActivity.fullViewAddView(view);
        mXCustomView = view;
        mXCustomViewCallback = callback;
        mIWebPageView.showVideoFullView();
    }

    /**
     * 视频播放退出全屏会被调用的
     */
    @Override
    public void onHideCustomView() {
        if (mXCustomView == null)// 不是全屏播放状态
            return;
        mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mXCustomView.setVisibility(View.GONE);
        if (mActivity.getVideoFullView() != null) {
            mActivity.getVideoFullView().removeView(mXCustomView);
        }
        mXCustomView = null;
        mIWebPageView.hindVideoFullView();
        mXCustomViewCallback.onCustomViewHidden();
        mIWebPageView.showWebView();
    }

    /**
     * 视频加载时进程loading
     */
    @Override
    public View getVideoLoadingProgressView() {
        if (mXProgressVideo == null) {
            LayoutInflater inflater = LayoutInflater.from(mActivity);
            mXProgressVideo = inflater.inflate(R.layout.video_loading_progress, null);
        }
        return mXProgressVideo;
    }

    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        super.onProgressChanged(view, newProgress);
        mIWebPageView.startProgress(newProgress);
    }

    /**
     * 判断是否是全屏
     */
    public boolean inCustomView() {
        return (mXCustomView != null);
    }

    @Override
    public void onReceivedTitle(WebView view, String title) {
        super.onReceivedTitle(view, title);
        // 设置title
        mActivity.setTitle(title);
    }

    //扩展浏览器上传文件
    //3.0++版本
    public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
        openFileChooserImpl(uploadMsg);
    }

    //3.0--版本
    public void openFileChooser(ValueCallback<Uri> uploadMsg) {
        openFileChooserImpl(uploadMsg);
    }

    public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
        openFileChooserImpl(uploadMsg);
    }

    // For Android > 5.0
    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> uploadMsg, FileChooserParams fileChooserParams) {

        String[] acceptsTypes = new String[]{};
        if(Build.VERSION.SDK_INT >= 21) {
            acceptsTypes = fileChooserParams.getAcceptTypes();
//            Log.e("test", acceptsTypes[1]);
        }
        openFileChooserImplForAndroid5(uploadMsg, acceptsTypes);
//        take();
//        test();
        mUploadMessageForAndroid5 = uploadMsg;
        return true;
    }

    private void openFileChooserImpl(ValueCallback<Uri> uploadMsg) {
        mUploadMessage = uploadMsg;
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        mActivity.startActivityForResult(Intent.createChooser(i, "文件选择"), FILECHOOSER_RESULTCODE);
    }

    private void openFileChooserImplForAndroid5(ValueCallback<Uri[]> uploadMsg, String[] acceptsTypes) {

        mUploadMessageForAndroid5 = uploadMsg;
        Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
        contentSelectionIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
        contentSelectionIntent.setType("image/*");

        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
        chooserIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
        chooserIntent.putExtra(Intent.EXTRA_TITLE, "图片选择");

        mActivity.startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE_FOR_ANDROID_5);
    }

    private void test(){
        Intent i = new Intent();
        i.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        mActivity.startActivityForResult(Intent.createChooser(i, "Choose"), FILECHOOSER_RESULTCODE_FOR_ANDROID_5);
    }



    private void take() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(mActivity,new String[]{Manifest.permission.CAMERA},1001);
            }
        }

        File imageStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MyApp");        // Create the storage directory if it does not exist
        if (!imageStorageDir.exists()) {
            imageStorageDir.mkdirs();
        }
        File file = new File(imageStorageDir + File.separator + "IMG_" + String.valueOf(System.currentTimeMillis()) + ".jpg");
        if (Build.VERSION.SDK_INT >= 24) {
            imageUri = FileProvider.getUriForFile(mActivity, "com.example.jingbin.webviewstudy", file);
        } else {
            imageUri = Uri.fromFile(file);
        }
        final List<Intent> cameraIntents = new ArrayList();
        final Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        final PackageManager packageManager = mActivity.getPackageManager();
        final List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
        for (ResolveInfo res : listCam) {
            final String packageName = res.activityInfo.packageName;
            final Intent i = new Intent(captureIntent);
            i.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            i.setPackage(packageName);
            i.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            cameraIntents.add(i);

        }
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        Intent chooserIntent = Intent.createChooser(i, "Image Chooser");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[]{}));
        mActivity.startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE_FOR_ANDROID_5);
    }

    /**
     * 5.0以下 上传图片成功后的回调
     */
    public void mUploadMessage(Intent intent, int resultCode) {
        if (null == mUploadMessage) {
            return;
        }
        Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
        mUploadMessage.onReceiveValue(result);
        mUploadMessage = null;
    }

    /**
     * 5.0以上 上传图片成功后的回调
     */
    public void mUploadMessageForAndroid5(Intent intent, int resultCode) {
        if (null == mUploadMessageForAndroid5) {
            return;
        }
        Uri result = (intent == null || resultCode != RESULT_OK) ? null : intent.getData();
        if (result != null) {
            mUploadMessageForAndroid5.onReceiveValue(new Uri[]{result});
        } else {
            mUploadMessageForAndroid5.onReceiveValue(new Uri[]{});
        }
        mUploadMessageForAndroid5 = null;
    }
}
