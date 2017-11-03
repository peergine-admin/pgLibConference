package com.peergine.conference.demo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class ParamActivity extends Activity {
    public static final String MODE_DEFAULT = "default";
    public static final String MODE_CALLING = "calling";
    EditText mEditUser = null;
    EditText mEditPass = null;
    EditText mEditSvraddr = null;
    EditText mEditRelayaddr = null;
    EditText mEditVideoparam = null;


    EditText mEditExpire = null;
    EditText mEditMaxpeer = null;
    EditText mEditMaxobject = null;
    EditText mEditMaxmcast = null;
    EditText mEditMaxhandle = null;


    Button mBtnInitDefaule = null;
    Button mBtnInitCalling = null;
    private String mMode = "";
    private BlankFragment blankFragment;

    private ContentFragment contentFragment;
    View.OnClickListener mOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.btnInitDefault:
                    mMode = MODE_DEFAULT;
                    break;
                case R.id.btnInitCalling:
                    FragmentManager fm = getFragmentManager();
                    // 开启Fragment事务
                    FragmentTransaction transaction = fm.beginTransaction();
                    FragmentTransaction replace = transaction.replace(R.id.rootLayout,blankFragment );
                    transaction.commit();
                    mMode = MODE_CALLING;
                    break;
                default:break;
            }
            if(!"".equals(mMode)){

                return;
            }
            if(checkCameraPermission(ParamActivity.this)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ParamActivity.this);
                builder.setTitle("信息");
                builder.setMessage("本Demo可以设置一个主席端，和多个成员端。");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String sUser = mEditUser.getText().toString().trim();
                        String sPass = mEditPass.getText().toString().trim();
                        String sSvrAddr = mEditSvraddr.getText().toString().trim();
                        String sRelayAddr = mEditRelayaddr.getText().toString().trim();
                        String sVideoParam = mEditVideoparam.getText().toString().trim();
                        if ("".equals(sUser) || "".equals(sSvrAddr) || "".equals(sVideoParam)) {
                            Toast.makeText(getApplicationContext(), "部分参数为空。请检查。", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String sExpire = mEditExpire.getText().toString().trim();
                        String sMaxPeer = mEditMaxpeer.getText().toString().trim();
                        String sMaxObject = mEditMaxobject.getText().toString().trim();
                        String sMaxMCast = mEditMaxmcast.getText().toString().trim();
                        String sMaxHandle = mEditMaxhandle.getText().toString().trim();

                        Intent intent = new Intent();

                        intent.putExtra("User", sUser);
                        intent.putExtra("Pass", sPass);
                        intent.putExtra("SvrAddr", sSvrAddr);
                        intent.putExtra("RelayAddr", sRelayAddr);
                        intent.putExtra("VideoParam", sVideoParam);
                        intent.putExtra("Expire", sExpire);
                        intent.putExtra("MaxPeer", sMaxPeer);
                        intent.putExtra("MaxObject", sMaxObject);
                        intent.putExtra("MaxMCast", sMaxMCast);
                        intent.putExtra("MaxHandle", sMaxHandle);
                        intent.putExtra("Mode",mMode);


                        intent.setClass(ParamActivity.this, MainActivity.class);
                        startActivity(intent);
                        mMode = "";
                    }
                });
                builder.show();

            }
            else {

                AlertDialog.Builder builder = new AlertDialog.Builder(ParamActivity.this);
                builder.setTitle("错误！");
                builder.setMessage("没有获取到摄像头权限。");
                builder.setPositiveButton("OK", null);
                builder.setNegativeButton("返回",null);
                builder.show();
            }
        }
    };


    public static boolean checkCameraPermission(Context context) {
        boolean canUse = true;
        Camera mCamera = null;
        try {
            mCamera = Camera.open(0);
            mCamera.setDisplayOrientation(90);
        } catch (Exception e) {
            Log.getStackTraceString(e);
            canUse = false;
        }
        if (canUse) {
            mCamera.release();
        }
        return canUse;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_param);

        blankFragment = new BlankFragment();

        mEditUser = findViewById(R.id.editText_user);
        mEditPass = findViewById(R.id.editText_pass);
        mEditSvraddr = findViewById(R.id.editText_svraddr);
        mEditRelayaddr = findViewById(R.id.editText_relay);
        mEditVideoparam = findViewById(R.id.editText_videoparam);


        mEditExpire = findViewById(R.id.editText_expire);
        mEditMaxpeer = findViewById(R.id.editText_MaxPeer);
        mEditMaxobject = findViewById(R.id.editText_MaxObject);
        mEditMaxmcast = findViewById(R.id.editText_MaxMCast);
        mEditMaxhandle = findViewById(R.id.editText_MaxHandle);

        mBtnInitDefaule = findViewById(R.id.btnInitDefault);
        mBtnInitCalling = findViewById(R.id.btnInitCalling);

        mBtnInitDefaule.setOnClickListener(mOnClick);
        mBtnInitCalling.setOnClickListener(mOnClick);
    }

    @SuppressLint("ValidFragment")
    class ContentFragment extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.activity_main, container,false);
        }

    }
}
