package com.peergine.android.conference;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceView;

import com.peergine.plugin.lib.pgLibJNINode;

import static com.peergine.android.conference.OnEventConst.EVENT_JOIN_REQUEST;
import static com.peergine.android.conference.OnEventConst.EVENT_KICK_OUT;
import static com.peergine.android.conference.OnEventConst.EVENT_LOGIN;
import static com.peergine.android.conference.OnEventConst.EVENT_LOGOUT;
import static com.peergine.android.conference.OnEventConst.EVENT_MESSAGE;
import static com.peergine.android.conference.OnEventConst.EVENT_PEER_SYNC;
import static com.peergine.android.conference.OnEventConst.EVENT_RPC_REQUEST;
import static com.peergine.android.conference.OnEventConst.EVENT_SVR_NOTIFY;
import static com.peergine.android.conference.pgLibNode.*;
import static com.peergine.android.conference.pgLibError.*;


public class pgLibConference2 {
    /**
     * 录制对端的视频和音频
     */
    public static final int PG_RECORD_NORMAL = 0;
    /**
     * 录制对端的视频
     */
    public static final int PG_RECORD_ONLYVIDEO = 1;
    /**
     * 录制对端的音频
     */
    public static final int PG_RECORD_ONLYAUDIO = 2;
    /**
     * 录制对端的视频，需要在外部调用其他录制音频的API（如RecordAudioBothStart）配合使用才能录制。
     */
    public static final int PG_RECORD_ONLYVIDEO_HASAUDIO = 3;
    /**
     * 录制对端的音频，需要在外部调用其他录制视频的API配合使用才能录制。
     */
    public static final int PG_RECORD_ONLYAUDIO_HASVIDEO = 4;

    private static final String LIB_VER = "30";
    private static final String ID_PREFIX = "_DEV_";

    private pgLibJNINode m_Node = null;
    private pgLibNodeThreadProc m_NodeThreadProc = null;
    private final pgLibTimer m_Timer = new pgLibTimer();
    private final GroupList m_GroupList = new GroupList();
    private String m_sObjSelf = "";


    //======================================================================

    private static final int KEEP_TIMER_INTERVAL = 2;
    private static final int ACTIVE_TIMER_INTERVAL = 2;

    private static final String PARAM_LOGIN = "NodeLogin";
    private static final String PARAM_LANSCAN = "LanScan";
    private static final String PARAM_SVRREQUEST = "SvrRequest";
    private static final String PARAM_PRE_CALLSEND = "CallSend";
    private static final String PARAM_PRE_VIDEO_OPEN = "VideoOpen";
    private static final String PARAM_FILE_GET_REQUEST = "FileGetRequest";
    private static final String PARAM_FILE_PUT_REQUEST = "FilePutRequest";
    private static final String PARAM_AUDIO_CTRL_VOLUME = "AudioCtrlVolume";
    private static final String PARAM_LOGOUT = "NodeLogout";
    private static final String PARAM_SVR_REDIRECT = "SvrRedirect";
    private static final String PARAM_PEER_GET_INFO = "PeerGetInfo";
    private static final String PARAM_PEER_GET_INFO_NO_REPORT = "PeerGetInfoNoReport";

    private static final String TIME_OUT_ACT_KEEP = "Keep";
    private static final String TIME_OUT_ACT_TIMER_ACTIVE = "TimerActive";
    private static final String TIME_OUT_ACT_CHAIR_PEER_CHECK = "ChairPeerCheck";
    private static final String TIME_OUT_ACT_CHAIRMAN_ADD = "ChairmanAdd";
    private static final String TIME_OUT_ACT_RELOGIN = "Relogin";
    private static final String TIME_OUT_ACT_PEER_GET_INFO = "PeerGetInfo";


    private static final int VIDEO_PEER_MODE_Leave  =0;
    private static final int VIDEO_PEER_MODE_Request = 1;
    private static final int VIDEO_PEER_MODE_Response = 2;
    private static final int VIDEO_PEER_MODE_Join = 3;
    private static final int VIDEO_RESPONSE_TIMEOUT = 30;

    private String m_sUser ;
    private String m_sPass ;

    private String m_sRelayAddr = "";
    private String m_sInitParam = "";

    private String m_sInitSvrName = "pgConnectSvr";
    private String m_sInitSvrAddr = "";
    private String m_sSvrName = "";
    private String m_sSvrAddr = "";

    private int m_iLoginFailCount;
    private boolean m_bLogin;
    private int m_iLoginDelayMax;
    private int m_iIDTimerRelogin;
    private boolean m_bReportPeerInfo = true;
    private boolean m_EventOutObjPeer = false;
    private String m_sListVideo = "";
    private String m_LocalAddr = "";

    private NodeEventHook m_eventHook = null;
    private OnEventListener m_eventListener = null;
    private String m_RpcResponseBuff = "";
    private String m_sPrvwParam = "";
    private boolean bPreviewStart = false;
    private boolean m_bInitialize = false;
    private boolean m_bNodeLibInit = false;
    private boolean m_bEventEnable = true;
    private boolean m_bLogined = false;
    // Static function

    /**
     * 判断字符串是否为空
     * @param s 字符串
     * @return true 为空 ，false 不为空
     */
    public static boolean _isEmpty(String s){
        return s == null || "".equals(s);
    }

    //处理int

    /**
     * 字符串转化成数字
     * @param sInt 字符串
     * @param iDef 转化失败时使用默认数字
     * @return 数字
     */
    public static int _ParseInt(String sInt, int iDef) {
        try {
            if ("".equals(sInt)) {
                return 0;
            }
            return Integer.parseInt(sInt);
        } catch (Exception ex) {
            return iDef;
        }
    }

    //log 打印

    /**
     * 日志输出
     * @param sOut 日志内容
     */
    public static void _OutString(String sOut) {
        //if (BuildConfig.DEBUG) {
        Log.d("pgLibConference", sOut);
        //}
    }

    /**
     * 获取当前版本
     * @return 版本号
     */
    public String Version(){
        if (m_Node != null) {
            String sVersion = "";
            String sVerTemp = m_Node.omlGetContent(m_Node.utilCmd("Version", ""), "Version");
            if (sVerTemp.length() > 1) {
                sVersion = sVerTemp.substring(1);
            }

            return (sVersion + "." + LIB_VER);
        }
        else {
            return LIB_VER;
        }
    }

    /**
     * 钩子结构回调接口，不建议使用，使用需要对中间件编程有足够的了解。
     */
    public interface NodeEventHook {
        int OnExtRequest(String sObj, int uMeth, String sData, int uHandle, String sPeer);
        int OnReply(String sObj, int uErr, String sData, String sParam);
    }

    /**
     * 设置钩子回调，不建议使用，使用需要对中间件编程有足够的了解。
     * @param eventHook
     */
    public void SetNodeEventHook(NodeEventHook eventHook) {
        m_eventHook = eventHook;
    }

    /**
     * 描述：设置消息接收回调接口。
     * 阻塞方式：非阻塞，立即返回
     * @param eventListener ：[IN] 实现了OnEventListner接口的对象，必须定义event函数。
     */
    public void SetEventListener(OnEventListener eventListener){
        m_eventListener = eventListener;
    }


    /**
     * 描述：P2P会议对象初始化函数
     * 阻塞方式：非阻塞，立即返回。
     * @param sUser ：[IN] 登录用户名，自身的设备ID
     * @param sPass ：[IN] 登录密码
     * @param sSvrAddr ：[IN] 登录服务器地址和端口，格式：x.x.x.x:x
     * @param sRelayAddr ：[IN] 转发服务器地址和端口，格式：x.x.x.x:x。
     * 如果传入空字符串，则使用登录服务器的IP地址加上443端口构成转发服务器地址。
     * @param sInitParam ：视频参数，格式为：(P2PTryTime){3}(SKTBufSize1){256}(SKTBufSize1){256}
     *       VideoInExternal: 启用视频输入回调接口。0为禁用，1为启用。
     *       VideoOutExternal: 启用视频解码后输出回调接口。0为禁用，1为启用。
     *       VideoOutExtCmp: 启用视频解码前输出回调接口。0为禁用，1为启用。
     *       AudioInExternal: 启用音频输入回调接口。0为禁用，1为启用。
     *       AudioOutExternal: 启用音频解码后输出回调接口。0为禁用，1为启用。
     *		 SKTBufSize0 :优先级0的发送缓冲区长度（单位为K字节），传0则使用缺省值，缺省为64(K)
     *		 SKTBufSize1 :优先级1的发送缓冲区长度（单位为K字节），传0则使用缺省值，缺省为64(K)
     *		 SKTBufSize2 :优先级2的发送缓冲区长度（单位为K字节），传0则使用缺省值，缺省为512(K)
     *		 SKTBufSize3 :优先级3的发送缓冲区长度（单位为K字节），传0则使用缺省值，缺省为128(K)
     *       Digest: 是否使用摘要方式传递密码。0为明文方式，1为摘要方式。（缺省为1）
     *		 P2PTryTime :P2P穿透尝试时间（单位为秒）。
     *       	(iP2PTryTime == 0)：使用缺省值，缺省值为6秒。
     *          (iP2PTryTime > 0 && iP2PTryTime <= 3600)：超时值为所传的iP2PTryTime
     *      	(iP2PTryTime > 3600)：禁用P2P穿透，直接用转发。
     *       LogLevel0: 是否开启Major级别的日志信息输出。1为开启，0为关闭，默认为1
     *       LogLevel1: 是否开启General级别的日志信息输出。1为开启，0为关闭，默认为1
     *       LogLevel2: 是否开启Suggestive级别的日志信息输出。1为开启，0为关闭，默认为0
     *       LogLevel3: 是否开启Info级别的日志信息输出。1为开启，0为关闭，默认为0
     *       Debug: 是否开启调试信息打印。1为开启，0为关闭，默认为0
     *       EncryptMsg: 是否加密传输(发送)消息。1为加密，0为不加密，默认为0
     *       LoginDelayInterval: 尝试重新登录的退避时间的增长步进（秒）。有效范围1 ~ 300，默认10
     *       LoginDelayMax: 尝试重新登录的退避时间的最大值（秒）。有效范围30 ~ 300，默认300

     * @param oCtx： Android程序的上下文对象
     * @return 错误码 PG_ERR_*
     */
    public int Initialize(String sUser, String sPass, String sSvrAddr,
                          String sRelayAddr, String sInitParam, Context oCtx){
        if(!m_bInitialize) {
            if(_isEmpty(sUser) || _isEmpty(sSvrAddr)){
                return PG_ERR_BadParam;
            }

            m_sUser = sUser;
            m_sPass = sPass;
            m_sInitSvrName = m_sSvrAddr = sSvrAddr;
            m_sRelayAddr = sRelayAddr;
            m_sInitParam = sInitParam;

            m_sObjSelf = _ObjPeerBuild(sUser);
            // Init JNI lib.
            if (!pgLibNode.NodeLibInit(oCtx)) {
                _OutString("Initialize: Peergine plugin invalid.");
                return PG_ERR_System;
            }
            m_bNodeLibInit = true;


            // Create Timer message handler.
            if (!m_Timer.timerInit(timerOut)) {
                m_Timer.timerClean();
                return PG_ERR_System;
            }

            // Create Node objects.
            try {
                m_Node = new pgLibJNINode();
                m_NodeThreadProc = new pgLibNodeThreadProc(m_Node, mNodeProc);
                if (!m_NodeThreadProc._NodeDispInit()) {
                    Clean();
                    _OutString("Initialize: Init dispatch failed.");
                    return PG_ERR_System;
                }

            } catch (Exception ex) {
                Clean();
                return PG_ERR_System;
            }

            // Init status
            _NodeOptionExter(sInitParam);

            m_iLoginFailCount = 0;
            m_iLoginDelayMax = 60;
            m_iIDTimerRelogin = -1;

            if (!_NodeStart()) {
                Clean();
                _OutString("Initialize: Node start failed.");
                return PG_ERR_System;
            }
            m_bInitialize = true;
            return 0;
        }
        return 0;
    }

    /**
     * 描述：P2P会议对象清理函数
     * 阻塞方式：非阻塞，立即返回。
     */
    public void Clean(){
        _NodeStop();
        if(m_NodeThreadProc != null){
            m_NodeThreadProc._NodeDispClean();
            m_NodeThreadProc = null;
        }

        m_Timer.timerClean();
        if(m_bNodeLibInit){
            pgLibNode.NodeLibClean();
            m_bNodeLibInit = false;
        }


        if(m_Node != null){
            m_Node = null;
        }
        m_bInitialize = false;
    }

    /**
     * 描述：获取自身的P2P节点名
     * 阻塞方式：非阻塞，立即返回。
     * 返回值：自身的P2P节点名
     * 作用：扩展时利用此类，进行底层操作。
     */
    public pgLibJNINode GetNode() {
        return m_Node;
    }

    /**
     * 描述：获取自身的P2P节点对象名
     * 阻塞方式：非阻塞，立即返回。
     * 返回值：自身的P2P节点名
     */
    public String GetSelfObjPeer() {
        return m_sObjSelf;
    }

    /**
     * Scan the in the same lan.
     * @return 错误码 PG_ERR_*
     */
    public int LanScanStart(){
        return 0;
    }

    /**
     * 描述：通过节点名与其他节点建立联系 （节点名在我们P2P网络的功能类似英特网的IP地址）
     * 阻塞方式：非阻塞。
     * @param sPeer  对端的节点名（用户名 ID）
     * @return 错误码 PG_ERR_*
     */
    public int PeerAdd(String sPeer){
        if (m_Node != null) {
            if (!"".equals(sPeer)) {
                String sObjPeer = _ObjPeerBuild(sPeer);

                String sClass = m_Node.ObjectGetClass(sObjPeer);
                if ("PG_CLASS_Peer".equals(sClass)) {
                    return PG_ERR_Normal;
                }

                if (!"".equals(sClass)) {
                    m_Node.ObjectDelete(sObjPeer);
                }
                return m_Node.ObjectAdd(sObjPeer, "PG_CLASS_Peer", "", 0x10000)?PG_ERR_Normal : PG_ERR_System;
            }
        }
        return PG_ERR_System;
    }

    /**
     * Sdk扩展运用之添加通信节点，  使用之后会产生PeerSync事件
     * 删除节点连接。（一般不用主动删除节点，因为如果没有通信，节点连接会自动老化。）
     * @param sPeer 对端的节点名（用户名）
     */
    public void PeerDelete(String sPeer){
        if (m_Node != null) {
            if (!"".equals(sPeer)) {
                String sObjPeer = _ObjPeerBuild(sPeer);

                m_Node.ObjectDelete(sObjPeer);
            }
        }
    }

    /**
     * 获取节点连接信息
     * @param sPeer 对端节点 名称
     * @param bReport 是否上报
     * @return 错误码 PG_ERR_*
     */
    public int PeerGetInfo(String sPeer, boolean bReport){
        if("".equals(sPeer)){
            return PG_ERR_BadParam;
        }

        String sObjPeer = _ObjPeerBuild(sPeer);

        String sReport = bReport?"1":"0";
        return _NodePeerGetInfo(sObjPeer,sReport);
    }

    /**
     * 描述：给指定节点发送消息
     * 阻塞方式：非阻塞，立即返回
     *
     * @param sMsg [IN] 消息内容
     * @param sPeer [IN]节点名称
     * @return 错误码 PG_ERR_*
     */
    public int MessageSend(String sPeer,String sMsg){

        if (m_Node == null) {
            return PG_ERR_System;
        }
        if(_isEmpty(sPeer) || _isEmpty(sMsg)){
            return PG_ERR_BadParam;
        }

        String sObjPeer = _ObjPeerBuild(sPeer);
        String sData = "UMessage?" + sMsg;
        int iErr = m_Node.ObjectRequest(sObjPeer, PG_METH_PEER_Message, sData, "");
        if (iErr > 0) {
            if (iErr == PG_ERR_BadObject) {
                _ChairPeerCheck();
            }
            _OutString("MessageSend: iErr=" + iErr);
        }
        return iErr;
    }

    /**
     * 描述：给指定节点发送RPC消息
     * 阻塞方式：非阻塞，立即返回
     *
     * @param sPeer ：[IN]节点名称
     * @param sMsg ：[IN] 消息内容
     * @param sParam :[IN]可以为空，发送成功后，
     *		sPeer端会收到RpcRequest事件，可以在回调中调用setRpcResponse 设置回复消息。
     *		可以收到RpcResponse事件，sParam 为 事件参数 sEventParam = sParam+":"+错误码 0 表示正常成功
     * @return 错误码 PG_ERR_*
     */
    public int RpcRequest(String sPeer,String sMsg, String sParam){

        if (m_Node == null) {
            return PG_ERR_System;
        }
        if(_isEmpty(sPeer) || _isEmpty(sMsg)){
            return PG_ERR_BadParam;
        }

        String sObjPeer = _ObjPeerBuild(sPeer);

        String sData = "UMessage?" + sMsg;
        int iErr = m_Node.ObjectRequest(sObjPeer, PG_METH_PEER_Call, sData, "RpcRequest:" + sParam);
        if (iErr > 0) {
            _OutString(".RpcRequest: iErr=" + iErr);
        }

        return iErr;
    }

    /**
     * 描述：设置RPC回复消息;
     *    马上设置，不支持线程设置
     * @param sData ：[IN] 消息内容
     *
     */
    public void setRpcResponse(String sData){
        m_RpcResponseBuff = sData;
    }

    /**
     * 描述：创建播放窗口对象
     * 阻塞方式：阻塞
     * @return  返回值：SurfaceView对象，可加入到程序主View中
     */
    public SurfaceView PreviewCreate(){
        if(m_Node == null){
            _OutString("PreviewCreate Node == null error");
            return null;
        }
         return (SurfaceView) m_Node.WndNew(0, 0, 320, 240);
    }

    /**
     * 描述：销毁播放窗口对象
     * 阻塞方式：阻塞
     */
    public void PreviewDestroy(){
        if (m_Node != null) {
            m_Node.WndDelete();
        }
    }

