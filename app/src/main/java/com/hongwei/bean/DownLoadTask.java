package com.hongwei.bean;

import android.content.Context;
import android.content.Intent;

import com.hongwei.DownLoadService;
import com.hongwei.dao.ThreadDaoImpl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by 宝宝 on 2018/1/22.
 */

public class DownLoadTask {
    Context context;
    FileInfo fileInfo;
    private final ThreadDaoImpl threadDao;
    private int mfinished=0;
    public  boolean isPause=false;
    public DownLoadTask(Context context, FileInfo fileInfo){
       this.fileInfo=fileInfo;
        this.context=context;
        threadDao = new ThreadDaoImpl(context);
    }
    public void DownLoad(){
        List<ThreadInfo> threads = threadDao.getThreads(fileInfo.getUrl());
        ThreadInfo threadInfo=null;
        if (threads.size()==0){
            threadInfo = new ThreadInfo(0, fileInfo.getUrl(), 0, fileInfo.getLength(), 0);
        }else{
            threadInfo = threads.get(0);
        }
//        创建子线程进行下载
         new DownLoadThread(threadInfo).start();
    }
    
    class DownLoadThread extends Thread{
        ThreadInfo threadInfo;
        private InputStream inputStream;
        private RandomAccessFile rand;
        private BufferedInputStream buffered;

        public DownLoadThread(ThreadInfo threadInfo){
            this.threadInfo=threadInfo;
        }
        @Override
        public void run() {
            super.run();

//            向数据库插入线程信息
          if (!threadDao.isExists(threadInfo.getUrl(),threadInfo.getId())){
              threadDao.insertThread(threadInfo);
          }
            URL url = null;
            HttpURLConnection urlConnection=null;
            try {
                url = new URL(threadInfo.getUrl());
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
                    //                设置更新UI的时间 500秒更新一次 这样保持App性能
                    long time= System.currentTimeMillis();
                    int fen=fileInfo.getLength()/100;
                    while ((length= buffered.read(butt,0,butt.length))!=-1){
                        rand.write(butt,0,length);
//                        把下载的进度通过广播发送个Activity
                        mfinished+=length;
                        if (System.currentTimeMillis()-time>500){
                            time= System.currentTimeMillis();
                             intent.putExtra("finished",mfinished/fen );
                             context.sendBroadcast(intent);

                        }
//                        在暂停状态中存入数据
                        if (isPause){
                            threadDao.updateThread(threadInfo.getUrl(),threadInfo.getId(),mfinished);
                            return;
                        }
                    }
//                    下载完成删除数据库线程的信息
                    threadDao.deleteThread(threadInfo.getUrl(),threadInfo.getId());
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
