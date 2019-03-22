package com.peergine.conference.demo2.example;

import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Camera;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.peergine.android.conference.OnEventListener;
import com.peergine.android.conference.pgLibConference2;
import com.peergine.android.conference.pgLibTimer;
import com.peergine.android.conference.pgLibView;
import com.peergine.plugin.exter.VideoAudioInputExternal;
import com.peergine.plugin.lib.pgLibJNINode;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.peergine.android.conference.OnEventConst.*;
import static com.peergine.android.conference.OnEventConst.PG_RECORD_ONLYVIDEO_HASAUDIO;
import static com.peergine.android.conference.pgLibError.*;
import static com.peergine.android.conference.pgLibError.PG_ERR_System;

public class Conference {

    int iCode = 3;
    int iMode = 3;
    int CameraNo = 0;
    int iStreamMode = 0;

    private String m_sPrewParam = "(Code){"+ iCode +"}(Mode){" + iMode + "}(Rate){40}" +
            "(Portrait){1}(BitRate){300}(CameraNo){" + Camera.CameraInfo.CAMERA_FACING_FRONT + "}";
    private String m_sVideoParam = "(Code){3}(Mode){2}(Rate){40}";
    private String m_sVideoParamLarge ="(Code){3}(Mode){2}(Rate){40}";


    private SurfaceView mPreview = null;
    public pgLibConference2 m_Conf2 = null;
    private pgLibJNINode m_Node = null;

    private LayoutMange m_LayoutMange = null;
    private VideoAudioInputExternal external=null;

    //定时器例子 超时处理实现
    final pgLibTimer m_Timer = new pgLibTimer();
    private String m_sUser;
    private Context m_Context=null;
    private boolean m_isInputExternal = false;
    private LinearLayout mPreviewLayout= null;

    private final ConferencePeerList conferencePeerList = new ConferencePeerList();