    /**
     *
     * @param sPrvwParam  预览视频参数
     *     Code: 视频压缩编码类型：1为MJPEG、2为VP8、3为H264。
     *     Mode: 视频图像的分辨率（尺寸），有效数值如下：
     *         0: 80x60, 1: 160x120, 2: 320x240, 3: 640x480,
     *         4: 800x600, 5: 1024x768, 6: 176x144, 7: 352x288,
     *          8: 704x576, 9: 854x480, 10: 1280x720, 11: 1920x1080
     *     FrmRate: 视频的帧间间隔（毫秒）。例如40毫秒的帧率为：1000/40 = 25 fps
     *     BitRate: 视频压缩后的码率。单位为 Kbps
     *     CameraNo: 摄像头编号，CameraInfo.facing的值。
     *     Portrait: 采集图像的方向。0为横屏，1为竖屏。
     * @return
     */
    public int PreviewStart(String sPrvwParam){
        //预览
        if(m_Node == null){
            return PG_ERR_System;
        }
        if(!bPreviewStart) {
            m_sPrvwParam = sPrvwParam;
            _VideoOption(sPrvwParam);

            int iCode = _ParseInt(m_Node.omlGetContent(sPrvwParam, "Code"), 0);
            int iMode = _ParseInt(m_Node.omlGetContent(sPrvwParam, "Mode"), 3);
            int iRate = _ParseInt(m_Node.omlGetContent(sPrvwParam, "Rate"), 40);

            if (iCode < 0 || iCode > 3 || iMode < 0 || iRate < 0) {
                return PG_ERR_BadParam;
            }

            m_Node.ObjectAdd(_PrvwBuild(), "PG_CLASS_Video", "", PG_ADD_VIDEO_Preview);
            String sWndRect = "(Code){" + iCode + "}(Mode){" + iMode + "}(Rate){" + iRate + "}(Wnd){}";

            int iErr = m_Node.ObjectRequest(_PrvwBuild(), PG_METH_VIDEO_Open, sWndRect, "PreviewStart");
            if(iErr > PG_ERR_Normal) {
                return iErr;
            }
            bPreviewStart = true;
        }
        return PG_ERR_Normal;
    }

    public void PreviewStop(){
        //预览
        if(m_Node == null){
            return;
        }

        if(bPreviewStart) {
            m_Node.ObjectRequest(_PrvwBuild(), PG_METH_VIDEO_Close, "", "PreviewStop");
            m_Node.ObjectDelete(_PrvwBuild());
        }
        bPreviewStart = false;
    }


    /**
     *  描述：开始会议，初始化视音频等会议相关数据。
     *  阻塞方式：非阻塞
     *
     * @param sConfName 会议名称
     * @param sChair 主席端ID名称
     * @return 错误码 PG_ERR_*
     */
    public int Start(String sConfName, String sChair){
        if(_isEmpty(sConfName) || _isEmpty(sChair)){
            return PG_ERR_Normal;
        }
        Group group = m_GroupList._GroupSearch(sConfName);
        if(group!=null){
            return PG_ERR_Exist;
        }
        group = new Group(sConfName,sChair,m_sUser);
        int iErr = _ServiceStart(group);
        if(iErr > 0 ){
            _OutString("Start : Error");
            return iErr;
        }
        return 0;
    }

    /**
     *  描述：停止会议，初始化视音频等会议相关数据。
     *  阻塞方式：非阻塞
     * @param sConfName 会议名称
     *
     */
    public void Stop(String sConfName){
        if(_isEmpty(sConfName)){
            return;
        }
        Group group = m_GroupList._GroupSearch(sConfName);
        if(group == null){
            return;
        }
        _ServiceStop(group);
        m_GroupList._GroupDelete(group);
    }

    /**
     * 描述：在会议中发送广播消息
     * 阻塞方式：非阻塞，立即返回
     *
     * @param sConfName [IN] 会议名称
     * @param sData [IN] 消息内容
     * @return 错误码 PG_ERR_*
     */
    public int NotifySend(String sConfName,String sData){
        if(_isEmpty(sConfName)){
            return PG_ERR_BadParam;
        }

        if(m_Node == null) {
            return PG_ERR_System;
        }

        Group group = m_GroupList._GroupSearch(sConfName);
        if(group == null){
            return PG_ERR_NoExist;
        }

        int iErr = m_Node.ObjectRequest(group.sObjD, PG_METH_DATA_Message, sData, "NotifySend:" + m_sObjSelf);
        if (iErr > 0) {
            _OutString("NotifySend: iErr=" + iErr);
        }
        return iErr;
    }

    /**
     * 描述：添加成员（主席端）
     * 阻塞方式：非阻塞，立即返回
     * @param sConfName [IN] 会议名称
     * @param sMember ：[IN] 成员名
     * @return 错误码 PG_ERR_*
     */
    public int MemberAdd(String sConfName,String sMember){
        if(_isEmpty(sConfName) || _isEmpty(sMember)){
            return PG_ERR_BadParam;
        }
        if(m_Node == null) {
            return PG_ERR_System;
        }

        Group group = m_GroupList._GroupSearch(sConfName);
        if(group == null){
            return PG_ERR_NoExist;
        }

        String sObjMember = _ObjPeerBuild(sMember);

        int uMask = 0x0200; // Tell all.
        String sDataMdf = "(Action){1}(PeerList){(" + m_Node.omlEncode(sObjMember) + "){" + uMask + "}}";
        int iErr = m_Node.ObjectRequest(group.sObjG, PG_METH_GROUP_Modify, sDataMdf, "MemberAdd:" + sObjMember);
        if (iErr > 0) {
            _OutString("MemberAdd: Add group member failed err=" + iErr);
        }
        return iErr;
    }

    /**
     * 描述：删除成员（主席端）
     * @param sConfName [IN] 会议名称
     * @param sMember ：[IN] 成员名
     * 阻塞方式：非阻塞，立即返回
     */
    public int MemberDelete(String sConfName,String sMember){
        if(_isEmpty(sConfName) || _isEmpty(sMember)){
            return PG_ERR_BadParam;
        }
        if(m_Node == null) {
            return PG_ERR_System;
        }

        Group group = m_GroupList._GroupSearch(sConfName);
        if(group == null){
            return PG_ERR_NoExist;
        }

        String sObjMember = _ObjPeerBuild(sMember);

        String sDataMdf = "(Action){0}(PeerList){(" + m_Node.omlEncode(sObjMember) + "){0}}";

        int iErr = this.m_Node.ObjectRequest(group.sObjG, PG_METH_GROUP_Modify, sDataMdf, "");
        if (iErr > 0) {
            _OutString("MemberDel: Add group member failed err=" + iErr);
        }
        return iErr;
    }

    /**
     * 描述：请求加入会议
     * 阻塞方式：非阻塞，立即返回
     * @param sConfName [IN] 会议名称
     * @return 错误码 PG_ERR_*
     */
    public int JoinRequest(String sConfName){
        if(_isEmpty(sConfName)){
            return PG_ERR_BadParam;
        }
        if(m_Node == null) {
            return PG_ERR_System;
        }

        Group group = m_GroupList._GroupSearch(sConfName);
        if(group == null){
            return PG_ERR_NoExist;
        }

        String sParam = "(ConfName){" + group.sConfName + "}(ObjPeer){" + m_sObjSelf + "}";

        String sData = "JoinRequest?" + sParam ;
        int iErr = this.m_Node.ObjectRequest(group.sObjChair, PG_METH_PEER_Message, sData, "JoinRequest");
        if (iErr > 0) {
            _OutString("JoinRequest:ObjectRequest Err=" + iErr);
        }
        return iErr;
    }


    /**
     * 描述：初始化视频设置
     *
     * 阻塞方式：非阻塞，立即返回
     * @param sConfName [in] 会议名称：
     * @param sVideoParamDef [in] 默认视频流参数,示例(Flag){0}(Code){3}(Mode){2}(Rate){40}
     *     Flag ：0 初始化视频正常 ; 1 初始化视频只接收视频不发送视频 ；2 初始化视频只发送视频不接收视频.
     *     Code: 视频压缩编码类型：1为MJPEG、2为VP8、3为H264。
     *     Mode: 视频图像的分辨率（尺寸），有效数值如下：
     *         0: 80x60, 1: 160x120, 2: 320x240, 3: 640x480,
     *         4: 800x600, 5: 1024x768, 6: 176x144, 7: 352x288,
     *          8: 704x576, 9: 854x480, 10: 1280x720, 11: 1920x1080
     *     FrmRate: 视频的帧间间隔（毫秒）。例如40毫秒的帧率为：1000/40 = 25 fps
     *     BitRate: 视频压缩后的码率。单位为 Kbps
     * @param sVideoParamLarge [in] 视频流参数
     * @return  true 操作成功，false 操作失败
     */
    public int VideoStart(String sConfName, String sVideoParamDef,String sVideoParamLarge){
        if(_isEmpty(sConfName)){
            return PG_ERR_BadParam;
        }
        if(m_Node == null) {
            return PG_ERR_System;
        }

        Group group = m_GroupList._GroupSearch(sConfName);
        if(group == null){
            return PG_ERR_NoExist;
        }

        group.sVideoParamDef = sVideoParamDef;
        group.sVideoParamLarge = sVideoParamLarge;

        return _VideoInit(group);
    }

    /**
     * 描述：停止播放
     * @param sConfName [IN] 会议名称
     * 阻塞方式：非阻塞，立即返回
     */
    public int VideoStop(String sConfName){
        if(_isEmpty(sConfName)){
            return PG_ERR_BadParam;
        }
        if(m_Node == null) {
            return PG_ERR_System;
        }

        Group group = m_GroupList._GroupSearch(sConfName);
        if(group == null){
            return PG_ERR_NoExist;
        }

        return _VideoClean(group);;
    }


    /**
     * 描述：打开某一成员另外一条流的视频
     * @param sConfName 会议名称
     * @param sPeer 成员节点名
     * @param iStreamMode 视频流选择 0：默认视频流 1：额外的视频流
     * @param oView 默认需要pgLibView.Get()产生的View
     * @param sExtParam 额外的参数：默认为空，(DevNo){1}
     * 	   DevNo : 外部接口播放时 作为参数。 DevNo >= 0 时
     * @return error code @link pgLibError.java
     */
    public int VideoOpenRequest(String sConfName,String sPeer,int iStreamMode,SurfaceView oView, String sExtParam){
        if(_isEmpty(sConfName) || _isEmpty(sPeer)){
            return PG_ERR_BadParam;
        }
        if(m_Node == null) {
            return PG_ERR_System;
        }

        String sWndEle = _GetEndEle(oView,sExtParam);
        if(_isEmpty(sWndEle)){
            return PG_ERR_BadParam;
        }

        //if(!_isEmpty(sParam))= pgLibView.GetNodeByView(oView).utilGetWndRect();

        Group group = m_GroupList._GroupSearch(sConfName);
        if(group == null){
            return PG_ERR_NoExist;
        }
        String sObjPeer = _ObjPeerBuild(sPeer);

        VideoPeer oPeer = group.videoPeerList._VideoPeerSearch(sObjPeer);
        if (oPeer == null) {
            oPeer = group.videoPeerList._VideoPeerAdd(sObjPeer);
            if(oPeer ==  null){
                return PG_ERR_System;
            }
        }

        String sObjV = iStreamMode > 0 ? group.sObjLV : group.sObjV;
        String sData  = "(Peer){" + m_Node.omlEncode(sObjPeer) + "}(Wnd){" + sWndEle + "}";
        String sParamVideoReq = "VideoOpenRequest:" + sObjPeer;
        int iResErr =  m_Node.ObjectRequest(sObjV, PG_METH_VIDEO_Join, sData, sParamVideoReq);
        if(iResErr > PG_ERR_Normal){

            return iResErr;
        }
        oPeer.VideoJoin(iStreamMode,group.peerHeartbeatStamp,sWndEle);
        return iResErr;
    }

    private String _GetEndEle(SurfaceView oView, String sParam) {
        String sEndEle;
        int iDevNo = _ParseInt(m_Node.omlGetContent(sParam,"DevNo"),-1);
        if(iDevNo > -1){
            sEndEle = "(PosX){0}(PosY){0}(SizeX){320}(SizeY){240}(Handle){"+ iDevNo + "}";
        }else{
            if(oView == null){
                return "";
            }
            sEndEle = pgLibView.GetNodeByView(oView).utilGetWndRect();;
        }
        return sEndEle;
    }

    /**
     * 描述：打开某一成员另外一条流的视频
     *
     * @param sConfName 会议名称
     * @param sPeer 成员节点名
     * @param iStreamMode 视频流选择 0：默认视频流 1：额外的视频流
     * @param iErr 传入错误码 PG_ERR_Normal 正常，PG_ERR_Reject 拒绝 如果iErr !=PG_ERR_Normal ,oVew 、sExtParam可传入空值
     * @param oView pgLibView产生的View
     * @param sExtParam 额外的参数，默认为空：(DevNo){1}
     * 	   DevNo : 外部接口播放时 作为参数。
     * @return error code @link pgLibError.java
     */
    public int VideoOpenResponse(String sConfName,String sPeer,int iStreamMode,int iErr,SurfaceView oView, String sExtParam){
        if(_isEmpty(sConfName) || _isEmpty(sPeer)){
            return PG_ERR_BadParam;
        }
        if(m_Node == null) {
            return PG_ERR_System;
        }
        String sEndEle = "";
        if(iErr == PG_ERR_Normal) {
            sEndEle = _GetEndEle(oView, sExtParam);
            if (_isEmpty(sEndEle)) {
                return PG_ERR_BadParam;
            }
        }

        //if(!_isEmpty(sParam))= pgLibView.GetNodeByView(oView).utilGetWndRect();

        Group group = m_GroupList._GroupSearch(sConfName);
        if(group == null){
            _OutString("VideoOpenResponse: No Conference.");
            return PG_ERR_NoExist;
        }
        String sObjPeer = _ObjPeerBuild(sPeer);

        VideoPeer oPeer = group.videoPeerList._VideoPeerSearch(sObjPeer);
        if (oPeer == null) {
            _OutString("VideoOpenResponse: Not Requested");
            return PG_ERR_NoExist;
        }

        int iVideoStatus = iStreamMode > 0 ? oPeer.largeVideoMode :oPeer.smallVideoMode;
        int iHandle = iStreamMode > 0 ? oPeer.largeVideoRequestHandle:oPeer.smallVideoRequestHandle;

        if(iVideoStatus != VIDEO_PEER_MODE_Response || iHandle <= 0){
            return PG_ERR_BadStatus;
        }

        String sObjV = iStreamMode > 0 ? group.sObjLV : group.sObjV;

        String sData  = "(Peer){" + m_Node.omlEncode(sObjPeer) + "}(Wnd){" + sEndEle + "}";

        int iErrRes =  m_Node.ObjectExtReply(sObjV, iErr, sData, iHandle);
        if(iErrRes > PG_ERR_Normal){
            _OutString("VideoOpenResponse: Node.ObjectExtReply iErr = " + iErr);
            return iErr;
        }
        oPeer.VideoJoined(iStreamMode,group.peerHeartbeatStamp,sEndEle);
        return iErrRes;
    }

    /**
     * 描述：关闭某一成员视频
     * 阻塞方式：非阻塞，立即返回
     * @param sConfName 会议名称
     * @param sPeer 成员节点名
     * @param iStreamMode 视频流选择 0：默认视频流 1：额外的视频流
     * @return error code @link pgLibError.java
     */
    public void VideoClose(String sConfName,String sPeer,int iStreamMode){
        if(_isEmpty(sConfName) || _isEmpty(sPeer)){
            return ;
        }
        if(m_Node == null) {
            return ;
        }

        Group group = m_GroupList._GroupSearch(sConfName);
        if(group == null){
            _OutString("VideoClose: No Conference.");
            return ;
        }
        String sObjPeer = _ObjPeerBuild(sPeer);

        VideoPeer oPeer = group.videoPeerList._VideoPeerSearch(sObjPeer);
        if (oPeer == null) {
            _OutString("VideoClose: Not Requested");
            return ;
        }

        String sObjV = iStreamMode > 0 ? group.sObjLV : group.sObjV;
        String sData = "(Peer){" + this.m_Node.omlEncode(sObjPeer) + "}";
        m_Node.ObjectRequest(sObjV, PG_METH_VIDEO_Leave, sData, "VideoClose:" + sObjPeer);
        oPeer.VideoLeave(iStreamMode);
    }


    /**
     * 描述：控制成员的视频流
     * 阻塞方式：非阻塞，立即返回
     *
     * @param sConfName 会议名称
     * @param sPeer 成员节点名
     * @param iStreamMode 视频流选择 0：默认视频流 1：额外的视频流
     * @param sPeer 节点ID或对象
     * @param bEnable 是否接收和发送视频流
     * @return error code @link pgLibError.java
     */
    public int VideoControl(String sConfName,String sPeer,int iStreamMode, boolean bEnable){
        if(_isEmpty(sConfName) || _isEmpty(sPeer)){
            return PG_ERR_BadParam;
        }

        if(m_Node == null) {
            return PG_ERR_System;
        }

        Group group = m_GroupList._GroupSearch(sConfName);
        if(group == null){
            _OutString("VideoControl: No Conference.");
            return PG_ERR_NoExist;
        }
        String sObjPeer = _ObjPeerBuild(sPeer);

        int iFlag = bEnable ? 1 : 0;
        String sObjV = group._VideoObjectGet(iStreamMode);

        String sIn = "(Peer){" + m_Node.omlEncode(sObjPeer) + "}(Local){" + iFlag + "}(Remote){" + iFlag + "}";
        return m_Node.ObjectRequest(sObjV, PG_METH_VIDEO_Transfer, sIn, "VideoControl");

    }


