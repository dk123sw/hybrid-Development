package com.example.jingbin.webviewstudy.config;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.aip.asrwakeup3.core.mini.AutoCheck;
import com.baidu.aip.asrwakeup3.core.recog.MyRecognizer;
import com.baidu.aip.asrwakeup3.core.recog.listener.IRecogListener;
import com.baidu.aip.asrwakeup3.core.recog.listener.MessageStatusRecogListener;
import com.baidu.ocr.sdk.OCR;
import com.baidu.ocr.sdk.OnResultListener;
import com.baidu.ocr.sdk.exception.OCRError;
import com.baidu.ocr.sdk.model.AccessToken;
import com.baidu.ocr.ui.camera.CameraActivity;
import com.example.jingbin.webviewstudy.R;
import com.example.jingbin.webviewstudy.WebViewActivity;
import com.example.jingbin.webviewstudy.record.RecordingService;
import com.example.jingbin.webviewstudy.utils.TimeCount;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jingbin on 2016/11/17.
 * js通信接口
 */
public class MyJavascriptInterface {
    private Context context;
    public Uri imageUrl;
    public ImageView image;
    private WebViewActivity mActivity;
    public View view;
    public Switch mswitch;
    public EditText editText;
    private TextView textView;
    private LinearLayout mClick;
    private Button close;

    //Recording controls
    private Button mRecordButton = null;
    private Button mPauseButton = null;
    private TextView mRecordingPrompt;
    private Chronometer mChronometer = null;
    private boolean mStartRecording = true;
    private int mRecordPromptCount = 0;
    long timeWhenPaused = 0; //stores time when user clicks pause button

    private int mSampleRateInHZ = 16000; //采样率

    public MyRecognizer myRecognizer;
    protected Handler handler;
    public EditText outputText;

    private String mFileName = null;
    private String mFilePath = null;

    private TimeCount time;

    public static final String CHECK_STATUS = "status";

    public MyJavascriptInterface(Context context) {
        this.context = context;
        this.mActivity = (WebViewActivity) context;
    }

    /**
     * 前端代码嵌入js：
     * imageClick 名应和js函数方法名一致
     *
     * @param src 图片的链接
     */
    @JavascriptInterface
    public void imageClick(String src) {
        Log.e("imageClick", "----点击了图片");
        Log.e("src", src);
    }

    /**
     * 前端代码嵌入js
     * 遍历<li>节点
     *
     * @param type    <li>节点下type属性的值
     * @param item_pk item_pk属性的值
     */
    @JavascriptInterface
    public void textClick(String type, String item_pk) {
        if (!TextUtils.isEmpty(type) && !TextUtils.isEmpty(item_pk)) {
            Log.e("textClick", "----点击了文字");
            Log.e("type", type);
            Log.e("item_pk", item_pk);
        }
    }

    /**
     * 网页使用的js，方法无参数
     */
    @JavascriptInterface
    public void startFunction() {
        Toast.makeText(context, "测试js调用android", Toast.LENGTH_LONG).show();
        Log.e("startFunction", "----无参");
    }

    /**
     * 网页使用的js，方法有参数，且参数名为data
     *
     * @param data 网页js里的参数名
     */
    @JavascriptInterface
    public void startFunction(String data) {
        Log.e("startFunction", "----有参" + data);
    }

