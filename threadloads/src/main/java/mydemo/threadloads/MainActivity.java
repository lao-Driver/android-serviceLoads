package mydemo.threadloads;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import mydemo.threadloads.bean.FileInfo;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView content;
    private ProgressBar progressBar;
    private Button load;
    private Button stop;
    private FileInfo fileInfo;
    private List<FileInfo> fileList=new ArrayList<>();
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
        filter.addAction(DownLoadService.ACTION_FINISHED);
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
                 int id = intent.getIntExtra("id", 0);


            } if (DownLoadService.ACTION_FINISHED.equals(intent.getAction())){
                FileInfo fileInfo = (FileInfo) intent.getSerializableExtra("fileInfo");
                setProgress(fileInfo.getId(),0);
                //Toast.makeText(MainActivity.this,fileInfo)
            }
        }
    };

    public void setProgress(String  id,int Progress){
        //progressBar.setProgress();
    }
}