    /**
     * 描述：抓拍 sObjPeer 节点的图片
     * 阻塞方式：非阻塞，立即返回
     *
     * @param sConfName 会议名称
     * @param sPeer 成员节点名
     * @param iStreamMode 视频流选择 0：默认视频流 1：额外的视频流
     * @param sPath 路径
     * @return error code @link pgLibError.java
     */
    public int VideoCamera(String sConfName,String sPeer,int iStreamMode, String sPath){
        if(_isEmpty(sConfName) || _isEmpty(sPeer) || _isEmpty(sPath)){
            return PG_ERR_BadParam;
        }

        if(m_Node == null) {
            return PG_ERR_System;
        }

        Group group = m_GroupList._GroupSearch(sConfName);
        if(group == null){
            _OutString("VideoCamera: No Conference.");
            return PG_ERR_NoExist;
        }
        String sObjPeer = _ObjPeerBuild(sPeer);
        String sPathTemp = sPath;
        if (sPathTemp.lastIndexOf(".jpg") < 0 && sPathTemp.lastIndexOf(".JPG") < 0) {
            sPathTemp += ".jpg";
        }

        String sObjV = group._VideoObjectGet(iStreamMode);

        String sIn = "(Peer){" + m_Node.omlEncode(sObjPeer) + "}(Path){" + m_Node.omlEncode(sPathTemp) + "}";
        return m_Node.ObjectRequest(sObjV, PG_METH_VIDEO_Camera, sIn, "VideoCamera:" + sObjPeer);
    }

    /**
     * 描述：开始播放或采集音频
     * 阻塞方式：非阻塞，立即返回
     * @param sConfName 会议名称
     * @param sAudioParam 音频初始化参数：(Flag){0}
     *     Flag ：0 初始化音频控制正常对讲；1 初始化音频控制自己静音 ；2 初始化音频控制静音其他成员； 3 初始化音频控制不接收音频也不发送音频。
     * @return error code @link pgLibError.java
     */
    public int AudioStart(String sConfName,String sAudioParam){
        if(_isEmpty(sConfName)){
            return PG_ERR_BadParam;
        }

        if(m_Node == null) {
            return PG_ERR_System;
        }

        Group group = m_GroupList._GroupSearch(sConfName);
        if(group == null){
            _OutString("AudioStart: No Conference.");
            return PG_ERR_NoExist;
        }
        group.sAudioParam = sAudioParam;
        return _AudioInit(group);
    }

    /**
     * 描述：停止播放或采集音频
     * 阻塞方式：非阻塞，立即返回
     * @param sConfName 会议名称
     */
    public void AudioStop(String sConfName){
        if(_isEmpty(sConfName)){
            return;
        }

        if(m_Node == null) {
            return;
        }

        Group group = m_GroupList._GroupSearch(sConfName);
        if(group == null){
            _OutString("AudioStop: No Conference.");
            return;
        }
        _AudioClean(group);
    }

    /**
     * 启用或禁用音频输入的静音。
     * @param sConfName 会议名称
     * @param iValue 1为启用，0为禁用。
     * @return 错误码 @link PG_ERR_
     */
    public int AudioMuteInput(String sConfName,int iValue){
        if(_isEmpty(sConfName)){
            return PG_ERR_BadParam;
        }

        if(m_Node == null) {
            return PG_ERR_System;
        }

        Group group = m_GroupList._GroupSearch(sConfName);
        if(group == null){
            _OutString("AudioMuteInput: No Conference.");
            return PG_ERR_NoExist;
        }
        String sData = "(Item){12}(Value){"+iValue+"}";

        return m_Node.ObjectRequest(group.sObjA, PG_METH_COMMON_SetOption, sData, "AudioMuteInput");
    }

    /**
     * 启用或禁用音频输出的静音。
     * @param iValue 1为启用，0为禁用。
     * @return 错误码 @link PG_ERR_
     */
    public int AudioMuteOutput(String sConfName,int iValue){
        if(_isEmpty(sConfName)){
            return PG_ERR_BadParam;
        }

        if(m_Node == null) {
            return PG_ERR_System;
        }

        Group group = m_GroupList._GroupSearch(sConfName);
        if(group == null){
            _OutString("AudioMuteInput: No Conference.");
            return PG_ERR_NoExist;
        }
        String sData = "(Item){13}(Value){"+iValue+"}";

        return m_Node.ObjectRequest(group.sObjA, PG_METH_COMMON_SetOption, sData, "AudioMuteOutput");
    }

    /**
     * 描述：控制某个节点是否能播放本节点的音频，本节点能否播放对方的音频
     * 阻塞方式：非阻塞，立即返回
     *
     * @param sConfName 会议名称
     * @param sPeer ：节点名
     * @param bSendEnable : true接收 ，false不接收
     * @param bRecvEnable  返回值： true 操作成功，false 操作失败
     */
    public int AudioSpeech(String sConfName,String sPeer, boolean bSendEnable, boolean bRecvEnable){
        if(_isEmpty(sConfName) || _isEmpty(sPeer)){
            return PG_ERR_BadParam;
        }

        if(m_Node == null) {
            return PG_ERR_System;
        }

        Group group = m_GroupList._GroupSearch(sConfName);
        if(group == null){
            _OutString("AudioMuteInput: No Conference.");
            return PG_ERR_NoExist;
        }
        String sObjPeer = _ObjPeerBuild(sPeer);

        boolean bRet = false;
        int iSendEnable = bSendEnable ? 1 : 0;
        int iRecvEnable = bRecvEnable ? 1 : 0;
        String sData = "(Peer){" + m_Node.omlEncode(sObjPeer) + "}(ActSelf){" + iSendEnable + "}(ActPeer){" + iRecvEnable + "}";
        return m_Node.ObjectRequest(group.sObjA, 36, sData, "AudioSpeech");
    }

    /**
     * 开始录制视频，要求：视频通话正在进行。
     * @param sConfName 会议名称
     * @param sPeer 录制端ID，ID为本身则录制本端视频
     * @param iStreamMode 视频流选择 0：默认视频流 1：额外的视频流
     * @param sAviPath 视频保存路径
     * @param iMode 录制模式，0 同时录制视音频；1 只录制视频；2 只录制音频
     * @return 错误码 @link pgLibError
     */
    public int RecordStart(String sConfName,String sPeer, int iStreamMode, String sAviPath, int iMode){
        if(_isEmpty(sConfName) || _isEmpty(sPeer)){
            return PG_ERR_BadParam;
        }

        if(m_Node == null) {
            return PG_ERR_System;
        }

        Group group = m_GroupList._GroupSearch(sConfName);
        if(group == null){
            _OutString("AudioMuteInput: No Conference.");
            return PG_ERR_NoExist;
        }
        String sObjPeer = _ObjPeerBuild(sPeer);
        String sObjVideo = group._VideoObjectGet(iStreamMode);
        String sObjAudio = group.sObjA;
        int iHas = (iMode == PG_RECORD_NORMAL ||iMode == PG_RECORD_ONLYVIDEO_HASAUDIO ||iMode == PG_RECORD_ONLYAUDIO_HASVIDEO)?1:0;


        boolean bRecord = false;
        if (iMode == PG_RECORD_NORMAL ||iMode == PG_RECORD_ONLYVIDEO ||iMode == PG_RECORD_ONLYVIDEO_HASAUDIO) {
            String sIn = "(Peer){" + m_Node.omlEncode(sObjPeer) + "}(Path){" + m_Node.omlEncode(sAviPath) + "}(HasAudio){" + iHas + "}";
            int iErr = m_Node.ObjectRequest(sObjVideo,
                    PG_METH_VIDEO_Record, sIn, "RecordStartVideo");
            if (iErr > PG_ERR_Normal) {
                _OutString("RecordStartVideo: iErr=" + iErr);
                return iErr;
            }
            bRecord = true;
        }

        if (iMode == PG_RECORD_NORMAL ||iMode == PG_RECORD_ONLYAUDIO||iMode == PG_RECORD_ONLYAUDIO_HASVIDEO) {

            String sIn = "(Peer){" + m_Node.omlEncode(sObjPeer) + "}(Path){" + m_Node.omlEncode(sAviPath) + "}(HasVideo){" + iHas + "}";

            int iErr = m_Node.ObjectRequest(sObjAudio,
                    PG_METH_AUDIO_Record, sIn, "RecordStartAudio");
            if (iErr > PG_ERR_Normal) {
                m_Node.ObjectRequest(sObjVideo, PG_METH_VIDEO_Record, "(Path){}", "RecordStopVideo");
                _OutString("RecordStartAudio: iErr=" + iErr);
                return iErr;
            }
            bRecord = true;
        }

        if (bRecord) {
            _RecordListAdd(iMode,sPeer);
        }

        return PG_ERR_Normal;
    }

    /**
     * 停止录制
     * @param sConfName 会议名称
     * @param sPeer 录制端ID，ID为本身则录制本端视频
     * @param iStreamMode 视频流选择 0：默认视频流 1：额外的视频流
     * @param iMode 录制模式，0 同时录制视音频；1 只录制视频；2 只录制音频
     */
    public void RecordStop(String sConfName,String sPeer, int iStreamMode,int iMode){
        if(_isEmpty(sConfName) || _isEmpty(sPeer)){
            return;
        }

        if(m_Node == null) {
            return;
        }

        Group group = m_GroupList._GroupSearch(sConfName);
        if(group == null){
            _OutString("AudioMuteInput: No Conference.");
            return;
        }

        String sRec = _RecordListSearch(iMode,sPeer);
        if ("".equals(sRec)) {
            return;
        }
        String sObjPeer = _ObjPeerBuild(sPeer);
        String sObjVideo = group._VideoObjectGet(iStreamMode);
        String sObjAudio = group.sObjA;

        if (iMode == PG_RECORD_NORMAL ||iMode == PG_RECORD_ONLYVIDEO ||iMode == PG_RECORD_ONLYVIDEO_HASAUDIO) {
            int iErr = m_Node.ObjectRequest(sObjVideo,
                    PG_METH_VIDEO_Record, "(Peer){" + m_Node.omlEncode(sObjPeer) + "}(Path){}", "RecordStopVideo");
            if (iErr > PG_ERR_Normal) {
                _OutString("RecordStopVideo: iErr=" + iErr);
            }
        }

        if (iMode == PG_RECORD_NORMAL ||iMode == PG_RECORD_ONLYAUDIO ||iMode == PG_RECORD_ONLYAUDIO_HASVIDEO) {
            int iErr = m_Node.ObjectRequest(sObjAudio,
                    PG_METH_AUDIO_Record, "(Peer){" + m_Node.omlEncode(sObjPeer) + "}(Path){}",
                    "RecordStopAudio");
            if (iErr > PG_ERR_Normal) {
                _OutString("RecordStopAudio: iErr=" + iErr);
            }
        }

        _RecordListDelete(iMode,sPeer);
    }


    /**
     * 添加文件传输通道
     * @param sTargePeer 对端ID
     * @param sSourcePeer 本端ID
     */
    public int FileAdd(String sTargePeer,String sSourcePeer){
        // Add file object.
//        if(!m_Group.bChairman) {
//            if (!_FileListAdd(m_Group.sChair, m_Self.sUser,m_Group.bChairman)) {
//                break;
//            }
//        }
        return 0;
    }

    /**
     * 删除文件传输通道
     * @param sTargePeer 对端ID
     * @param sSourcePeer 本端ID
     */
    public int FileDelete(String sTargePeer,String sSourcePeer){
        return 0;
    }


    /**
     * 上传文件请求
     * @param sTargePeer 对端ID
     * @param sSourcePeer 本端ID
     * @param sPath 本地文件路径
     * @param sPeerPath 建议对端文件保存路径
     * @return 错误码
     */
    public int FilePutRequest(String sTargePeer,String sSourcePeer, String sPath, String sPeerPath){
        return 0;
    }

    /**
     * 下载文件请求
     * @param sTargePeer 对端ID
     * @param sSourcePeer 本端ID
     * @param sPath 本地文件保存路径
     * @param sPeerPath 对端文件路径
     * @return 错误码
     */
    public int FileGetRequest(String sTargePeer,String sSourcePeer, String sPath, String sPeerPath){
        return 0;
    }

    /**
     * 接受文件传输请求
     * @param sTargePeer 对端ID
     * @param sSourcePeer 成员端或对端ID
     * @param sPath 文件路径（空为默认值，下载请求时下载文件路径，上传请求时上传文件保存路径）
     * @return 错误码
     */
    public int FileAccept(String sTargePeer,String sSourcePeer, String sPath){
        return 0;
    }


    /**
     * 拒绝文件传输
     * @param sTargePeer 对端ID
     * @param sTargePeer 本端ID
     * @param iErrCode 错误码
     * @return 错误码
     */
    public int FileReject(String sTargePeer,String sSourcePeer, int iErrCode){
        return 0;
    }

    /**
     * 取消文件传输
     * @param sTargePeer 对端ID
     * @param sTargePeer 本端ID
     * @return 错误码
     */
    public int FileCancel(String sTargePeer,String sSourcePeer){
        return 0;
    }


    //=====================================

    /**
     * 基于Peer构建ObjPeer
     * @param sPeer 节点名称
     * @return 节点对象名称
     */
    public static String _ObjPeerBuild(String sPeer) {
        if (sPeer.indexOf(ID_PREFIX) != 0) {
            return ID_PREFIX + sPeer;
        }
        return sPeer;
    }

    /**
     * 基于节点对象解析节点名称
     * @param sObjPeer 节点对象
     * @return 节点名称
     */
    public static String _ObjPeerParsePeer(String sObjPeer) {
        int ind = sObjPeer.indexOf(ID_PREFIX);
        if (ind == 0) {
            return sObjPeer.substring(5);
        }
        return sObjPeer;
    }

    public static String _GroupBuildObject(String sGroup) {
        String sObjVideo = ("_G_" + sGroup);
        if (sObjVideo.length() > 127) {
            _OutString("_GroupBuildObject: '" + sObjVideo + "' to long !");
        }
        return sObjVideo;
    }

    public static  boolean _GroupObjectIs(String sObject) {
        return (sObject.indexOf("_G_") == 0);
    }

    public static  String _GroupObjectParseGroup(String sObject) {
        return _GroupObjectIs(sObject) ?sObject.substring(3) : sObject;
    }
    public static String _DataBuildObject(String sGroup) {
        String sObjVideo = ("_G_" + sGroup);
        if (sObjVideo.length() > 127) {
            _OutString("_GroupBuildObject: '" + sObjVideo + "' to long !");
        }
        return sObjVideo;
    }

    public static  boolean _DataObjectIs(String sObject) {
        return (sObject.indexOf("_G_") == 0);
    }

    public static  String _DataObjectParseGroup(String sObject) {
        return _DataObjectIs(sObject) ?sObject.substring(3) : sObject;
    }

    public static  String _PrvwBuild() {
        return "Prvw";
    }

    public static String _VideoBuildObject(String sGroup) {
        String sObjVideo = ("_V_" + sGroup);
        if (sObjVideo.length() > 127) {
            _OutString("_VideoBuildObject: '" + sObjVideo + "' to long !");
        }
        return sObjVideo;
    }

    public static boolean _VideoObjectIs(String sObject) {
        return (sObject.indexOf("_V_") == 0);
    }

    public static String _VideoObjectParseGroup(String sObject) {
        return _VideoObjectIs(sObject) ?sObject.substring(3) : sObject;
    }

    public static String _VideoLBuildObject(String sGroup) {
        String sObjVideo = ("_LV_" + sGroup);
        if (sObjVideo.length() > 127) {
            _OutString("_FileBuildObject: '" + sObjVideo + "' to long !");
        }
        return sObjVideo;
    }

    public static boolean _VideoLObjectIs(String sObject) {
        return (sObject.indexOf("_LV_") == 0);
    }

    public static String _VideoLObjectParseGroup(String sObject) {
        return _VideoLObjectIs(sObject) ?sObject.substring(4) : sObject;
    }
    public static String _AudioBuildObject(String sGroup) {
        String sObjVideo = ("_A_" + sGroup);
        if (sObjVideo.length() > 127) {
            _OutString("_FileBuildObject: '" + sObjVideo + "' to long !");
        }
        return sObjVideo;
    }

    public static boolean _AudioObjectIs(String sObject) {
        return (sObject.indexOf("_A_") == 0);
    }

