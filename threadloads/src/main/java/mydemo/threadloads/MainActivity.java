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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import mydemo.threadloads.bean.FileInfo;
import mydemo.threadloads.dao.ThreadDaoImpl;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView content;
    private ProgressBar progressBar;
    private Button load;
    private Button stop;
    private FileInfo fileInfo;
//    多个下载任务添加到集合，文件的id对应文件在集合中的下标
    private List<FileInfo> fileList=new ArrayList<>();
    private ThreadDaoImpl threadDao;
    private int lengh;
    private int NOTIFICATION_ID=111;
    private final String BROATCAST_LOAD="BROATCAST_LOAD";
    private Intent intent;
    private Intent intent2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        fileInfo = new FileInfo("0", "911Mothers_2010W-480p.mp4", "http://mirror.aarnet.edu.au/pub/TED-talks/911Mothers_2010W-480p.mp4", 0, 0);
        fileList.add(fileInfo);
        content.setText(fileInfo.getFileName());
        progressBar.setMax(100);
        threadDao = new ThreadDaoImpl(this);
        lengh = fileInfo.getLength()/100;
        //        注册广播
        IntentFilter filter=new IntentFilter();
        filter.addAction(DownLoadService.ACTION_UPDATE);
        filter.addAction(DownLoadService.ACTION_FINISHED);
        filter.addAction(BROATCAST_LOAD);
        registerReceiver(broadcastReceiver,filter);

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
                intent = new Intent(MainActivity.this,DownLoadService.class);
                intent.setAction(DownLoadService.ACTION_START);
                intent.putExtra("fileinfo",fileInfo);
                startService(intent);

                break;
            case R.id.stop:
                intent2 = new Intent(MainActivity.this,DownLoadService.class);
                intent2.setAction(DownLoadService.ACTION_STOP);
                intent2.putExtra("fileinfo",fileInfo);
                startService(intent2);
                break;
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (broadcastReceiver!=null){
            unregisterReceiver(broadcastReceiver);
        }
        if (intent!=null){
            stopService(intent);
        }
        if (intent2!=null){
            stopService(intent2);
        }
    }

    //    广播接收器
    BroadcastReceiver broadcastReceiver=new BroadcastReceiver() {


        @Override
        public void onReceive(Context context, final Intent intent) {
            if (DownLoadService.ACTION_UPDATE.equals(intent.getAction())){
                 int finished = intent.getIntExtra("finished", 0);
                 int id = intent.getIntExtra("id", 0);
                setProgress(String.valueOf(id),finished);
//                更新通知栏的进度
                DownLoadService.updateNotification(finished);
            }else if (DownLoadService.ACTION_FINISHED.equals(intent.getAction())){
                FileInfo fileInfo = (FileInfo) intent.getSerializableExtra("fileInfo");
                setProgress(fileInfo.getId(),0);
//                关闭通知栏
                DownLoadService.LoadSuccense();
//                下载完成的通知
                Toast.makeText(MainActivity.this,fileList.get(Integer.valueOf(fileInfo.getId())).getFileName(),Toast.LENGTH_SHORT).show();
            }else if(BROATCAST_LOAD.equals(intent.getAction())){
                Intent intent2=new Intent(MainActivity.this,DownLoadService.class);
                intent2.setAction(DownLoadService.ACTION_START);
                intent2.putExtra("fileinfo",fileInfo);
                startService(intent2);
            }
        }
    };

    public void setProgress(String  id,int Progress){
        FileInfo fileInfo = fileList.get(Integer.valueOf(id));
        fileInfo.setFinished(Progress);
        progressBar.setProgress(fileInfo.getFinished());
    }



}
