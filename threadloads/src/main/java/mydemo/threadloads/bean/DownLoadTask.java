package mydemo.threadloads.bean;

import android.content.Context;
import android.content.Intent;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

import mydemo.threadloads.DownLoadService;
import mydemo.threadloads.dao.ThreadDaoImpl;

/**
 * Created by 宝宝 on 2018/1/22.
 */

public class DownLoadTask {
    Context context;
    FileInfo fileInfo;
    private final ThreadDaoImpl threadDao;
    private int mfinished=0;
    public  boolean isPause=false;
    public int mThreadCount=1; // 线程的数量
    public List<DownLoadThread> threadList=null;
//    创建线程池
    public static ExecutorService mexecutor= Executors.newCachedThreadPool();

    public DownLoadTask(Context context, FileInfo fileInfo,int mThreadCount){
       this.fileInfo=fileInfo;
        this.context=context;
        this.mThreadCount=mThreadCount;
        threadDao = new ThreadDaoImpl(context);

    }
    public void DownLoad(){
        List<ThreadInfo> threads = threadDao.getThreads(fileInfo.getUrl());

        // 判断是否是第一次下载
        if (threads.size()==0){
//       获取每个下载线程的长度
            int len = fileInfo.getLength() / mThreadCount;
            for (int i=0;i<mThreadCount;i++){
//                创建线程信息
                ThreadInfo threadInfo=new ThreadInfo(i,fileInfo.getUrl(),len*i,(i+1)*len-1,0);
                if (i==mThreadCount-1){
                    //为了防止最后一个线程除不尽 所以把下载结束的位置设成文件的长度
                    threadInfo.setEnd(fileInfo.getLength());
                }
                //把线程信息添加到集合
                threads.add(threadInfo);
                //            向数据库插入线程信息
                threadDao.insertThread(threadInfo);
            }
        }
        threadList=new ArrayList<>();
//        启动多个线程进行下载
        for (ThreadInfo th:threads){
            DownLoadThread downLoadThread = new DownLoadThread(th);
//            使用线程池来启动线程
            downLoadThread.start();
           // DownLoadTask.mexecutor.execute(downLoadThread);
//            添加到线程集合
            threadList.add(downLoadThread);

        }
    }
//    判断是否所有线程都执行完毕
    private synchronized void checkThreadsFinished(){
        boolean allThread=true;
        for (DownLoadThread dow:threadList){
            if (!dow.isFinished){
                allThread=false;
                break;
            }
        }
        if (allThread){
            //                    下载完成删除数据库线程的信息
            threadDao.deleteThread(fileInfo.getUrl());
//            发送广播通知UI下载任务结束
            Intent intent=new Intent(DownLoadService.ACTION_FINISHED);
            intent.putExtra("fileInfo",fileInfo);
            context.sendBroadcast(intent);
        }
    }
    class DownLoadThread extends Thread{
        ThreadInfo threadInfo;
        private InputStream inputStream;
        private RandomAccessFile rand;
        private BufferedInputStream buffered;
        public boolean isFinished=false;//判断线程是否执行完毕

        public DownLoadThread(ThreadInfo threadInfo){
            this.threadInfo=threadInfo;
        }
        @Override
        public void run() {
            super.run();
            URL url = null;
            HttpURLConnection urlConnection=null;
            try {
                url = new URL(threadInfo.getUrl());
                //Log.d("TAG",threadInfo.getUrl()+"bu--");
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setConnectTimeout(5000);
                urlConnection.setRequestMethod("GET");
//                设置下载位置  "Range"-下载的范围  "bytes="+start+"-"+threadInfo.getEnd() 设置文件的开始位置和结束位置
                int start=threadInfo.getStart()+threadInfo.getFinished();
                urlConnection.setRequestProperty("Range","bytes="+start+"-"+threadInfo.getEnd());

//                设置文件的写入位置
                File file=new File(DownLoadService.DOWNLOAD_PATH,fileInfo.getFileName());
                rand = new RandomAccessFile(file,"rwd");
                rand.seek(start);
                Intent intent=new Intent(DownLoadService.ACTION_UPDATE);
                mfinished+=threadInfo.getFinished();
//                开始下载   HttpsURLConnection.HTTP_PARTIAL(206 状态码的意思)最后一条会话返回了HTTP/206 “Partial Content”响应.这种响应是在客户端表明自己
//                 只需要目标URL上的部分资源的时候返回的.这种情况经常发生在客户端继续请求一个未完成的下载的时候
                //为什么请求码是206 不是200 是因为urlConnection.setRequestProperty("Range", 方法，请求的是部分资源
                if (urlConnection.getResponseCode()== HttpsURLConnection.HTTP_PARTIAL){
                    inputStream = urlConnection.getInputStream();
                    buffered = new BufferedInputStream(inputStream);
                    byte[] butt=new byte[1024 * 4];
                    int length=0;
                    //                设置更新UI的时间 1000秒更新一次 这样保持App性能
                    long time= System.currentTimeMillis();
                    int fen=fileInfo.getLength()/100;
                    while ((length= buffered.read(butt,0,butt.length))!=-1){
                        rand.write(butt,0,length);
//                        把下载的进度通过广播发送个Activity
                        mfinished+=length;
//                        累加每个线程完成的进度

                        threadInfo.setFinished(threadInfo.getFinished()+length);

                        if (System.currentTimeMillis()-time>1000){
                            time= System.currentTimeMillis();
                                intent.putExtra("finished",mfinished/fen );
                                intent.putExtra("id",fileInfo.getId() );
                                context.sendBroadcast(intent);

                        }
//                        在暂停状态中存入数据
                        if (isPause){
                            threadDao.updateThread(threadInfo.getUrl(),threadInfo.getId(),threadInfo.getFinished());

                            return;
                        }
                    }
//                    表示线程执行完毕
                    isFinished=true;

//                    检查线程是否执行完毕
                    checkThreadsFinished();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }finally {

                try {
                    rand.close();
                    buffered.close();
                    urlConnection.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
    }
}
