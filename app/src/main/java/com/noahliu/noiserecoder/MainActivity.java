package com.noahliu.noiserecoder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private boolean hasPermission = false, isRecoding;
    private MediaRecorder mediaRecorder;
    private TextView tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //取德麥克風使用權限
        checkPermission();
        Button btStart,btStop;
        btStart = findViewById(R.id.button_Start);
        btStop = findViewById(R.id.button_Stop);
        tvResult = findViewById(R.id.textView_Result);
        btStart.setOnClickListener(v->{startMeasure();});
        btStop.setOnClickListener(v->{stopMeasure();});

    }
    /**確認是否有麥克風使用權限*/
    private void checkPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ActivityCompat.checkSelfPermission(this
                        , Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this
                    ,new String[]{Manifest.permission.RECORD_AUDIO},100);
        }else hasPermission = true;
    }
    /**取得權限回傳*/
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            hasPermission = true;
        }
    }
    /**開啟檢測*/
    private void startMeasure(){
        if (!hasPermission || isRecoding)return;
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile("/dev/null");
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            handlerMeasure.post(taskMeasure);
            isRecoding = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**關閉檢測*/
    private void stopMeasure(){
        if (!hasPermission || !isRecoding)return;
        handlerMeasure.removeCallbacks(taskMeasure);
        try {
            mediaRecorder.release();
            mediaRecorder.stop();
        }catch (IllegalStateException e){
            e.printStackTrace();
        }
        isRecoding = false;
    }
    /**透過HandlerTask 取得檢測結果*/
    @SuppressLint("HandlerLeak")
    private Handler handlerMeasure = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case 1:
                    int amp = mediaRecorder.getMaxAmplitude();
                    //公式：Gdb = 20log10(V1/V0)
                    //Google已提供方法幫你取得麥克風的檢測電壓(V1)以及參考電壓(V0)
                    double db = 20*(Math.log10(Math.abs(amp)));
                    //if -Infinity
                    if (Math.round(db) == -9223372036854775808.0) tvResult.setText("0 db");
                    else tvResult.setText(Math.round(db)+" db");
                    break;
            }
            super.handleMessage(msg);

        }
    };
    private Runnable taskMeasure = new Runnable() {
        @Override
        public void run() {
            handlerMeasure.sendEmptyMessage(1);
            //每500毫秒抓取一次檢測結果
            handlerMeasure.postDelayed(this,500);
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        stopMeasure();
    }
}