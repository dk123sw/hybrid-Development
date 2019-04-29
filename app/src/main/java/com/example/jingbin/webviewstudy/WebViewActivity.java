package com.example.jingbin.webviewstudy;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jingbin.webviewstudy.config.FullscreenHolder;
import com.example.jingbin.webviewstudy.config.IWebPageView;
import com.example.jingbin.webviewstudy.config.MyJavascriptInterface;
import com.example.jingbin.webviewstudy.config.MyWebChromeClient;
import com.example.jingbin.webviewstudy.config.MyWebViewClient;
import com.example.jingbin.webviewstudy.record.DBHelper;
import com.example.jingbin.webviewstudy.utils.BaseTools;
import com.example.jingbin.webviewstudy.utils.StatusBarUtil;
import com.example.jingbin.webviewstudy.utils.Tools;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.tencent.smtt.utils.b.c;


/**
 * 网页可以处理:
 * 点击相应控件：
 * - 拨打电话、发送短信、发送邮件
 * - 上传图片(版本兼容)
 * - 全屏播放网络视频
 * - 进度条显示
 * - 返回网页上一层、显示网页标题
 * JS交互部分：
 * - 前端代码嵌入js(缺乏灵活性)
 * - 网页自带js跳转
 * 被作为第三方浏览器打开
 */
public class WebViewActivity extends AppCompatActivity implements IWebPageView {

    // 进度条
    private ProgressBar mProgressBar;
    public WebView webView;
    // 全屏时视频加载view
    private FrameLayout videoFullView;
    // 加载视频相关
    private MyWebChromeClient mWebChromeClient;
    // 网页链接
    private String mUrl;
    private Toolbar mTitleToolBar;
    // 可滚动的title 使用简单 没有渐变效果，文字两旁有阴影
    private TextView tvGunTitle;
    private String mTitle;

    private MyJavascriptInterface object;
    private String word;

    private AlertDialog.Builder alertDialog;
    private String imagePath;

    public DBHelper mDatabase;

