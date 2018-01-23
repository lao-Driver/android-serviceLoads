package com.hongwei;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.hongwei.bean.FileInfo;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView content;
    private ProgressBar progressBar;
    private Button load;
    private Button stop;
    private FileInfo fileInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        fileInfo = new FileInfo("0", "911Mothers_2010W-480p.mp4", "http://mirror.aarnet.edu.au/pub/TED-talks/911Mothers_2010W-480p.mp4", 0, 0);
        content.setText(fileInfo.getFileName());
        progressBar.setMax(100);

    }

    private void initView() {
        content = (TextView) findViewById(R.id.content);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        load = (Button) findViewById(R.id.load);
        stop = (Button) findViewById(R.id.stop);
        load.setOnClickListener(this);
        stop.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.load:
                Intent intent=new Intent(MainActivity.this,DownLoadService.class);
                intent.setAction(DownLoadService.ACTION_START);
                intent.putExtra("fileinfo",fileInfo);
                startService(intent);
                break;
            case R.id.stop:
                Intent intent2=new Intent(MainActivity.this,DownLoadService.class);
                intent2.setAction(DownLoadService.ACTION_STOP);
                intent2.putExtra("fileinfo",fileInfo);
                startService(intent2);
                break;
        }
//        注册广播
        IntentFilter filter=new IntentFilter();
        filter.addAction(DownLoadService.ACTION_UPDATE);
        registerReceiver(broadcastReceiver,filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }

    //    广播接收器
    BroadcastReceiver broadcastReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            if (DownLoadService.ACTION_UPDATE.equals(intent.getAction())){
                 int finished = intent.getIntExtra("finished", 0);
                Log.d("TAG",finished+"da--");
                progressBar.setProgress(finished);

            }
        }
    };
}