    private String _AudioObjectParseGroup(String sObject) {
        return _AudioObjectIs(sObject) ?sObject.substring(3) : sObject;
    }

//    private void _LanScanResult(String sData) {
//        if (m_Node == null) {
//            return;
//        }
//
//        m_LanScan.sLanScanRes = "";
//
//        int iInd = 0;
//        while (true) {
//            String sEle = m_Node.omlGetEle(sData, "PeerList.", 1, iInd);
//            if ("".equals(sEle)) {
//                break;
//            }
//
//            String sPeer = m_Node.omlGetName(sEle, "");
//            int iPos = sPeer.indexOf(ID_PREFIX);
//            if (iPos == 0) {
//                String sAddr = m_Node.omlGetContent(sEle, ".Addr");
//                if (m_LanScan.bApiLanScan) {
//                    String sID = sPeer.substring(5);
//                    String sDataTemp = "id=" + sID + "&addr=" + sAddr;
//                    _OnEvent(EVENT_LAN_SCAN_RESULT, sDataTemp, "");
//                }
//                m_LanScan.sLanScanRes += ("(" + sPeer + "){" + sAddr + "}");
//            }
//
//            iInd++;
//        }
//
//        if (!m_Status.bLogined) {
//            _ChairPeerStatic();
//        }
//
//        m_LanScan.bApiLanScan = false;
//    }

//    private void _TimerOutKeep() {
//        //OutString("->Keep TimeOut");
//
//        if (m_Node != null) {
//
//            if (!m_Status.bServiceStart || m_Stamp.iExpire == 0 || m_Group.bEmpty) {
//                m_Stamp.iKeepStamp = 0;
//                m_Stamp.iKeepChainmanStamp = 0;
//                m_Stamp.iRequestChainmanStamp = 0;
//                _SyncPeerClean();
//                return;
//            }
//
//            m_Stamp.iKeepStamp += KEEP_TIMER_INTERVAL;
//
//            m_Group.iKeepTimer = TimerStartKeep();
//
//            //取消心跳的接收和发送
//            if (m_Group.bChairman) {
//
//                //如果是主席，主动给所有成员发心跳
//                int i = 0;
//                while (i < m_listSyncPeer.size()) {
//                    PG_SYNC oSync = m_listSyncPeer.get(i);
//
//                    // 超过3倍心跳周期，没有接收到成员端的心跳应答，说明成员端之间连接断开了
//                    if ((m_Stamp.iKeepStamp - oSync.iKeepStamp) > (m_Stamp.iExpire * 3)) {
//                        _OnEvent(EVENT_PEER_OFFLINE, "reason=1", oSync.sObjPeer);
//                        PeerDelete(oSync.sObjPeer);
//                        _SyncPeerDelete(oSync);
//                        continue;
//                    }
//
//                    // 每个心跳周期发送一个心跳请求给成员端
//                    if ((m_Stamp.iKeepStamp - oSync.iRequestStamp) >= m_Stamp.iExpire) {
//                        m_Node.ObjectRequest(oSync.sObjPeer, 36, "Keep?", "MessageSend");
//                        oSync.iRequestStamp = m_Stamp.iKeepStamp;
//                    }
//
//                    i++;
//                }
//            } else {
//                // 超过3倍心跳周期，没有接收到主席端的心跳请求，说明主席端之间连接断开了
//                if ((m_Stamp.iKeepStamp - m_Stamp.iKeepChainmanStamp) > (m_Stamp.iExpire * 3)) {
//
//                    // 每个心跳周期尝试一次连接主席端
//                    if ((m_Stamp.iKeepStamp - m_Stamp.iRequestChainmanStamp) >= m_Stamp.iExpire) {
//                        m_Stamp.iRequestChainmanStamp = m_Stamp.iKeepStamp;
//                        _ChairmanAdd();
//                    }
//                }
//            }
//        }
//    }

    private int TimerStartKeep() {
        String sTimerParam = "(Act){"+TIME_OUT_ACT_KEEP+"}";
        return  m_Timer.timerStart(sTimerParam, KEEP_TIMER_INTERVAL, false);
    }

    private int TimerStartActive() {
        String sTimerParam = "(Act){"+TIME_OUT_ACT_TIMER_ACTIVE+"}";
        return  m_Timer.timerStart(sTimerParam, ACTIVE_TIMER_INTERVAL, false);
    }

    private int TimerStartCheckChairPeer(int iDelay) {
        String sTimerParam = "(Act){"+TIME_OUT_ACT_CHAIR_PEER_CHECK+"}";
        return  m_Timer.timerStart(sTimerParam, iDelay, false);
    }
    private int TimerStartChairAdd(int iDelay) {
        String sTimerParam = "(Act){"+TIME_OUT_ACT_CHAIRMAN_ADD+"}";
        return  m_Timer.timerStart(sTimerParam, iDelay, false);
    }
    private int TimerStartRelogin(int iDelay) {
        String sTimerParam = "(Act){"+TIME_OUT_ACT_RELOGIN+"}";
        return  m_Timer.timerStart(sTimerParam, iDelay, false);
    }

    private void TimerStartPeerGetInfo(String sObjPeer) {
        String sTimerParam = "(Act){"+TIME_OUT_ACT_PEER_GET_INFO+"}(Peer){" + sObjPeer + "}(Report){1}";
        m_Timer.timerStart( sTimerParam , 5, false);
    }
    //事件下发程序
    private void _OnEvent(String sAct, String sData, String sPeer,String sGroup,String sEventParam) {
        if (m_eventListener != null && m_bEventEnable) {
            //OutString("EventProc: sAct=" + sAct + ", sData=" + sData + ", sObjPeer=" + sObjPeer);
            m_eventListener.event(sAct, sData, sPeer, sGroup, sEventParam);
        }
    }

    // Set capture extend option.
    //摄像头参数设置
    private void _VideoOption(String sVideoParam) {
        if (m_Node != null) {
            int iVideoFrmRate = _ParseInt(m_Node.omlGetContent(sVideoParam, "FrmRate"), 0);
            int iVideoBitRate = _ParseInt(m_Node.omlGetContent(sVideoParam, "BitRate"), 0);
            int bVideoPortrait = _ParseInt(m_Node.omlGetContent(sVideoParam, "Portrait"), 0);
            int bVideoRotate = _ParseInt(m_Node.omlGetContent(sVideoParam, "Rotate"), 0);
            int iCameraNo = _ParseInt(m_Node.omlGetContent(sVideoParam, "CameraNo"), 0);

            if (m_Node.ObjectAdd("_vTemp", "PG_CLASS_Video", "", 0)) {
                if (iVideoFrmRate != 0) {
                    m_Node.ObjectRequest("_vTemp", PG_METH_COMMON_SetOption, "(Item){4}(Value){" + iVideoFrmRate + "}", "");

                    String sParam = "(FrmRate){" + iVideoFrmRate + "}(KeyFrmRate){4000}";
                    m_Node.ObjectRequest("_vTemp", PG_METH_COMMON_SetOption, "(Item){5}(Value){" + m_Node.omlEncode(sParam) + "}", "");
                }
                if (iVideoBitRate != 0) {
                    String sParam = "(BitRate){" + iVideoBitRate + "}";
                    m_Node.ObjectRequest("_vTemp", PG_METH_COMMON_SetOption, "(Item){5}(Value){" + m_Node.omlEncode(sParam) + "}", "");
                }
                if (bVideoPortrait != 0) {
                    int angle = bVideoPortrait * 90;
                    m_Node.ObjectRequest("_vTemp", PG_METH_COMMON_SetOption, "(Item){2}(Value){" + angle + "}", "");
                } else if (bVideoRotate != 0) {
                    m_Node.ObjectRequest("_vTemp", PG_METH_COMMON_SetOption, "(Item){2}(Value){" + bVideoRotate + "}", "");
                }
                if (iCameraNo == Camera.CameraInfo.CAMERA_FACING_FRONT
                        || iCameraNo == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    m_Node.ObjectRequest("_vTemp", PG_METH_COMMON_SetOption, "(Item){0}(Value){" + iCameraNo + "}", "");
                }
                m_Node.ObjectDelete("_vTemp");
            }
        }
    }

    //外部采集设置
    private void _NodeOptionExter(String sInitParam) {
        _OutString("._NodeOptionExter");
        if (m_Node != null) {
            int iVideoInExternal = _ParseInt(m_Node.omlGetContent(sInitParam, "VideoInExternal"), 0);
            int iInputExternal = _ParseInt(m_Node.omlGetContent(sInitParam, "InputExternal"), 0);
            int iOutputExternal = _ParseInt(m_Node.omlGetContent(sInitParam, "OutputExternal"), 0);
            int iOutputExtCmp = _ParseInt(m_Node.omlGetContent(sInitParam, "OutputExtCmp"), 0);

            if (iVideoInExternal != 0 || iInputExternal != 0 || iOutputExternal != 0 || iOutputExtCmp != 0) {
                if (m_Node.ObjectAdd("_vTemp", "PG_CLASS_Video", "", 0)) {

                    if (iVideoInExternal != 0 || iInputExternal != 0) {
                        m_Node.ObjectRequest("_vTemp", PG_METH_COMMON_SetOption, "(Item){8}(Value){1}", "");
                    }

                    if (iOutputExtCmp != 0) {
                        m_Node.ObjectRequest("_vTemp", PG_METH_COMMON_SetOption, "(Item){13}(Value){1}", "");
                    } else if (iOutputExternal != 0) {
                        m_Node.ObjectRequest("_vTemp", PG_METH_COMMON_SetOption, "(Item){11}(Value){1}", "");
                    }

                    m_Node.ObjectDelete("_vTemp");
                }
            }
        }
    }

    private String _NodeInitConfigParserControl(String sInitParam){
        String sControl = "Type=1;PumpMessage=1;LogLevel0=1;LogLevel1=1";
        int Type = 1;
        int PumpMessage = 1;

        int LogLevel0 = _ParseInt(m_Node.omlGetContent(sInitParam, "LogLevel0"), 1);
        int LogLevel1 = _ParseInt(m_Node.omlGetContent(sInitParam, "LogLevel1"), 1);
        int LogLevel2 = _ParseInt(m_Node.omlGetContent(sInitParam, "LogLevel2"), 0);
        int LogLevel3 = _ParseInt(m_Node.omlGetContent(sInitParam, "LogLevel3"), 0);
        sControl = "Type=" + Type
                + ";PumpMessage=" + PumpMessage
                + ";LogLevel0=" + LogLevel0
                + ";LogLevel1=" + LogLevel1
                + ";LogLevel2=" + LogLevel2
                + ";LogLevel3=" + LogLevel3;

        return sControl;
    }
    private String _NodeInitConfigParserNode(String sInitParam){
        String sNodeCfg;
        /**
         * 节点类型。不建议修改
         */
        int Type = 0;
        /**
         * 不建议修改
         * Option：本节点实例的选项，分别为以下的掩码组合：
         * 0x01：启用网络异常时自动重新尝试登录（客户端有效）
         * 0x02：启用集群模式的P2P穿透握手机制（服务器端有效）
         * 0x04：启用踢出重复登录的用户功能（服务器端有效）
         * 0x08：启用节点协助转发功能的握手功能（服务器端有效）
         */
        int Option = 1;

        /**
         * 节点对象的最大数目，取值范围：1 ~ 32768
         */
        int MaxPeer =  _ParseInt(m_Node.omlGetContent(sInitParam, "MaxPeer"), 256);
        /**
         * 组对象的最大数目，取值范围：1 ~ 32768
         */
        int MaxGroup =  _ParseInt(m_Node.omlGetContent(sInitParam, "MaxGroup"), 32);
        /**
         * 对象的最大数目，取值范围：1 ~ 65534
         */
        int MaxObject =  _ParseInt(m_Node.omlGetContent(sInitParam, "MaxObject"), 512);
        /**
         * 组播句柄的最大数目，取值范围：1 ~ 65534
         */
        int MaxMCast =  _ParseInt(m_Node.omlGetContent(sInitParam, "MaxMCast"), 512);
        /**
         * 常驻接口事件队列的最大长度，取值范围：1 ~ 65534
         */
        int MaxHandle = _ParseInt(m_Node.omlGetContent(sInitParam, "MaxHandle"), 256);
        /**
         * 消息流的Socket队列长度（报文个数），取值范围：1 ~ 32768
         */
        int SKTBufSize0 = _ParseInt(m_Node.omlGetContent(sInitParam, "SKTBufSize0"), 128);
        /**
         * 音频流的Socket队列长度（报文个数），取值范围：1 ~ 32768
         */
        int SKTBufSize1 = _ParseInt(m_Node.omlGetContent(sInitParam, "SKTBufSize1"), 64);
        /**
         * 视频流的Socket队列长度（报文个数），取值范围：1 ~ 32768
         */
        int SKTBufSize2 = _ParseInt(m_Node.omlGetContent(sInitParam, "SKTBufSize2"), 64);
        /**
         * 文件流的Socket队列长度（报文个数），取值范围：1 ~ 32768
         */
        int SKTBufSize3 = _ParseInt(m_Node.omlGetContent(sInitParam, "SKTBufSize3"),64);
        /**
         * P2P尝试时间。
         */
        int P2PTryTime = _ParseInt(m_Node.omlGetContent(sInitParam, "P2PTryTime"),3);


        sNodeCfg = "Type=" + Type +
                ";Option=" + Option +
                ";MaxPeer=" + MaxPeer +
                ";MaxGroup=" + MaxGroup +
                ";MaxObject=" + MaxObject +
                ";MaxMCast=" + MaxMCast +
                ";MaxHandle=" + MaxHandle +
                ";SKTBufSize0=" + SKTBufSize0 +
                ";SKTBufSize1=" + SKTBufSize1 +
                ";SKTBufSize2=" + SKTBufSize2 +
                ";SKTBufSize3=" + SKTBufSize3 +
                ";P2PTryTime=" + P2PTryTime;
        return sNodeCfg;
    }

    //设置Node 上线参数
    private boolean _NodeStart() {
        _OutString("->NodeStart");
        if (m_Node != null) {
            String sSvrName = m_sSvrName = m_sInitSvrName;
            String sSvrAddr = m_sSvrAddr = m_sInitSvrAddr;
            String sRelayAddr = m_sRelayAddr;
            int Digest = _ParseInt(m_Node.omlGetContent(m_sInitParam, "Digest"),1);
//            m_Svr.init(m_InitSvr.sSvrName, m_InitSvr.sSvrAddr, m_InitSvr.sRelayAddr);
            // Config jni node.
            m_Node.Control = _NodeInitConfigParserControl(m_sInitParam);
            m_Node.Node = _NodeInitConfigParserNode(m_sInitParam);
            m_Node.Class = "PG_CLASS_Data:128;PG_CLASS_Video:128;PG_CLASS_Audio:128;PG_CLASS_File:128";

            if(m_LocalAddr.isEmpty()) {
                m_Node.Local = "Addr=0:0:0:127.0.0.1:0:0";
            }else{
                m_Node.Local = m_LocalAddr;
            }

            m_Node.Server = "Name=" + sSvrName + ";Addr=" + sSvrAddr + ";Digest=" + Digest ;
            m_Node.NodeProc = m_NodeThreadProc;
            if (!"".equals(sRelayAddr)) {
                m_Node.Relay = "(Relay0){(Type){0}(Load){0}(Addr){" + sRelayAddr + "}}";
            } else {
                int iInd = sSvrAddr.lastIndexOf(':');
                if (iInd > 0) {
                    String sSvrIP = sSvrAddr.substring(0, iInd);
                    m_Node.Relay = "(Relay0){(Type){0}(Load){0}(Addr){" + sSvrIP + ":443}}";
                }
            }

            // Start atx node.
            if (!m_Node.Start(0)) {
                _OutString("NodeStart: Start node failed.");
                return false;
            }

            m_bEventEnable = true;
            // Login to server.
            if (_NodeLogin()!=PG_ERR_Normal) {
                _OutString("NodeStart: failed.");
                _NodeStop();
                return false;
            }

            // Enable LAN scan.
            String sValue = "(Enable){1}(Peer){" + m_Node.omlEncode(sSvrName) + "}(Label){pgConf}";
            String sData = "(Item){1}(Value){" + m_Node.omlEncode(sValue) + "}";
            int iErr = m_Node.ObjectRequest(sSvrName, PG_METH_COMMON_SetOption, sData, "EnableLanScan");
            if (iErr > 0) {
                _OutString("NodeStart: Enable lan scan failed. iErr=" + iErr);
            }

            return true;
        }
        return false;
    }

    //停止Node联网
    private void _NodeStop() {
        if (m_Node != null) {
            _NodeLogout();
//            m_bEventEnable = false;
            m_Node.Stop();
        }
    }
    private int _NodeLogin() {
        String sVersion = "";
        String sVerTemp = m_Node.omlGetContent(m_Node.utilCmd("Version", ""), "Version");
        if (sVerTemp.length() > 1) {
            sVersion = sVerTemp.substring(1);
        }

        String sObjSelf = m_sObjSelf;
        String sPass = m_sPass;
        String sSvrName = m_sSvrName;

        String sParam = "(Ver){" + sVersion + "." + LIB_VER + "}";
        _OutString("_NodeLogin: Version=" + sParam);

        String sData = "(User){" + m_Node.omlEncode(sObjSelf) + "}(Pass){"
                + m_Node.omlEncode(sPass) + "}(Param){" + m_Node.omlEncode(sParam) + "}";
        int iErr = m_Node.ObjectRequest(sSvrName, PG_METH_PEER_Login, sData, PARAM_LOGIN);
        if (iErr > PG_ERR_Normal) {
            _OutString("_NodeLogin: Login failed. iErr=" + iErr);
            return iErr;
        }

        return PG_ERR_Normal;
    }

    private void _NodeLogout() {
        String sSvrName = m_sSvrName;

        m_Node.ObjectRequest(sSvrName, PG_METH_PEER_Logout, "", PARAM_LOGOUT);
        if (m_bLogined) {
            _OnEvent(EVENT_LOGOUT, "", "","","");
            m_bLogined = false;
        }
    }

    private void _NodeRelogin(int uDelay) {
        _NodeLogout();
        _NodeTimerRelogin(uDelay);
    }

    private void _NodeRedirect(String sRedirect) {

        _NodeLogout();

        String sSvrName_old = m_sSvrName;


        String sSvrName = m_Node.omlGetContent(sRedirect, "SvrName");
        if (!"".equals(sSvrName) && !sSvrName.equals(sSvrName_old)) {
            m_Node.ObjectDelete(sSvrName_old);
            if (!m_Node.ObjectAdd(sSvrName, PG_CLASS_Peer, "", (PG_ADD_COMMON_Sync | PG_ADD_PEER_Server))) {
                _OutString("_NodeRedirect: Add server object failed");
                return;
            }
            m_sSvrName = sSvrName;
//            m_sSvrAddr = "";
        }
        String sSvrAddr_old = m_sSvrAddr;
        String sSvrAddr = m_Node.omlGetContent(sRedirect, "SvrAddr");
        if (!"".equals(sSvrAddr) && !sSvrAddr.equals( sSvrAddr_old )) {
            String sData = "(Addr){" + sSvrAddr + "}(Proxy){}";
            int iErr = m_Node.ObjectRequest( sSvrName, PG_METH_PEER_SetAddr , sData, PARAM_SVR_REDIRECT);

            if (iErr > 0) {
                _OutString("_NodeRedirect: Set server address. iErr=" + iErr);
                return;
            }
            m_sSvrAddr = sSvrAddr;
        }

        _OutString("_NodeRedirect: sSvrName=" + sSvrName + ", sSvrAddr=" + sSvrAddr);

        _NodeTimerRelogin(1);
    }

