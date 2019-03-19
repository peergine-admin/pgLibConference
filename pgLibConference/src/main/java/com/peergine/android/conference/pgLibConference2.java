package com.peergine.android.conference;

import android.content.Context;
import android.util.Log;
import android.view.SurfaceView;

import com.peergine.plugin.lib.pgLibJNINode;

import static com.peergine.android.conference.pgLibNode.*;
import static com.peergine.android.conference.pgLibError.*;


public class pgLibConference2 {


    private static final String LIB_VER = "30";
    private static final String ID_PREFIX = "_DEV_";

    private pgLibJNINode m_Node = null;
    private pgLibNodeThreadProc m_NodeProc = null;
    private String sObjSelf = "";

    // Static function

    /**
     * 判断字符串是否为空
     * @param s 字符串
     * @return true 为空 ，false 不为空
     */
    public static boolean isEmpty(String s){
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
     * 描述：设置消息接收回调接口。
     * 阻塞方式：非阻塞，立即返回
     * @param eventListener ：[IN] 实现了OnEventListner接口的对象，必须定义event函数。
     */
    public void SetEventListener(OnEventListener eventListener){

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
        // Init JNI lib.
        if (!pgLibNode.NodeLibInit(oCtx)) {
            _OutString("Initialize: Peergine plugin invalid.");
            return false;
        }
        m_bJNILibInited = true;
        // Create Timer message handler.
        if (!TimerInit()) {
            TimerClean();
            return false;
        }

        TimerOutAdd(timerOut);
        // Create Node objects.
        try {
            m_Node = new pgLibJNINode();
            m_NodeProc = new pgLibNodeThreadProc();
        } catch (Exception ex) {
            m_Node = null;
            m_NodeProc = null;
            TimerOutDel(timerOut);
            TimerClean();
            return false;
        }

        // Init status

        m_iLoginFailCount = 0;
        m_iLoginDelayMax = 60;
        m_iIDTimerRelogin = -1;
        m_Self.init(sUser, sPass, sVideoParam, m_Node);
        String m_sInitSvrName = "pgConnectSvr";
        m_InitSvr.init(m_sInitSvrName, sSvrAddr, sRelayAddr);
        m_InitGroup.Init(sName, sChair, sUser);

        if (!_NodeStart()) {
            _OutString("Initialize: Node start failed.");
            Clean();
            return false;
        }

        if (!_NodeDispInit()) {
            Clean();
            _OutString("pgLibLiveMultiCapture.Initialize: Init dispatch failed.");
            return false;
        }

        return 0;
    }

    /**
     * 描述：P2P会议对象清理函数
     * 阻塞方式：非阻塞，立即返回。
     */
    public void Clean(){
        _NodeDispClean();
        _NodeStop();
        TimerOutDel(timerOut);
        TimerClean();
        //pgLibJNINode.Clean();

        if (m_bJNILibInited) {
            pgLibNode.NodeLibClean();
            m_bJNILibInited = false;
        }
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
     * 描述：获取自身的P2P节点名
     * 阻塞方式：非阻塞，立即返回。
     * 返回值：自身的P2P节点名
     */
    public String GetSelfPeer() {
        return sObjSelf;
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
                    return true;
                }

                if (!"".equals(sClass)) {
                    m_Node.ObjectDelete(sObjPeer);
                }
                return m_Node.ObjectAdd(sObjPeer, "PG_CLASS_Peer", "", 0x10000);
            }
        }
        return false;
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
        return 0;
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
        return 0;
    }

    /**
     * 描述：设置RPC回复消息;
     *    马上设置，不支持线程设置
     * @param sData ：[IN] 消息内容
     *
     */
    public void setRpcResponse(String sData){

    }

    /**
     * 描述：创建播放窗口对象
     * 阻塞方式：阻塞
     * @param sPrvwParam 预览视频参数
     *     Code: 视频压缩编码类型：1为MJPEG、2为VP8、3为H264。
     *     Mode: 视频图像的分辨率（尺寸），有效数值如下：
     *         0: 80x60, 1: 160x120, 2: 320x240, 3: 640x480,
     *         4: 800x600, 5: 1024x768, 6: 176x144, 7: 352x288,
     *          8: 704x576, 9: 854x480, 10: 1280x720, 11: 1920x1080
     *     FrmRate: 视频的帧间间隔（毫秒）。例如40毫秒的帧率为：1000/40 = 25 fps
     *     BitRate: 视频压缩后的码率。单位为 Kbps
     *     CameraNo: 摄像头编号，CameraInfo.facing的值。
     *     Portrait: 采集图像的方向。0为横屏，1为竖屏。
     * @return  返回值：SurfaceView对象，可加入到程序主View中
     */
    public SurfaceView PreviewCreate(String sPrvwParam){
        return null;
    }

    /**
     * 描述：销毁播放窗口对象
     * 阻塞方式：阻塞
     */
    public void PreviewDestroy(){

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
        return 0;
    }

    /**
     *  描述：停止会议，初始化视音频等会议相关数据。
     *  阻塞方式：非阻塞
     * @param sConfName 会议名称
     *
     */
    public void Stop(String sConfName){

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
        return 0;
    }

    /**
     * 描述：添加成员（主席端）
     * 阻塞方式：非阻塞，立即返回
     * @param sConfName [IN] 会议名称
     * @param sMember ：[IN] 成员名
     * @return 错误码 PG_ERR_*
     */
    public int MemberAdd(String sConfName,String sMember){
        return 1;
    }

    /**
     * 描述：删除成员（主席端）
     * @param sConfName [IN] 会议名称
     * @param sMember ：[IN] 成员名
     * 阻塞方式：非阻塞，立即返回
     */
    public void MemberDelete(String sConfName,String sMember){

    }

    /**
     * 描述：请求加入会议
     * 阻塞方式：非阻塞，立即返回
     * @param sConfName [IN] 会议名称
     * @param sChair 主席端ID名称
     * @return 错误码 PG_ERR_*
     */
    public int JoinRequest(String sConfName, String sChair){
        return 0;
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
        return 0;
    }

    /**
     * 描述：停止播放
     * @param sConfName [IN] 会议名称
     * 阻塞方式：非阻塞，立即返回
     */
    public int VideoStop(String sConfName){
        return 0;
    }


    /**
     * 描述：打开某一成员另外一条流的视频
     * @param sConfName 会议名称
     * @param sPeer 成员节点名
     * @param iStreamMode 视频流选择 0：默认视频流 1：额外的视频流
     * @param oView pgLibView产生的View
     * @param sParam 额外的参数：(DevNo){1}
     * 	   DevNo : 外部接口播放时 作为参数。
     * @return error code @link pgLibError.java
     */
    public int VideoOpenRequest(String sConfName,String sPeer,int iStreamMode,SurfaceView oView, String sParam){
        return 0;
    }

    /**
     * 描述：打开某一成员另外一条流的视频
     *
     * @param sConfName 会议名称
     * @param sPeer 成员节点名
     * @param iStreamMode 视频流选择 0：默认视频流 1：额外的视频流
     * @param iErr 传入错误码 PG_ERR_Normal 正常，PG_ERR_Reject 拒绝
     * @param oView pgLibView产生的View
     * @param sParam 额外的参数：(DevNo){1}
     * 	   DevNo : 外部接口播放时 作为参数。
     * @return error code @link pgLibError.java
     */
    public int VideoOpenResponse(String sConfName,String sPeer,int iStreamMode,int iErr,SurfaceView oView, String sParam){
        return 0;
    }

    /**
     * 描述：关闭某一成员视频
     * 阻塞方式：非阻塞，立即返回
     * @param sConfName 会议名称
     * @param sPeer 成员节点名
     * @param iMode 视频流选择 0：默认视频流 1：额外的视频流
     * @return error code @link pgLibError.java
     */
    public void VideoClose(String sConfName,String sPeer,int iMode){

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
        return 0;
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
        return 0;
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
        return 0;
    }

    /**
     * 描述：停止播放或采集音频
     * 阻塞方式：非阻塞，立即返回
     * @param sConfName 会议名称
     */
    public void AudioStop(String sConfName){

    }

    /**
     * 启用或禁用音频输入的静音。
     * @param sConfName 会议名称
     * @param iValue 1为启用，0为禁用。
     * @return 错误码 @link PG_ERR_
     */
    public int AudioMuteInput(String sConfName,int iValue){
        return  0;
    }

    /**
     * 启用或禁用音频输出的静音。
     * @param iValue 1为启用，0为禁用。
     * @return 错误码 @link PG_ERR_
     */
    public int AudioMuteOutput(String sConfName,int iValue){
        return 0;
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
        return 0;
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
        return 0;
    }

    /**
     * 停止录制
     * @param sConfName 会议名称
     * @param sPeer 录制端ID，ID为本身则录制本端视频
     * @param iStreamMode 视频流选择 0：默认视频流 1：额外的视频流
     * @param iMode 录制模式，0 同时录制视音频；1 只录制视频；2 只录制音频
     */
    public void RecordStop(String sConfName,String sPeer, int iStreamMode,int iMode){

    }


    /**
     * 添加文件传输通道
     * @param sTargePeer 对端ID
     * @param sSourcePeer 本端ID
     */
    public int FileAdd(String sTargePeer,String sSourcePeer){
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

    _TimerOutKeep() {
        //OutString("->Keep TimeOut");

        if (m_Node != null) {

            if (!m_Status.bServiceStart || m_Stamp.iExpire == 0 || m_Group.bEmpty) {
                m_Stamp.iKeepStamp = 0;
                m_Stamp.iKeepChainmanStamp = 0;
                m_Stamp.iRequestChainmanStamp = 0;
                _SyncPeerClean();
                return;
            }

            m_Stamp.iKeepStamp += KEEP_TIMER_INTERVAL;

            m_Group.iKeepTimer = TimerStartKeep();

            //取消心跳的接收和发送
            if (m_Group.bChairman) {

                //如果是主席，主动给所有成员发心跳
                int i = 0;
                while (i < m_listSyncPeer.size()) {
                    PG_SYNC oSync = m_listSyncPeer.get(i);

                    // 超过3倍心跳周期，没有接收到成员端的心跳应答，说明成员端之间连接断开了
                    if ((m_Stamp.iKeepStamp - oSync.iKeepStamp) > (m_Stamp.iExpire * 3)) {
                        _OnEvent(EVENT_PEER_OFFLINE, "reason=1", oSync.sObjPeer);
                        PeerDelete(oSync.sObjPeer);
                        _SyncPeerDelete(oSync);
                        continue;
                    }

                    // 每个心跳周期发送一个心跳请求给成员端
                    if ((m_Stamp.iKeepStamp - oSync.iRequestStamp) >= m_Stamp.iExpire) {
                        m_Node.ObjectRequest(oSync.sObjPeer, 36, "Keep?", "MessageSend");
                        oSync.iRequestStamp = m_Stamp.iKeepStamp;
                    }

                    i++;
                }
            } else {
                // 超过3倍心跳周期，没有接收到主席端的心跳请求，说明主席端之间连接断开了
                if ((m_Stamp.iKeepStamp - m_Stamp.iKeepChainmanStamp) > (m_Stamp.iExpire * 3)) {

                    // 每个心跳周期尝试一次连接主席端
                    if ((m_Stamp.iKeepStamp - m_Stamp.iRequestChainmanStamp) >= m_Stamp.iExpire) {
                        m_Stamp.iRequestChainmanStamp = m_Stamp.iKeepStamp;
                        _ChairmanAdd();
                    }
                }
            }
        }
    }

    private int TimerStartKeep() {
        String sTimerParam = "(Act){"+TIME_OUT_ACT_KEEP+"}";
        return  TimerStart(sTimerParam, KEEP_TIMER_INTERVAL, false);
    }

    private int TimerStartActive() {
        String sTimerParam = "(Act){"+TIME_OUT_ACT_TIMER_ACTIVE+"}";
        return  TimerStart(sTimerParam, ACTIVE_TIMER_INTERVAL, false);
    }

    private int TimerStartCheckChairPeer(int iDelay) {
        String sTimerParam = "(Act){"+TIME_OUT_ACT_CHAIR_PEER_CHECK+"}";
        return  TimerStart(sTimerParam, iDelay, false);
    }
    private int TimerStartChairAdd(int iDelay) {
        String sTimerParam = "(Act){"+TIME_OUT_ACT_CHAIRMAN_ADD+"}";
        return  TimerStart(sTimerParam, iDelay, false);
    }
    private int TimerStartRelogin(int iDelay) {
        String sTimerParam = "(Act){"+TIME_OUT_ACT_RELOGIN+"}";
        return  TimerStart(sTimerParam, iDelay, false);
    }

    private void TimerStartPeerGetInfo(String sObjPeer) {
        String sTimerParam = "(Act){"+TIME_OUT_ACT_PEER_GET_INFO+"}(Peer){" + sObjPeer + "}(Report){1}";
        TimerStart( sTimerParam , 5, false);
    }
    //事件下发程序
    private void _OnEvent(String sAct, String sData, String sObjPeer) {
        if (m_eventListener != null && m_Status.bEventEnable) {
            //OutString("EventProc: sAct=" + sAct + ", sData=" + sData + ", sObjPeer=" + sObjPeer);
            String sPeer = sObjPeer;
            if(m_EventOutObjPeer){
                sPeer = _ObjPeerParsePeer(sObjPeer);
            }
            m_eventListener.event(sAct, sData, sPeer);
        }
    }

    // Set capture extend option.
    //摄像头参数设置
    private void _VideoOption() {
        if (m_Node != null) {

            if (m_Node.ObjectAdd("_vTemp", "PG_CLASS_Video", "", 0)) {
                if (m_Self.iVideoFrmRate != 0) {
                    m_Node.ObjectRequest("_vTemp", PG_METH_COMMON_SetOption, "(Item){4}(Value){" + m_Self.iVideoFrmRate + "}", "");

                    String sParam = "(FrmRate){" + m_Self.iVideoFrmRate + "}(KeyFrmRate){4000}";
                    m_Node.ObjectRequest("_vTemp", PG_METH_COMMON_SetOption, "(Item){5}(Value){" + m_Node.omlEncode(sParam) + "}", "");
                }
                if (m_Self.iVideoBitRate != 0) {
                    String sParam = "(BitRate){" + m_Self.iVideoBitRate + "}";
                    m_Node.ObjectRequest("_vTemp", PG_METH_COMMON_SetOption, "(Item){5}(Value){" + m_Node.omlEncode(sParam) + "}", "");
                }
                if (m_Self.bVideoPortrait != 0) {
                    int angle = m_Self.bVideoPortrait * 90;
                    m_Node.ObjectRequest("_vTemp", PG_METH_COMMON_SetOption, "(Item){2}(Value){" + angle + "}", "");
                } else if (m_Self.bVideoRotate != 0) {
                    m_Node.ObjectRequest("_vTemp", PG_METH_COMMON_SetOption, "(Item){2}(Value){" + m_Self.bVideoRotate + "}", "");
                }
                if (m_Self.iCameraNo == CAMERA_FACING_FRONT
                        || m_Self.iCameraNo == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    m_Node.ObjectRequest("_vTemp", PG_METH_COMMON_SetOption, "(Item){0}(Value){" + m_Self.iCameraNo + "}", "");
                }
                m_Node.ObjectDelete("_vTemp");
            }
        }
    }

    //外部采集设置
    private void _NodeVideoExter() {
        _OutString("->NodeVideoExter");
        if (m_Node != null) {
            int iVideoInExternal = _ParseInt(m_Node.omlGetContent(m_Self.sVideoParam, "VideoInExternal"), 0);
            int iInputExternal = _ParseInt(m_Node.omlGetContent(m_Self.sVideoParam, "InputExternal"), 0);
            int iOutputExternal = _ParseInt(m_Node.omlGetContent(m_Self.sVideoParam, "OutputExternal"), 0);
            int iOutputExtCmp = _ParseInt(m_Node.omlGetContent(m_Self.sVideoParam, "OutputExtCmp"), 0);

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

    //设置Node 上线参数
    private boolean _NodeStart() {
        _OutString("->NodeStart");
        if (m_Node != null) {

            m_Svr.init(m_InitSvr.sSvrName, m_InitSvr.sSvrAddr, m_InitSvr.sRelayAddr);
            // Config jni node.
            m_Node.Control = "Type=1;PumpMessage=1;LogLevel0=1;LogLevel1=1";
            m_Node.Node = msConfigNode;
            m_Node.Class = "PG_CLASS_Data:128;PG_CLASS_Video:128;PG_CLASS_Audio:128;PG_CLASS_File:128";

            if(m_LocalAddr.isEmpty()) {
                m_Node.Local = "Addr=0:0:0:127.0.0.1:0:0";
            }else{
                m_Node.Local = m_LocalAddr;
            }

            m_Node.Server = "Name=" + m_Svr.sSvrName + ";Addr=" + m_Svr.sSvrAddr + ";Digest=1";
            m_Node.NodeProc = m_NodeProc;
            if (!"".equals(m_Svr.sRelayAddr)) {
                m_Node.Relay = "(Relay0){(Type){0}(Load){0}(Addr){" + m_Svr.sRelayAddr + "}}";
            } else {
                int iInd = m_Svr.sSvrAddr.lastIndexOf(':');
                if (iInd > 0) {
                    String sSvrIP = m_Svr.sSvrAddr.substring(0, iInd);
                    m_Node.Relay = "(Relay0){(Type){0}(Load){0}(Addr){" + sSvrIP + ":443}}";
                }
            }

            // Start atx node.
            if (!m_Node.Start(0)) {
                _OutString("NodeStart: Start node failed.");
                return false;
            }

            m_Status.bEventEnable = true;
            // Enable video input external
            _NodeVideoExter();
            // Login to server.
            if (_NodeLogin()!=PG_ERR_Normal) {
                _OutString("NodeStart: failed.");
                _NodeStop();
                return false;
            }

            // Enable LAN scan.
            String sValue = "(Enable){1}(Peer){" + m_Node.omlEncode(m_Svr.sSvrName) + "}(Label){pgConf}";
            String sData = "(Item){1}(Value){" + m_Node.omlEncode(sValue) + "}";
            int iErr = m_Node.ObjectRequest(m_Svr.sSvrName, PG_METH_COMMON_SetOption, sData, "EnableLanScan");
            if (iErr > 0) {
                _OutString("NodeStart: Enable lan scan failed. iErr=" + iErr);
            }


            m_Group.Init(m_InitGroup.sName, m_InitGroup.sChair, m_InitGroup.sUser);
            if (!m_InitGroup.bEmpty) {
                m_Stamp.restore();
                if (_ServiceStart()!=PG_ERR_Normal) {
                    _OutString("ServiceStart: failed.");
                    _ServiceStop();
                    return false;
                }
            }

            return true;
        }
        return false;
    }

    //停止Node联网
    private void _NodeStop() {
        if (m_Node != null) {
            _ServiceStop();
            m_Group.bEmpty = true;
            _NodeLogout();

            m_Status.bEventEnable = false;
            m_Node.Stop();
        }
    }
    private int _NodeLogin() {
        String sVersion = "";
        String sVerTemp = m_Node.omlGetContent(m_Node.utilCmd("Version", ""), "Version");
        if (sVerTemp.length() > 1) {
            sVersion = sVerTemp.substring(1);
        }

        String sParam = "(Ver){" + sVersion + "." + LIB_VER + "}";
        _OutString("_NodeLogin: Version=" + sParam);

        String sData = "(User){" + m_Node.omlEncode(m_Self.sObjSelf) + "}(Pass){"
                + m_Node.omlEncode(m_Self.sPass) + "}(Param){" + m_Node.omlEncode(sParam) + "}";
        int iErr = m_Node.ObjectRequest(m_Svr.sSvrName, PG_METH_PEER_Login, sData, PARAM_LOGIN);
        if (iErr > PG_ERR_Normal) {
            _OutString("_NodeLogin: Login failed. iErr=" + iErr);
            return iErr;
        }

        return PG_ERR_Normal;
    }

    private void _NodeLogout() {

        m_Node.ObjectRequest(m_Svr.sSvrName, PG_METH_PEER_Logout, "", PARAM_LOGOUT);
        if (m_Status.bLogined) {
            _OnEvent(EVENT_LOGOUT, "", "");
        }

        m_Status.bLogined = false;
    }

    private void _NodeRelogin(int uDelay) {
        _NodeLogout();
        _NodeTimerRelogin(uDelay);
    }

    private void _NodeRedirect(String sRedirect) {

        _NodeLogout();

        String sSvrName = m_Node.omlGetContent(sRedirect, "SvrName");
        if (!"".equals(sSvrName) && !sSvrName.equals(m_Svr.sSvrName)) {
            m_Node.ObjectDelete(m_Svr.sSvrName);
            if (!m_Node.ObjectAdd(sSvrName, PG_CLASS_Peer, "", (PG_ADD_COMMON_Sync | PG_ADD_PEER_Server))) {
                _OutString("_NodeRedirect: Add server object failed");
                return;
            }
            m_Svr.sSvrName = sSvrName;
            m_Svr.sSvrAddr = "";
        }

        String sSvrAddr = m_Node.omlGetContent(sRedirect, "SvrAddr");
        if (!"".equals(sSvrAddr) && !sSvrAddr.equals( m_Svr.sSvrAddr)) {
            String sData = "(Addr){" + sSvrAddr + "}(Proxy){}";
            int iErr = m_Node.ObjectRequest( m_Svr.sSvrName, PG_METH_PEER_SetAddr , sData, PARAM_SVR_REDIRECT);

            if (iErr > 0) {
                _OutString("_NodeRedirect: Set server address. iErr=" + iErr);
                return;
            }
            m_Svr.sSvrAddr = sSvrAddr;
        }

        _OutString("_NodeRedirect: sSvrName=" + sSvrName + ", sSvrAddr=" + sSvrAddr);

        _NodeTimerRelogin(1);
    }

    private void _NodeRedirectReset(int uDelay) {
        if (!m_Svr.sSvrAddr.equals(m_InitSvr.sSvrAddr)) {
            String sRedirect = "(SvrName){" + m_InitSvr.sSvrName
                    + "}(SvrAddr){" + m_InitSvr.sSvrAddr + "}";
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

            _OnEvent(EVENT_LOGIN, ("" + uErr), "");

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

        _OnEvent(EVENT_LOGIN, "" + PG_ERR_Normal, "");
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
            TimerStop(m_iIDTimerRelogin);
            m_iIDTimerRelogin = -1;
        }

        m_iIDTimerRelogin = TimerStartRelogin(iDelay);
    }
    //节点 登录


    //节点下线


    //节点重新登录


    //重新配置节点信息


    //登录回复信息

    // 建立通讯组 视音频通讯类
    private int _ServiceStart() {
        if (m_Node != null && !m_Group.bEmpty) {
            do {
                _SyncPeerClean();
                _VideoPeerClean();
                if (m_Group.bChairman) {
                    int iFlagGroup = PG_ADD_COMMON_Sync| PG_ADD_GROUP_Update| PG_ADD_GROUP_Refered|PG_ADD_GROUP_Modify |PG_ADD_GROUP_Offline;
                    if (!m_Node.ObjectAdd(m_Group.sObjG, PG_CLASS_Group, "", iFlagGroup)) {
                        _OutString("ServiceStart: Add group object failed");
                        break;
                    }

                    int iMask = 0x0200; // Tell all.
                    String sDataMdf = "(Action){1}(PeerList){(" + m_Self.sObjSelf + "){" + iMask + "}}";
                    int iErr = m_Node.ObjectRequest(m_Group.sObjG, PG_METH_GROUP_Modify , sDataMdf, "");
                    if (iErr > 0) {
                        _OutString("ServiceStart: Add group Chairman failed");
                        break;
                    }
                } else {
                    int iFlagGroup = PG_ADD_COMMON_Sync| PG_ADD_GROUP_Update| PG_ADD_GROUP_Modify | PG_ADD_GROUP_Offline;
                    if (!m_Node.ObjectAdd(m_Group.sObjG, PG_CLASS_Group, m_Group.sObjChair, iFlagGroup)) {
                        _OutString("ServiceStart: Add group object failed");
                        break;
                    }
                    _ChairmanAdd();
                }

                if (!m_Node.ObjectAdd(m_Group.sObjD, PG_CLASS_Data, m_Group.sObjG, 0)) {
                    _OutString("ServiceStart: Add  Data object failed");
                    break;
                }

                // 开始节点连接状态检测定时器。

                m_Group.iKeepTimer = TimerStartKeep();
                m_Stamp.iKeepStamp = 0;

                // 成员端检测主席端的状态时戳
                m_Stamp.iKeepChainmanStamp = 0;
                m_Stamp.iRequestChainmanStamp = 0;

                // Add file object.
                if(!m_Group.bChairman) {
                    if (!_FileListAdd(m_Group.sChair, m_Self.sUser,m_Group.bChairman)) {
                        break;
                    }
                }

                m_Status.bServiceStart = true;
                return PG_ERR_Normal;
            } while (false);
            _ServiceStop();
        }
        return PG_ERR_System;
    }

    //视音频去同步 会议去同步
    private void _ServiceStop() {
        _OutString(" ->ServiceStop");

        if (m_Node != null && !m_Group.bEmpty) {
            if(!m_Group.bChairman) {
                _FileListDelete(m_Group.sChair, m_Self.sUser);
            }
            if (m_Group.iKeepTimer > 0) {
                TimerStop(m_Group.iKeepTimer);
            }

            m_Status.bServiceStart = false;
            //停止心跳包发送

            if (this.m_Status.bApiVideoStart) {
                this.VideoStop();
            }
            if (this.m_Status.bApiAudioStart) {
                this.AudioStop();
            }

            this.m_Status.bApiVideoStart = false;
            this.m_Status.bApiAudioStart = false;

            String sDataMdf = "(Action){0}(PeerList){(" + m_Node.omlEncode(m_Self.sObjSelf) + "){0}}";
            m_Node.ObjectRequest(m_Group.sObjG, PG_METH_GROUP_Modify , sDataMdf, "");

            m_Node.ObjectDelete(m_Group.sObjD);
            m_Node.ObjectDelete(m_Group.sObjG);
            if (!m_Group.bChairman) {
                _ChairmanDel();
            }
        }
    }

    //视频开始后的心跳检测可发送
    private void _TimerActive() {
//        OutString(" ->TimerActive TimeOut");
        if (!m_Status.bApiVideoStart) {
            m_Stamp.iActiveStamp = 0;
            return;
        }

        m_Stamp.iActiveStamp += ACTIVE_TIMER_INTERVAL;

        m_Group.iActiveTimer = TimerStartActive();


        _LostPeerClean();

        for (int i = 0; i < m_listVideoPeer.size(); i++) {

            PG_PEER oPeer = m_listVideoPeer.get(i);
            if(oPeer.bModeL == VIDEO_PEER_MODE_Leave && oPeer.bMode == VIDEO_PEER_MODE_Leave){
                m_listVideoPeer.remove(oPeer);
                continue;
            }
            if ((!oPeer.sObjPeer.equals(m_Self.sObjSelf)) && (oPeer.View != null)) {

                // 超过3倍心跳周期，没有接收到对端的心跳应答，说明与对端之间连接断开了
                if ((m_Stamp.iActiveStamp - oPeer.iActStamp) > (m_Stamp.iActiveExpire * 3) && (!oPeer.bVideoLost)) {
                    _LostPeerAdd(oPeer.sObjPeer);
                    oPeer.bVideoLost = true;
                }

                // 每个心跳周期发送一个心跳请求给对端
                if ((m_Stamp.iActiveStamp - oPeer.iRequestStamp) >= m_Stamp.iActiveExpire) {
                    m_Node.ObjectRequest(oPeer.sObjPeer, PG_METH_PEER_Message , "Active?", "pgLibConference.MessageSend");
                    oPeer.iRequestStamp = m_Stamp.iActiveStamp;
                }
            }
        }

        for ( int i = 0;i < m_listLostPeer.size();i++) {
            _OnEvent(EVENT_VIDEO_LOST, "", m_listLostPeer.get(i));
        }
    }

    private boolean _KeepAdd(String sObjPeer) {
        // 添加
        _OutString("->KeepAdd");
        PG_SYNC oSync = _SyncPeerSearch(sObjPeer);
        if (oSync == null) {
            boolean bRet = _SyncPeerAdd(sObjPeer, m_Stamp.iKeepStamp);
            if(!bRet) return bRet;
        }
        m_Node.ObjectRequest(sObjPeer, PG_METH_PEER_Message , "Keep?", "pgLibConference.MessageSend");
        return true;
    }

    private void _KeepDel(String sObjPeer) {
        //作为成员端只接受主席端心跳 删除
        _OutString("->KeepDel");
        _SyncPeerDelete(sObjPeer);
    }

    //收到Keep 处理
    private void _KeepRecv(String sObjPeer) {
        _OutString("->KeepRecv sObjPeer=" + sObjPeer);

        if (m_Status.bServiceStart) {

            if (m_Group.bChairman) {
                PG_SYNC oSync = _SyncPeerSearch(sObjPeer);
                if (oSync != null) {
                    oSync.iKeepStamp = m_Stamp.iKeepStamp;
                } else {
                    _KeepAdd(sObjPeer);
                    _OnEvent(EVENT_PEER_SYNC, "reason=1", sObjPeer);
                }
            } else {
                m_Node.ObjectRequest(sObjPeer, PG_METH_PEER_Message , "Keep?", "pgLibConference.MessageSend");
                m_Stamp.iKeepChainmanStamp = m_Stamp.iKeepStamp;
            }

        }
    }

    //成员端登录后与主席端连接保存
    private void _TimerOutKeep() {
        //OutString("->Keep TimeOut");

        if (m_Node != null) {

            if (!m_Status.bServiceStart || m_Stamp.iExpire == 0 || m_Group.bEmpty) {
                m_Stamp.iKeepStamp = 0;
                m_Stamp.iKeepChainmanStamp = 0;
                m_Stamp.iRequestChainmanStamp = 0;
                _SyncPeerClean();
                return;
            }

            m_Stamp.iKeepStamp += KEEP_TIMER_INTERVAL;

            m_Group.iKeepTimer = TimerStartKeep();

            //取消心跳的接收和发送
            if (m_Group.bChairman) {

                //如果是主席，主动给所有成员发心跳
                int i = 0;
                while (i < m_listSyncPeer.size()) {
                    PG_SYNC oSync = m_listSyncPeer.get(i);

                    // 超过3倍心跳周期，没有接收到成员端的心跳应答，说明成员端之间连接断开了
                    if ((m_Stamp.iKeepStamp - oSync.iKeepStamp) > (m_Stamp.iExpire * 3)) {
                        _OnEvent(EVENT_PEER_OFFLINE, "reason=1", oSync.sObjPeer);
                        PeerDelete(oSync.sObjPeer);
                        _SyncPeerDelete(oSync);
                        continue;
                    }

                    // 每个心跳周期发送一个心跳请求给成员端
                    if ((m_Stamp.iKeepStamp - oSync.iRequestStamp) >= m_Stamp.iExpire) {
                        m_Node.ObjectRequest(oSync.sObjPeer, 36, "Keep?", "MessageSend");
                        oSync.iRequestStamp = m_Stamp.iKeepStamp;
                    }

                    i++;
                }
            } else {
                // 超过3倍心跳周期，没有接收到主席端的心跳请求，说明主席端之间连接断开了
                if ((m_Stamp.iKeepStamp - m_Stamp.iKeepChainmanStamp) > (m_Stamp.iExpire * 3)) {

                    // 每个心跳周期尝试一次连接主席端
                    if ((m_Stamp.iKeepStamp - m_Stamp.iRequestChainmanStamp) >= m_Stamp.iExpire) {
                        m_Stamp.iRequestChainmanStamp = m_Stamp.iKeepStamp;
                        _ChairmanAdd();
                    }
                }
            }
        }
    }




    //添加主席节点  使之能在加入会议前与主席通信，发送Join信号
    private void _ChairmanAdd() {
        if (!m_Group.bEmpty) {
            if (PG_CLASS_Peer.equals(m_Node.ObjectGetClass(m_Group.sObjChair))) {
                _PeerSync(m_Group.sObjChair, "", 1);
                _ChairPeerCheck();
            } else {
                if (!this.m_Node.ObjectAdd(this.m_Group.sObjChair, PG_CLASS_Peer, "", PG_ADD_COMMON_Sync )) {
                    _OutString("ChairmanAdd:  failed.");
                }
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
    private void _ChairmanDel() {
        _OutString(" ->ChairmanDel ");
        if (m_Node != null) {
            try {
                this.m_Node.ObjectDelete(this.m_Group.sObjChair);
            } catch (Exception ex) {
                _OutString("->ChairmanDel ex = " + ex.toString());
            }
        }
    }


    //自身登录事件处理
    private void _OnSelfSync(String sData, String sObjPeer) {
        _OutString("->SelfSync");

        String sAct = this.m_Node.omlGetContent(sData, "Action");
        if ("1".equals(sAct)) {
            if (sObjPeer.equals(this.m_Svr.sSvrName)) {
                TimerStartPeerGetInfo(sObjPeer);

            }
        } else {
            if (sObjPeer.equals(this.m_Svr.sSvrName)) {
                this._NodeRelogin(10);
            }
        }

    }



    private int _OnSelfCall(String sData, String sObjPeer, int iHandle) {
        _OutString("->SelfCall");

        String sCmd = "";
        String sParam;
        int iInd = sData.indexOf('?');
        if (iInd > 0) {
            sCmd = sData.substring(0, iInd);
            sParam = sData.substring(iInd + 1);
        } else {
            sParam = sData;
        }
        if ("Msg".equals(sCmd)) {
            this._OnEvent(EVENT_MESSAGE, sParam, sObjPeer);
        }
        m_Node.ObjectExtReply(sObjPeer, 0, "", iHandle);

        return 1;
    }

    //自身消息处理
    private int _OnSelfMessage(String sData, String sObjPeer) {
        _OutString("->SelfMessage");

        String sCmd = "";
        String sParam;
        int iInd = sData.indexOf('?');
        if (iInd > 0) {
            sCmd = sData.substring(0, iInd);
            sParam = sData.substring(iInd + 1);
        } else {
            sParam = sData;
        }

        if ("Join".equals(sCmd)) {
            this._OnEvent(EVENT_ASK_JOIN, "", sObjPeer);
        } else if ("Leave".equals(sCmd)) {
            this._OnEvent(EVENT_ASK_LEAVE, "", sObjPeer);
        } else if ("Msg".equals(sCmd)) {
            this._OnEvent(EVENT_MESSAGE, sParam, sObjPeer);
        } else if ("Active".equals(sCmd)) {
            if (m_Status.bServiceStart) {
                PG_PEER oPeer = _VideoPeerSearch(sObjPeer);
                if (oPeer != null) {
                    oPeer.iActStamp = m_Stamp.iActiveStamp;
                    oPeer.bVideoLost = false;
                }
            }
            return 0;

        } else if ("Keep".equals(sCmd)) {
            _KeepRecv(sObjPeer);
        }
        return 0;
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
            this._OnEvent(EVENT_SVR_NOTIFY, sParam, sObjPeer);
            return 0;
        }
        if ("Restart".equals(sCmd)) {
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
            return 0;
        }


        return 0;
    }

    private void _OnServerKickOut(String sData) {
        String sParam = m_Node.omlGetContent(sData, "Param");
        _OnEvent(EVENT_KICK_OUT, sParam, "");
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
            _OnEvent(EVENT_LOGIN, "0", "");
        } else if (sError.equals("" + PG_ERR_Network)
                || sError.equals("" + PG_ERR_Timeout)
                || sError.equals("" + PG_ERR_Busy)) {
            _NodeRedirectReset(0);

            m_bLogin = false;
            _OnEvent(EVENT_LOGIN, sError, "");
        } else {
            m_bLogin = false;
            _OnEvent(EVENT_LOGIN, sError, "");
        }

        return 0;
    }

    private void _OnServerSync(String sData) {
        String sAct = m_Node.omlGetContent(sData, "Action");
        if (!"1".equals(sAct)) {
            _NodeRelogin(3);
        }
    }

    private void _OnChairPeerSync(String sObj, String sData){
        String sAct = this.m_Node.omlGetContent(sData, "Action");
        if ("1".equals(sAct)) {
            if (m_bReportPeerInfo) {
                TimerStartPeerGetInfo(sObj);
            }

            _KeepAdd(sObj);
            m_LanScan.bPeerCheckTimer = false;
            this._OnEvent(EVENT_CHAIRMAN_SYNC, sAct, sObj);
        }
    }
    private void _OnChairPeerError(String sObj, String sData){
        String sMeth = this.m_Node.omlGetContent(sData, "Meth");
        if ("34".equals(sMeth)) {
            String sError = this.m_Node.omlGetContent(sData, "Error");

            _PeerOffline(sObj, sError);
            _KeepDel(sObj);
        }
    }

    private void _OnPeerSync(String sObj, String sData) {
        String sAct = this.m_Node.omlGetContent(sData, "Action");
        if ("1".equals(sAct)) {
            if (m_bReportPeerInfo) {
                TimerStartPeerGetInfo(sObj);
            }
            //心跳包列表 添加
            if (!m_Group.bEmpty && m_Group.bChairman) {
                _KeepAdd(sObj);
            }
            this._OnEvent(EVENT_PEER_SYNC, sAct, sObj);
        }
    }

    private void _OnPeerError(String sObj, String sData) {

        String sMeth = this.m_Node.omlGetContent(sData, "Meth");
        String sError = this.m_Node.omlGetContent(sData, "Error");
        if ("34".equals(sMeth)) {
            //心跳包列表 删除
            if (!m_Group.bEmpty && m_Group.bChairman) {
                _KeepDel(sObj);
            }
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

    private int _VideoPrvwStart(int iPrvwMode){
        this.m_Node.ObjectAdd(_PrvwBuild(), "PG_CLASS_Video", "", PG_ADD_VIDEO_Preview);

        String sWndRect = "(Code){0}(Mode){"+ iPrvwMode +"}(Rate){40}(Wnd){}";

        return this.m_Node.ObjectRequest(_PrvwBuild(), 32, sWndRect, "pgLibConference.PrvwStart");
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

    pgLibNodeProc mNodeProc = new pgLibNodeProc() {
        @Override
        public int _NodeOnExtRequest(String sObj, int uMeth, String sData, int uHandle, String sObjPeer) {
            if (m_Node != null) {
                String sClass = this.m_Node.ObjectGetClass(sObj);
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
