package com.peergine.conference.demo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
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
import com.peergine.plugin.exter.VideoAudioInputExternal;
import com.peergine.plugin.lib.pgLibJNINode;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import me.yokeyword.fragmentation.SupportFragment;

import static com.peergine.android.conference.pgLibConference.OnEventListener;
import static com.peergine.android.conference.pgLibConference.PG_NODE_CFG;
import static com.peergine.android.conference.pgLibConference.PG_RECORD_NORMAL;
import static com.peergine.android.conference.pgLibConference.VIDEO_NORMAL;
import static com.peergine.android.conference.pgLibConference.VIDEO_ONLY_INPUT;
import static com.peergine.android.conference.pgLibConferenceEvent.*;
import static com.peergine.android.conference.pgLibError.PG_ERR_Normal;

/**
 * Updata 2017 02 15 V13
 * 添加定时器的使用示范
 * 修改VideoStart  AudioStart  VideoOpen 的使用时机示范。
 */

public class MainFragment extends SupportFragment {

    private String mSGroup = "";
    private String msChair = "";

    private String m_sUser = "";
    private String m_sPass = "";

    private String m_sSvrAddr = "connect.peergine.com:7781";
    private String m_sRelayAddr = "";
    private String m_sInitParam;
    private String m_sVideoParam =
                    "(Code){3}(Mode){2}(FrmRate){40}" +
                    "(LCode){3}(LMode){3}(LFrmRate){30}" +
                    "(Portrait){0}(Rotate){0}(BitRate){300}(CameraNo){" + Camera.CameraInfo.CAMERA_FACING_FRONT + "}" +
                    "(AudioSpeechDisable){0}";
    private String mSmemb = "";

    private pgLibConference mConf = null;
    private pgLibJNINode m_Node = null;

    private int[] ridlaout = {R.id.layoutVideoS1, R.id.layoutVideoS2, R.id.layoutVideoS3};

    private EditText mEditchair = null;

    private EditText mEdittextNotify = null;

    private TextView text_info = null;


    private LinearLayout mPreviewLayout = null;
    private SurfaceView mPreview = null;
    private Button m_BtnClearlog = null;
    private String mMode = "";
    private int REQ_MSG = 10;


    // view 放大
    private View.OnClickListener layoutOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int iWndID = v.getId();

            SurfaceView videoview = (SurfaceView) ((LinearLayout)v).getChildAt(0);

            ((LinearLayout)v).removeAllViews();

