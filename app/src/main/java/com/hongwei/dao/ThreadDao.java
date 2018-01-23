package com.hongwei.dao;

import com.hongwei.bean.ThreadInfo;

import java.util.List;

/**
 * Created by 宝宝 on 2018/1/22.
 */

public interface ThreadDao {
//    插入线程的信息
    public void insertThread(ThreadInfo threadInfo);
//    删除线程
    public void deleteThread(String url,int thread_id);
//    更新下载进度
    public void updateThread(String url,int thread_id,int finished);
//    查询文件的线程信息
    public List<ThreadInfo> getThreads(String url);
//    判断线程信息是否存在
    public boolean isExists(String url,int thread_id);

}