    public String filePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
        mDatabase = new DBHelper(getApplicationContext());
        alertDialog = new AlertDialog.Builder(this);
        createFile();
        getIntentData();
        initTitle();
        initWebView();
        webView.loadUrl(mUrl);
        getDataFromBrowser(getIntent());
    }

    private void getIntentData() {
        mUrl = getIntent().getStringExtra("mUrl");
        mTitle = getIntent().getStringExtra("mTitle");
    }

    private void createFile() {
        String path = Environment.getExternalStorageDirectory() + "/a_myDocument";
        Tools.makeRootDirectory(path);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");
        Date date = new Date();
        filePath = path + "/" + simpleDateFormat.format(date);
        Tools.makeRootDirectory(filePath);
    }

    private void initTitle() {
        StatusBarUtil.setColor(this, ContextCompat.getColor(this, R.color.colorPrimary), 0);
        mProgressBar = findViewById(R.id.pb_progress);
        webView = findViewById(R.id.webview_detail);
        videoFullView = findViewById(R.id.video_fullView);
        mTitleToolBar = findViewById(R.id.title_tool_bar);
        tvGunTitle = findViewById(R.id.tv_gun_title);
        initToolBar();
    }

    private void initToolBar() {
        setSupportActionBar(mTitleToolBar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            //去除默认Title显示
            actionBar.setDisplayShowTitleEnabled(false);
        }
        mTitleToolBar.setOverflowIcon(ContextCompat.getDrawable(this, R.drawable.actionbar_more));
        tvGunTitle.postDelayed(new Runnable() {
            @Override
            public void run() {
                tvGunTitle.setSelected(true);
            }
        }, 1900);
        setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_webview, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:// 返回键
                handleFinish();
                break;
            case R.id.actionbar_share:// 分享到
                String shareText = webView.getTitle() + webView.getUrl();
                BaseTools.share(WebViewActivity.this, shareText);
                break;
            case R.id.actionbar_cope:// 复制链接
                if (!TextUtils.isEmpty(webView.getUrl())) {
                    BaseTools.copy(webView.getUrl());
                    Toast.makeText(this, "复制成功", Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.actionbar_open:// 打开链接
                BaseTools.openLink(WebViewActivity.this, webView.getUrl());
                break;
            case R.id.actionbar_webview_refresh:// 刷新页面
                if (webView != null) {
                    webView.reload();
                }
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void initWebView() {
        mProgressBar.setVisibility(View.VISIBLE);
        WebSettings ws = webView.getSettings();
        // 网页内容的宽度是否可大于WebView控件的宽度
        ws.setLoadWithOverviewMode(false);
        // 保存表单数据
        ws.setSaveFormData(true);
        // 是否应该支持使用其屏幕缩放控件和手势缩放
        ws.setSupportZoom(true);
        ws.setBuiltInZoomControls(true);
        ws.setDisplayZoomControls(false);
        // 启动应用缓存
        ws.setAppCacheEnabled(true);
        // 设置缓存模式
        ws.setCacheMode(WebSettings.LOAD_DEFAULT);
        // setDefaultZoom  api19被弃用
        // 设置此属性，可任意比例缩放。
        ws.setUseWideViewPort(true);
        // 不缩放
        webView.setInitialScale(100);
        // 告诉WebView启用JavaScript执行。默认的是false。
        ws.setJavaScriptEnabled(true);
        //  页面加载好以后，再放开图片
        ws.setBlockNetworkImage(false);
        // 使用localStorage则必须打开
        ws.setDomStorageEnabled(true);
        // 排版适应屏幕
        ws.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        ws.setAllowFileAccess(true);
        ws.setAllowContentAccess(true);
        // WebView是否新窗口打开(加了后可能打不开网页)
//        ws.setSupportMultipleWindows(true);

        // webview从5.0开始默认不允许混合模式,https中不能加载http资源,需要设置开启。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        /** 设置字体默认缩放大小(改变网页字体大小,setTextSize  api14被弃用)*/
        ws.setTextZoom(100);

        mWebChromeClient = new MyWebChromeClient(this);
        webView.setWebChromeClient(mWebChromeClient);
        // 与js交互

        object = new MyJavascriptInterface(this);
        webView.addJavascriptInterface(object, "injectedObject");
        webView.setWebViewClient(new MyWebViewClient(this));
        webView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return handleLongImage();
            }
        });
    }

    public void openAlbum() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent, 2);
    }


    private void alertText(final String title, final String message) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                alertDialog.setTitle(title)
                        .setMessage(message)
                        .setPositiveButton("确定", null)
                        .show();
            }
        });
    }
    private void infoPopText(final String result) {
        alertText("", result);
    }

    /**
     * 上传图片之后的回调
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        switch (requestCode) {
            case 1:
                try {
                    final Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(object.imageUrl));
                    //在UI线程更新界面
                    object.image.post(new Runnable() {
                        @Override
                        public void run() {
                            object.image.setImageBitmap(bitmap);
                        }
                    });
//                    object.image.setImageBitmap(bitmap);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                // 传递参数调用

//                webView.loadUrl("javascript:javacalljstest('" + object.imageUrl.toString() + "')");
                Log.e("WebView", object.imageUrl.toString());
                if (object.mswitch.isChecked() && resultCode != 0) {
                    RecognizeService.recAccurateBasic(this, new File(this.getExternalCacheDir(), "output_image.jpg").getAbsolutePath(),
                            new RecognizeService.ServiceListener() {
                                @Override
                                public void onResult(final String result) {
//                                    infoPopText(result);
                                    try {
                                        word = "";
                                        JSONObject jsonObject = new JSONObject(result);
                                        JSONArray resultArray = new JSONArray(jsonObject.getString("words_result"));
                                        for(int i = 0; i < resultArray.length(); i++) {
                                            JSONObject value = resultArray.getJSONObject(i);
                                            word = word + " " + value.getString("words");
                                        }
                                        object.editText.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    object.editText.setText(word);
                                                }catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        });
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                }
//                object.image.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        Glide.with(WebViewActivity.this).load(object.imageUrl.toString()).into(object.image);
//                    }
//                });

//                Handler handler = new Handler() {
//                    @Override
//                    public void handleMessage(Message msg) {
//                        switch (msg.what) {
//                            case 1:
//                                Glide.with(WebViewActivity.this).load(object.imageUrl.toString()).into(object.image);
//                                break;
//                        }
//                        super.handleMessage(msg);
//                    }
//                };
//                Message message = new Message();
//                message.what = 1;
//                handler.sendMessage(message);
                break;
            case 2:
                if (resultCode == RESULT_OK) {
                    if (Build.VERSION.SDK_INT >= 19) {
                        handldImageOnkitKat(intent);
                    } else {
                        handleImageBeforeKitKat(intent);
                    }
                    if (object.mswitch.isChecked()) {
                        Uri uri = Tools.geturi(this, intent);
                        RecognizeService.recAccurateBasic(this, imagePath,
                                new RecognizeService.ServiceListener() {
                                    @Override
                                    public void onResult(final String result) {
//                                        infoPopText(result);
                                        try {
                                            word = "";
                                            JSONObject jsonObject = new JSONObject(result);
                                            JSONArray resultArray = new JSONArray(jsonObject.getString("words_result"));
                                            for(int i = 0; i < resultArray.length(); i++) {
                                                JSONObject value = resultArray.getJSONObject(i);
                                                word = word + " " + value.getString("words");
                                            }
                                            object.editText.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    try {
                                                        object.editText.setText(word);
                                                    }catch (Exception e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            });
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                    }
                }
                break;
            default:
                break;
        }
        if (requestCode == MyWebChromeClient.FILECHOOSER_RESULTCODE) {
            mWebChromeClient.mUploadMessage(intent, resultCode);
        } else if (requestCode == MyWebChromeClient.FILECHOOSER_RESULTCODE_FOR_ANDROID_5) {
            mWebChromeClient.mUploadMessageForAndroid5(intent, resultCode);
        }
    }

    private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(contentURI, null, null, null, null);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        if (cursor == null) {
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            for(int i = 0; i < cursor.getColumnCount(); i++) {
                Log.e("WEB", cursor.getString(i) );
                Log.e("WEB", cursor.getColumnName(i) );
            }
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            if (idx != -1) {
                result = cursor.getString(idx);
           } else {
                result = contentURI.getPath();
            }
            cursor.close();
        }
        return result;
    }


    @TargetApi(19)
    private void handldImageOnkitKat(Intent data) {
        imagePath = null;
        Uri uri = data.getData();
        if (DocumentsContract.isDocumentUri(this, uri)) {
            String docId = DocumentsContract.getDocumentId(uri);
            if("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1];
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath  = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())){
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://download/public_downloads"), Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);
            }
        } else if( "content".equalsIgnoreCase(uri.getScheme())) {
            imagePath = getImagePath(uri, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            imagePath = uri.getPath();
        }
        displayImage(imagePath);
    }

    private void handleImageBeforeKitKat(Intent data) {
        Uri uri = data.getData();
        imagePath = getImagePath(uri, null);
        displayImage(imagePath);
    }

    private String getImagePath(Uri uri, String selection) {
        String path = null;
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null );
        if(cursor != null) {
            if(cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    private  void displayImage(String imagePath) {
        if (imagePath != null) {
            final Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
//            object.image.setImageBitmap(bitmap);
            //在UI线程更新界面
            object.image.post(new Runnable() {
                @Override
                public void run() {
                    object.image.setImageBitmap(bitmap);
                }
            });
        } else {
            Toast.makeText(this, "获取图片失败", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void hindProgressBar() {
        mProgressBar.setVisibility(View.GONE);
    }

    @Override
    public void showWebView() {
        webView.setVisibility(View.VISIBLE);
    }

    @Override
    public void hindWebView() {
        webView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void fullViewAddView(View view) {
        FrameLayout decor = (FrameLayout) getWindow().getDecorView();
        videoFullView = new FullscreenHolder(WebViewActivity.this);
        videoFullView.addView(view);
        decor.addView(videoFullView);
    }

    @Override
    public void showVideoFullView() {
        videoFullView.setVisibility(View.VISIBLE);
    }

    @Override
    public void hindVideoFullView() {
        videoFullView.setVisibility(View.GONE);
    }

    @Override
    public void startProgress(int newProgress) {
        mProgressBar.setVisibility(View.VISIBLE);
        mProgressBar.setProgress(newProgress);
        if (newProgress == 100) {
            mProgressBar.setVisibility(View.GONE);
        }
    }

    public void setTitle(String mTitle) {
        tvGunTitle.setText(mTitle);
    }

    /**
     * android与js交互：
     * 前端注入js代码：不能加重复的节点，不然会覆盖
     * 前端调用js代码
     */
    @Override
    public void addImageClickListener() {
        loadImageClickJS();
        loadTextClickJS();
        loadCallJS();
    }

    /**
     * 前端注入JS：
     * 这段js函数的功能就是，遍历所有的img节点，并添加onclick函数，函数的功能是在图片点击的时候调用本地java接口并传递url过去
     */
    private void loadImageClickJS() {
        webView.loadUrl("javascript:(function(){" +
                "var objs = document.getElementsByTagName(\"img\");" +
                "for(var i=0;i<objs.length;i++)" +
                "{" +
                "objs[i].onclick=function(){window.injectedObject.imageClick(this.getAttribute(\"src\"));}" +
                "}" +
                "})()");
    }

    /**
     * 前端注入JS：
     * 遍历所有的<li>节点,将节点里的属性传递过去(属性自定义,用于页面跳转)
     */
    private void loadTextClickJS() {
        webView.loadUrl("javascript:(function(){" +
                "var objs =document.getElementsByTagName(\"li\");" +
                "for(var i=0;i<objs.length;i++)" +
                "{" +
                "objs[i].onclick=function(){" +
                "window.injectedObject.textClick(this.getAttribute(\"type\"),this.getAttribute(\"item_pk\"));}" +
                "}" +
                "})()");
    }

    /**
     * 传应用内的数据给html，方便html处理
     */
    private void loadCallJS() {
        // 无参数调用
        webView.loadUrl("javascript:javacalljs()");
        // 传递参数调用
        webView.loadUrl("javascript:javacalljswithargs('" + "android传入到网页里的数据，有参" + "')");
    }

    public FrameLayout getVideoFullView() {
        return videoFullView;
    }

    /**
     * 全屏时按返加键执行退出全屏方法
     */
    public void hideCustomView() {
        mWebChromeClient.onHideCustomView();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }


    /**
     * 使用singleTask启动模式的Activity在系统中只会存在一个实例。
     * 如果这个实例已经存在，intent就会通过onNewIntent传递到这个Activity。
     * 否则新的Activity实例被创建。
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        getDataFromBrowser(intent);
    }

    /**
     * 作为三方浏览器打开传过来的值
     * Scheme: https
     * host: www.jianshu.com
     * path: /p/1cbaf784c29c
     * url = scheme + "://" + host + path;
     */
    private void getDataFromBrowser(Intent intent) {
        Uri data = intent.getData();
        if (data != null) {
            try {
                String scheme = data.getScheme();
                String host = data.getHost();
                String path = data.getPath();
                String text = "Scheme: " + scheme + "\n" + "host: " + host + "\n" + "path: " + path;
                Log.e("data", text);
                String url = scheme + "://" + host + path;
                webView.loadUrl(url);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 直接通过三方浏览器打开时，回退到首页
     */
    public void handleFinish() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAfterTransition();
        } else {
            finish();
        }
        if (!MainActivity.isLaunch) {
            MainActivity.start(this);
        }
    }

    /**
     * 长按图片事件处理
     */
    private boolean handleLongImage() {
        final WebView.HitTestResult hitTestResult = webView.getHitTestResult();
        // 如果是图片类型或者是带有图片链接的类型
        if (hitTestResult.getType() == WebView.HitTestResult.IMAGE_TYPE ||
                hitTestResult.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
            // 弹出保存图片的对话框
            new AlertDialog.Builder(WebViewActivity.this)
                    .setItems(new String[]{"查看大图", "保存图片到相册"}, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String picUrl = hitTestResult.getExtra();
                            //获取图片
                            Log.e("picUrl", picUrl);
                            switch (which) {
                                case 0:
                                    break;
                                case 1:
                                    break;
                                default:
                                    break;
                            }
                        }
                    })
                    .show();
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            //全屏播放退出全屏
            if (mWebChromeClient.inCustomView()) {
                hideCustomView();
                return true;

                //返回网页上一页
            } else if (webView.canGoBack()) {
                webView.goBack();
                return true;

                //退出网页
            } else {
                handleFinish();
            }
        }
        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        // 支付宝网页版在打开文章详情之后,无法点击按钮下一步
        webView.resumeTimers();
        // 设置为横屏
        if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    @Override
    protected void onDestroy() {
        videoFullView.removeAllViews();
        if (webView != null) {
            ViewGroup parent = (ViewGroup) webView.getParent();
            if (parent != null) {
                parent.removeView(webView);
            }
            webView.removeAllViews();
            webView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    /**
     * 打开网页:
     *
     * @param mContext 上下文
     * @param mUrl     要加载的网页url
     * @param mTitle   标题
     */
    public static void loadUrl(Context mContext, String mUrl, String mTitle) {
        Intent intent = new Intent(mContext, WebViewActivity.class);
        intent.putExtra("mUrl", mUrl);
        intent.putExtra("mTitle", mTitle == null ? "加载中..." : mTitle);
        mContext.startActivity(intent);
    }

    public static void loadUrl(Context mContext, String mUrl) {
        Intent intent = new Intent(mContext, WebViewActivity.class);
        intent.putExtra("mUrl", mUrl);
        intent.putExtra("mTitle", "详情");
        mContext.startActivity(intent);
    }

    public void setUrl(){
        File outputImage = new File(getExternalCacheDir(), "output_image.jpg");
        try {
            if (outputImage.exists()) {
                outputImage.delete();
            }
            outputImage.createNewFile();
        }catch (IOException e){
            e.printStackTrace();
        }
        object.imageUrl = FileProvider.getUriForFile(this, "com.example.jingbin.webviewstudy", outputImage);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if(grantResults.length > 0 && grantResults[0] == getPackageManager().PERMISSION_GRANTED) {
                    openAlbum();
                } else {
                    Toast.makeText(this, "缺少权限", Toast.LENGTH_LONG).show();
                }
                break;
            case 3:
                if(grantResults.length > 0 && grantResults[0] == getPackageManager().PERMISSION_GRANTED) {
//                    openAlbum();
//                    setUrl();
                } else {
                    Toast.makeText(this, "缺少权限", Toast.LENGTH_LONG).show();
                }
                break;
            case 103:
                if (grantResults.length > 0) {
                    List<String> deniedPermissions = new ArrayList<>();
                    for (int i = 0; i < grantResults.length; i++) {
                        int grantResult = grantResults[i];
                        String permission = permissions[i];
                        if (grantResult != PackageManager.PERMISSION_GRANTED) {
                            deniedPermissions.add(permission);
                        }
                    }
                    Log.d("========", deniedPermissions.toString());
                    //被拒绝权限
                    if (deniedPermissions.isEmpty()) {
                        Toast.makeText(WebViewActivity.this, "请前往权限管理开启相机和相册相关权限", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(WebViewActivity.this, "请前往权限管理开启相机和相册相关权限", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case 123:
                //6.0+获取录音权限后回调
                Toast.makeText(this, "获取到录音权限", Toast.LENGTH_LONG).show();
        }

//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