            startForResult(FullScreenFragment.newInstance(videoview,iWndID),REQ_MSG);
        }
    };
    private String sMode = "";
    private VideoAudioInputExternal external=null;
    //R.id.layoutVideoS0,

    class MEMBER {
        String sPeer = "";
        Boolean bVideoSync = false;
        Boolean bJoin = false;
        SurfaceView pView = null;
        LinearLayout pLayout = null;
    }

    private static ArrayList<MEMBER> mListMemberS = new ArrayList<>();

    //定时器例子 超时处理实现
    final pgLibTimer mTimer = new pgLibTimer();

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

    public static MainFragment newInstance(String sUser, String sPass, String sSvrAddr, String sRelayAddr,
                                           String sInitParam, String sVideoParam, String sExpire ,String sMode) {
        MainFragment fragment = new MainFragment();
        Bundle args = new Bundle();
        args.putString("User", sUser);
        args.putString("Pass", sPass);
        args.putString("SvrAddr", sSvrAddr);
        args.putString("RelayAddr", sRelayAddr);
        args.putString("InitParam", sInitParam);
        args.putString("VideoParam", sVideoParam);
        args.putString("Expire", sExpire);
        args.putString("Mode", sMode);
        fragment.setArguments(args);
        return fragment;
    }

    void initView(final View view) {
        /**
         * 4个窗口初始化
         */
        mPreviewLayout = view.findViewById(R.id.layoutVideoS0);
        mPreviewLayout.setOnClickListener(layoutOnClick);
        for (int aRIDLaout : ridlaout) {
            MEMBER oMemb = new MEMBER();
            oMemb.pLayout = view.findViewById(aRIDLaout);
            oMemb.pLayout.setOnClickListener(layoutOnClick);
            mListMemberS.add(oMemb);
        }


        /**
         * 初始化控件
         */


        mEditchair = (EditText) view.findViewById(R.id.editText_chair);

        view.findViewById(R.id.btn_Start).setOnClickListener(mOnclink);
        view.findViewById(R.id.btn_stop).setOnClickListener(mOnclink);
        view.findViewById(R.id.btn_Clean).setOnClickListener(mOnclink);
        view.findViewById(R.id.btn_LanScan).setOnClickListener(mOnclink);

        mEdittextNotify = (EditText) view.findViewById(R.id.editText_notify);

        view.findViewById(R.id.btn_notifysend).setOnClickListener(mOnclink);
        view.findViewById(R.id.btn_msg).setOnClickListener(mOnclink);
        view.findViewById(R.id.btn_svr_request).setOnClickListener(mOnclink);

        view.findViewById(R.id.btn_recordstart).setOnClickListener(mOnclink);
        view.findViewById(R.id.btn_recordstop).setOnClickListener(mOnclink);
        view.findViewById(R.id.btn_test).setOnClickListener(mOnclink);

        view.findViewById(R.id.btn_file_put).setOnClickListener(mOnclink);
        view.findViewById(R.id.btn_file_get).setOnClickListener(mOnclink);
        view.findViewById(R.id.btn_clearlog).setOnClickListener(mOnclink);
        //显示一些信息
        text_info = (TextView) view.findViewById(R.id.text_info);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        mListMemberS.clear();
        initView(view);

        createTestDir();

        Bundle args = getArguments();
        m_sUser = args.getString("User");
        m_sPass = args.getString("User");
        m_sSvrAddr = args.getString("SvrAddr");
        m_sRelayAddr = args.getString("RelayAddr");
        m_sInitParam = args.getString("InitParam");
        m_sVideoParam = args.getString("VideoParam");

        int iExpire = ParseInt(args.getString("Expire"), 10);
        sMode = args.getString("Mode");
        if (mConf == null) {
            mConf = new pgLibConference();
            mConf.SetEventListener(m_OnEvent);

            mConf.SetExpire(iExpire);
            PG_NODE_CFG mNodeCfg = new PG_NODE_CFG();
            mNodeCfg.P2PTryTime = 65535;
            mConf.ConfigNode(mNodeCfg);

        }

        if("1".equals(sMode)){
            m_sVideoParam += "(VideoInExternal){1}";
        }

        if (!mConf.Initialize(m_sUser, m_sPass, m_sSvrAddr, m_sRelayAddr, m_sVideoParam, getContext())) {
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
            builder.show();
            return view;
        }

        mPreview = mConf.PreviewCreate(160, 120);
        if("1".equals(sMode)){
            int iVideoMode = ParseInt(mConf.GetNode().omlGetContent(m_sVideoParam,"Mode"),0);
            external = new VideoAudioInputExternal(mConf.GetNode(),mPreviewLayout,iVideoMode,getContext());
        	external.VideoInputExternalEnable();
        }else{
            mPreviewLayout.removeAllViews();
            mPreviewLayout.addView(mPreview);
        }

        m_Node = mConf.GetNode();
        if(!mTimer.timerInit(timerOut)){
            showInfo("定时器初始化失败！");
            pop();
        }
        return view;
    }

    private void createTestDir() {
        File file = new File("/sdcard/test");
        if(!file.exists()){
            file.mkdirs();
            showInfo("创建 测试文件夹成功 /sdcard/test");
        }else{
            showInfo("测试文件夹已经存在 /sdcard/test");
        }
    }

    @Override
    public void onDestroyView() {
        if("1".equals(sMode)) {
            external.VideoInputExternalDisable();
        }
        pgStop();
        mConf.Clean();
        super.onDestroyView();

    }


    @Override
    public boolean onBackPressedSupport() {
        // 对于 4个类别的主Fragment内的回退back逻辑,已经在其onBackPressedSupport里各自处理了
        return true;
    }


    @Override
    public void onFragmentResult(int requestCode, int resultCode, Bundle data) {
        super.onFragmentResult(requestCode, resultCode, data);
        if (requestCode == REQ_MSG && resultCode == RESULT_OK) {

            int iWndID = data.getInt("WndID");
            LinearLayout linearLayout = this.getView().findViewById(iWndID);

            if(R.id.layoutVideoS0 == iWndID){
                if("1".equals(sMode)){
                    external.VideoInputExternalDisable();
                    external.VideoInputExternalEnable();
                }else{
                    linearLayout.addView(mPreview);
                }

            }
            else {
                for (MEMBER oMemb : mListMemberS) {
                    if (oMemb.pLayout.equals(linearLayout)) {
                        if (oMemb.pView!=null){
                            linearLayout.addView(oMemb.pView);
                        }
                    }
                }
            }
        }
    }

    private DialogInterface.OnClickListener m_DlgClick = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == AlertDialog.BUTTON_POSITIVE) {
                pgStop();
                mConf.Clean();
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        }
    };

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

    public String getSDCardDir() {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState()
                .equals(Environment.MEDIA_MOUNTED);  //判断sd卡是否存在
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();//获取跟目录
        }

        return (sdDir == null) ? "" : sdDir.toString();

    }

    private Toast toast = null;
    private void showInfo(String s) {
        if(toast == null){
            toast = Toast.makeText(getContext(),s,Toast.LENGTH_SHORT);
        }else {
            toast.setText(s);
        }
        toast.show();
    }

    //视频传输的状态
    private void EventVideoFrameStat(String sAct, String sData, String sPeer) {
        //Show()

    }

    //服务端下发的通知
    private void EventSvrNotify(String sAct, String sData, String sPeer) {
        showInfo("SvrNotify :" + sData + " : " + sPeer);
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
        // TODO: 2016/11/7 提醒应用程序可以和此节点相互发送消息了
        showInfo(sPeer + "节点建立连接");
        mConf.MessageSend("MessageSend test", sPeer);
        mConf.CallSend("CallSend test", sPeer, "123");

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
        mConf.Join();

        mConf.MessageSend("MessageSend test", sPeer);
        mConf.CallSend("CallSend test", sPeer, "123");
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
        mConf.MemberAdd(sPeer);
    }

    //sPeer的离线消息
    private boolean m_bVideoStart = false;

    private void EventJoin(String sAct, String sData, String sPeer) {
        // TODO: 2016/11/7 这里可以获取所有会议成员  可以尝试把sPeer加入会议成员表中
        showInfo(sPeer + "加入会议");
        /*这个是开始一个定时器*/
        String sParam = "(Act){VIDEO_OPEN}(Peer){" + sPeer + "}";
        mTimer.timerStart(sParam, 1);

        mConf.NotifySend(sPeer + " : join ");
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
        public void event(String sAct, String sData, final String sPeer) {
            // TODO Auto-generated method stub

            String sObjPeer = sPeer;
            if(sObjPeer.contains("_DEV_")){
                sObjPeer = sPeer.substring(5);
            }

            //
            if (sAct.equals(EVENT_VIDEO_FRAME_STAT)) {
                EventVideoFrameStat(sAct, sData, sObjPeer);
            } else if (sAct.equals(EVENT_LOGIN)) {
                EventLogin(sAct, sData, sObjPeer);
            } else if (sAct.equals(EVENT_LOGOUT)) {
                EventLogout(sAct, sData, sObjPeer);
            } else if (sAct.equals(EVENT_PEER_OFFLINE)) {
                EventPeerOffline(sAct, sData, sObjPeer);
            } else if (sAct.equals(EVENT_PEER_SYNC)) {
                EventPeerSync(sAct, sData, sObjPeer);
            } else if (sAct.equals(EVENT_CHAIRMAN_OFFLINE)) {
                EventChairmanOffline(sAct, sData, sObjPeer);
            } else if (sAct.equals(EVENT_CHAIRMAN_SYNC)) {
                EventChairmanSync(sAct, sData, sObjPeer);
            } else if (sAct.equals(EVENT_ASK_JOIN)) {
                EventAskJoin(sAct, sData, sObjPeer);
            } else if (sAct.equals(EVENT_JOIN)) {
                EventJoin(sAct, sData, sObjPeer);
            } else if (sAct.equals(EVENT_LEAVE)) {
                EventLeave(sAct, sData, sObjPeer);
            } else if (sAct.equals(EVENT_VIDEO_SYNC)) {
                EventVideoSync(sAct, sData, sObjPeer);
            } else if (sAct.equals(EVENT_VIDEO_SYNC_1)) {
                EventVideoSyncL(sAct, sData, sObjPeer);
            } else if (sAct.equals(EVENT_VIDEO_OPEN)) {
                EventVideoOpen(sAct, sData, sObjPeer);
            } else if (sAct.equals(EVENT_VIDEO_LOST)) {
                EventVideoLost(sAct, sData, sObjPeer);
            } else if (sAct.equals(EVENT_VIDEO_CLOSE)) {
                EventVideoClose(sAct, sData, sObjPeer);
            } else if (sAct.equals(EVENT_VIDEO_JOIN)) {
                EventVideoJoin(sAct, sData, sObjPeer);
            } else if (sAct.equals(EVENT_VIDEO_CAMERA)) {
                EventVideoCamera(sAct, sData, sObjPeer);
            } else if (sAct.equals(EVENT_VIDEO_RECORD)) {
                EventVideoRecord(sAct, sData, sObjPeer);
            } else if (sAct.equals(EVENT_MESSAGE)) {
                EventMessage(sAct, sData, sObjPeer);
            } else if (sAct.equals(EVENT_CALLSEND_RESULT)) {
                EventCallSend(sAct, sData, sObjPeer);
            } else if (sAct.equals(EVENT_NOTIFY)) {
                EventNotify(sAct, sData, sObjPeer);
            } else if (sAct.equals(EVENT_SVR_NOTIFY)) {
                EventSvrNotify(sAct, sData, sObjPeer);
            } else if (sAct.equals(EVENT_SVR_RELAY)) {
                EventSvrReply(sAct, sData, sObjPeer);
            } else if (sAct.equals(EVENT_SVR_REPLYR_ERROR)) {
                EventSvrReplyError(sAct, sData, sObjPeer);
            } else if (sAct.equals(EVENT_LAN_SCAN_RESULT)) {
                EventLanScanResult(sAct, sData, sObjPeer);
            } else if (sAct.equals(EVENT_FILE_ACCEPT)) {
                EventFileAccept(sAct, sData, sObjPeer);
            } else if (sAct.equals(EVENT_FILE_REJECT)) {
                EventFileReject(sAct, sData, sObjPeer);
            } else if (sAct.equals(EVENT_FILE_ABORT)) {
                EventFileAbrot(sAct, sData, sObjPeer);
            } else if (sAct.equals(EVENT_FILE_FINISH)) {
                EventFileFinish(sAct, sData, sObjPeer);
            } else if (sAct.equals(EVENT_FILE_PROGRESS)) {
                EventFileProgress(sAct, sData, sObjPeer);
            }else if (sAct.equals(EVENT_FILE_PUT_REQUEST)) {
                EventFilePutRequest(sAct, sData, sObjPeer);
            }else if (sAct.equals(EVENT_FILE_GET_REQUEST)) {
                EventFileGetRequest(sAct, sData, sObjPeer);
            }else {
                showInfo("MainFragment.OnEvent: Act=" + sAct + ", Data=" + sData + ", Peer=" + sObjPeer);
            }
        }
    };

    private void EventFileGetRequest(String sAct, String sData, String sObjPeer) {
        showInfo("MainFragment.OnEvent: Act=" + sAct + ", Data=" + sData + ", Peer=" + sObjPeer);
        mConf.FileAccept(msChair,sObjPeer,"/sdcard/test/test.avi" );
    }

    private void EventFilePutRequest(String sAct, String sData, String sObjPeer) {
        showInfo("MainFragment.OnEvent: Act=" + sAct + ", Data=" + sData + ", Peer=" + sObjPeer);
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String sDate = formatter.format(currentTime);
        //sData = "peerpath=xxxxxxxxx" so xxxxxxx = sData.substring(9)
        mConf.FileAccept(msChair,sObjPeer,"/sdcard/test/GetFile_" + sDate + sData.substring(9) );
    }

    private void EventFileProgress(String sAct, String sData, String sObjPeer) {
        showInfo("MainFragment.OnEvent: Act=" + sAct + ", Data=" + sData + ", Peer=" + sObjPeer);
    }

    private void EventFileFinish(String sAct, String sData, String sObjPeer) {
        showInfo("MainFragment.OnEvent: Act=" + sAct + ", Data=" + sData + ", Peer=" + sObjPeer);
    }

    private void EventFileAbrot(String sAct, String sData, String sObjPeer) {
        showInfo("MainFragment.OnEvent: Act=" + sAct + ", Data=" + sData + ", Peer=" + sObjPeer);
    }

    private void EventFileReject(String sAct, String sData, String sObjPeer) {
        showInfo("MainFragment.OnEvent: Act=" + sAct + ", Data=" + sData + ", Peer=" + sObjPeer);
    }

    private void EventFileAccept(String sAct, String sData, String sObjPeer) {
        showInfo("MainFragment.OnEvent: Act=" + sAct + ", Data=" + sData + ", Peer=" + sObjPeer);
    }

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

    private void pgStart() {
        msChair = mEditchair.getText().toString().trim();
        if ("".equals(msChair)) {
            Alert("错误", "主席端ID不能为空。");
        }
        String sName = msChair;
        mConf.Start(sName, msChair);
        int iVideoFlag = VIDEO_NORMAL;
//        if(msChair.equals(m_sUser)){
//            iVideoFlag = VIDEO_ONLY_INPUT;
//        }

        mConf.VideoStart(iVideoFlag);
        mConf.AudioStart();
    }

    private void pgStop() {

        for (int i = 0; i < mListMemberS.size(); i++) {
            MEMBER oMemb = mListMemberS.get(i);
            if (!"".equals(oMemb.sPeer)) {
                pgVideoClose(oMemb.sPeer);
            }
        }
        mConf.AudioStop();
        mConf.VideoStop();
        mConf.Stop();

    }


    //结束会议模块
    private void pgClean() {
        pgStop();
        m_Node = null;

        mConf.Clean();
    }

    //搜索成员列表
    MEMBER memberSearch(String sPeer) {

        try {
            if ("".equals(sPeer)) {
                Log.d("", "Search can't Search Start");
                return null;
            }
            for (int i = 0; i < mListMemberS.size(); i++) {

                if (mListMemberS.get(i).sPeer.equals(sPeer) || "".equals(mListMemberS.get(i).sPeer)) {
                    return mListMemberS.get(i);
                }
            }

        } catch (Exception ex) {
            Log.d("", "VideoOption. ex=" + ex.toString());
        }
        return null;
    }

    /**
     * 打开视频时完成窗口和相关数据的改变
     *
     * @param sPeer 对象ID
     * @return
     */
    private boolean pgVideoOpen(String sPeer) {

        MEMBER MembTmp = memberSearch(sPeer);
        //没有窗口了
        if (MembTmp == null) {
            mConf.VideoReject(sPeer);
            return false;
        }
        if ("".equals(MembTmp.sPeer)) {
            MembTmp.sPeer = sPeer;
        }

        MembTmp.pView = mConf.VideoOpen(sPeer, 160, 120);
        if (MembTmp.pView != null) {
            MembTmp.pLayout.removeAllViews();
            MembTmp.pLayout.addView(MembTmp.pView);
        }

        return true;

    }

    /**
     * 重置节点的显示窗口
     *
     * @param sPeer 重置节点
     */
    private void pgVideoRestore(String sPeer) {
        MEMBER membTmp = memberSearch(sPeer);
        if (membTmp != null) {
            if (membTmp.sPeer.equals(sPeer)) {
                membTmp.pLayout.removeView(membTmp.pView);
                membTmp.pView = null;
                membTmp.sPeer = "";
                membTmp.bJoin = false;
                membTmp.bVideoSync = false;
            }
        }
    }

    //清理窗口数据和关闭视频
    private void pgVideoClose(String sPeer) {
        mConf.VideoClose(sPeer);
        pgVideoRestore(sPeer);
    }

    private void pgRecordStart(){
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String sDate = formatter.format(currentTime);
        String sPath = getSDCardDir() + "/test/record" + sDate + ".avi";
        boolean iErr = mConf.RecordStart(msChair, sPath);
        if ((iErr = false)) {
            Toast.makeText(getContext(), "录像失败。 已经关闭", Toast.LENGTH_SHORT).show();
            mConf.RecordStop(msChair,PG_RECORD_NORMAL);

        }
    }

    private void pgRecordStop(){
        mConf.RecordStop(msChair);
    }

    //给所有加入会议的成员发送消息
    private boolean pgNotifySend() {
        String sData = mEdittextNotify.getText().toString();
        if ("".equals(sData)) {
            return false;
        }
        mConf.NotifySend(sData);

        return true;
    }

    private boolean m_bSpeechEnable = true;

    /**
     * 选择自己的声音是否在对端播放
     */
    private void Speech() {
        if (!mConf.AudioSpeech(mSmemb, m_bSpeechEnable)) {
            Log.d("pgRobotClient", "Enable speech failed");
        } else {
            m_bSpeechEnable = !m_bSpeechEnable;
        }
    }


    public void SetCameraRate(int iCameraRate) {
        pgLibJNINode Node = mConf.GetNode();
        if (Node != null) {
            if (Node.ObjectAdd("_aTemp", "PG_CLASS_Video", "", 0)) {

                String sData = "(Item){4}(Value){"+ iCameraRate +"}";

                Node.ObjectRequest("_aTemp", 2, sData, "");
                Node.ObjectDelete("_aTemp");
            }
        }
    }


    private final View.OnClickListener mOnclink = new View.OnClickListener() {
        @Override
        public void onClick(View args0) {
            int k = 0;
            switch (args0.getId()) {
                case R.id.btn_Start:
                    m_bVideoStart = false;
                    pgStart();
                    Log.d("OnClink", "init button");
                    break;
                case R.id.btn_stop:
                    pgStop();
                    Log.d("OnClink", "MemberAdd button");
                    break;
                case R.id.btn_Clean:
                    pgClean();
                    if(getFragmentManager().getBackStackEntryCount() > 1){
                        pop();
                    }

                    Log.d("OnClink", "MemberAdd button");
                    break;
                case R.id.btn_LanScan:
                    mConf.LanScanStart();
                    break;
                case R.id.btn_notifysend: {
                    String sMsg = mEdittextNotify.getText().toString().trim();
                    mConf.NotifySend(sMsg);
                    break;
                }
                case R.id.btn_msg: {
                    String sMsg = mEdittextNotify.getText().toString().trim();
                    mConf.MessageSend(sMsg, msChair);
                    break;

                } case R.id.btn_svr_request: {
                    String sMsg = mEdittextNotify.getText().toString().trim();
                    mConf.SvrRequest(sMsg);
                    break;

                }
                case R.id.btn_recordstart: {
                    pgRecordStart();
                    break;
                }
                case R.id.btn_recordstop: {
                    pgRecordStop();
                    break;
                }
                case R.id.btn_test: {
                    test();
                    break;

                }case R.id.btn_file_put: {

                    mConf.FilePutRequest(msChair,m_sUser,"/sdcard/test/test.avi","");

                    break;

                }case R.id.btn_file_get: {
                    Date currentTime = new Date();
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
                    String sDate = formatter.format(currentTime);
                    mConf.FileGetRequest(msChair,m_sUser,"/sdcard/test/GetFile_" + sDate + ".avi","");
                    break;

                }
                case R.id.btn_clearlog: {
                    text_info.setText("");
                }
                default:

                    break;
            }
        }
    };

    private void test() {

    }
}