    @JavascriptInterface
    public void displayPhotoChange() {
        AlertDialog.Builder buildPhoto = new AlertDialog.Builder(mActivity);
        view = View.inflate(mActivity, R.layout.alert_dialog_photo, null);
        Button takePhotoButton = view.findViewById(R.id.take_photo);
        Button chooseFileButton = view.findViewById(R.id.choose_from_album);
        mswitch = view.findViewById(R.id.switch1);
        Button confirm = view.findViewById(R.id.confirm);
        Button close = view.findViewById(R.id.close);
        image = view.findViewById(R.id.picture);
        editText = view.findViewById(R.id.editText);
        textView = view.findViewById(R.id.textView);
        buildPhoto
                .setTitle("test")
                .setView(view)
                .create();
        final AlertDialog dialog = buildPhoto.show();
        takePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File outputImage = new File(mActivity.getExternalCacheDir(), "output_image.jpg");
                try {
                    if (outputImage.exists()) {
                        outputImage.delete();
                    }
                    outputImage.createNewFile();
                }catch (IOException e){
                    e.printStackTrace();
                }
                if (Build.VERSION.SDK_INT >= 24) {
//                    if ((ContextCompat.checkSelfPermission(mActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
//                            && ContextCompat.checkSelfPermission(mActivity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
//                    && ContextCompat.checkSelfPermission(mActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {
//                        ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 3);
//                        ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.CAMERA}, 3);
//                    } else {
//                        setUrl();
//                    }
                    initPicturePermission();
                    setUrl();
                } else {
                    imageUrl = Uri.fromFile(outputImage);
                }
                //启动相机程序
                Intent intent;
                if(mswitch.isChecked()) {
                    intent = new Intent(mActivity, CameraActivity.class);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUrl);
                    intent.putExtra(CHECK_STATUS, mswitch.isChecked());
                    intent.putExtra(CameraActivity.KEY_OUTPUT_FILE_PATH,
                            outputImage.getAbsolutePath());
                    intent.putExtra(CameraActivity.KEY_CONTENT_TYPE,
                            CameraActivity.CONTENT_TYPE_GENERAL);
                } else {
                    intent = new Intent("android.media.action.IMAGE_CAPTURE");
                    intent.putExtra(CHECK_STATUS, mswitch.isChecked());
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUrl);
                }
                mActivity.startActivityForResult(intent, 1);
            }
        });
        chooseFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((ContextCompat.checkSelfPermission(mActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                        && ContextCompat.checkSelfPermission(mActivity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED )) {
                    ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                }else {
                    Intent intent = new Intent("android.intent.action.GET_CONTENT");
                    intent.setType("image/*");
                    intent.putExtra(CHECK_STATUS, mswitch.isChecked());
                    mActivity.startActivityForResult(intent, 2);
                }
            }
        });
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.webView.loadUrl("javascript:javacalljspicture('" + editText.getText() + "')");
                    }
                });
                dialog.dismiss();
            }
        });
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        mswitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    editText.setVisibility(View.VISIBLE);
                    textView.setVisibility(View.VISIBLE);
                }else {
                    editText.setVisibility(View.GONE);
                    textView.setVisibility(View.GONE);
                }
            }
        });
        initAccessTokenWithAkSk();
    }

    public void setUrl(){
        File outputImage = new File(mActivity.getExternalCacheDir(), "output_image.jpg");
        try {
            if (outputImage.exists()) {
                outputImage.delete();
            }
            outputImage.createNewFile();
        }catch (IOException e){
            e.printStackTrace();
        }
        imageUrl = FileProvider.getUriForFile(mActivity, "com.example.jingbin.webviewstudy", outputImage);

    }

    @JavascriptInterface
    public void recordAudio() {
        AlertDialog.Builder buildPhoto = new AlertDialog.Builder(mActivity);
        view = View.inflate(mActivity, R.layout.alert_dialog_record, null);
        mChronometer = (Chronometer) view.findViewById(R.id.chronometer);
        outputText = view.findViewById(R.id.output_text);
        //update recording prompt text
        mRecordingPrompt = (TextView) view.findViewById(R.id.recording_status_text);
        mRecordButton = (Button) view.findViewById(R.id.btnRecord);
        mClick = view.findViewById(R.id.btnClick);
        Button btnConfirm = view.findViewById(R.id.btn_confirm);
        close = view.findViewById(R.id.btn_close);
        buildPhoto
                .setTitle("test")
                .setView(view)
                .create();
        final AlertDialog dialog = buildPhoto.show();
        // 基于DEMO集成第1.1, 1.2, 1.3 步骤 初始化EventManager类并注册自定义输出事件
        // DEMO集成步骤 1.2 新建一个回调类，识别引擎会回调这个类告知重要状态和识别结果
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                handleMsg(msg);
            }
        };
        IRecogListener listener = new MessageStatusRecogListener(handler);
        // DEMO集成步骤 1.1 1.3 初始化：new一个IRecogListener示例 & new 一个 MyRecognizer 示例,并注册输出事件
        myRecognizer = new MyRecognizer(mActivity, listener);
        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRecord1(mStartRecording);
                mStartRecording = !mStartRecording;
            }
        });
        mClick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRecord1(mStartRecording);
                mStartRecording = !mStartRecording;
            }
        });
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.webView.loadUrl("javascript:javacalljsrecord('" + outputText.getText() + "')");
                    }
                });
                dialog.dismiss();
                // 如果之前调用过myRecognizer.loadOfflineEngine()， release()里会自动调用释放离线资源
                // 基于DEMO5.1 卸载离线资源(离线时使用) release()方法中封装了卸载离线资源的过程
                // 基于DEMO的5.2 退出事件管理器
                myRecognizer.release();
            }
        });
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
//                time.cancel();
                // 如果之前调用过myRecognizer.loadOfflineEngine()， release()里会自动调用释放离线资源
                // 基于DEMO5.1 卸载离线资源(离线时使用) release()方法中封装了卸载离线资源的过程
                // 基于DEMO的5.2 退出事件管理器
                myRecognizer.release();
            }
        });
    }

    private void onRecord1(boolean start) {
        initPermission();
        File folder = new File(Environment.getExternalStorageDirectory() + "/baiduASR");
        if (!folder.exists()) {
            //folder /SoundRecorder doesn't exist, create the folder
            folder.mkdir();
        }
        if (start) {
            //start Chronometer
            startChronometer();
            setFileNameAndPath();
//            time = new TimeCount(60000, 1000, mFilePath);

            final Map<String, Object> params = new HashMap<>();
            params.put("accept-audio-data", true);
            params.put("vad.endpoint-timeout", 0);
            params.put("outfile", mFilePath + ".pcm");
            params.put("pid", 15372);
            params.put("accept-audio-volume", false);
            (new AutoCheck(mActivity.getApplicationContext(), new Handler() {
                public void handleMessage(Message msg) {
                    if (msg.what == 100) {
                        AutoCheck autoCheck = (AutoCheck) msg.obj;
                        synchronized (autoCheck) {
                            String message = autoCheck.obtainErrorMessage(); // autoCheck.obtainAllMessage();
                            ; // 可以用下面一行替代，在logcat中查看代码
                            // Log.w("AutoCheckMessage", message);
                        }
                    }
                }
            }, false)).checkAsr(params);
            mRecordButton.setBackgroundResource(R.drawable.stop);

            // 这里打印出params， 填写至您自己的app中，直接调用下面这行代码即可。
            // DEMO集成步骤2.2 开始识别
            myRecognizer.start(params);
        } else {
            mChronometer.stop();
            mChronometer.setBase(SystemClock.elapsedRealtime());
            mRecordingPrompt.setText(mActivity.getString(R.string.record_prompt));

            myRecognizer.stop();
            mRecordButton.setBackgroundResource(R.drawable.sound);
        }
    }

    private void handleMsg(Message msg) {
        if(msg.arg2 == 1) {
            outputText.setText(msg.obj.toString() + outputText.getText());
        }else if(msg.arg2 == 0) {
//            time.start();
            pcmToWave(mFilePath + ".pcm", mFilePath + ".wav");
        }else if(msg.arg2 == 2) {
            Toast.makeText(mActivity, msg.obj.toString() , Toast.LENGTH_LONG).show();
        }
    }
    /**
     * android 6.0 以上需要动态申请权限
     */
    private void initPicturePermission() {
        String[] permissions = {
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        ArrayList<String> toApplyList = new ArrayList<String>();

        for (String perm : permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(mActivity, perm)) {
                toApplyList.add(perm);
                // 进入到这里代表没有权限.
                Toast.makeText(mActivity, "没有充足的权限", Toast.LENGTH_LONG).show();
            }
        }
        String[] tmpList = new String[toApplyList.size()];
        if (!toApplyList.isEmpty()) {
            ActivityCompat.requestPermissions(mActivity, toApplyList.toArray(tmpList), 3);
        }
    }

    /**
     * android 6.0 以上需要动态申请权限
     */
    private void initPermission() {
        String[] permissions = {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        ArrayList<String> toApplyList = new ArrayList<String>();

        for (String perm : permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(mActivity, perm)) {
                toApplyList.add(perm);
                // 进入到这里代表没有权限.
                Toast.makeText(mActivity, "没有充足的权限", Toast.LENGTH_LONG).show();
            }
        }
        String[] tmpList = new String[toApplyList.size()];
        if (!toApplyList.isEmpty()) {
            ActivityCompat.requestPermissions(mActivity, toApplyList.toArray(tmpList), 123);
        }
    }

    private void onRecord2(boolean start){

        Intent intent = new Intent(mActivity, RecordingService.class);

        if (start) {
            // start recording
            mRecordButton.setBackgroundResource(R.drawable.stop);
            //mPauseButton.setVisibility(View.VISIBLE);
            Toast.makeText(mActivity,R.string.toast_recording_start,Toast.LENGTH_SHORT).show();
            File folder = new File(Environment.getExternalStorageDirectory() + "/SoundRecorder");
            if (!folder.exists()) {
                //folder /SoundRecorder doesn't exist, create the folder
                folder.mkdir();
            }

            //start Chronometer
            startChronometer();

            //start RecordingService
            mActivity.startService(intent);
            //keep screen on while recording
//            mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            mRecordPromptCount++;

        } else {
            //stop recording
            mRecordButton.setBackgroundResource(R.drawable.sound);
            //mPauseButton.setVisibility(View.GONE);
            mChronometer.stop();
            mChronometer.setBase(SystemClock.elapsedRealtime());
            timeWhenPaused = 0;
            mRecordingPrompt.setText(mActivity.getString(R.string.record_prompt));

            mActivity.stopService(intent);
            //allow the screen to turn off again once recording is finished
//            mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    public void setFileNameAndPath(){
        int count = 0;
        File f;
        do{
            count++;
            mFileName = mActivity.getString(R.string.default_file_name)
                    + "_" + count;
            mFilePath = Environment.getExternalStorageDirectory().getAbsolutePath();
            mFilePath += "/baiduASR/" + mFileName;

            f = new File(mFilePath+ ".pcm");
        }while (f.exists() && !f.isDirectory());
    }

    private void startChronometer() {
        mChronometer.setBase(SystemClock.elapsedRealtime());
        mChronometer.start();
        mChronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                if (mRecordPromptCount == 0) {
                    mRecordingPrompt.setText(mActivity.getString(R.string.record_in_progress) + ".");
                } else if (mRecordPromptCount == 1) {
                    mRecordingPrompt.setText(mActivity.getString(R.string.record_in_progress) + "..");
                } else if (mRecordPromptCount == 2) {
                    mRecordingPrompt.setText(mActivity.getString(R.string.record_in_progress) + "...");
                    mRecordPromptCount = -1;
                }

                mRecordPromptCount++;
            }
        });
        mRecordingPrompt.setText(mActivity.getString(R.string.record_in_progress) + ".");
    }

    /**
     * 用明文ak，sk初始化
     */
    private void initAccessTokenWithAkSk() {
        OCR.getInstance(context).initAccessTokenWithAkSk(new OnResultListener<AccessToken>() {
            @Override
            public void onResult(AccessToken result) {
                String token = result.getAccessToken();
            }

            @Override
            public void onError(OCRError error) {
                error.printStackTrace();
//                alertText("AK，SK方式获取token失败", error.getMessage());
            }
        }, mActivity.getApplicationContext(),  "ou91mBqS1bUMkkTa0nDNvYSR", "CfnXB3G1RTHGlUxDwkl8Bdn12mGSkgbS");
    }

    private void pcmToWave(String inFileName, String outFileName) {
        int mRecorderBufferSize = AudioRecord.getMinBufferSize(mSampleRateInHZ,  AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long longSampleRate = mSampleRateInHZ;
        long totalDataLen = totalAudioLen + 36;
        int channels = 1;//你录制是单声道就是1 双声道就是2（如果错了声音可能会急促等）
        long byteRate = 16 * longSampleRate * channels / 8;

        byte[] data = new byte[mRecorderBufferSize];
        try {
            in = new FileInputStream(inFileName);
            out = new FileOutputStream(outFileName);

            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;
            writeWaveFileHeader(out, totalAudioLen, totalDataLen, longSampleRate, channels, byteRate);
            while (in.read(data) != -1) {
                out.write(data);
            }

            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /*
任何一种文件在头部添加相应的头文件才能够确定的表示这种文件的格式，wave是RIFF文件结构，每一部分为一个chunk，其中有RIFF WAVE chunk，
FMT Chunk，Fact chunk,Data chunk,其中Fact chunk是可以选择的，
*/
    private void writeWaveFileHeader(FileOutputStream out, long totalAudioLen, long totalDataLen, long longSampleRate,
                                     int channels, long byteRate) throws IOException {
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);//数据大小
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';//WAVE
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        //FMT Chunk
        header[12] = 'f'; // 'fmt '
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';//过渡字节
        //数据大小
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        //编码方式 10H为PCM编码格式
        header[20] = 1; // format = 1
        header[21] = 0;
        //通道数
        header[22] = (byte) channels;
        header[23] = 0;
        //采样率，每个通道的播放速度
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        //音频数据传送速率,采样率*通道数*采样深度/8
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        // 确定系统一次要处理多少个这样字节的数据，确定缓冲区，通道数*采样位数
        header[32] = (byte) (1 * 16 / 8);
        header[33] = 0;
        //每个样本的数据位数
        header[34] = 16;
        header[35] = 0;
        //Data chunk
        header[36] = 'd';//data
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }
}

