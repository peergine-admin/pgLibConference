package com.peergine.conference.demo;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Fragment;
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

import me.yokeyword.fragmentation.SupportFragment;

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

            if(checkCameraPermission(getContext())) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("信息");
                builder.setMessage("本Demo可以设置一个主席端，和多个成员端。");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String sUser = mEditUser.getText().toString().trim();
                        String sPass = mEditPass.getText().toString().trim();
                        String sSvrAddr = mEditSvraddr.getText().toString().trim();
                        String sRelayAddr = mEditRelayaddr.getText().toString().trim();
                        String sInitParam = mEditInitparam.getText().toString().trim();
                        String sVideoParam = mEditVideoparam.getText().toString().trim();
                        if ("".equals(sUser) || "".equals(sSvrAddr) || "".equals(sVideoParam)) {
                            Toast.makeText(getContext(), "部分参数为空。请检查。", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String sExpire = mEditExpire.getText().toString().trim();
                        switch (btnID){
                            case R.id.btnInitDefault:
                                start(MainFragment.newInstance(sUser,sPass,sSvrAddr,sRelayAddr,
                                        sInitParam,sVideoParam,sExpire,"0"));
                                break;
                            case R.id.btnInitCalling:
                                start(MainFragmentCalling.newInstance(sUser,sPass,sSvrAddr,sRelayAddr,
                                        sInitParam,sVideoParam,sExpire));
                                break;
                            case R.id.btnInitExter:
                                start(MainFragment.newInstance(sUser,sPass,sSvrAddr,sRelayAddr,
                                        sInitParam,sVideoParam,sExpire,"1"));
                                break;
                            default:break;
                        }
                    }
                });
                builder.show();

            }
            else {

                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("错误！");
                builder.setMessage("没有获取到摄像头权限。");
                builder.setPositiveButton("OK", null);
                builder.setNegativeButton("返回",null);
                builder.show();
            }
        }
    };



    public static ParamFragment newInstance(){
        ParamFragment fragment = new ParamFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }


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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_param, container, false);
        InitView(view);
        return view;
    }

    void InitView(View view){

        mEditUser = view.findViewById(R.id.editText_user);
        mEditPass = view.findViewById(R.id.editText_pass);
        mEditSvraddr = view.findViewById(R.id.editText_svraddr);
        mEditRelayaddr = view.findViewById(R.id.editText_relay);
        mEditInitparam = view.findViewById(R.id.editText_initparam);
        mEditVideoparam = view.findViewById(R.id.editText_videoparam);

        mEditExpire = view.findViewById(R.id.editText_expire);

        view.findViewById(R.id.btnInitDefault).setOnClickListener(mOnClick);
        view.findViewById(R.id.btnInitCalling).setOnClickListener(mOnClick);
        view.findViewById(R.id.btnInitExter).setOnClickListener(mOnClick);
    }
}
