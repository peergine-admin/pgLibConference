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

import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;
import com.yanzhenjie.permission.Rationale;
import com.yanzhenjie.permission.RequestExecutor;
import com.yanzhenjie.permission.SettingService;

import java.util.List;

import me.yokeyword.fragmentation.SupportFragment;

import static com.yanzhenjie.permission.Permission.transformText;

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
                    Toast.makeText(getContext(), "进入Demo。", Toast.LENGTH_SHORT).show();
                    start(MainFragment.newInstance(sUser,sPass,sSvrAddr,sRelayAddr,
                            sInitParam,sVideoParam,sExpire,"0"));
                    break;
                case R.id.btnInitCalling:
                    Toast.makeText(getContext(), "进入Demo 模拟电话呼叫流程。", Toast.LENGTH_SHORT).show();
                    start(MainFragmentCalling.newInstance(sUser,sPass,sSvrAddr,sRelayAddr,
                            sInitParam,sVideoParam,sExpire));
                    break;
                case R.id.btnInitExter:
                    Toast.makeText(getContext(), "进入Demo。", Toast.LENGTH_SHORT).show();
                    start(MainFragment.newInstance(sUser,sPass,sSvrAddr,sRelayAddr,
                            sInitParam,sVideoParam,sExpire,"1"));
                    break;
                default:break;
            }
        }
    };



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


        AndPermission.with(this)
                .permission(
                        Permission.CAMERA,
                        Permission.RECORD_AUDIO
                )
                //.rationale(mRationale)
                .onGranted(new Action() {
                    @Override
                    public void onAction(List<String> permissions) {
                        // TODO what to do.
                        Toast.makeText(getContext(),"获取到的权限有：" + permissions.toString(),Toast.LENGTH_SHORT).show();

                    }
                })
                .onDenied(new Action() {
                    @Override
                    public void onAction(List<String> permissions) {
                        // TODO what to do
                        Toast.makeText(getContext(),"未获取到的权限有：" + transformText(getContext(),permissions).toString(),Toast.LENGTH_SHORT).show();
                        if (AndPermission.hasAlwaysDeniedPermission(getContext(), permissions)) {
                            // 这里使用一个Dialog展示没有这些权限应用程序无法继续运行，询问用户是否去设置中授权。

                            final SettingService settingService = AndPermission.permissionSetting(getContext());

                            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                            builder.setTitle("询问！");
                            builder.setMessage("没有视音频权限，是否去设置中授权。");
                            builder.setPositiveButton("好的", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // 如果用户同意去设置：
                                    settingService.execute();
                                    android.os.Process.killProcess(android.os.Process.myPid());
                                }
                            });
                            builder.setNegativeButton("拒绝", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // 如果用户不同意去设置：
                                    settingService.cancel();
                                    android.os.Process.killProcess(android.os.Process.myPid());
                                }
                            });
                            builder.show();
                        }


                    }
                })
                .start();
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
