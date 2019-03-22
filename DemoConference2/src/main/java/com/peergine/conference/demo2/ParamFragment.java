package com.peergine.conference.demo2;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.peergine.conference.demo2.sqlite.DatabaseHelper;
import com.peergine.conference.democonference2.R;
import com.peergine.util.Checker;


import me.yokeyword.fragmentation.SupportFragment;

import static android.text.TextUtils.isEmpty;


public class ParamFragment extends SupportFragment {

    EditText mEditUser = null;
    EditText mEditPass = null;
    EditText mEditSvraddr = null;
    EditText mEditRelayaddr = null;
    EditText mEditVideoparam = null;
    private EditText mEditInitparam;
    private EditText mEditExpire;


    final View.OnClickListener mOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final int btnID = v.getId();

            String sUser = mEditUser.getText().toString().trim();
//            String sPass = mEditPass.getText().toString().trim();
            String sSvrAddr = mEditSvraddr.getText().toString().trim();
//            String sRelayAddr = mEditRelayaddr.getText().toString().trim();
//            String sInitParam = mEditInitparam.getText().toString().trim();
//            String sVideoParam = mEditVideoparam.getText().toString().trim();
            if ("".equals(sUser) || "".equals(sSvrAddr) ) {
                Toast.makeText(getContext(), "部分参数为空。请检查。", Toast.LENGTH_SHORT).show();
                return;
            }
            wirteSql(sSvrAddr,sUser);
//            String sExpire = mEditExpire.getText().toString().trim();
            switch (btnID){
                case R.id.btnInitDefault:
                    Toast.makeText(getContext(), "进入Demo。", Toast.LENGTH_SHORT).show();
                    start(MainFragment.newInstance(sUser,"",sSvrAddr,"",
                            "","","","0"));
                    break;
//                case R.id.btnInitDouble:
//                    Toast.makeText(getContext(), "进入Demo。双源测试", Toast.LENGTH_SHORT).show();
//                    start(ComplexFragment.newInstance(sUser,sPass,sSvrAddr,sRelayAddr,
//                            sInitParam,sVideoParam,sExpire,"0"));
//                    break;
//                case R.id.btnInitCalling:
//                    Toast.makeText(getContext(), "进入Demo 模拟电话呼叫流程。", Toast.LENGTH_SHORT).show();
//                    start(MainFragmentCalling.newInstance(sUser,sPass,sSvrAddr,sRelayAddr,
//                            sInitParam,sVideoParam,sExpire));
//                    break;
//                case R.id.btnInitExter:
//                    Toast.makeText(getContext(), "进入Demo。", Toast.LENGTH_SHORT).show();
//                    start(MainFragmentExter.newInstance(sUser,sPass,sSvrAddr,sRelayAddr,
//                            sInitParam,sVideoParam,sExpire,"1"));
//                    break;
                default:break;
            }
        }
    };
    private DatabaseHelper database;
    private SQLiteDatabase db;


    public static ParamFragment newInstance(){
        ParamFragment fragment = new ParamFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_param, container, false);
        InitView(view);


        if(Checker.CameraCheck(this.getContext())){
            Toast.makeText(this.getContext(),"打开摄像头成功！",Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(this.getContext(),"打开摄像头失败。请检查摄像头权限！",Toast.LENGTH_SHORT).show();
        }
        if(Checker.RecordAudioCheck()){
            Toast.makeText(this.getContext(),"打开录音设备成功！",Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(this.getContext(),"打开录音设备失败。请检查录音设备权限！",Toast.LENGTH_SHORT).show();
        }

        return view;

    }

    void InitView(View view){

        mEditUser = view.findViewById(R.id.editText_user);

        mEditSvraddr = view.findViewById(R.id.editText_svraddr);
//        mEditInitparam = view.findViewById(R.id.editText_initparam);
//        mEditVideoparam = view.findViewById(R.id.editText_videoparam);

        view.findViewById(R.id.btnInitDefault).setOnClickListener(mOnClick);
//        view.findViewById(R.id.btnInitDouble).setOnClickListener(mOnClick);
//        view.findViewById(R.id.btnInitCalling).setOnClickListener(mOnClick);
//        view.findViewById(R.id.btnInitExter).setOnClickListener(mOnClick);
        readSql();
    }

    private void readSql(){
        if(database==null) {
            database = new DatabaseHelper(getActivity().getApplicationContext());//这段代码放到Activity类中才用this

            db = database.getWritableDatabase();
        }
        String addr = "";
        String id = "";
        try {
            Cursor c = db.query("config", null, null, null, null, null, null);//查询并获得游标
            if (c.moveToFirst()) {//判断游标是否为空

                c.move(c.getCount() - 1);//移动到指定记录
                addr = c.getString(c.getColumnIndex("addr"));
                id = c.getString(c.getColumnIndex("id"));
                c.close();
            }
        }catch (Exception e){

        }

        if(!isEmpty(addr)){
            mEditSvraddr.setText(addr);
        }
        if(!isEmpty(id)){
            mEditUser.setText(id);
        }
    }

    private void wirteSql(String sSvr ,String sID){
        if(database==null) {
            database = new DatabaseHelper(getActivity().getApplicationContext());//这段代码放到Activity类中才用this
            db = database.getWritableDatabase();
        }

        String whereClause = "addr=?";//删除的条件
        String[] whereArgs = {"*"};//删除的条件参数
        db.delete("config",whereClause,whereArgs);//执行删除

        //
        ContentValues cv = new ContentValues();//实例化一个ContentValues用来装载待插入的数据
        cv.put("addr", sSvr);
        cv.put("id", sID);
        db.insert("config",null,cv);//执行插入操作

    }
}