    private void _NodeRedirectReset(int uDelay) {
        if (!m_sSvrAddr.equals(m_sInitSvrName)) {
            String sRedirect = "(SvrName){" + m_sInitSvrName
                    + "}(SvrAddr){" + m_sInitSvrAddr + "}";
            _NodeRedirect(sRedirect);
        }
        else {
            if (uDelay != 0) {
                _NodeRelogin(uDelay);
            }
        }
    }

    private int _NodeLoginReply(int uErr, String sData) {
        if (uErr != PG_ERR_Normal) {
            _OutString("_NodeLoginReply: Login failed. uErr=" + uErr);

            _OnEvent(EVENT_LOGIN, ("" + uErr), "","","");

            if (uErr == PG_ERR_Network
                    || uErr == PG_ERR_Timeout
                    || uErr == PG_ERR_Busy)
            {
                int iDelay = _NodeLoginFailDelay();
                _NodeRedirectReset(iDelay);
            }
            else {
                _NodeRelogin(m_iLoginDelayMax * 10);
            }

            return 0;
        }

        String sParam = m_Node.omlGetContent(sData, "Param");
        String sRedirect = m_Node.omlGetEle(sParam, "Redirect.", 10, 0);
        if (!"".equals(sRedirect)) {
            _NodeRedirect(sRedirect);
            return 1;
        }

        m_iLoginFailCount = 0;
        m_bLogin = true;

        _OnEvent(EVENT_LOGIN, "" + PG_ERR_Normal, "","","");
        return 1;
    }

    private int _NodePeerGetInfo(String sPeer,String sReport) {
        String sParam = PARAM_PEER_GET_INFO;
        if("0".equals(sReport)){
            sParam = PARAM_PEER_GET_INFO_NO_REPORT;
        }


        int iErr = m_Node.ObjectRequest(sPeer, PG_METH_PEER_GetAddr , "", sParam);
        if (iErr > PG_ERR_Normal) {
            _OutString("_NodePeerGetInfo: iErr=" + iErr);
        }
        return iErr;
    }

    private int _NodeLoginFailDelay() {
        int iDelay = m_iLoginFailCount * 10;
        if (m_iLoginFailCount < m_iLoginDelayMax) {
            m_iLoginFailCount++;
        }
        return ((iDelay > 0) ? iDelay : 1);
    }

    private void _NodeTimerRelogin(int iDelay) {
        if (m_iIDTimerRelogin >= 0) {
            m_Timer.timerStop(m_iIDTimerRelogin);
            m_iIDTimerRelogin = -1;
        }

        m_iIDTimerRelogin = TimerStartRelogin(iDelay);
    }


    // 建立通讯组 视音频通讯类
    private int _ServiceStart(Group group) {
        if(group == null) {
            return PG_ERR_BadParam;
        }

        group.videoPeerList._VideoPeerClean();
        group.syncPeerList._SyncPeerClean();
        do {
            if (group.isChairman()) {
                int iFlagGroup = PG_ADD_COMMON_Sync | PG_ADD_GROUP_Update | PG_ADD_GROUP_Refered | PG_ADD_GROUP_Modify | PG_ADD_GROUP_Offline;
                if (!m_Node.ObjectAdd(group.sObjG, PG_CLASS_Group, "", iFlagGroup)) {
                    _OutString("ServiceStart: Add group object failed");
                    break;
                }

                int iMask = 0x0200; // Tell all.
                String sDataMdf = "(Action){1}(PeerList){(" + m_sObjSelf + "){" + iMask + "}}";
                int iErr = m_Node.ObjectRequest(group.sObjG, PG_METH_GROUP_Modify, sDataMdf, "");
                if (iErr > 0) {
                    _OutString("ServiceStart: Add group Chairman failed");
                    break;
                }
            } else {
                int iFlagGroup = PG_ADD_COMMON_Sync | PG_ADD_GROUP_Update | PG_ADD_GROUP_Modify | PG_ADD_GROUP_Offline;
                if (!m_Node.ObjectAdd(group.sObjG, PG_CLASS_Group, group.sObjChair, iFlagGroup)) {
                    _OutString("ServiceStart: Add group object failed");
                    break;
                }
                _ChairmanAdd();
            }

            if (!m_Node.ObjectAdd(group.sObjD, PG_CLASS_Data, group.sObjG, 0)) {
                _OutString("ServiceStart: Add  Data object failed");
                break;
            }


//        // 开始节点连接状态检测定时器。
//        m_Group.iKeepTimer = TimerStartKeep();
//        m_Stamp.iKeepStamp = 0;
//
//        // 成员端检测主席端的状态时戳
//        m_Stamp.iKeepChainmanStamp = 0;
//        m_Stamp.iRequestChainmanStamp = 0;
        }while (false);
        _ServiceStop(group);
        return PG_ERR_System;
    }

    //视音频去同步 会议去同步
    private void _ServiceStop(Group group) {
        if(group == null) {
            _OutString(".ServiceStop: group == null ");
            return;
        }


        if (m_Node != null) {
//            if(!m_Group.bChairman) {
//                _FileListDelete(m_Group.sChair, m_Self.sUser);
//            }
//            if (m_Group.iKeepTimer > 0) {
//                TimerStop(m_Group.iKeepTimer);
//            }

            String sDataMdf = "(Action){0}(PeerList){(" + m_Node.omlEncode(m_sObjSelf) + "){0}}";
            m_Node.ObjectRequest(group.sObjG, PG_METH_GROUP_Modify , sDataMdf, "");

            m_Node.ObjectDelete(group.sObjD);
            m_Node.ObjectDelete(group.sObjG);
            if (!group.isChairman()) {
                _ChairmanDel();
            }
        }
    }

//    //视频开始后的心跳检测可发送
//    private void _TimerActive() {
////        OutString(" ->TimerActive TimeOut");
//        if (!m_Status.bApiVideoStart) {
//            m_Stamp.iActiveStamp = 0;
//            return;
//        }
//
//        m_Stamp.iActiveStamp += ACTIVE_TIMER_INTERVAL;
//
//        m_Group.iActiveTimer = TimerStartActive();
//
//
//        _LostPeerClean();
//
//        for (int i = 0; i < m_listVideoPeer.size(); i++) {
//
//            PG_PEER oPeer = m_listVideoPeer.get(i);
//            if(oPeer.bModeL == VIDEO_PEER_MODE_Leave && oPeer.bMode == VIDEO_PEER_MODE_Leave){
//                m_listVideoPeer.remove(oPeer);
//                continue;
//            }
//            if ((!oPeer.sObjPeer.equals(m_Self.sObjSelf)) && (oPeer.View != null)) {
//
//                // 超过3倍心跳周期，没有接收到对端的心跳应答，说明与对端之间连接断开了
//                if ((m_Stamp.iActiveStamp - oPeer.iActStamp) > (m_Stamp.iActiveExpire * 3) && (!oPeer.bVideoLost)) {
//                    _LostPeerAdd(oPeer.sObjPeer);
//                    oPeer.bVideoLost = true;
//                }
//
//                // 每个心跳周期发送一个心跳请求给对端
//                if ((m_Stamp.iActiveStamp - oPeer.iRequestStamp) >= m_Stamp.iActiveExpire) {
//                    m_Node.ObjectRequest(oPeer.sObjPeer, PG_METH_PEER_Message , "Active?", "pgLibConference.MessageSend");
//                    oPeer.iRequestStamp = m_Stamp.iActiveStamp;
//                }
//            }
//        }
//
//        for ( int i = 0;i < m_listLostPeer.size();i++) {
//            _OnEvent(EVENT_VIDEO_LOST, "", m_listLostPeer.get(i));
//        }
//    }

//    private boolean _KeepAdd(String sObjPeer) {
//        // 添加
//        _OutString("->KeepAdd");
//        PG_SYNC oSync = _SyncPeerSearch(sObjPeer);
//        if (oSync == null) {
//            boolean bRet = _SyncPeerAdd(sObjPeer, m_Stamp.iKeepStamp);
//            if(!bRet) return bRet;
//        }
//        m_Node.ObjectRequest(sObjPeer, PG_METH_PEER_Message , "Keep?", "pgLibConference.MessageSend");
//        return true;
//    }
//
//    private void _KeepDel(String sObjPeer) {
//        //作为成员端只接受主席端心跳 删除
//        _OutString("->KeepDel");
//        _SyncPeerDelete(sObjPeer);
//    }
//
//    //收到Keep 处理
//    private void _KeepRecv(String sObjPeer) {
//        _OutString("->KeepRecv sObjPeer=" + sObjPeer);
//
//        if (m_Status.bServiceStart) {
//
//            if (m_Group.bChairman) {
//                PG_SYNC oSync = _SyncPeerSearch(sObjPeer);
//                if (oSync != null) {
//                    oSync.iKeepStamp = m_Stamp.iKeepStamp;
//                } else {
//                    _KeepAdd(sObjPeer);
//                    _OnEvent(EVENT_PEER_SYNC, "reason=1", sObjPeer);
//                }
//            } else {
//                m_Node.ObjectRequest(sObjPeer, PG_METH_PEER_Message , "Keep?", "pgLibConference.MessageSend");
//                m_Stamp.iKeepChainmanStamp = m_Stamp.iKeepStamp;
//            }
//
//        }
//    }
//
//    //成员端登录后与主席端连接保存
//    private void _TimerOutKeep() {
//        //OutString("->Keep TimeOut");
//
//        if (m_Node != null) {
//
//            if (!m_Status.bServiceStart || m_Stamp.iExpire == 0 || m_Group.bEmpty) {
//                m_Stamp.iKeepStamp = 0;
//                m_Stamp.iKeepChainmanStamp = 0;
//                m_Stamp.iRequestChainmanStamp = 0;
//                _SyncPeerClean();
//                return;
//            }
//
//            m_Stamp.iKeepStamp += KEEP_TIMER_INTERVAL;
//
//            m_Group.iKeepTimer = TimerStartKeep();
//
//            //取消心跳的接收和发送
//            if (m_Group.bChairman) {
//
//                //如果是主席，主动给所有成员发心跳
//                int i = 0;
//                while (i < m_listSyncPeer.size()) {
//                    PG_SYNC oSync = m_listSyncPeer.get(i);
//
//                    // 超过3倍心跳周期，没有接收到成员端的心跳应答，说明成员端之间连接断开了
//                    if ((m_Stamp.iKeepStamp - oSync.iKeepStamp) > (m_Stamp.iExpire * 3)) {
//                        _OnEvent(EVENT_PEER_OFFLINE, "reason=1", oSync.sObjPeer);
//                        PeerDelete(oSync.sObjPeer);
//                        _SyncPeerDelete(oSync);
//                        continue;
//                    }
//
//                    // 每个心跳周期发送一个心跳请求给成员端
//                    if ((m_Stamp.iKeepStamp - oSync.iRequestStamp) >= m_Stamp.iExpire) {
//                        m_Node.ObjectRequest(oSync.sObjPeer, 36, "Keep?", "MessageSend");
//                        oSync.iRequestStamp = m_Stamp.iKeepStamp;
//                    }
//
//                    i++;
//                }
//            } else {
//                // 超过3倍心跳周期，没有接收到主席端的心跳请求，说明主席端之间连接断开了
//                if ((m_Stamp.iKeepStamp - m_Stamp.iKeepChainmanStamp) > (m_Stamp.iExpire * 3)) {
//
//                    // 每个心跳周期尝试一次连接主席端
//                    if ((m_Stamp.iKeepStamp - m_Stamp.iRequestChainmanStamp) >= m_Stamp.iExpire) {
//                        m_Stamp.iRequestChainmanStamp = m_Stamp.iKeepStamp;
//                        _ChairmanAdd();
//                    }
//                }
//            }
//        }
//    }
//



    //添加主席节点  使之能在加入会议前与主席通信，发送Join信号
    private void _ChairmanAdd(Group group) {
        if(group == null) {
            return;
        }

        if(m_Node == null){
            return;
        }

        if (PG_CLASS_Peer.equals(m_Node.ObjectGetClass(group.sObjChair))) {
            _PeerSync(group.sObjChair, "", 1);
            _ChairPeerCheck();
        } else {
            if (!this.m_Node.ObjectAdd(group.sObjChair, PG_CLASS_Peer, "", PG_ADD_COMMON_Sync )) {
                _OutString("ChairmanAdd:  failed.");
            }
        }
    }

    private void _PeerSync(String sObject, String sPeer, int uAction) {
        _OutString(" ->PeerSync Act=" + uAction);
        if (m_Node != null) {
            uAction = (uAction <= 0) ? 0 : 1;
            try {
                m_Node.ObjectSync(sObject, sPeer, uAction);
            } catch (Exception ex) {
                _OutString("->PeerSync ex = " + ex.toString());
            }
        }
    }

    //删除主席节点  使能在添加主席节点失败后能重新添加
    private void _ChairmanDel(Group group) {

        if(group == null) {
            _OutString(" .ChairmanDel : group == null");
            return;
        }

        if(m_Node == null){
            _OutString(" .ChairmanDel : m_Node == null");
            return;
        }

        try {
            this.m_Node.ObjectDelete(group.sObjChair);
        } catch (Exception ex) {
            _OutString(".ChairmanDel ex = " + ex.toString());
        }

    }


    //自身登录事件处理
    private void _OnSelfSync(String sData, String sObjPeer) {
        String sAct = this.m_Node.omlGetContent(sData, "Action");
        if ("1".equals(sAct)) {
            if (sObjPeer.equals(m_sSvrName)) {
                TimerStartPeerGetInfo(sObjPeer);
            }
        } else {
            if (sObjPeer.equals(m_sSvrName)) {
                this._NodeRelogin(10);
            }
        }
    }


    private int _OnRpcRequest(String sData, String sObjPeer, int iHandle) {

        String sCmd = "";
        String sParam;
        int iInd = sData.indexOf('?');
        if (iInd > 0) {
            sCmd = sData.substring(0, iInd);
            sParam = sData.substring(iInd + 1);
        } else {
            sParam = sData;
        }
        if ("UMessage".equals(sCmd)) {
            _OutString("_OnRpcRequest.");
            this._OnEvent(EVENT_RPC_REQUEST, sParam, sObjPeer,"","");
        }
        m_Node.ObjectExtReply(sObjPeer, 0, m_RpcResponseBuff, iHandle);

        return 1;
    }


    private void _OnJoinRequest(String sParam, String sObjPeer){
        String sObjPeerRoute = m_Node.omlGetContent(sParam,"ObjPeer");
        String sConfName = m_Node.omlGetContent(sParam,"ConfName");

        this._OnEvent(EVENT_JOIN_REQUEST, "", sObjPeerRoute ,sConfName,"");
    }

    private void _OnMessageSend(String sParam, String sObjPeer){
          this._OnEvent(EVENT_MESSAGE, sParam, sObjPeer,"","");
    }

    private void _OnVideoHeartBeat(String sParam, String sObjPeer){
        String sConfName = m_Node.omlGetContent(sParam,"ConfName");
        Group group = m_GroupList._GroupSearch(sConfName);
        if(group == null){
            return;
        }
        VideoPeer videoPeer = group.videoPeerList._VideoPeerSearch(sObjPeer);

//        if (m_Status.bServiceStart) {
//            PG_PEER oPeer = _VideoPeerSearch(sObjPeer);
//            if (oPeer != null) {
//                oPeer.iActStamp = m_Stamp.iActiveStamp;
//                oPeer.bVideoLost = false;
//            }
//        }
    }

    //自身消息处理
    private int _OnSelfMessage(String sData, String sObjPeer) {

        String sCmd = "";
        String sParam;
        int iInd = sData.indexOf('?');
        if (iInd > 0) {
            sCmd = sData.substring(0, iInd);
            sParam = sData.substring(iInd + 1);
        } else {
            sParam = sData;
        }

        if ("JoinRequest".equals(sCmd)) {
            _OnJoinRequest(sParam,sObjPeer);
        } else if ("UMessage".equals(sCmd)) {
            _OnMessageSend(sParam,sObjPeer);
        } else if ("VHBeat".equals(sCmd)) {
            _OnVideoHeartBeat(sParam,sObjPeer);
        } else if ("PHBeat".equals(sCmd)) {
//            _KeepRecv(sObjPeer);
        }
        return 0;
    }


    private void  _OnNodeRestart(String sParam){
        if (sParam.contains("redirect=1")) {
            _NodeRedirectReset(3);
        } else {
            int iDelay = 3;
            int iInd1 = sParam.indexOf("delay=");
            if (iInd1 >= 0) {
                // Skip the leng of "delay="
                String sValue = sParam.substring(iInd1 + 6);
                int iValue = _ParseInt(sValue, 3);
                iDelay = (iValue < 3) ? 3 : iValue;
            }
            _NodeRelogin(iDelay);
        }
    }
    //服务器消息处理
    private int _OnServerMessage(String sData, String sObjPeer) {
        _OutString("->ServerMessage");
        String sCmd = "";
        String sParam;
        int iInd = sData.indexOf('?');
        if (iInd > 0) {
            sCmd = sData.substring(0, iInd);
            sParam = sData.substring(iInd + 1);
        } else {
            sParam = sData;
        }

        if ("UserExtend".equals(sCmd)) {
            this._OnEvent(EVENT_SVR_NOTIFY, sParam, sObjPeer,"","");
            return 0;
        }
        if ("Restart".equals(sCmd)) {
            _OnNodeRestart(sParam);
            return 0;
        }


        return 0;
    }

    private void _OnServerKickOut(String sData) {
        String sParam = m_Node.omlGetContent(sData, "Param");
        _OnEvent(EVENT_KICK_OUT, sParam, "","","");
    }

    private int _OnServerError(String sData, String sPeer) {
        String sMeth = m_Node.omlGetContent(sData, "Meth");
        if ("32".equals(sMeth)) {
            String sError = m_Node.omlGetContent(sData, "Error");
            if (sError.equals("" + PG_ERR_NoLogin)) {
                _NodeRelogin(3);
            } else if (sError.equals("" + PG_ERR_Network)
                    || sError.equals("" + PG_ERR_Timeout)
                    || sError.equals("" + PG_ERR_Busy)) {
                _NodeRedirectReset(0);
            }
        }

        return 0;
    }

