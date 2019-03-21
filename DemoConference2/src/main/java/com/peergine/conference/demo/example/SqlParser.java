package com.peergine.conference.demo.example;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.peergine.conference.demo.sqlite.DatabaseHelper;

import static android.text.TextUtils.isEmpty;

public class SqlParser {

    public static String readSqlChairmanID(Context context){
        //这段代码放到Activity类中才用this
        DatabaseHelper database = new DatabaseHelper(context);
        SQLiteDatabase db = database.getWritableDatabase();

        String chairman_id = "";

        try {
            Cursor c = db.query("config2", null, null, null, null, null, null);//查询并获得游标
            if (c.moveToFirst()) {//判断游标是否为空

                c.move(c.getCount() - 1);//移动到指定记录
                chairman_id = c.getString(c.getColumnIndex("chairman_id"));
                c.close();
            }
        }catch (Exception e){

        }

        return chairman_id;
    }

    public static void wirteSql(String chairman_id,Context context){

        DatabaseHelper database = new DatabaseHelper(context);//这段代码放到Activity类中才用this
        SQLiteDatabase db = database.getWritableDatabase();

        String whereClause = "chairman_id=?";//删除的条件
        String[] whereArgs = {"*"};//删除的条件参数
        db.delete("config2",whereClause,whereArgs);//执行删除
        //
        ContentValues cv = new ContentValues();//实例化一个ContentValues用来装载待插入的数据

        cv.put("chairman_id", chairman_id);
        db.insert("config2",null,cv);//执行插入操作

    }

}
