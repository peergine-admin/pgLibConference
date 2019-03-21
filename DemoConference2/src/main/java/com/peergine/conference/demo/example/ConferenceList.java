package com.peergine.conference.demo.example;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;

import com.peergine.android.conference.pgLibConference2;
import com.peergine.android.conference.pgLibTimer;
import com.peergine.plugin.exter.VideoAudioInputExternal;
import com.peergine.plugin.lib.pgLibJNINode;

public class ConferenceList {

    private pgLibConference2 m_Conf2 = null;
    private pgLibJNINode m_Node = null;
    private VideoAudioInputExternal external=null;

    //定时器例子 超时处理实现
    final pgLibTimer mTimer = new pgLibTimer();

    private String _ObjPeerBuild(String sPeer) {
        if (sPeer.indexOf("_DEV_") != 0) {
            return "_DEV_" + sPeer;
        }
        return sPeer;
    }

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
                String sObjUser = _ObjPeerBuild(m_sUser);
                if (sObjUser.compareTo(sPeer) > 0) {
                    showInfo(" 发起视频请求");
                    pgVideoOpen(sPeer);
                }
            }
        }
    };

    private void TimerStartOpen(String sPeer) {
        String sParam = "(Act){VIDEO_OPEN}(Peer){" + sPeer + "}";
        mTimer.timerStart(sParam, 1,false);
    }

    void OnEventConference(String sAct, String sData, String sPeer , String sConfName,String sEventParam){

    }


    public int Initliaze(){

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
        if(mConf.MessageSend("MessageSend test", sPeer) == false){
            showInfo("MessageSend return false");
        }
        if(mConf.CallSend("CallSend test", sPeer, "123")==false){
            showInfo("CallSend return false");
        }

    }

    //sPeer的离线消息
    private void EventPeerOffline(String sAct, String sData, String sPeer) {
        // TODO: 2016/11/7 提醒应用程序此节点离线了
        showInfo(sPeer + "节点离线 sData = " + sData);
        pgVideoClose(sPeer);
    }

    //sPeer的离线消息
    private void EventChairmanSync(String sAct, String sData, String sPeer) {
        // TODO: 2016/11/7 提醒应用程序可以和主席发送消息了
        showInfo("主席节点建立连接 Act = " + sAct + " : " + sData + " : " + sPeer);
        if(mConf.Join() == false){
            showInfo("Join return false ");
        }

        mConf.MessageSend("MessageSend test", sPeer);
        mConf.CallSend("CallSend test", sPeer, "123");
    }

    //sPeer的离线消息
    private void EventChairmanOffline(String sAct, String sData, String sPeer) {
        showInfo("主席节点离线 sData = " + sPeer);
        pgVideoClose(sPeer);
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
        /*这个是开始一个定时器*/
        TimerStartOpen(sPeer);
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
        pgVideoClose(sPeer);
        TimerStartOpen(sPeer);
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
            showInfo(sPeer + ":" + "视频打开失败 iErr = " + sData);
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
            showInfo("主席端ID不能为空。");
            return;
        }

        SqlParser.wirteSql(msChair,getActivity().getApplicationContext());

        String sName = msChair;
        if(mConf.Start(sName, msChair) == false){
            showInfo("Start 失败。");
        }
        int iVideoFlag = VIDEO_NORMAL;
//        if(msChair.equals(m_sUser)){
//            iVideoFlag = VIDEO_ONLY_INPUT;
//        }

        if(mConf.VideoStart(iVideoFlag) == false){
            Alert("错误", "VideoStart 失败。");
        }
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



}