    private int _OnServerRelogin(String sData, String sPeer) {
        String sError = m_Node.omlGetContent(sData, "ErrCode");
        if (sError.equals("" + PG_ERR_Normal)) {
            String sParam = m_Node.omlGetContent(sData, "Param");
            String sRedirect = m_Node.omlGetEle(sParam, "Redirect.", 10, 0);
            if (!"".equals(sRedirect)) {
                _NodeRedirect(sRedirect);
                return 0;
            }

            m_iLoginFailCount = 0;
            m_bLogin = true;
            _OnEvent(EVENT_LOGIN, "0", "","","");
        } else if (sError.equals("" + PG_ERR_Network)
                || sError.equals("" + PG_ERR_Timeout)
                || sError.equals("" + PG_ERR_Busy)) {
            _NodeRedirectReset(0);

            m_bLogin = false;
            _OnEvent(EVENT_LOGIN, sError, "","","");
        } else {
            m_bLogin = false;
            _OnEvent(EVENT_LOGIN, sError, "","","");
        }

        return 0;
    }

//    private void _OnServerSync(String sData) {
//        String sAct = m_Node.omlGetContent(sData, "Action");
//        if (!"1".equals(sAct)) {
//            _NodeRelogin(3);
//        }
//    }
//
//    private void _OnChairPeerSync(String sObj, String sData){
//        String sAct = this.m_Node.omlGetContent(sData, "Action");
//        if ("1".equals(sAct)) {
//            if (m_bReportPeerInfo) {
//                TimerStartPeerGetInfo(sObj);
//            }
//
//            _KeepAdd(sObj);
//            m_LanScan.bPeerCheckTimer = false;
//            this._OnEvent(EVENT_CHAIRMAN_SYNC, sAct, sObj);
//        }
//    }
//    private void _OnChairPeerError(String sObj, String sData){
//        String sMeth = this.m_Node.omlGetContent(sData, "Meth");
//        if ("34".equals(sMeth)) {
//            String sError = this.m_Node.omlGetContent(sData, "Error");
//
//            _PeerOffline(sObj, sError);
//            _KeepDel(sObj);
//        }
//    }

    private void _OnPeerSync(String sObj, String sData) {
        String sAct = this.m_Node.omlGetContent(sData, "Action");
        if ("1".equals(sAct)) {
            if (m_bReportPeerInfo) {
                TimerStartPeerGetInfo(sObj);
            }
//            //心跳包列表 添加
//            if (!m_Group.bEmpty && m_Group.bChairman) {
//                _KeepAdd(sObj);
//            }
            this._OnEvent(EVENT_PEER_SYNC, sAct, sObj,"","");
        }
    }

    private void _OnPeerError(String sObj, String sData) {

        String sMeth = this.m_Node.omlGetContent(sData, "Meth");
        String sError = this.m_Node.omlGetContent(sData, "Error");
        if ("34".equals(sMeth)) {
//            //心跳包列表 删除
//            if (!m_Group.bEmpty && m_Group.bChairman) {
//                _KeepDel(sObj);
//            }
            _PeerOffline(sObj, sError);
        }
    }

    //peer离线
    private void _PeerOffline(String sObjPeer, String sError) {
        _OutString("->PeerOffline");

        String sAct;
        if (!m_Group.bEmpty && sObjPeer.equals(m_Group.sObjChair)) {
            sAct = EVENT_CHAIRMAN_OFFLINE;
            this._OnEvent(sAct, sError, sObjPeer);
            _ChairPeerStatic();
            if (!m_LanScan.bPeerCheckTimer) {

                if (TimerStartCheckChairPeer(3) >= 0) {
                    m_LanScan.bPeerCheckTimer = true;
                }
            }
        } else {
            sAct = EVENT_PEER_OFFLINE;
            this._OnEvent(sAct, sError, sObjPeer);
        }
    }



    private boolean _ChairPeerAdd(boolean bStatic) {
        if (m_Node == null) {
            return false;
        }
        if (m_Group.bEmpty || m_Group.bChairman) {
            return false;
        }
        if ("".equals(m_Group.sObjChair)) {
            return false;
        }

        m_LanScan.sLanAddr = "";
        m_Node.ObjectDelete(m_Group.sObjChair);

        boolean bAddSuccess = false;
        if (!m_Status.bLogined || bStatic) {
            String sEle = m_Node.omlGetEle(m_LanScan.sLanScanRes, m_Group.sObjChair, 1, 0);
            if (!"".equals(sEle)) {
                if (m_Node.ObjectAdd(m_Group.sObjChair, "PG_CLASS_Peer", "", (0x10000 | 0x4))) {
                    // Set static peer's address.
                    String sAddr = m_Node.omlGetContent(sEle, "");
                    String sData = "(Type){0}(Addr){0:0:0:" + sAddr + ":0}(Proxy){}";
                    if (m_Node.ObjectRequest(m_Group.sObjChair, 37, sData, "pgLibConference.SetAddr") <= 0) {
                        _OutString("PeerAdd: Set '" + m_Group.sObjChair + "' in static.");
                        m_LanScan.sLanAddr = sAddr;
                        bAddSuccess = true;
                    } else {
                        _OutString("PeerAdd: Set '" + m_Group.sObjChair + "' address failed.");
                    }
                } else {
                    _OutString("PeerAdd: Add '" + m_Group.sObjChair + "' with static flag failed.");
                }
            }
        }

        if (!bAddSuccess) {
            if (m_Node.ObjectAdd(m_Group.sObjChair, "PG_CLASS_Peer", "", 0x10000)) {
                _OutString("PeerAdd: Add '" + m_Group.sObjChair + "' without static flag.");
                bAddSuccess = true;
            } else {
                _OutString("PeerAdd: Add '" + m_Group.sObjChair + "' failed.");
            }
        }

        return bAddSuccess;
    }

    private void _ChairPeerCheck() {
        if (m_Node == null) {
            return;
        }

        if (m_Group.bEmpty || m_Group.bChairman) {
            return;
        }

        int iErr = m_Node.ObjectRequest(m_Group.sObjChair, 41, "(Check){1}(Value){3}(Option){}", "");
        if (iErr <= 0) {
            m_Node.ObjectRequest(m_Group.sObjChair, 36, "Keep?", "MessageSend");
            return;
        }
        if (iErr == 5) {
            _ChairPeerAdd(false);
        } else {
            m_Node.ObjectSync(m_Group.sObjChair, "", 1);
        }
    }

    private void _ChairPeerCheckTimeout() {
        _ChairPeerCheck();
        if (m_LanScan.bPeerCheckTimer) {
            TimerStartCheckChairPeer( 5);
        }
        _OutString("ChairPeerCheckTimeout: sObjChair = " + m_Group.sObjChair);
    }

    private void _ChairPeerStatic() {
        if (m_Node == null) {
            return;
        }

        if (!m_Status.bServiceStart) {
            return;
        }

        if (m_Group.bChairman) {
            return;
        }

        String sEle = m_Node.omlGetEle(m_LanScan.sLanScanRes, m_Group.sObjChair, 1, 0);
        if (!"".equals(sEle)) {
            String sAddr = m_Node.omlGetContent(sEle, "");
            if (!sAddr.equals(m_LanScan.sLanAddr)) {
                _ChairPeerAdd(true);
            }
        }
    }

    private void _LanScanResult(String sData) {
        if (m_Node == null) {
            return;
        }

        m_LanScan.sLanScanRes = "";

        int iInd = 0;
        while (true) {
            String sEle = m_Node.omlGetEle(sData, "PeerList.", 1, iInd);
            if ("".equals(sEle)) {
                break;
            }

            String sPeer = m_Node.omlGetName(sEle, "");
            int iPos = sPeer.indexOf(ID_PREFIX);
            if (iPos == 0) {
                String sAddr = m_Node.omlGetContent(sEle, ".Addr");
                if (m_LanScan.bApiLanScan) {
                    String sID = sPeer.substring(5);
                    String sDataTemp = "id=" + sID + "&addr=" + sAddr;
                    _OnEvent(EVENT_LAN_SCAN_RESULT, sDataTemp, "");
                }
                m_LanScan.sLanScanRes += ("(" + sPeer + "){" + sAddr + "}");
            }

            iInd++;
        }

        if (!m_Status.bLogined) {
            _ChairPeerStatic();
        }

        m_LanScan.bApiLanScan = false;
    }
    //---------------------------------------------Group

    private boolean _GroupObjectAdd(String sGroupName,String sGroup,int uFlag){
        if (!m_Node.ObjectAdd(sGroupName, PG_CLASS_Group, sGroup, uFlag)) {
            _OutString("ServiceStart: Add group object failed");
            return false
                    ;
        }

        return true;
    }

    private int _GroupModify(){

        return PG_ERR_Normal;
    }

    //会议成员更新   每加入一个新成员 其他成员获得他的信息  新成员获得其他所有成员的信息
    private void _GroupUpdate(String sData) {
        _OutString("->GroupUpdate");
        if (m_Node != null) {
            String sAct = this.m_Node.omlGetContent(sData, "Action");
            String sPeerList = this.m_Node.omlGetEle(sData, "PeerList.", 256, 0);
            _OutString("GroupUpdate: sAct=" + sAct + " sPeerList=" + sPeerList);
            int iInd = 0;
            while (true) {
                String sEle = this.m_Node.omlGetEle(sPeerList, "", 1, iInd);
                if ("".equals(sEle)) {
                    break;
                }

                String sObjPeer = this.m_Node.omlGetName(sEle, "");
                _OutString("GroupUpdate: sAct=" + sAct + " sObjPeer=" + sObjPeer);
                if (sObjPeer.indexOf(ID_PREFIX) == 0) {
                    String sPeer = _ObjPeerParsePeer(sObjPeer);
                    if ("1".equals(sAct)) {
                        _FileListAdd(m_Group.sChair,sPeer,m_Group.bChairman);
                        _OnEvent(EVENT_JOIN, "", sObjPeer);
                    } else {
                        _FileListDelete(m_Group.sChair,sPeer);
                        _OnEvent(EVENT_LEAVE, "", sObjPeer);
                    }
                }

                iInd++;
            }
        }
    }

    //-------------------------------------------Video

    private String _VideoObjectGet(boolean bLagre){
        String sObjV;
        if (bLagre) {
            sObjV = m_Group.sObjLV;
        } else {
            sObjV = m_Group.sObjV;
        }
        return sObjV;
    }

    private int _VideoFlagGet(int iFlag){
        int uFlag = PG_ADD_COMMON_Sync | PG_ADD_VIDEO_Conference|PG_ADD_VIDEO_FrameStat|PG_ADD_VIDEO_DrawThread;
        switch (iFlag) {
            case VIDEO_ONLY_INPUT: {
                uFlag = uFlag | PG_ADD_VIDEO_OnlyInput;
                break;
            }
            case VIDEO_ONLY_OUTPUT: {
                uFlag = uFlag | PG_ADD_VIDEO_OnlyOutput;
                break;
            }
            case VIDEO_NORMAL:
            default:
        }
        return uFlag;
    }

    private int _VideoViewStart(String sGroup , String sObjV, int uFlag, int iCode, int iMode, int iRate){

        if (!this.m_Node.ObjectAdd(sObjV, PG_CLASS_Video, sGroup, uFlag)) {
            _OutString("pgLibConference.VideoInit: Add 'Video' failed.");
            return PG_ERR_System;
        }

        String sData = "(Code){" + iCode + "}(Mode){" + iMode + "}(Rate){" + iRate+ "}";

        _OutString("VideoInit ->  sData = " + sData);

        return this.m_Node.ObjectRequest(sObjV, 32, sData, "pgLibConference.VideoStart");

    }

    //视频相关初始化
    private boolean _VideoInit(int iFlag) {
        _OutString("->VideoInit iFlag = " + iFlag);

        this._VideoOption();
        if (m_Status.bServiceStart) {

            if (!m_Group.bChairman) {
                _ChairPeerCheck();
            }
            if(iFlag > VIDEO_NORMAL) {
                m_Self.iVideoInitFlag = iFlag;
            }

            int uFlag = _VideoFlagGet(m_Self.iVideoInitFlag);
            int uFlagL = _VideoFlagGet(m_Self.iLVideoInitFlag);
            //预览
            int iPrvwMode =  ((m_Self.iVideoMode>m_Self.iLVideoMode)?(m_Self.iVideoMode):(m_Self.iLVideoMode));
            int iErr = _VideoPrvwStart(iPrvwMode);
            if(iErr>PG_ERR_Normal){
                _OutString("pgLibConference.VideoInit: VideoPrvwStart failed.");
                return false;
            }

            //======================================================================================

            iErr = _VideoViewStart(this.m_Group.sObjG,this.m_Group.sObjV,uFlag,this.m_Self.iVideoCode,this.m_Self.iVideoMode,this.m_Self.iVideoFrmRate);
            if (iErr > 0) {
                _OutString("pgLibConference.VideoInit: VideoViewStart failed. iErr=" + iErr);
                return false;
            }

            iErr = _VideoViewStart(this.m_Group.sObjG,this.m_Group.sObjLV,uFlagL,this.m_Self.iLVideoCode,this.m_Self.iLVideoMode,this.m_Self.iLVideoFrmRate);
            if (iErr > 0) {
                _OutString("pgLibConference.VideoInit: VideoViewStart L failed. iErr=" + iErr);
                return false;
            }

            // 开始视频连接状态检测定时器
            m_Group.iActiveTimer = TimerStartActive();
            m_Stamp.iActiveStamp = 0;
            return true;
        }
        return false;
    }


    private int VideoJoinRequest(String sObjV,String sData,String sParam){

        return m_Node.ObjectRequest(sObjV, PG_METH_VIDEO_Join, sData, sParam);
    }

    private int VideoJoinResponse(String sObjV,int iErr, String sData, int iHandle){
        _OutString("Video open Relay iHandle=" + iHandle);
        return this.m_Node.ObjectExtReply(sObjV, iErr, sData, iHandle);
    }

    private boolean _VideoJoin(String sObjV,String sPeer, int iResErr, String sData, int iHandle){
        boolean bJoinRes = false;
        if (iHandle > 0) {

            int iErrTemp = VideoJoinResponse(sObjV,iResErr,sData,iHandle);
            if (iErrTemp <= 0) {
                if (iResErr == 0) {
                    bJoinRes = true;
                }
            } else {
                _OutString("pgLibConference.VideoOpen: Reply, iErr=" + iErrTemp);
            }
        } else {

            String sParamTmp = "VideoOpen:" + sPeer;
            int iErrTemp = VideoJoinRequest(sObjV,sData,sParamTmp);
            if (iErrTemp <= 0) {
                bJoinRes = true;
            } else {
                _OutString("pgLibConference.VideoOpen: Request, iErr=" + iErrTemp);
            }
        }
        return bJoinRes;
    }

    private SurfaceView VideoOpen(String sPeer, int iW, int iH, boolean bLarge) {
        _OutString("VideoOpen :sObjPeer=" + sPeer + "; iW=" + iW + "; iH=" + iH);
        SurfaceView retView = null;
        PG_PEER oPeer;
        if (m_Status.bApiVideoStart && !"".equals(sPeer)) {
            do {
                String sObjPeer = _ObjPeerBuild(sPeer);
                oPeer = _VideoPeerSearch(sObjPeer);
                if (oPeer == null) {
                    oPeer = _VideoPeerAdd(sObjPeer);
                    if(oPeer ==  null){
                        break;
                    }
                    oPeer.iActStamp = m_Stamp.iActiveStamp;
                }


                String sObjV;
                int iResErr;
                String sData;
                String sEndEle ;
                int iHandle;

                if(!bLarge){
                    sEndEle = oPeer.GetWndEle(iW,iH);
                    sObjV = m_Group.sObjV;
                    iHandle = oPeer.iHandle;
                    retView = oPeer.View;
                    oPeer.iHandle = 0;
                }else{
                    sEndEle = oPeer.GetWndEleL(iW,iH);
                    sObjV = m_Group.sObjLV;
                    iHandle = oPeer.iHandleL;
                    retView = oPeer.ViewL;
                    oPeer.iHandleL = 0;
                }

                //
                if (!"".equals(sEndEle)) {
                    iResErr = PG_ERR_Normal;
                    sData = "(Peer){" + m_Node.omlEncode(sObjPeer) + "}(Wnd){" + sEndEle + "}";
                    _OutString("VideoOpen: sData=" + sData);
                } else {
                    iResErr = PG_ERR_Reject;
                    sData = "";
                    _OutString("pgLibConference.VideoOpen: New node wnd failed!");
                }


                boolean  bJoinRes = _VideoJoin(sObjV,sPeer,iResErr,sData,iHandle);

                if (bJoinRes) {
                    if(iHandle>0) {
                        if (iResErr == PG_ERR_Normal) {
                            if (bLarge) {
                                oPeer.bModeL = VIDEO_PEER_MODE_Join;
                            } else {
                                oPeer.bMode = VIDEO_PEER_MODE_Join;
                            }
                        }
                    }else{
                        if (bLarge) {
                            oPeer.bModeL = VIDEO_PEER_MODE_Request;
                        } else {
                            oPeer.bMode = VIDEO_PEER_MODE_Request;
                        }
                    }
                    _OutString("VideoOpen: scussce");
                }

            } while (false);
        }
        return retView;
    }

