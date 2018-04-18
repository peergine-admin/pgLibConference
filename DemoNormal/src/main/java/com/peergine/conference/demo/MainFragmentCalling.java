package com.peergine.conference.demo;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.peergine.android.conference.pgLibConference;
import com.peergine.android.conference.pgLibTimer;
import com.peergine.plugin.lib.pgLibJNINode;

import java.util.ArrayList;
import java.util.List;

import me.yokeyword.fragmentation.SupportFragment;

import static com.peergine.android.conference.pgLibConference.OnEventListener;
import static com.peergine.android.conference.pgLibConference.PG_NODE_CFG;
import static com.peergine.android.conference.pgLibConference.VIDEO_NORMAL;
import static com.peergine.android.conference.pgLibConferenceEvent.*;

/**
 * Updata 2017 02 15 V13
 * 添加定时器的使用示范
 * 修改VideoStart  AudioStart  VideoOpen 的使用时机示范。
 */

public class MainFragmentCalling extends SupportFragment {

    private String m_sGroup = "";
    private String m_sChair = "";

    private String m_sUser = "";
    private String m_sPass = "";

    private String m_sSvrAddr = "connect.peergine.com:7781";
    private String m_sRelayAddr = "";
    private String m_sVideoParam =
            "(Code){3}(Mode){2}(FrmRate){40}" +
                    "(LCode){3}(LMode){3}(LFrmRate){30}" +
                    "(Portrait){0}(Rotate){0}(BitRate){300}(CameraNo){" + Camera.CameraInfo.CAMERA_FACING_FRONT + "}" +
                    "(AudioSpeechDisable){0}";
    private String m_sMemb = "";

    private pgLibConference m_Conf = null;
    private pgLibJNINode m_Node = null;

    private int RIDLaout[] = {R.id.CallinglayoutVideoS0};

    private EditText m_edit_tag = null;
    private Button m_btnCall = null;

    private LinearLayout PreviewLayout = null;
    private SurfaceView m_Preview = null;
    private boolean m_bInited = false;
    private String m_sInitParam;

    //R.id.layoutVideoS0,

    class PG_MEMB {
        String sPeer = "";
        Boolean bVideoSync = false;
        Boolean bJoin = false;
        SurfaceView pView = null;
        LinearLayout pLayout = null;
    }

    private static ArrayList<PG_MEMB> memberArray = new ArrayList<>();


    //定时器例子 超时处理实现
    final pgLibTimer timer = new pgLibTimer();

    final pgLibTimer.OnTimeOut timerOut = new pgLibTimer.OnTimeOut() {
        @Override
        public void onTimeOut(String sParam) {
            if (m_Node == null) {
                return;
            }
            //中间件oml 格式数据解析示例
            String sAct = m_Node.omlGetContent(sParam, "Act");
            String sPeer = m_Node.omlGetContent(sParam, "Peer");

            //执行打开视频的动作
            if ("VIDEO_OPEN".equals(sAct)) {

                //Demo 是为了演示方便 在这里实现自动打开视频的功能
                //所以才做了这个ID大的主动打开视频
                //实际情况中建议从Join出得到设备列表，或者本地保存列表，用ListView显示，点击某个ID然后开始打开视频

                if (m_sUser.compareTo(sPeer) > 0) {
                    showInfo(" 发起视频请求");
                    pgVideoOpen(sPeer);
                }
            }
        }
    };