    public static String GetSdcardDir() {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState()
                .equals(Environment.MEDIA_MOUNTED);  //判断sd卡是否存在
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();//获取跟目录
        }
        return (sdDir == null) ? "" : sdDir.toString();
    }

    private Toast toast = null;
    public void showInfo(String s) {
        if(m_Context == null){
            Log.i("Conference" ," s");
        }

        if(toast == null ){
            toast = Toast.makeText(m_Context,s,Toast.LENGTH_SHORT);
        }else {
            toast.setText(s);
        }
        toast.show();
    }

    public void showAlert(String s){
        AlertDialog.Builder builder = new AlertDialog.Builder(m_Context);
        builder.setTitle("警告：");
        builder.setMessage( s );
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.show();
    }

    public void _OutString(String s){
        Log.d("Conference Demo",s);
    }

    public int Initialize(String sUser, String sSvrAddr, Context context,boolean isInputExternal,LayoutMange layoutMange){
        m_sUser = sUser;
        m_Context = context;
        m_isInputExternal = isInputExternal;
        m_LayoutMange = layoutMange;

        if(!m_Timer.timerInit(timerOut)){
            Clean();
            return PG_ERR_System;
        }

        String sInitParam = "(P2PTryTime){3}(LogLevel0){1}(Debug)(0)";
        if(isInputExternal){
            sInitParam += "(VideoInExternal){1}";
        }

        m_Conf2 = new pgLibConference2();
        m_Conf2.SetEventListener(m_OnEvent);

        int iErr = m_Conf2.Initialize(sUser, "", sSvrAddr, "", sInitParam, context);
        if (iErr>PG_ERR_Normal) {
            Log.d("pgLibConference2", "Init failed");
            return iErr;
        }
        m_Node = m_Conf2.GetNode();

        mPreviewLayout = (LinearLayout) layoutMange.Alloc();

        if(isInputExternal){
            external = new VideoAudioInputExternal(m_Conf2.GetNode(),mPreviewLayout,iMode,context);
            external.VideoInputExternalEnable();
        }else{
            mPreview = m_Conf2.PreviewCreate();
            mPreviewLayout.removeAllViews();
            mPreviewLayout.addView(mPreview);
        }


        iErr = m_Conf2.PreviewStart(m_sPrewParam);
        if(iErr > PG_ERR_Normal){


            return iErr;
        }
        return 0;
    }

    public void Clean(){

        m_Conf2.PreviewStop();

        if(m_isInputExternal) {
            if(external != null) {
                external.VideoInputExternalDisable();
                external = null;
            }
        }

        if(mPreview != null){
            LinearLayout linearLayout = (LinearLayout) mPreview.getParent();
            if(linearLayout != null){
                linearLayout.removeAllViews();
            }
            m_Conf2.PreviewDestroy();
            mPreview = null;
        }
        if(mPreviewLayout != null){
            mPreviewLayout.removeAllViews();
            m_LayoutMange.Free(mPreviewLayout);
            mPreviewLayout = null;
        }
        m_Conf2.Clean();
        m_Node = null;
        m_Conf2 =null;
        m_Timer.timerClean();
    }

    /**
     * 创建和初始化会议音视频
     * @param sConfName 会议名称
     * @param sChair 主席ID
     * @return ErrCode
     */
    public int pgStart(String sConfName ,String sChair) {

        SqlParser.wirteSql(sChair,m_Context);
        int iErr = m_Conf2.Start(sConfName, sChair);
        if(iErr > PG_ERR_Normal){
            showInfo("创建会议失败。 iErr = " +  pgLibErr2Str(iErr)  + " sConfName = " + sConfName);
            return iErr;
        }

        iErr = m_Conf2.VideoStart(sConfName,m_sVideoParam,m_sVideoParamLarge);
        if(iErr > PG_ERR_Normal){
            showAlert("初始化视频失败： iErr = " +  pgLibErr2Str(iErr) + " sConfName = " + sConfName);
        }
        int iFlag = AUDIO_SPEECH;
        String sAudioParam = "(Flag){" + iFlag + "}";

        iErr = m_Conf2.AudioStart(sConfName,sAudioParam);
        if(iErr > PG_ERR_Normal) {
            showAlert("初始化音频失败： iErr = " +  pgLibErr2Str(iErr) + " sConfName = " + sConfName);
        }
        if(!isChairman(sChair)) {
            iErr = m_Conf2.JoinRequest(sConfName);
            if (iErr > PG_ERR_Normal) {
                showInfo("发送加入会议请求失败： iErr = " + pgLibErr2Str(iErr) + " sConfName = " + sConfName);
            }
        }
        return iErr;
    }

    private boolean isChairman(String sChair) {
        return sChair.equals(m_sUser);
    }

    public void pgStop(String sConfName ) {
        m_Conf2.AudioStop(sConfName );
        m_Conf2.VideoStop(sConfName );
        m_Conf2.Stop(sConfName );

    }

    /**
     * todo 验证Peer可不可以加入会议
     * @param sConfName
     * @param sPeer
     * @return
     */
    private boolean verifyPeer(String sConfName, String sPeer) {
        return true;
    }