    private int VideoOpen(String sPeer, int iDevID, boolean bLarge) {
        _OutString("VideoOpen :sObjPeer=" + sPeer + "; ");
        int iErrRet = PG_ERR_Normal;
        PG_PEER oPeer;
        if (m_Status.bApiVideoStart && !"".equals(sPeer)) {
            do {
                String sObjPeer = _ObjPeerBuild(sPeer);
                oPeer = _VideoPeerSearch(sObjPeer);
                if (oPeer == null) {
                    oPeer = _VideoPeerAdd(sObjPeer);
                    if(oPeer ==  null){
                        iErrRet = PG_ERR_BadParam;
                        break;
                    }
                    oPeer.iActStamp = m_Stamp.iActiveStamp;
                }


                String sObjV;
                int iResErr;
                String sData;
                String sEndEle ;
                int iHandle;

                if(!bLarge){
                    sEndEle = "(PosX){0}(PosY){0}(SizeX){320}(SizeY){240}(Handle){"+ iDevID + "}";
                    sObjV = m_Group.sObjV;
                    iHandle = oPeer.iHandle;

                    oPeer.iHandle = 0;
                }else{
                    sEndEle = "(PosX){0}(PosY){0}(SizeX){320}(SizeY){240}(Handle){"+ iDevID + "}";
                    sObjV = m_Group.sObjLV;
                    iHandle = oPeer.iHandleL;

                    oPeer.iHandleL = 0;
                }

                //
                if (!"".equals(sEndEle)) {
                    iResErr = PG_ERR_Normal;
                    sData = "(Peer){" + m_Node.omlEncode(sObjPeer) + "}(Wnd){" + sEndEle + "}";
                    _OutString("VideoOpen: sData=" + sData);
                } else {
                    iResErr = PG_ERR_Reject;
                    sData = "";
                    _OutString("pgLibConference.VideoOpen: New node wnd failed!");
                }


                boolean  bJoinRes = _VideoJoin(sObjV,sPeer,iResErr,sData,iHandle);

                if (bJoinRes) {
                    if(iHandle>0) {
                        if (iResErr == PG_ERR_Normal) {
                            if (bLarge) {
                                oPeer.bModeL = VIDEO_PEER_MODE_Join;
                            } else {
                                oPeer.bMode = VIDEO_PEER_MODE_Join;
                            }
                        }
                    }else{
                        if (bLarge) {
                            oPeer.bModeL = VIDEO_PEER_MODE_Request;
                        } else {
                            oPeer.bMode = VIDEO_PEER_MODE_Request;
                        }
                    }
                    _OutString("VideoOpen: scussce");
                }

            } while (false);
        }
        return iErrRet;
    }

    private int _VideoLeave(String sObjV, String sObjPeer){
        String sData = "(Peer){" + this.m_Node.omlEncode(sObjPeer) + "}";
        return this.m_Node.ObjectRequest(sObjV, 36, sData, "VideoClose:" + sObjPeer);
    }

    private boolean _VideoClose(PG_PEER oPeer, boolean bLarge) {
        boolean bRet = false;
        if (oPeer != null && m_Status.bApiVideoStart) {

            String sObjV;
            if (bLarge) {
                oPeer.VideoLeaveL();
                sObjV = m_Group.sObjLV;
            } else {
                oPeer.VideoLeave();
                sObjV = m_Group.sObjV;
            }
            int iErr = _VideoLeave(sObjV,oPeer.sObjPeer);
            if (iErr == 0) {
                bRet = true;
                if(oPeer.bMode == VIDEO_PEER_MODE_Leave && oPeer.bModeL == VIDEO_PEER_MODE_Leave){
                    _VideoPeerDelete(oPeer);
                }
            }
        }
        return bRet;
    }

    private void _VideoObjectDelete(String sObjV){
        this.m_Node.ObjectRequest(sObjV, PG_METH_VIDEO_Close, "", "VideoClean:"+sObjV);
        this.m_Node.ObjectDelete(sObjV);
    }

    //视频相关清理
    private void _VideoClean() {
        _OutString("->VideoClean");
        if (m_Status.bApiVideoStart) {
            if (m_Group.iActiveTimer > 0) {
                TimerStop(m_Group.iActiveTimer);
            }

            _VideoObjectDelete(this.m_Group.sObjLV);
            _VideoObjectDelete(this.m_Group.sObjV);

            _VideoObjectDelete(_PrvwBuild());

            _VideoPeerClean();
        }
        m_Status.bApiVideoStart = false;
    }

    private void _OnVideoSync(String sObj , String sData , String sObjPeer){

        String sAct = this.m_Node.omlGetContent(sData, "Action");
        if ("1".equals(sAct)) {
            String sEventAct = EVENT_VIDEO_SYNC;

            if(_VideoObjectIs(sObj)){
                sEventAct = EVENT_VIDEO_SYNC;
            }else if(_VideoLObjectIs(sObj)){
                sEventAct = EVENT_VIDEO_SYNC_1;
            }
            _OnEvent(sEventAct, "", sObjPeer);
        }
    }

    //保存对端视频请求句柄
    private void _OnVideoJoin(String sObj, String sData, int iHandle, String sObjPeer) {
        _OutString("->VideoJoin");

        PG_PEER oPeer = _VideoPeerSearch(sObjPeer);
        if (oPeer == null) {
            oPeer = _VideoPeerAdd(sObjPeer);
            if(oPeer == null){
                VideoJoinResponse(sObj,PG_ERR_System,sData,iHandle);
                return;
            }
            oPeer.iActStamp = m_Stamp.iActiveStamp;
        }

        String sEventAct =  EVENT_VIDEO_OPEN;
        if(_VideoObjectIs(sObj)){
            oPeer.VideoJoin(iHandle,m_iCurStamp);
            sEventAct = EVENT_VIDEO_OPEN;
        }else if(_VideoLObjectIs(sObj)){
            sEventAct = EVENT_VIDEO_OPEN_1;
            oPeer.VideoJoinL(iHandle,m_iCurStamp);
        }
        _OnEvent(sEventAct, sData, sObjPeer);
    }

    private void _OnVideoJoinReply(String sObj, int iErr, String sData, String sParam) {
        //视频加入通知
        String sPeer = sParam.substring(10);
        String sObjPeer = _ObjPeerBuild(sPeer);

        PG_PEER oPeer = _VideoPeerSearch(sObjPeer);
        if (oPeer == null) {
            _VideoLeave(sObj,sObjPeer);
            return;
        }

        String sEventAct = EVENT_VIDEO_JOIN;
        if(_VideoObjectIs(sObj)){
            if(iErr == PG_ERR_Normal) {
                oPeer.bMode = VIDEO_PEER_MODE_Join;
            }
            sEventAct = EVENT_VIDEO_JOIN;
        }else if(_VideoLObjectIs(sObj)){
            if(iErr == PG_ERR_Normal) {
                sEventAct = EVENT_VIDEO_JOIN_1;
            }
            oPeer.bModeL = VIDEO_PEER_MODE_Join;
        }

        this._OnEvent(sEventAct, "" + iErr, sObjPeer );
    }

    private void _OnVideoLeave(String sObj, String sData, int iHandle, String sObjPeer) {
        _OutString("->VideoLeave");
        PG_PEER oCtrl = _VideoPeerSearch(sObjPeer);
        String sEventAct =  EVENT_VIDEO_CLOSE;
        if(_VideoObjectIs(sObj)){
            sEventAct = EVENT_VIDEO_CLOSE;
            if(oCtrl!=null){
                oCtrl.VideoLeave();
            }
        }else if(_VideoLObjectIs(sObj)){
            sEventAct = EVENT_VIDEO_CLOSE_1;

            if(oCtrl!=null){
                oCtrl.VideoLeaveL();
            }
        }

        _OnEvent(sEventAct, sData, sObjPeer);

        if(oCtrl.bMode == VIDEO_PEER_MODE_Leave && oCtrl.bModeL == VIDEO_PEER_MODE_Leave){
            _VideoPeerDelete(oCtrl);
        }
    }

    //上报发送视频帧信息
    private void _VideoFrameStat(String sObj, String sData) {
        //OutString("->VideoFrameStat");
        String sEventAct =  EVENT_VIDEO_FRAME_STAT;
        if(_VideoObjectIs(sObj)){
            sEventAct = EVENT_VIDEO_FRAME_STAT;
        }else if(_VideoLObjectIs(sObj)){
            sEventAct = EVENT_VIDEO_FRAME_STAT_1;
        }
        String sObjPeerTemp = m_Node.omlGetContent(sData, "Peer");
        String sFrmTotal = m_Node.omlGetContent(sData, "Total");
        String sFrmDrop = m_Node.omlGetContent(sData, "Drop");

        _OnEvent(sEventAct, ("total=" + sFrmTotal + "&drop=" + sFrmDrop), sObjPeerTemp);
    }
    //-----------------------------------------------------

    private int _AudioFlagGet(int iFlag){
        int uFlag = PG_ADD_COMMON_Sync| PG_ADD_AUDIO_Conference;
        switch (iFlag) {
            case AUDIO_NO_SPEECH_SELF:
                uFlag = uFlag |  PG_ADD_AUDIO_NoSpeechSelf;
                break;
            case AUDIO_NO_SPEECH_PEER:
                uFlag = uFlag |  PG_ADD_AUDIO_NoSpeechPeer;
                break;
            case AUDIO_NO_SPEECH_SELF_AND_PEER:
                uFlag = uFlag | PG_ADD_AUDIO_NoSpeechSelf | PG_ADD_AUDIO_NoSpeechPeer;
                break;
            case AUDIO_SPEECH:
            default:
                break;
        }
        return uFlag;
    }

    private int _AudioOpen(String sGroup , String sObjA , int uFlag){
        if (!this.m_Node.ObjectAdd(sObjA, "PG_CLASS_Audio", sGroup, uFlag)) {
            _OutString("pgLibConference.m_AudioInit: Add Audio failed.");
            return PG_ERR_System;
        }

        return this.m_Node.ObjectRequest(sObjA, PG_METH_AUDIO_Open , "(Code){1}(Mode){0}", "pgLibConference.AudioInit");
    }

    private void _AudioClose(String sObjA){
        this.m_Node.ObjectRequest(sObjA, PG_METH_AUDIO_Close , "", "pgLibConference.AudioClean");
        this.m_Node.ObjectDelete(sObjA);
    }

    //音频相关初始化
    private boolean _AudioInit() {
        _OutString("->AudioInit");

        int uFlag =  _AudioFlagGet( m_Self.iAudioSpeechDisable);

        int iErr = _AudioOpen(this.m_Group.sObjG,this.m_Group.sObjA,uFlag);
        if (iErr > 0) {
            _OutString("pgLibConference.AudioInit: Open audio failed. iErr=" + iErr);
            return false;
        }
        return true;
    }

    //音频相关清理
    private void _AudioClean() {
        _OutString("->AudioClean");

        _AudioClose(this.m_Group.sObjA);
    }

    private void _OnAudioSync(String sData, String sObjPeer) {
        String sAct = this.m_Node.omlGetContent(sData, "Action");
        if ("1".equals(sAct)) {
            _OnEvent(EVENT_AUDIO_SYNC, "", sObjPeer);
        }
    }

    //------------------------------------------------------
    // Record list handles

    private String m_sListRecord = "";

    private String _RecordListSearch(int iMode,String sID) {
        return m_Node.omlGetEle(m_sListRecord, ("\n*" + iMode + "_" + sID), 1, 0);
    }

    private void _RecordListAdd(int iMode, String sID) {
        String sRec = _RecordListSearch(iMode,sID);
        if ("".equals(sRec)) {
            m_sListRecord += "(" + m_Node.omlEncode(iMode + "_" + sID)
                    + "){}";
        }
    }

    private boolean _RecordListDelete(int iMode,String sID) {
        String sRec = _RecordListSearch(iMode,sID);
        if (!"".equals(sRec)) {
            m_sListRecord = m_Node.omlDeleteEle(m_sListRecord, ("\n*" + iMode + "_" + sID), 1, 0);
            return true;
        }
        return false;
    }

    //------------------------------------------------------
    // File handles.

    private String m_sListFile = "";

    private String _FileBuildObject(String sChairID,String sID) {
        String sObjFile = ("File_" + sChairID + "\n" + sID);
        if (sObjFile.length() > 127) {
            _OutString("_FileBuildObject: '" + sObjFile + "' to long !");
        }
        return sObjFile;
    }

    private boolean _FileObjectIs(String sObject) {
        return (sObject.indexOf("File_") == 0);
    }

    private String _FileObjectParseChairID(String sObject) {

        String sCapRender = "";
        if (sObject.indexOf("File_") == 0) {
            sCapRender = sObject.substring(5);
        }
        int iInd = sCapRender.indexOf("\n");
        if (iInd > 0) {
            return sCapRender.substring(0, iInd);
        }
        return "";

    }
    private String _FileObjectParseMemberID(String sObject) {

        String sCapRender = "";
        if (sObject.indexOf("File_") == 0) {
            sCapRender = sObject.substring(5);
        }
        int iInd = sCapRender.indexOf("\n");
        if (iInd > 0) {
            return sCapRender.substring(iInd+1);
        }
        return "";

    }

    private String _FileListSearch(String sChairID,String sID) {
        return m_Node.omlGetEle(m_sListFile, ("\n*" + sChairID + "_" + sID), 1, 0);
    }

    private boolean _FileListAdd(String sChairID,String sID,boolean bChairman) {
        String sFile = _FileListSearch(sChairID,sID);
        if ("".equals(sFile)) {
            m_sListFile += "(" + m_Node.omlEncode(sChairID + "_" + sID) + "){(Status){0}(Handle){0}}";
        }

        String sObjFile = _FileBuildObject(sChairID , sID);

        if (!"PG_CLASS_File".equals(m_Node.ObjectGetClass(sObjFile))) {
            String sObj = bChairman?_ObjPeerBuild(sID):_ObjPeerBuild(sChairID);
            if (!m_Node.ObjectAdd(sObjFile, "PG_CLASS_File", sObj, 0x10000)) {
                _OutString("_FileListAdd: Add '" + sObjFile + "' failed!");
                return false;
            }
        }

        return true;
    }

    private boolean _FileListDelete(String sChairID,String sID) {
        String sObjFile = _FileBuildObject(sChairID,sID);

        m_Node.ObjectRequest(sObjFile, 35, "", "");
        m_Node.ObjectDelete(sObjFile);

        String sFile = _FileListSearch(sChairID,sID);
        if (!"".equals(sFile)) {
            m_sListFile = m_Node.omlDeleteEle(m_sListFile, ("\n*" + sChairID + "_" + sID), 1, 0);
            return true;
        }

        return false;
    }

    private boolean _FileListSet(String sChairID,String sID, String sItem, String sValue) {
        String sFile = _FileListSearch(sChairID,sID);
        if (!"".equals(sFile)) {
            String sPath = "\n*" +  sChairID + "_" + sID + "*" + sItem;
            m_sListFile = m_Node.omlSetContent(m_sListFile, sPath, sValue);
            return true;
        }
        return false;
    }

    private String _FileListGet(String sChairID,String sID, String sItem) {
        String sPath = "\n*" + sChairID + "_" + sID + "*" + sItem;
        return m_Node.omlGetContent(m_sListFile, sPath);
    }

    private int _FileRequest(String sChairID,String sID, String sPath, String sPeerPath, int iMethod) {
        if ("1".equals(_FileListGet(sChairID,sID, "Status"))) {
            return PG_ERR_Opened;
        }

        String sData = "(Path){" + m_Node.omlEncode(sPath) + "}(PeerPath){"
                + m_Node.omlEncode(sPeerPath) + "}(TimerVal){1}(Offset){0}(Size){0}";

        String sParam = (iMethod == 32) ? "FilePutRequest" : "FileGetRequest";

        String sObjFile = _FileBuildObject(sChairID,sID);
        int iErr =  m_Node.ObjectRequest(sObjFile, iMethod, sData, sParam);
        if (iErr > PG_ERR_Normal) {
            _OutString("_FileRequest: iMethod=" + iMethod + ", iErr=" + iErr);
            return iErr;
        }

        _FileListSet(sChairID,sID, "Status", "1");
        return iErr;
    }

    private int _FileReply(int iErrReply, String sChairID,String sID, String sPath) {

        String sData = "";
        if (iErrReply != PG_ERR_Normal) {
            _FileListSet(sChairID,sID, "Status", "0");
        }
        else {
            _FileListSet(sChairID,sID, "Status", "1");
            sData = "(Path){" + m_Node.omlEncode(sPath) + "}(TimerVal){1}";
        }

        _OutString("_FileReply: iErrReply=" + iErrReply + ", sChairID=" + sChairID + ", sData=" + sData);

        String sHandle = _FileListGet(sChairID,sID, "Handle");
        _OutString("_FileReply: sHandle=" + sHandle);

        int iHandle = _ParseInt(sHandle, 0);
        if (iHandle == 0) {
            _FileListSet(sChairID, sID, "Status", "0");
            return PG_ERR_BadStatus;
        }


        String sObjFile = _FileBuildObject(sChairID,sID);
        int iErr = m_Node.ObjectExtReply(sObjFile, iErrReply, sData, iHandle);
        if (iErr <= PG_ERR_Normal) {
            _FileListSet(sChairID,sID, "Handle", "0");
        }

        _OutString("_FileReply: iErr=" + iErr);
        return iErr;
    }

    private int _FileCancel(String sChairID,String sID) {

        String sObjFile = _FileBuildObject(sChairID,sID);
        int iErr = m_Node.ObjectRequest(sObjFile, 35, "", "FileCancel");
        if (iErr <= PG_ERR_Normal) {
            _FileListSet(sChairID,sID, "Status", "0");
        }

        return iErr;
    }

    private int _OnFileRequest(String sObj, int iMethod, String sData, int iHandle) {
        String sChairID = _FileObjectParseChairID(sObj);
        String sID = _FileObjectParseMemberID(sObj);

        if ("1".equals(_FileListGet(sChairID, sID, "Status"))) {
            return PG_ERR_BadStatus;
        }

        _FileListSet(sChairID,sID,"Handle", (iHandle + ""));
        _FileListSet(sChairID,sID, "Status", "1");

        _OutString("_OnFileRequest: sData=" + sData);

        String sPeerPath = m_Node.omlGetContent(sData, "PeerPath");
        String sParam = "peerpath=" + sPeerPath;

        if (iMethod == 32) {
            _OnEvent(EVENT_FILE_PUT_REQUEST, sParam, sID);
        }
        else if (iMethod == 33) {
            _OnEvent(EVENT_FILE_GET_REQUEST, sParam, sID);
        }

        return -1; // Async reply
    }


