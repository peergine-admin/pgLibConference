package com.peergine.conference.demo2;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.KeyEvent;
import android.view.WindowManager;

import com.peergine.conference.democonference2.R;

import java.util.List;

import me.yokeyword.fragmentation.SupportActivity;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends SupportActivity implements EasyPermissions.PermissionCallbacks{
    private static final int CAMERA_REQUEST_CODE = 0xff00;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        String[] perms = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (!EasyPermissions.hasPermissions(this, perms)) {
            EasyPermissions.requestPermissions(this, "需要摄像头、录音、读写SD卡权限",
                    CAMERA_REQUEST_CODE, perms);
        }
        if(findFragment(ParamFragment.class) == null){
            loadRootFragment(R.id.fragment,ParamFragment.newInstance());
        }
    }

    @Override
    //调用requestPermissions()后，系统弹出权限申请的对话框，选择后回调到下面这个函数，授权结果会封装到grantResults
    //grant授权
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
//        light();
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Dialog dialog = new AlertDialog.Builder(this)
                .setTitle("错误")
                .setMessage("申请打开摄像头权限")
                .setPositiveButton("去打开", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(Settings.ACTION_APPLICATION_SETTINGS));
                    }
                })
                .setNegativeButton("取消", null)
                .create();
        dialog.show();
    }

    @Override
    public boolean onKeyDown(int keyCode,KeyEvent event){
        if(keyCode==KeyEvent.KEYCODE_BACK && getSupportFragmentManager().getBackStackEntryCount() > 1) {
            return true;//不执行父类点击事件
        }
        return super.onKeyDown(keyCode, event);//继续执行父类其他点击事件
    }
}
