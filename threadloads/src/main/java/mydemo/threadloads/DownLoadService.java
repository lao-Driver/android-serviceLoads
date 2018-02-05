package mydemo.threadloads;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.widget.RemoteViews;

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
   //通知栏的消息
    private static RemoteViews mRemoteViews;
    private NotificationManager systemService;
    private static Notification notification;
    private static NotificationManager notificationManager;
    private static int NOTIFICATION_ID=111;
    private static final int REQUEST_CODE_BROADCAST = 0X0001;
    private final String BROATCAST_LOAD="BROATCAST_LOAD";
//    下载任务集合
    private Map<String,DownLoadTask>  taskMap=new LinkedHashMap<>();
    private NotificationCompat.Builder builderProgress;

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
            initThread.start();
           // DownLoadTask.mexecutor.execute(initThread);
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
                //通知栏消息
                showNotificationProgress(DownLoadService.this);
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
    public void showNotificationProgress(Context context) {
        if (builderProgress==null){
        /**进度条通知构建**/
        builderProgress = new NotificationCompat.Builder(context);
        /**设置为一个正在进行的通知 作用是为了不能手动删除**/
        builderProgress.setOngoing(true);
        /**设置小图标**/
        builderProgress.setSmallIcon(R.mipmap.ic_launcher);

        /**新建通知自定义布局**/
        mRemoteViews = new RemoteViews(context.getPackageName(),R.layout.notification);
        /**进度条ProgressBar**/
        mRemoteViews.setProgressBar(R.id.pb, 100, 0, false);
        /**提示信息的TextView**/
        mRemoteViews.setTextViewText(R.id.tv_message, "下载中...");
        /**操作按钮的Button**/
        mRemoteViews.setTextViewText(R.id.bt, "下载");

        /**设置左侧小图标*/
        mRemoteViews.setImageViewResource(R.id.iv, R.mipmap.ic_launcher);
        /**设置通过广播形式的PendingIntent**/
        Intent intent = new Intent(BROATCAST_LOAD);
//        通过广播的形式控制下载和暂停
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,REQUEST_CODE_BROADCAST, intent, 0);
        mRemoteViews.setOnClickPendingIntent(R.id.bt, pendingIntent);
        /**设置自定义布局**/
        builderProgress.setContent(mRemoteViews);
        /**设置滚动提示**/
        builderProgress.setTicker("开始下载...");
        notification = builderProgress.build();
        /**设置不可手动清除**/
        notification.flags = Notification.FLAG_NO_CLEAR;
        /**获取通知管理器**/
        notificationManager = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
        /**发送一个通知 参数一 是为了确定同一条消息，参数二 notification的对象**/
        notificationManager.notify(NOTIFICATION_ID, notification);}
    }
//    更新通知栏的方法
    public static void updateNotification(int current) {
        mRemoteViews.setTextViewText(R.id.tv_size, current + "/" + "100");
        mRemoteViews.setTextViewText(R.id.tv_progress, current + "%");
        mRemoteViews.setProgressBar(R.id.pb, 100, current, false);
        notificationManager.notify(NOTIFICATION_ID, notification);

    }
//    关闭通知栏
    public  static void LoadSuccense(){
        notificationManager.cancel(NOTIFICATION_ID);
    }


}
