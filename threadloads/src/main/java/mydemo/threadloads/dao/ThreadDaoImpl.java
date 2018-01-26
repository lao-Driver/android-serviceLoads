package mydemo.threadloads.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import mydemo.threadloads.bean.ThreadInfo;

/**
 * Created by 宝宝 on 2018/1/22.
 */

public class ThreadDaoImpl implements ThreadDao {
    DBHelper dbHelper=null;

    public ThreadDaoImpl(Context context){
        dbHelper = DBHelper.getInstance(context);
    }

    @Override
    public synchronized void insertThread(ThreadInfo threadInfo) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values=new ContentValues();
        values.put("thread_id",threadInfo.getId());
        values.put("url",threadInfo.getUrl());
        values.put("start",threadInfo.getStart());
        values.put("end",threadInfo.getEnd());
        values.put("finished",threadInfo.getFinished());
        db.insert("thread_info", "_id", values);
    }

    @Override
    public synchronized void deleteThread(String url) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("thread_info","url=? ",new String[]{url});
        db.close();
    }

    @Override
    public synchronized void updateThread(String url, int thread_id, int finished) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values=new ContentValues();
        values.put("finished",String.valueOf(finished));
        db.update("thread_info",values,"url=? and thread_id=?",new String[]{url,String.valueOf(thread_id)});
        db.close();
    }

    @Override
    public List<ThreadInfo> getThreads(String url) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<ThreadInfo> list=new ArrayList<>();
        Cursor th = db.query("thread_info", null, "url=?", new String[]{url}, null, null, null, null);
        while (th.moveToNext()){
            ThreadInfo threadInfo = new ThreadInfo(th.getInt(th.getColumnIndex("thread_id")), th.getString(th.getColumnIndex("url")), th.getInt(th.getColumnIndex("start")), th.getInt(th.getColumnIndex("end")), th.getInt(th.getColumnIndex("finished")));
              list.add(threadInfo);
        }
        th.close();
        db.close();
        return list;
    }

    @Override
    public boolean isExists(String url, int thread_id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor th = db.query("thread_info", null, "url=? and thread_id=?",new String[]{url,String.valueOf(thread_id)}, null, null, null, null);
        boolean b = th.moveToNext();
        return b;
    }
}
