package com.peergine.conference.demo2.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    //类没有实例化,是不能用作父类构造器的参数,必须声明为静态
    private static final String name = "config.db"; //数据库名称
    private static final int version = 1; //数据库版本

    public DatabaseHelper(Context context) {
        //第三个参数CursorFactory指定在执行查询时获得一个游标实例的工厂类,设置为null,代表使用系统默认的工厂类
        super(context, name, null, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("CREATE TABLE IF NOT EXISTS config(addr varchar(20) not null , id varchar(60) not null);");
        db.execSQL("CREATE TABLE IF NOT EXISTS config2(chairman_id varchar(60) not null);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        //db.execSQL("ALTER TABLE person ADD phone VARCHAR(12)"); //往表中增加一列

    }


}
