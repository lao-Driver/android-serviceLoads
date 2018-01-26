package mydemo.threadloads.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static android.R.attr.version;

/**
 * Created by 宝宝 on 2018/1/22.
 */

public class DBHelper extends SQLiteOpenHelper {
    private final  static String DB_NAME="download.db";
    private final int VERSION=1;
    private final String SQ_CREATA="create table thread_info(_id integer primary key,thread_id integer,url text,start integer,end integer,finished integer)";
    private final String SQ_DROP="drop table if exists thread_info";
    private static DBHelper dbHelper=null;
    private DBHelper(Context context) {
        super(context,DB_NAME, null, version);
    }
public static DBHelper getInstance(Context context){
    if (dbHelper==null){
        synchronized (DBHelper.class){
            if (dbHelper==null){
                dbHelper = new DBHelper(context);
                return dbHelper;
            }
            }
        }
    return dbHelper;
}
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(SQ_CREATA);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL(SQ_DROP);
        sqLiteDatabase.execSQL(SQ_CREATA);
    }
}