    private int _OnFileStatus(String sObj, String sData) {
        String sChairID = _FileObjectParseChairID(sObj);
        String sID = _FileObjectParseMemberID(sObj);

        String sStatus = m_Node.omlGetContent(sData, "Status");
        int iStatus = _ParseInt(sStatus, -1);
        if (iStatus != 3) {
            String sPath = m_Node.omlGetContent(sData, "Path");
            String sReqSize = m_Node.omlGetContent(sData, "ReqSize");
            String sCurSize = m_Node.omlGetContent(sData, "CurSize");
            String sParam = "path=" + sPath + "&total=" + sReqSize	+ "&position=" + sCurSize;
            _OnEvent(EVENT_FILE_PROGRESS, sParam, sChairID);
        }
        else { // Stop
            _FileListSet(sChairID,sID, "Status", "0");

            String sPath = m_Node.omlGetContent(sData, "Path");
            String sReqSize = m_Node.omlGetContent(sData, "ReqSize");
            String sCurSize = m_Node.omlGetContent(sData, "CurSize");

            String sParam = "path=" + sPath + "&total=" + sReqSize + "&position=" + sCurSize;
            _OnEvent(EVENT_FILE_PROGRESS, sParam, sChairID);

            int iCurSize = _ParseInt(sCurSize, 0);
            int iReqSize = _ParseInt(sReqSize, 0);
            if (iCurSize >= iReqSize && iReqSize > 0) {
                _OnEvent(EVENT_FILE_FINISH, sParam, sChairID);
            }
            else {
                _OnEvent(EVENT_FILE_ABORT, sParam, sChairID);
            }
        }

        return 0;
    }

    private void _OnFileCancel(String sObj) {
        String sRenID = _FileObjectParseChairID(sObj);
        String sID = _FileObjectParseMemberID(sObj);
        if ("".equals(sRenID)) {
            return;
        }

        _FileListSet(sRenID,sID, "Status", "0");
        _OnEvent(EVENT_FILE_ABORT , "", sRenID);
    }
    private void _VideoCameraReply(String sData) {
        if ( m_Node != null) {

            String sObjPeer = m_Node.omlGetContent(sData, "Peer");
            String sPath = m_Node.omlGetContent(sData, "Path");
            _OnEvent(EVENT_VIDEO_CAMERA, sPath, sObjPeer);
        }
    }

    private void _VideoRecordReply(String sData) {
        if (m_Node != null) {
            String sObjPeer = m_Node.omlGetContent(sData, "Peer");
            String sPath = m_Node.omlGetContent(sData, "Path");
            _OnEvent(EVENT_VIDEO_RECORD, sPath, sObjPeer);
        }
    }

    //服务器下发数据
    private void _SvrReply(int iErr, String sData) {
        if (iErr != 0) {
            _OnEvent(EVENT_SVR_REPLYR_ERROR, "" + iErr, m_Svr.sSvrName);
        } else {
            _OnEvent(EVENT_SVR_RELAY, sData, m_Svr.sSvrName);
        }
    }

    private void _OnPrcRelay(String sObj, int iErr, String sParam) {
        String sSession;
        sSession = sParam.substring(9);
        String sPeer = _ObjPeerParsePeer(sObj);
        _OnEvent(EVENT_CALLSEND_RESULT, sSession + ":" + iErr, sPeer);
    }

    private void _OnPeerGetInfoReply(String sObj, int iErr, String sData,boolean bReport) {
        if (iErr != PG_ERR_Normal) {
            return;
        }

        String sPeer = _ObjPeerParsePeer(sObj);


        String sThrough = m_Node.omlGetContent(sData, "Through");
        String sProxy = addrToReadable(m_Node.omlGetContent(sData, "Proxy"));

        String sAddrLcl = addrToReadable(m_Node.omlGetContent(sData, "AddrLcl"));

        String sAddrRmt = addrToReadable(m_Node.omlGetContent(sData, "AddrRmt"));

        String sTunnelLcl = addrToReadable(m_Node.omlGetContent(sData, "TunnelLcl"));

        String sTunnelRmt = addrToReadable(m_Node.omlGetContent(sData, "TunnelRmt"));

        String sPrivateRmt = addrToReadable(m_Node.omlGetContent(sData, "PrivateRmt"));

        String sDataInfo = "";

        if(bReport) {
            sDataInfo = "16:(" + m_Node.omlEncode(sObj) + "){(Through){" + sThrough + "}(Proxy){"
                    + m_Node.omlEncode(sProxy) + "}(AddrLcl){" + m_Node.omlEncode(sAddrLcl) + "}(AddrRmt){"
                    + m_Node.omlEncode(sAddrRmt) + "}(TunnelLcl){" + m_Node.omlEncode(sTunnelLcl) + "}(TunnelRmt){"
                    + m_Node.omlEncode(sTunnelRmt) + "}(PrivateRmt){" + m_Node.omlEncode(sPrivateRmt) + "}}";

            int iErrTemp = m_Node.ObjectRequest(m_Svr.sSvrName, 35, sDataInfo, "ReportPeerInfo");
            if (iErrTemp > PG_ERR_Normal) {
                _OutString("_OnPeerGetInfoReply: iErr=" + iErrTemp);
            }
        }
        // Report to app.
        sDataInfo = "peer=" + sPeer + "&through=" + sThrough + "&proxy=" + sProxy
                + "&addrlcl=" + sAddrLcl + "&addrrmt=" + sAddrRmt + "&tunnellcl=" + sTunnelLcl
                + "&tunnelrmt=" + sTunnelRmt + "&privatermt=" + sPrivateRmt;
        _OnEvent(EVENT_PEER_INFO, sDataInfo, sPeer);
    }


    private int _NodeOnExtRequestVideo(String sObj, int uMeth, String sData, int iHandle, String sObjPeer) {
        //接收视频类方法

        if (uMeth == PG_METH_COMMON_Sync) {
            _OnVideoSync(sObj,sData,sObjPeer);
        } else if (uMeth == PG_METH_VIDEO_Join) {
            _OnVideoJoin(sObj, sData, iHandle, sObjPeer);
            return -1;
        } else if (uMeth == PG_METH_VIDEO_Leave) {
            _OnVideoLeave(sObj, sData, iHandle, sObjPeer);
        } else if (uMeth == PG_METH_VIDEO_FrameStat) {
            _VideoFrameStat(sObj, sData);

        }
        return 0;

    }

    private int _NodeOnExtRequestAudio(String sObj, int uMeth, String sData, int iHandle, String sObjPeer) {
        //音频类相关
        if (!m_Group.bEmpty && sObj.equals(m_Group.sObjA)) {
            if (uMeth == PG_METH_COMMON_Sync) {
                _OnAudioSync(sData,sObjPeer);

            }
        }
        return 0;
    }

    private int _NodeOnExtRequestFile(String sObj, int uMeth, String sData, int iHandle, String sObjPeer) {
        if (_FileObjectIs(sObj)) {
            if (uMeth == PG_METH_FILE_Put) {
                // put file request
                return _OnFileRequest(sObj, uMeth, sData, iHandle);
            }

            if (uMeth == PG_METH_FILE_Get) {
                // get file request
                return _OnFileRequest(sObj, uMeth, sData, iHandle);
            }

            if (uMeth == PG_METH_FILE_Status) {
                // File transfer status report.
                _OnFileStatus(sObj, sData);
                return 0;
            }

            if (uMeth == PG_METH_FILE_Cancel) {
                // Cancel file request
                _OnFileCancel(sObj);
                return 0;
            }

            return 0;
        }

        return 0;
    }

    private int _NodeOnExtRequestData(String sObj, int uMeth, String sData, int iHandle, String sObjPeer) {
        //DData类相关
        if (!m_Group.bEmpty && sObj.equals(this.m_Group.sObjD)) {
            if (uMeth == PG_METH_DATA_Message) {
                this._OnEvent(EVENT_NOTIFY, sData, sObjPeer);
            }
            return 0;
        }
        return 0;
    }

    private int _NodeOnExtRequestGroup(String sObj, int uMeth, String sData, int iHandle, String sObjPeer) {
        if (!m_Group.bEmpty && sObj.equals(m_Group.sObjG)) {
            if (uMeth == PG_METH_GROUP_Update) {
                //成员有更新
                //加入列表，
                this._GroupUpdate(sData);

            }
        }
        return 0;
    }



    private int _NodeOnExtRequestPeer(String sObj, int uMeth, String sData, int iHandle, String sObjPeer) {
        //Peer类相关
        if (sObj.equals(m_Svr.sSvrName)) {
            if (uMeth == PG_METH_COMMON_Sync) {
                _OnServerSync(sData);
            } else if (uMeth == PG_METH_COMMON_Error) {
                _OnServerError(sData, sObjPeer);
            } else if (uMeth == PG_METH_PEER_ReloginReply) {
                _OnServerRelogin(sData, sObjPeer);
            }

        } else if (sObj.equals(m_Self.sObjSelf)) {
            if (uMeth == PG_METH_COMMON_Sync) {
                _OnSelfSync(sData, sObjPeer);
            } else if (uMeth == PG_METH_PEER_Call) {
                return this._OnSelfCall(sData, sObjPeer, iHandle);
            } else if (uMeth == PG_METH_PEER_Message ) {
                if (sObjPeer.equals(m_Svr.sSvrName)) {
                    return this._OnServerMessage(sData, sObjPeer);
                } else {
                    return this._OnSelfMessage(sData, sObjPeer);
                }
            } else if (uMeth == PG_METH_PEER_KickOut) {
                //ID冲突 被踢下线了
                if (sObjPeer.equals(m_Svr.sSvrName)) {
                    _OnServerKickOut(sData);
                }
            }

        } else if (_ObjChairmanIs(sObj)) {

            if (uMeth == PG_METH_COMMON_Sync) {
                _OnChairPeerSync(sObj,sData);
            } else if (uMeth == PG_METH_COMMON_Error) {
                _OnChairPeerError(sObj,sData);

            }

        } else  {
            if (uMeth == PG_METH_COMMON_Sync) {
                _OnPeerSync(sObj, sData);
            } else if (uMeth == PG_METH_COMMON_Error) {
                _OnPeerError(sObj, sData);
            }


        }
        return 0;
    }

    private int _NodeOnReplyVideo(String sObj, int iErr, String sData, String sParam) {
        if (sParam.indexOf(PARAM_PRE_VIDEO_OPEN) == 0) {
            _OnVideoJoinReply(sObj,iErr,sData,sParam);

            return 1;
        }
        if (sParam.indexOf(EVENT_VIDEO_CAMERA) == 0) {
            _VideoCameraReply(sData);
            return 1;
        }
        if (sParam.indexOf(EVENT_VIDEO_RECORD) == 0) {
            _VideoRecordReply(sData);
            return 1;
        }
        return 1;
    }



    private int _NodeOnReplyAudio(String sObj, int iErr, String sData, String sParam) {

        if (PARAM_AUDIO_CTRL_VOLUME.equals(sParam)) {
            // Cancel file
            _OnEvent(EVENT_AUDIO_CTRL_VOLUME, Integer.valueOf(iErr).toString(), sObj);
        }
        return 1;
    }

    private int _NodeOnReplyFile(String sObj, int iErr, String sData, String sParam) {
        if (_FileObjectIs(sObj)) {
            if (PARAM_FILE_GET_REQUEST.equals(sParam)
                    || PARAM_FILE_PUT_REQUEST.equals(sParam))
            {
                String sRenID = _FileObjectParseChairID(sObj);
                String sID = "";
                if (iErr != PG_ERR_Normal) {
                    _FileListSet(sRenID,sID, "Status", "0");
                    _OnEvent(EVENT_FILE_REJECT, (iErr + ""), sRenID);
                    return 1;
                }
                else {
                    _FileListSet(sRenID,sID, "Status", "1");
                    _OnEvent(EVENT_FILE_ACCEPT, "0" , sRenID);
                    return 1;
                }
            }

            return 1;
        }
        return 1;
    }

    private int _NodeOnReplyData(String sObj, int iErr, String sData, String sParam) {
        return 1;
    }

    private int _NodeOnReplyGroup(String sObj, int iErr, String sData, String sParam) {
        return 1;
    }

    private int _NodeOnReplyPeer(String sObj, int iErr, String sData, String sParam) {
        if (sParam.equals(PARAM_PEER_GET_INFO)) {
            _OnPeerGetInfoReply(sObj, iErr, sData,true);
            return 1;
        }
        if (sParam.equals(PARAM_PEER_GET_INFO_NO_REPORT)) {
            _OnPeerGetInfoReply(sObj, iErr, sData,false);
            return 1;
        }
        if (sObj.equals(m_Svr.sSvrName)) {
            if (PARAM_LOGIN.equals(sParam)) {
                int iRet = _NodeLoginReply(iErr, sData);
                if(iRet == 1){
                    _ChairPeerCheck();
                }
            } else if (PARAM_LANSCAN.equals(sParam)) {
                _LanScanResult(sData);
            } else if (PARAM_SVRREQUEST.equals(sParam)) {
                _SvrReply(iErr, sData);
            }

            return 1;
        }



        if (sParam.indexOf(PARAM_PRE_CALLSEND) == 0) {
            _OnPrcRelay(sObj, iErr, sParam);
            return 1;
        }
        return 1;
    }

    //定时器处理程序
    private final pgLibTimer.OnTimeOut timerOut = new pgLibTimer.OnTimeOut() {
        @Override
        public void onTimeOut(String sParam) {
            String sAct = m_Node.omlGetContent(sParam, "Act");
            if (TIME_OUT_ACT_KEEP.equals(sAct)) {
                _TimerOutKeep();
            } else if (TIME_OUT_ACT_TIMER_ACTIVE.equals(sAct)) {
                _TimerActive();
            } else if (TIME_OUT_ACT_CHAIR_PEER_CHECK.equals(sAct)) {
                _ChairPeerCheckTimeout();
            } else if (TIME_OUT_ACT_CHAIRMAN_ADD.equals(sAct)) {
                _ChairmanAdd();
            } else if (TIME_OUT_ACT_RELOGIN.equals(sAct)) {
                m_iIDTimerRelogin = -1;
                _NodeLogin();
            } else if (TIME_OUT_ACT_PEER_GET_INFO.equals(sAct)) {
                String sPeer = m_Node.omlGetContent(sParam, "Peer");
                String sReport = m_Node.omlGetContent(sParam, "Report");
                _NodePeerGetInfo(sPeer,sReport);
            }

        }
    };

    private final pgLibNodeProc mNodeProc = new pgLibNodeProc() {
        @Override
        public int _NodeOnExtRequest(String sObj, int uMeth, String sData, int iHandle, String sObjPeer) {
            if (m_Node != null) {
                String sClass = m_Node.ObjectGetClass(sObj);
                if (!(uMeth == PG_METH_VIDEO_FrameStat && sClass.equals(PG_CLASS_Video))) {
                    _OutString("NodeOnExtRequest: " + sObj + ", " + uMeth + ", " + sData + ", " + sObjPeer);
                }
                try {
                    if (m_eventHook != null) {
                        int iErr = m_eventHook.OnExtRequest(sObj, uMeth, sData, iHandle, sObjPeer);
                        if (iErr != PG_ERR_Unknown) {
                            return iErr;
                        }
                    }
                } catch (Exception ex) {
                    _OutString("pgLibLiveMultiCapture._NodeOnExtRequest: call event hook. ex=" + ex.toString());
                }

                if (PG_CLASS_Peer.equals(sClass)) {
                    return _NodeOnExtRequestPeer(sObj, uMeth, sData, iHandle, sObjPeer);
                }
                if(PG_CLASS_Group.equals(sClass)){
                    return _NodeOnExtRequestGroup(sObj, uMeth, sData, iHandle, sObjPeer);
                }
                if(PG_CLASS_Data.equals(sClass)){
                    return _NodeOnExtRequestData(sObj, uMeth, sData, iHandle, sObjPeer);
                }
                if(PG_CLASS_File.equals(sClass)){
                    return _NodeOnExtRequestFile(sObj, uMeth, sData, iHandle, sObjPeer);
                }
                if(PG_CLASS_Audio.equals(sClass)){
                    return _NodeOnExtRequestAudio(sObj, uMeth, sData, iHandle, sObjPeer);
                }
                if(PG_CLASS_Video.equals(sClass)){
                    return _NodeOnExtRequestVideo(sObj, uMeth, sData, iHandle, sObjPeer);
                }

            }
            return 0;
        }

        @Override
        public int _NodeOnReply(String sObj, int uErrCode, String sData, String sParam) {
            _OutString("NodeOnReply: " + sObj + ", " + iErr + ", " + sData + ", " + sParam);
            if (m_Node != null) {
                String sClass = this.m_Node.ObjectGetClass(sObj);
                try {
                    if (m_eventHook != null) {
                        int uErr = m_eventHook.OnReply(sObj, iErr, sData, sParam);
                        if (uErr >= 0) {
                            return iErr;
                        }
                    }
                } catch (Exception ex) {
                    _OutString("_NodeOnReply: call event hook. ex=" + ex.toString());
                }

                if (PG_CLASS_Peer.equals(sClass)) {
                    return _NodeOnReplyPeer(sObj,  iErr,  sData, sParam);
                }
                if(PG_CLASS_Group.equals(sClass)){
                    return _NodeOnReplyGroup(sObj,  iErr,  sData, sParam);
                }
                if(PG_CLASS_Data.equals(sClass)){
                    return _NodeOnReplyData(sObj,  iErr,  sData, sParam);
                }
                if(PG_CLASS_File.equals(sClass)){
                    return _NodeOnReplyFile(sObj,  iErr,  sData, sParam);
                }
                if(PG_CLASS_Audio.equals(sClass)){
                    return _NodeOnReplyAudio(sObj,  iErr,  sData, sParam);
                }
                if(PG_CLASS_Video.equals(sClass)){
                    return _NodeOnReplyVideo(sObj,  iErr,  sData, sParam);
                }

            }
            return 1;
        }
    };
}