    public static MainFragmentCalling newInstance(String sUser, String sPass, String sSvrAddr, String sRelayAddr,
                                                  String sInitParam, String sVideoParam, String sExpire) {
        MainFragmentCalling fragment = new MainFragmentCalling();
        Bundle args = new Bundle();
        args.putString("User", sUser);
        args.putString("Pass", sPass);
        args.putString("SvrAddr", sSvrAddr);
        args.putString("RelayAddr", sRelayAddr);
        args.putString("InitParam", sInitParam);
        args.putString("VideoParam", sVideoParam);
        args.putString("Expire", sExpire);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for getContext() fragment
        View view = inflater.inflate(R.layout.fragment_main_calling, container, false);
        memberArray.clear();
        initView(view);

        Bundle args = getArguments();
        m_sUser = args.getString("User");
        m_sPass = args.getString("User");
        m_sSvrAddr = args.getString("SvrAddr");
        m_sRelayAddr = args.getString("RelayAddr");
        m_sInitParam = args.getString("InitParam");
        m_sVideoParam = args.getString("VideoParam");

        int iExpire = ParseInt(args.getString("Expire"), 10);


        if (m_Conf == null) {
            m_Conf = new pgLibConference();
            m_Conf.SetEventListener(m_OnEvent);

            m_Conf.SetExpire(iExpire);
            PG_NODE_CFG mNodeCfg = new PG_NODE_CFG();

            m_Conf.ConfigNode(mNodeCfg);

        }
        if (!m_Conf.Initialize(m_sUser, m_sPass, m_sSvrAddr, m_sRelayAddr, m_sVideoParam, getContext())) {
            Log.d("pgConference", "Init failed");
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Error");
            builder.setMessage("请安装pgPlugin xx.APK 或者检查网络状况!");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    pop();
                }
            });
            return view;
        }
        m_bInited = true;

        m_Preview = m_Conf.PreviewCreate(160, 120);
        PreviewLayout.removeAllViews();
        PreviewLayout.addView(m_Preview);

        //初始化定时器
        m_Node = m_Conf.GetNode();
        if (!timer.timerInit(timerOut)) {
            showInfo("定时器初始化失败！");
            pop();
        }
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        pgStop();
        m_Conf.Clean();
    }

    private void initView(View view) {
        //todo 4个窗口初始化
        PreviewLayout = (LinearLayout) view.findViewById(R.id.CallinglayoutVideoS1);
        for (int aRIDLaout : RIDLaout) {
            PG_MEMB oMemb = new PG_MEMB();
            oMemb.pLayout = (LinearLayout) view.findViewById(aRIDLaout);
            memberArray.add(oMemb);
        }

        //初始化控件

        m_edit_tag = (EditText) view.findViewById(R.id.editText_tag);

        view.findViewById(R.id.btn_Call).setOnClickListener(m_OnClink);
        view.findViewById(R.id.btn_handup).setOnClickListener(m_OnClink);
        view.findViewById(R.id.btn_clean).setOnClickListener(m_OnClink);

    }


    private Toast toast = null;

    private void showInfo(String s) {
        if (toast == null) {
            toast = Toast.makeText(getContext(), s, Toast.LENGTH_SHORT);
        } else {
            toast.setText(s);
        }
        toast.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void Alert(String sTitle, String sMsg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(sTitle);
        builder.setMessage(sMsg);
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private AlertDialog m_dialogCall = null;
    private String m_TagID = "";
    private DialogInterface.OnClickListener m_DlgCall = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == AlertDialog.BUTTON_POSITIVE) {
                pgStart(m_TagID);
                SvrPush(m_TagID, "accept");
            } else if (which == AlertDialog.BUTTON_NEGATIVE) {
                SvrPush(m_TagID, "reject");
            }
            m_dialogCall = null;
        }
    };

    private void CallDialogShow(String tagID) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Video calling ...");
        builder.setMessage("The device '" + tagID + "' is calling you");
        m_TagID = tagID;
        builder.setPositiveButton("Accept", m_DlgCall);
        builder.setNegativeButton("Reject", m_DlgCall);
        m_dialogCall = builder.show();
    }

    private void CallDialogClose() {
        if (m_dialogCall != null) {
            m_dialogCall.dismiss();
            m_dialogCall = null;
        }
    }

    private boolean isForeground(String className) {
        ActivityManager am = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = am.getRunningTasks(1);
        if (list != null && list.size() > 0) {
            ComponentName cpn = list.get(0).topActivity;
            if (className.equals(cpn.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private View.OnClickListener m_OnClink = new View.OnClickListener() {
        // Control clicked
        @Override
        public void onClick(View args0) {
            int k = 0;
            String tagID = m_edit_tag.getText().toString();
            if ("".equals(tagID)) {
                if(args0.getId() == R.id.btn_clean){
                    pgClean();
                    return;
                }
                Alert("error", "please input tag ID!");
                return;
            }
            switch (args0.getId()) {
                case R.id.btn_Call:
                    pgStop();

                    pgStart(m_sUser);
                    SvrPush(tagID, "calling");
                    //m_btnCall.setEnabled(false);
                    Log.d("OnClink", "init button");
                    break;
                case R.id.btn_handup:

                    pgStop();
                    SvrPush(tagID, "handup");
                    Log.d("OnClink", "MemberAdd button");
                    break;
                case R.id.btn_clean:
                    SvrPush(tagID, "handup");
                    pgClean();
                    Log.d("OnClink", "MemberAdd button");
                    break;
                default:

                    break;
            }
        }
    };


    //视频传输的状态
    private void EventVideoFrameStat(String sAct, String sData, String sPeer) {
        //Show()

    }

    //服务端下发的通知
    private void EventSvrNotify(String sAct, String sData, String sPeer) {
        showInfo("SvrNotify :" + sData + " : " + sPeer);
        pgLibJNINode Node = m_Conf.GetNode();
        if (Node != null) {
            String sDevID = Node.omlGetContent(sData, "User");
            String sMsg = Node.omlGetContent(sData, "Msg");

            if ("calling".equals(sMsg)) {
                pgStop();

                m_edit_tag.setText(sDevID);
//				m_sDevID = sDevID;

                try {
                    // Show main activity to foreground.
                    Intent start = new Intent(getContext(), MainFragment.class);
                    start.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(start);
                } catch (Exception ex) {
                }

                // Show call dialog.
                CallDialogShow(sDevID);

                SvrPush(sDevID, "alerting");
            } else if ("cancel".equals(sMsg)) {
                CallDialogClose();
                pgStop();
            } else if ("handup".equals(sMsg)) {
                pgStop();
            }
        }
    }

    //发给服务端的消息的回执
    private void EventSvrReply(String sAct, String sData, String sPeer) {

    }

    //发给服务端的消息的回执
    private void EventSvrReplyError(String sAct, String sData, String sPeer) {

    }


    //登录的结果
    private void EventLogin(String sAct, String sData, String sPeer) {
        // Login reply
        // TODO: 2016/11/7 登录成功与否关系到后面的步骤能否执行 ，如果登录失败请再次初始化
        if ("0".equals(sData)) {
            showInfo("已经登录");
            Log.d("", "已经登录");
        } else {
            showInfo("登录失败 err = " + sData);
            Log.d("", "登录失败");
        }
    }

    //登出
    private void EventLogout(String sAct, String sData, String sPeer) {
        showInfo("已经注销" + sData);
    }

    //sPeer的离线消息
    private void EventPeerSync(String sAct, String sData, String sPeer) {
        // TODO: 2016/11/7 提醒应用程序可以和此节点相互发 送消息了
        showInfo(sPeer + "节点建立连接");
        m_Conf.MessageSend("MessageSend test", sPeer);
        m_Conf.CallSend("CallSend test", sPeer, "123");

    }

    //sPeer的离线消息
    private void EventPeerOffline(String sAct, String sData, String sPeer) {
        // TODO: 2016/11/7 提醒应用程序此节点离线了
        showInfo(sPeer + "节点离线 sData = " + sData);
    }

    //sPeer的离线消息
    private void EventChairmanSync(String sAct, String sData, String sPeer) {
        // TODO: 2016/11/7 提醒应用程序可以和主席发送消息了
        showInfo("主席节点建立连接 Act = " + sAct + " : " + sData + " : " + sPeer);
        m_Conf.Join();
        m_Conf.MessageSend("MessageSend test", sPeer);
        m_Conf.CallSend("CallSend test", sPeer, "123");
    }

    //sPeer的离线消息
    private void EventChairmanOffline(String sAct, String sData, String sPeer) {
        showInfo("主席节点离线 sData = " + sPeer);
    }
//-------------------------------------------------------------------------
    //sPeer的离线消息

    private void EventAskJoin(String sAct, String sData, String sPeer) {
        // TODO: 2016/11/7 sPeer请求加入会议  MemberAdd表示把他加入会议
        showInfo(sPeer + "请求加入会议->同意");
        m_Conf.MemberAdd(sPeer);
    }

    //sPeer的离线消息
    private void EventJoin(String sAct, String sData, String sPeer) {
        // TODO: 2016/11/7 这里可以获取所有会议成员  可以尝试把sPeer加入会议成员表中
        showInfo(sPeer + "加入会议");
        String sParam = "(Act){VIDEO_OPEN}(Peer){" + sPeer + "}";
        timer.timerStart(sParam, 1);

        m_Conf.NotifySend(sPeer + " : join ");
        Log.d("", sPeer + " 加入会议");
    }

    //sPeer的离线消息
    private void EventLeave(String sAct, String sData, String sPeer) {
        showInfo(sPeer + "离开会议");

        Log.d("", " 离开会议");
    }
//---------------------------------------------------------------------

    //sPeer的离线消息
    private void EventVideoSync(String sAct, String sData, String sPeer) {
        // TODO: 2016/11/7 提醒应用程序可以打开这个sPeer的视频了
        showInfo("视频同步");
    }

    //sPeer的离线消息
    private void EventVideoSyncL(String sAct, String sData, String sPeer) {
        // TODO: 2016/11/7 提醒应用可以打开这个sPeer的第二种流
        Log.d("", " 第二种视频同步");
        showInfo("第二种视频同步");
    }

    private void EventVideoOpen(String sAct, String sData, String sPeer) {
        //收到视频请求
        showInfo(sPeer + " 请求视频连线->同意");
        //// TODO: 2016/11/7 在这之后回复
        //调用
        pgVideoOpen(sPeer);
    }

    private void EventVideoLost(String sAct, String sData, final String sPeer) {
        // TODO: 2016/11/8  对方视频已经丢失 挂断对方视频 并尝试重新打开
        showInfo(sPeer + " 的视频已经丢失 可以尝试重新连接");
    }


    private void EventVideoClose(String sAct, String sData, String sPeer) {
        // TODO: 2016/11/8  通知应用程序视频已经挂断
        showInfo(sPeer + " 已经挂断视频");
        pgVideoRestore(sPeer);

    }

    private void EventVideoJoin(String sAct, String sData, String sPeer) {
        // TODO: 2016/11/8 请求端会收到请求打开视频的结果，打开视频成功除了显示和播放视频外，还有这个事件
        if ("0".equals(sData)) {
            showInfo(sPeer + ":" + "视频成功打开");
            Log.d("", sPeer + " 成功打开");
        } else {
            showInfo(sPeer + ":" + "视频成功失败");
            Log.d("", sPeer + " 打开失败");
            pgVideoRestore(sPeer);
        }
    }

    //-------------------------------------------------------------------
    //组消息
    private void EventNotify(String sAct, String sData, String sPeer) {
        showInfo(sPeer + ": sData = " + sData);
    }

    //sPeer的消息处理
    private void EventMessage(String sAct, String sData, String sPeer) {
        // TODO: 2016/11/7 处理sPeer发送过来的消息
        showInfo(sPeer + ":" + sData);
        Log.d("", sPeer + ":" + sData);
    }

    private OnEventListener m_OnEvent = new OnEventListener() {

        @Override
        public void event(String sAct, String sData, final String sObjPeer) {
            // TODO Auto-generated method stub

            String sPeer = sObjPeer;
            if (sObjPeer.contains("_DEV_")) {
                sPeer = sObjPeer.substring(5);
            }

            Log.d("ConferenceDemo", "OnEvent: Act=" + sAct + ", Data=" + sData + ", Peer=" + sPeer);
            if (sAct.equals(EVENT_VIDEO_FRAME_STAT)) {
                EventVideoFrameStat(sAct, sData, sPeer);
            } else if (sAct.equals(EVENT_LOGIN)) {
                EventLogin(sAct, sData, sPeer);
            } else if (sAct.equals(EVENT_LOGOUT)) {
                EventLogout(sAct, sData, sPeer);
            } else if (sAct.equals(EVENT_PEER_OFFLINE)) {
                EventPeerOffline(sAct, sData, sPeer);
            } else if (sAct.equals(EVENT_PEER_SYNC)) {
                EventPeerSync(sAct, sData, sPeer);
            } else if (sAct.equals(EVENT_CHAIRMAN_OFFLINE)) {
                EventChairmanOffline(sAct, sData, sPeer);
            } else if (sAct.equals(EVENT_CHAIRMAN_SYNC)) {
                EventChairmanSync(sAct, sData, sPeer);
            } else if (sAct.equals(EVENT_ASK_JOIN)) {
                EventAskJoin(sAct, sData, sPeer);
            } else if (sAct.equals(EVENT_JOIN)) {
                EventJoin(sAct, sData, sPeer);
            } else if (sAct.equals(EVENT_LEAVE)) {
                EventLeave(sAct, sData, sPeer);
            } else if (sAct.equals(EVENT_VIDEO_SYNC)) {
                EventVideoSync(sAct, sData, sPeer);
            } else if (sAct.equals(EVENT_VIDEO_SYNC_1)) {
                EventVideoSyncL(sAct, sData, sPeer);
            } else if (sAct.equals(EVENT_VIDEO_OPEN)) {
                EventVideoOpen(sAct, sData, sPeer);
            } else if (sAct.equals(EVENT_VIDEO_LOST)) {
                EventVideoLost(sAct, sData, sPeer);
            } else if (sAct.equals(EVENT_VIDEO_CLOSE)) {
                EventVideoClose(sAct, sData, sPeer);
            } else if (sAct.equals(EVENT_VIDEO_JOIN)) {
                EventVideoJoin(sAct, sData, sPeer);
            } else if (sAct.equals(EVENT_VIDEO_CAMERA)) {
                EventVideoCamera(sAct, sData, sPeer);
            } else if (sAct.equals(EVENT_VIDEO_RECORD)) {
                EventVideoRecord(sAct, sData, sPeer);
            } else if (sAct.equals(EVENT_MESSAGE)) {
                EventMessage(sAct, sData, sPeer);
            } else if (sAct.equals(EVENT_CALLSEND_RESULT)) {
                EventCallSend(sAct, sData, sPeer);
            } else if (sAct.equals(EVENT_NOTIFY)) {
                EventNotify(sAct, sData, sPeer);
            } else if (sAct.equals(EVENT_SVR_NOTIFY)) {
                EventSvrNotify(sAct, sData, sPeer);
            } else if (sAct.equals(EVENT_SVR_RELAY)) {
                EventSvrReply(sAct, sData, sPeer);
            } else if (sAct.equals(EVENT_SVR_REPLYR_ERROR)) {
                EventSvrReplyError(sAct, sData, sPeer);
            } else if (sAct.equals(EVENT_LAN_SCAN_RESULT)) {
                EventLanScanResult(sAct, sData, sPeer);
            }
        }
    };

    private void EventCallSend(String sAct, String sData, String sPeer) {
        // CallSend （具有回执的信息） 最终结果
        showInfo("CallSend 回执 sData = " + sData);
    }

    private void EventVideoRecord(String sAct, String sData, String sPeer) {
        // VideoRecord 视频录制的结果
    }

    private void EventVideoCamera(String sAct, String sData, String sPeer) {
        // VideoCamera 视频拍照的结果
    }

    private void EventLanScanResult(String sAct, String sData, String sPeer) {
        showInfo("Act : LanScanResult  -- sData: " + sData + "  sPeer  : " + sPeer);
    }

    private static int ParseInt(String sInt, int iDef) {
        try {
            if ("".equals(sInt)) {
                return 0;
            }
            return Integer.parseInt(sInt);
        } catch (Exception ex) {
            return iDef;
        }
    }

    boolean bStarted = false;

    private void pgStart(String sChair) {
        if (bStarted) {
            pgStop();
        }
        String sName = sChair;
        m_Conf.Start(sName, sChair);
        m_Conf.VideoStart(VIDEO_NORMAL);
        m_Conf.AudioStart();
        bStarted = true;
    }

    private void pgStop() {

        for (int i = 0; i < memberArray.size(); i++) {
            PG_MEMB oMemb = memberArray.get(i);
            if (!"".equals(oMemb.sPeer)) {
                pgVideoClose(oMemb.sPeer);
            }
        }
        m_Conf.AudioStop();
        m_Conf.VideoStop();
        m_Conf.Stop();
        bStarted = false;
    }


    //结束会议模块
    private void pgClean() {
        pgStop();
        m_Node = null;
        m_Conf.Clean();
        pop();
    }

    //搜索成员列表
    PG_MEMB MembSearch(String sPeer) {

        try {
            if ("".equals(sPeer)) {
                Log.d("", "Search can't Search Start");
                return null;
            }
            for (int i = 0; i < memberArray.size(); i++) {

                if (memberArray.get(i).sPeer.equals(sPeer) || "".equals(memberArray.get(i).sPeer)) {
                    return memberArray.get(i);
                }
            }

        } catch (Exception ex) {
            Log.d("", "VideoOption. ex=" + ex.toString());
        }
        return null;
    }

    //打开视频时完成窗口和相关数据的改变
    private boolean pgVideoOpen(String sPeer) {

        PG_MEMB MembTmp = MembSearch(sPeer);
        //没有窗口了
        if (MembTmp == null) {
            m_Conf.VideoReject(sPeer);
            return false;
        }
        if ("".equals(MembTmp.sPeer)) {
            MembTmp.sPeer = sPeer;
        }

        MembTmp.pView = m_Conf.VideoOpen(sPeer, 160, 120);
        if (MembTmp.pView != null) {
            MembTmp.pLayout.removeAllViews();
            MembTmp.pLayout.addView(MembTmp.pView);
        }

        return true;

    }

    //给服务器发送转发消息
    private void SvrPush(String tagID, String sMsg) {
        if (m_bInited) {
            m_Conf.SvrRequest("Forward?(User){" + tagID + "}(Msg){" + sMsg + "}");
        }
    }

    /*
    * 重置节点的显示窗口
    * */
    private void pgVideoRestore(String sPeer) {
        PG_MEMB MembTmp = MembSearch(sPeer);
        if (MembTmp != null) {
            if (MembTmp.sPeer.equals(sPeer)) {
                MembTmp.pLayout.removeView(MembTmp.pView);
                MembTmp.pView = null;
                MembTmp.sPeer = "";
                MembTmp.bJoin = false;
                MembTmp.bVideoSync = false;
            }
        }
    }

    //清理窗口数据和关闭视频
    private void pgVideoClose(String sPeer) {
        m_Conf.VideoClose(sPeer);
        pgVideoRestore(sPeer);
    }

}