//    int m_iVelue = 0;
//    private void test() {
//        boolean bReport = m_iVelue > 0;
//
//        int iErr = m_Conf2.PeerGetInfo(msChair,bReport);
//        if (iErr> PG_ERR_Normal){
//            showInfo("PeerGetInfo iErr = " + iErr);
//        }
//        m_iVelue ++;
//        if(m_iVelue > 1){
//            m_iVelue = 0;
//        }
//    }
//    private void testAudioMuteInput() {
//
//        int iErr = m_Conf2.AudioMuteInput(m_iVelue);
//        showInfo("AudioMuteInput " + m_iVelue + " , Err = " + iErr);
//        m_iVelue ++;
//        if(m_iVelue > 1){
//            m_iVelue = 0;
//        }
//    }

    /**
     * 打开视频时完成窗口和相关数据的改变
     *
     * @param sPeer 对象ID
     * @return ErrCode
     */
    private int pgVideoOpenRequest(String sConfName,String sPeer) {

        ConferencePeer peer = conferencePeerList._Add(sConfName,sPeer);
        if(peer == null){
            showAlert("申请内存失败" + sConfName + " sPeer = " + sPeer);
            return PG_ERR_System;
        }

        if(peer.pView == null){
            //从中间件内部 pgLibView 分配一个SurfaceView,只有这样创建的才有效
            peer.pView = pgLibView.Get(sConfName + sPeer);
        }
        if(peer.pLayout == null){
            //申请桌面的linear
            peer.pLayout=m_LayoutMange.Alloc();
            if(peer.pLayout == null) {
                showAlert("无法申请到LenearLayout，VideoOpenRequest ： " + sConfName + " sPeer = " + sPeer);
                return PG_ERR_System;
            }
        }

        int iErr = m_Conf2.VideoOpenRequest(sConfName,sPeer, iStreamMode,peer.pView,"");
        if (iErr > PG_ERR_Normal) {
            showInfo("失败");

            return iErr;
        }
        peer.pLayout.removeAllViews();
        peer.pLayout.addView(peer.pView);
        return iErr;
    }

    /**
     * 打开视频时完成窗口和相关数据的改变
     *
     * @param sConfName 对象ID
     * @param sPeer 对象ID
     * @return ErrCode 错误码
     */
    private int pgVideoOpenResponse(String sConfName,String sPeer){
        ConferencePeer peer = conferencePeerList._Add(sConfName,sPeer);
        if(peer == null){
            showAlert("申请内存失败" + sConfName + " sPeer = " + sPeer);
            return PG_ERR_System;
        }

        if(peer.pView == null){
            //从中间件内部 pgLibView 分配一个SurfaceView,只有这样创建的才有效
            peer.pView = pgLibView.Get(sConfName + sPeer);
        }
        if(peer.pLayout == null){
            //申请桌面的linear
            peer.pLayout=m_LayoutMange.Alloc();
            if(peer.pLayout == null) {
                showAlert("无法申请到LenearLayout，VideoOpenRequest ： " + sConfName + " sPeer = " + sPeer);
                return PG_ERR_System;
            }
        }

        int iErr = m_Conf2.VideoOpenResponse(sConfName,sPeer,PG_ERR_Normal, iStreamMode,peer.pView,"");
        if (iErr > PG_ERR_Normal) {
            showInfo("失败");

            return iErr;
        }
        peer.pLayout.removeAllViews();
        peer.pLayout.addView(peer.pView);
        return iErr;
    }

    //清理窗口数据和关闭视频
    private void pgVideoClose(String sConfName,String sPeer) {
        m_Conf2.VideoClose(sConfName,sPeer,iStreamMode);
        ConferencePeer peer = conferencePeerList._Search(sConfName,sPeer);
        pgVideoClosePeerDelete(peer);
    }
    private void pgOnVideoClose(String sConfName,String sPeer) {
        ConferencePeer peer = conferencePeerList._Search(sConfName,sPeer);
        pgVideoClosePeerDelete(peer);
    }

    private void pgVideoClosePeerDelete(ConferencePeer peer){
        if(peer == null){
//            showAlert("申请内存失败" + sConfName + " sPeer = " + sPeer);
            return;
        }
        if(peer.pLayout == null){
            peer.pLayout.removeAllViews();
            m_LayoutMange.Free(peer.pLayout);
            peer.pLayout = null;
        }
        if(peer.pView  == null){
            pgLibView.Release(peer.pView);
            peer.pView = null;
        }
        conferencePeerList._Delete(peer);
        peer = null;
    }

    private void pgVideoClean(String sConfName){
        for(int i = 0; i< conferencePeerList.m_listConferencePeer.size(); i++) {
            ConferencePeer peer = conferencePeerList.m_listConferencePeer.get(i);
            if(peer.sConfName.equals(sConfName)){
                m_Conf2.VideoClose(sConfName,peer.sPeer,iStreamMode);
                pgVideoClosePeerDelete(peer);
            }
        }
    }

    String sBothPath = "";
    private void pgRecordStartNew(String sConfName,String sPeer){
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String sDate = formatter.format(currentTime);
        sBothPath = GetSdcardDir() + "/test/record" + sDate + ".avi";
        int iErr = m_Conf2.RecordStart(sConfName,sPeer,iStreamMode,sBothPath, PG_RECORD_ONLYVIDEO_HASAUDIO);
        if(iErr!=0){
//            Toast.makeText(getContext(), "录像失败。 已经关闭 Err = " + iErr, Toast.LENGTH_SHORT).show();
        }
        //boolean iErr = m_Conf2.RecordStart(msChair, sPath);
//        if ((!iErr)) {
//            Toast.makeText(getContext(), "录像失败。 已经关闭", Toast.LENGTH_SHORT).show();
//            m_Conf2.RecordStop(msChair,PG_RECORD_NORMAL);
//
//        }else{l
//
//        }
        int iErr1 = RecordAudioBothStart(sBothPath);
        if(iErr1!=0){
//            Toast.makeText(getContext(), "录音失败。 已经关闭 Err = " + iErr1, Toast.LENGTH_SHORT).show();
        }

    }

    private void pgRecordStopNew(String sConfName,String sPeer){
        m_Conf2.RecordStop(sConfName,sPeer, iStreamMode ,PG_RECORD_ONLYVIDEO_HASAUDIO);
        int iErr = RecordAudioBothStop(sBothPath);
        if(iErr!=0) {
//            Toast.makeText(getContext(), "RecordAudioBothStop 录音停止。 iErr = "+iErr, Toast.LENGTH_SHORT).show();
        }

    }

