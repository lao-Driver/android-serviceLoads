package mydemo.threadloads;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import mydemo.threadloads.bean.DownLoadTask;
import mydemo.threadloads.bean.FileInfo;

/**
 * Created by 宝宝 on 2018/1/22.
 */

public class DownLoadService extends Service {
    public static final String DOWNLOAD_PATH= Environment.getExternalStorageDirectory().getAbsolutePath()+"/downloads/";
//    开始下载
    public static final String ACTION_START="ACTION_START";
//    停止下载
    public static final String ACTION_STOP="ACTION_STOP";
//    更新UI
    public static final String ACTION_UPDATE="ACTION_UPDATE";
//    结束下载命令
    public static final String ACTION_FINISHED="ACTION_FINISHED";
    public static final int MSG_INIT=0;
//    下载任务集合
    private Map<String,DownLoadTask>  taskMap=new LinkedHashMap<>();
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ACTION_START.equals(intent.getAction())){
//            获取文件信息对象
            FileInfo fileinfo = (FileInfo) intent.getSerializableExtra("fileinfo");
//             使用线程池启动线程
            InitThread initThread = new InitThread(fileinfo);
            DownLoadTask.mexecutor.execute(initThread);
        }else if (ACTION_STOP.equals(intent.getAction())){
            //            获取文件信息对象
            FileInfo fileinfo = (FileInfo) intent.getSerializableExtra("fileinfo");
//          从集合中去除下载任务
            DownLoadTask downLoadTask = taskMap.get(fileinfo.getId());
            if (downLoadTask!=null){
                downLoadTask.isPause=true;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }
    Handler handler=new Handler(){



        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what==MSG_INIT){
                FileInfo obj = (FileInfo) msg.obj;
//                开始下载
                DownLoadTask  downLoadTask = new DownLoadTask(DownLoadService.this,obj,3);
                downLoadTask.DownLoad();
//                添加到集合
                taskMap.put(obj.getId(),downLoadTask);
            }
        }
    };
    class InitThread extends Thread{
        FileInfo fileinfo=null;
        private HttpURLConnection urlConnection;
        private RandomAccessFile rand;

        public InitThread(FileInfo fileinfo){
           this.fileinfo= fileinfo;
        }
        @Override
        public void run() {
            super.run();
            try {
                URL url = new URL(fileinfo.getUrl());
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setConnectTimeout(5000);
                urlConnection.setReadTimeout(5000);
                urlConnection.setRequestMethod("GET");
               int length=-1;
                if ( urlConnection.getResponseCode() == 200) {
//                    获取下载文件的长度
                    length = urlConnection.getContentLength();
                }
                if (length<=0){
                    return;
                }
//                创建目录
                File fiel=new File(DOWNLOAD_PATH);
                if (!fiel.exists()){
                    fiel.mkdir();
                }
//                创建文件
                File file=new File(fiel,fileinfo.getFileName());
//                打开文件的类似与IO流 “rwd”模式可读可写可删
                rand = new RandomAccessFile(file,"rwd");
//                设置文件的长度
                rand.setLength(length);
                fileinfo.setLength(length);
                handler.obtainMessage(MSG_INIT,fileinfo).sendToTarget();

            } catch (Exception e) {
                e.printStackTrace();
            }finally {
                try {
                    urlConnection.disconnect();
                    rand.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