//    private void pgRecordStart(){
//        Date currentTime = new Date();
//        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
//        String sDate = formatter.format(currentTime);
//        String sPath = getSDCardDir() + "/test/record" + sDate + ".avi";
//        boolean iErr = m_Conf2.RecordStart(msChair, sPath);
//        if ((iErr == false)) {
//            Toast.makeText(getContext(), "录像失败。 已经关闭", Toast.LENGTH_SHORT).show();
//            m_Conf2.RecordStop(msChair,PG_RECORD_NORMAL);
//
//        }
//    }
//
//    private void pgRecordStop(){
//        m_Conf2.RecordStop(msChair);
//    }

    //给所有加入会议的成员发送消息
    private boolean pgNotifySend(String sConfName,String sData) {

        if(m_Conf2.NotifySend(sConfName,sData)  > PG_ERR_Normal){
            showInfo( "NotifySend 失败。");
        }

        return true;
    }

    private boolean m_bSpeechEnable = true;

//    /**
//     * 选择自己的声音是否在对端播放
//     */
//    private void Speech() {
//        if (!m_Conf2.AudioSpeech(mSmemb, m_bSpeechEnable)) {
//            Log.d("pgRobotClient", "Enable speech failed");
//        } else {
//            m_bSpeechEnable = !m_bSpeechEnable;
//        }
//    }


    public void SetCameraRate(int iCameraRate) {
        pgLibJNINode Node = m_Conf2.GetNode();
        if (Node != null) {
            if (Node.ObjectAdd("_aTemp", "PG_CLASS_Video", "", 0)) {

                String sData = "(Item){4}(Value){"+ iCameraRate +"}";

                Node.ObjectRequest("_aTemp", 2, sData, "");
                Node.ObjectDelete("_aTemp");
            }
        }
    }
    //
    public int RecordAudioBothStart(String sAviPath) {
        pgLibJNINode Node = m_Conf2.GetNode();
        if (Node != null) {
            if (Node.ObjectAdd("_vTemp", "PG_CLASS_Audio", "", 0)) {
                String sData = "(Path){" + Node.omlEncode(sAviPath) + "}(Action){1}(MicNo){65535}(SpeakerNo){65535}(HasVideo){1}";
                /*String sData = "(Path){" + Node.omlEncode(sAviPath) + "}(Action){1}(MicNo){1}(SpeakerNo){65535}(HasVideo){1}";*/
                int iErr = Node.ObjectRequest("_vTemp", 38, sData, "");
                Log.d("pgLiveCapture", "RecordAudioBothStart, iErr=" + iErr);
                Node.ObjectDelete("_vTemp");
                return iErr;
            }
        }
        return 1;
    }


    ///
    // 停止录制双方对讲的音频数据到一个avi文件。
    //     sAviPath：保存音频数据的*.avi文件，必须与RecordAudioBothStart传入的sAviPath参数相同。
    //
    public int RecordAudioBothStop(String sAviPath) {
        pgLibJNINode Node = m_Conf2.GetNode();
        if (Node != null) {
            if (Node.ObjectAdd("_vTemp", "PG_CLASS_Audio", "", 0)) {
                String sData = "(Path){" + Node.omlEncode(sAviPath) + "}(Action){0}";
                int iErr = Node.ObjectRequest("_vTemp", 38, sData, "");
                Log.d("pgLiveCapture", "RecordAudioBothStop, iErr=" + iErr);
                Node.ObjectDelete("_vTemp");
                return iErr;
            }
        }
        return 1;
    }

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
            String sConfName  = m_Node.omlGetContent(sParam, "ConfName");

            //执行打开视频的动作
            if ("VIDEO_OPEN".equals(sAct)) {

                //Demo 是为了演示方便 在这里实现自动打开视频的功能
                //所以才做了这个ID大的主动打开视频
                //实际情况中建议从Join出得到设备列表，或者本地保存列表，用ListView显示，点击某个ID然后开始打开视频
                String sObjUser = _ObjPeerBuild(m_sUser);
                String sObjPeer = _ObjPeerBuild(sPeer);
                if (sObjUser.compareTo(sPeer) > 0) {
                    showInfo(" 发起视频请求");
                    pgVideoOpenRequest(sConfName,sPeer);
                }
            }
        }
    };

    private void TimerStartOpen(String sConfName ,String sPeer) {
        String sParam = "(Act){VIDEO_OPEN}(Peer){" + sPeer + "}(ConfName){" + sConfName + "}";
        m_Timer.timerStart(sParam, 1,false);
    }


    //服务端下发的通知
    private void EventSvrNotify(String sAct, String sData, String sPeer,String sConfName, String sEventPara) {
        showInfo("SvrNotify :" + sData + " : " + sPeer);
    }

    //发给服务端的消息的回执
    private void EventSvrReply(String sAct, String sData, String sPeer,String sConfName, String sEventPara) {

    }

    //发给服务端的消息的回执
    private void EventSvrReplyError(String sAct, String sData, String sPeer,String sConfName, String sEventPara) {

    }


    //登录的结果
    private void EventLogin(String sAct, String sData, String sPeer,String sConfName, String sEventPara) {
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
    private void EventLogout(String sAct, String sData, String sPeer,String sConfName, String sEventPara) {
        showInfo("已经注销" + sData);
    }

    private void EventRpcResponse(String sAct, String sData, String sPeer, String sConfName, String sEventParam) {

    }

    private void EventRpcRequest(String sAct, String sData, String sPeer, String sConfName, String sEventParam) {


    }
    //sPeer的离线消息
    private void EventPeerSync(String sAct, String sData, String sPeer,String sConfName, String sEventPara) {
        // TODO: 2016/11/7 提醒应用程序可以和此节点相互发送消息了
        showInfo(sPeer + "节点建立连接");
        m_Conf2.MessageSend("MessageSend test", sPeer);
        m_Conf2.RpcRequest("RpcRequest test", sPeer, "123");

    }

    //sPeer的离线消息
    private void EventPeerOffline(String sAct, String sData, String sPeer,String sConfName, String sEventPara) {
        // TODO: 2016/11/7 提醒应用程序此节点离线了
        showInfo(sPeer + "节点离线 sData = " + sData);
//        pgVideoClose(sPeer);
    }

//-------------------------------------------------------------------------
    //sPeer的离线消息

    private void EventJoinRequest(String sAct, String sData, String sPeer,String sConfName, String sEventPara) {
        // TODO: 2016/11/7 sPeer请求加入会议  MemberAdd表示把他加入会议
        showInfo(sPeer + "请求加入会议->同意");
        if(verifyPeer(sConfName,sPeer)){
            m_Conf2.MemberAdd(sConfName,sPeer);
        }

    }

    //sPeer的离线消息
    private boolean m_bVideoStart = false;

    private void EventJoin(String sAct, String sData, String sPeer,String sConfName, String sEventPara) {
        // TODO: 2016/11/7 这里可以获取所有会议成员  可以尝试把sPeer加入会议成员表中
        showInfo(sPeer + "加入会议");

        m_Conf2.NotifySend(sConfName,sPeer + " : join ");
        Log.d("", sPeer + " 加入会议");
    }



    //sPeer的离线消息
    private void EventLeave(String sAct, String sData, String sPeer,String sConfName, String sEventPara) {
        showInfo(sPeer + "离开会议");

        Log.d("", " 离开会议");
    }
//---------------------------------------------------------------------
    //视频传输的状态
    private void EventVideoFrameStat(String sAct, String sData, String sPeer,String sConfName, String sEventPara) {
        //Show()

    }

    //sPeer的离线消息
    private void EventVideoSync(String sAct, String sData, String sPeer,String sConfName, String sEventPara) {
        // TODO: 2016/11/7 提醒应用程序可以打开这个sPeer的视频了
        showInfo("视频同步");

        if(sEventPara.equals("0")){
            // sEventPara == iStreamMode , Demo 只打开 0
            /*这个是开始一个定时器*/
            TimerStartOpen(sConfName,sPeer);
        }

    }

    private void EventVideoOpenRequest(String sAct, String sData, String sPeer, String sConfName, String sEventPara) {
        //收到视频请求
        showInfo(sPeer + " 请求视频连线->同意");
        //// TODO: 2016/11/7 在这之后回复
        //调用
        if(sEventPara.equals("0")){
            pgVideoOpenResponse(sConfName,sPeer);
        }

    }

    private void EventVideoLost(String sAct, String sData, final String sPeer,String sConfName, String sEventPara) {
        // TODO: 2016/11/8  对方视频已经丢失 挂断对方视频 并尝试重新打开
        showInfo(sPeer + " 的视频已经丢失 可以尝试重新连接");

        if(sEventPara.equals("0")){
            pgVideoClose(sConfName,sPeer);
            TimerStartOpen(sConfName,sPeer);
        }
    }

    private void EventVideoClose(String sAct, String sData, String sPeer,String sConfName, String sEventPara) {
        // TODO: 2016/11/8  通知应用程序视频已经挂断
        showInfo(sPeer + " 已经挂断视频");
        if(sEventPara.equals("0")) {
            pgOnVideoClose(sConfName, sPeer);
        }
    }

    private void EventVideoResponse(String sAct, String sData, String sPeer, String sConfName, String sEventPara) {
        // TODO: 2016/11/8 请求端会收到请求打开视频的结果，打开视频成功除了显示和播放视频外，还有这个事件
        if ("0".equals(sData)) {
            showInfo(sPeer + ":" + "视频成功打开");
            Log.d("", sPeer + " 成功打开");
        } else {
            showInfo(sPeer + ":" + "视频打开失败 iErr = " + sData);
            Log.d("", sPeer + " 打开失败");
            if(sEventPara.equals("0")) {
                pgVideoClose(sConfName, sPeer);
            }
        }
    }
    private void EventVideoCamera(String sAct, String sData, String sPeer,String sConfName, String sEventPara) {
        // VideoCamera 视频拍照的结果
    }

    private void EventLanScanResult(String sAct, String sData, String sPeer,String sConfName, String sEventPara) {
        showInfo("Act : LanScanResult  -- sData: " + sData + "  sPeer  : " + sPeer);
    }
    //-------------------------------------------------------------------
    //组消息
    private void EventNotify(String sAct, String sData, String sPeer,String sConfName, String sEventPara) {
        showInfo(sPeer + ": sData = " + sData);
    }

    //sPeer的消息处理
    private void EventMessage(String sAct, String sData, String sPeer,String sConfName, String sEventPara) {
        // TODO: 2016/11/7 处理sPeer发送过来的消息
        showInfo(sPeer + ":" + sData);
        Log.d("", sPeer + ":" + sData);
    }

//
//    private void EventFileGetRequest(String sAct, String sData, String sObjPeer) {
//        showInfo("MainFragment.OnEvent: Act=" + sAct + ", Data=" + sData + ", Peer=" + sObjPeer);
//        m_Conf2.FileAccept(msChair,sObjPeer,"/sdcard/test/test.avi" );
//    }
//
//    private void EventFilePutRequest(String sAct, String sData, String sObjPeer) {
//        showInfo("MainFragment.OnEvent: Act=" + sAct + ", Data=" + sData + ", Peer=" + sObjPeer);
//        Date currentTime = new Date();
//        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
//        String sDate = formatter.format(currentTime);
//        //sData = "peerpath=xxxxxxxxx" so xxxxxxx = sData.substring(9)
//        m_Conf2.FileAccept(msChair,sObjPeer,"/sdcard/test/GetFile_" + sDate + sData.substring(9) );
//    }
//
//    private void EventFileProgress(String sAct, String sData, String sObjPeer) {
//        showInfo("MainFragment.OnEvent: Act=" + sAct + ", Data=" + sData + ", Peer=" + sObjPeer);
//    }
//
//    private void EventFileFinish(String sAct, String sData, String sObjPeer) {
//        showInfo("MainFragment.OnEvent: Act=" + sAct + ", Data=" + sData + ", Peer=" + sObjPeer);
//    }
//
//    private void EventFileAbrot(String sAct, String sData, String sObjPeer) {
//        showInfo("MainFragment.OnEvent: Act=" + sAct + ", Data=" + sData + ", Peer=" + sObjPeer);
//    }
//
//    private void EventFileReject(String sAct, String sData, String sObjPeer) {
//        showInfo("MainFragment.OnEvent: Act=" + sAct + ", Data=" + sData + ", Peer=" + sObjPeer);
//    }
//
//    private void EventFileAccept(String sAct, String sData, String sObjPeer) {
//        showInfo("MainFragment.OnEvent: Act=" + sAct + ", Data=" + sData + ", Peer=" + sObjPeer);
//    }

//    private void EventCallSend(String sAct, String sData, String sPeer) {
//        // CallSend （具有回执的信息） 最终结果
//        showInfo("CallSend 回执 sData = " + sData);
//    }
//
//    private void EventVideoRecord(String sAct, String sData, String sPeer) {
//        // VideoRecord 视频录制的结果
//    }


    private OnEventListener m_OnEvent = new OnEventListener() {

        @Override
        public void event(String sAct, String sData, final String sPeer,String sConfName,String sEventParam) {
            // TODO Auto-generated method stub

//            String sObjPeer = _ObjPeerBuild(sPeer);
            if (sAct.equals(EVENT_VIDEO_FRAME_STAT)) {
                EventVideoFrameStat(sAct, sData, sPeer,sConfName,sEventParam);
            } else if (sAct.equals(EVENT_LOGIN)) {
                EventLogin(sAct, sData, sPeer,sConfName,sEventParam);
            } else if (sAct.equals(EVENT_LOGOUT)) {
                EventLogout(sAct, sData, sPeer,sConfName,sEventParam);
            }
            else if (sAct.equals(EVENT_SVR_NOTIFY)) {
                EventSvrNotify(sAct, sData, sPeer,sConfName,sEventParam);
            } else if (sAct.equals(EVENT_SVR_RELAY)) {
                EventSvrReply(sAct, sData, sPeer,sConfName,sEventParam);
            } else if (sAct.equals(EVENT_SVR_REPLYR_ERROR)) {
                EventSvrReplyError(sAct, sData, sPeer,sConfName,sEventParam);
            }else if (sAct.equals(EVENT_PEER_SYNC)) {
                EventPeerSync(sAct, sData, sPeer,sConfName,sEventParam);
            }
            else if (sAct.equals(EVENT_PEER_OFFLINE)) {
                EventPeerOffline(sAct, sData, sPeer,sConfName,sEventParam);
            }
            else if (sAct.equals(EVENT_MESSAGE)) {
                EventMessage(sAct, sData, sPeer,sConfName,sEventParam);
            }
            else if (sAct.equals(EVENT_RPC_REQUEST)) {
                EventRpcRequest(sAct, sData, sPeer,sConfName,sEventParam);
            }
            else if (sAct.equals(EVENT_RPC_RESPONSE)) {
                EventRpcResponse(sAct, sData, sPeer,sConfName,sEventParam);
            }
            else if (sAct.equals(EVENT_JOIN_REQUEST)) {
                EventJoinRequest(sAct, sData, sPeer, sConfName,sEventParam);
            } else if (sAct.equals(EVENT_JOIN)) {
                EventJoin(sAct, sData, sPeer,sConfName,sEventParam);
            } else if (sAct.equals(EVENT_LEAVE)) {
                EventLeave(sAct, sData, sPeer,sConfName,sEventParam);
            } else if (sAct.equals(EVENT_VIDEO_SYNC)) {
                EventVideoSync(sAct, sData, sPeer,sConfName,sEventParam);
            }
            else if (sAct.equals(EVENT_VIDEO_REQUEST)) {
                EventVideoOpenRequest(sAct, sData, sPeer,sConfName,sEventParam);
            }
            else if (sAct.equals(EVENT_VIDEO_RESPONSE)) {
                EventVideoResponse(sAct, sData, sPeer,sConfName,sEventParam);
            }
            else if (sAct.equals(EVENT_VIDEO_LOST)) {
                EventVideoLost(sAct, sData, sPeer,sConfName,sEventParam);
            } else if (sAct.equals(EVENT_VIDEO_CLOSE)) {
                EventVideoClose(sAct, sData, sPeer,sConfName,sEventParam);
            }
            else if (sAct.equals(EVENT_VIDEO_CAMERA)) {
                EventVideoCamera(sAct, sData, sPeer,sConfName,sEventParam);
            }
            else if (sAct.equals(EVENT_NOTIFY)) {
                EventNotify(sAct, sData, sPeer,sConfName,sEventParam);
            }
//            else if (sAct.equals(EVENT_LAN_SCAN_RESULT)) {
//                EventLanScanResult(sAct, sData, sPeer);
//            }
//            else if (sAct.equals(EVENT_FILE_ACCEPT)) {
//                EventFileAccept(sAct, sData, sPeer);
//            } else if (sAct.equals(EVENT_FILE_REJECT)) {
//                EventFileReject(sAct, sData, sPeer);
//            } else if (sAct.equals(EVENT_FILE_ABORT)) {
//                EventFileAbrot(sAct, sData, sPeer);
//            } else if (sAct.equals(EVENT_FILE_FINISH)) {
//                EventFileFinish(sAct, sData, sPeer);
//            } else if (sAct.equals(EVENT_FILE_PROGRESS)) {
//                EventFileProgress(sAct, sData, sPeer);
//            }else if (sAct.equals(EVENT_FILE_PUT_REQUEST)) {
//                EventFilePutRequest(sAct, sData, sPeer);
//            }else if (sAct.equals(EVENT_FILE_GET_REQUEST)) {
//                EventFileGetRequest(sAct, sData, sPeer);
//            }
            else {
                showInfo("MainFragment.OnEvent: Act=" + sAct + ", Data=" + sData + ", Peer=" + sPeer);
            }
        }
    };




}
