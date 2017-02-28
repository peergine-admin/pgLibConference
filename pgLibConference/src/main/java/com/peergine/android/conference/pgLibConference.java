package com.peergine.android.conference;

import android.content.Context;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceView;

import com.peergine.plugin.lib.pgLibJNINode;
import com.peergine.plugin.lib.pgLibJNINodeProc;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by ctkj-004 on 2016/8/16.
 *
 * Update 2016/11/1 v1.0.3
 * 添加SvrRequest API函数：给服务器发送扩展消息
 *      包含事件：SvrReplyError  Data为错误代码 和 SvrReply Data为服务器回复消息
 *
 * Updata 2016/11/17 v1.0.6
 * 添加视频的抓拍和录制功能
 * 做了一个超时检测 在执行MemberAdd MemberDel Leave 操作是 如果45秒内没有退出和加入会议   。就产生TimeOut 的回调    sData 数操作名   sPeer是参数
 * 这个还没有测试稳定 只是测试了一下程序能跑过去
 * 还添加了CallSend   会产生CallSend的回执
 * CallSend函数的最后一个参数自定义
 * CallSend回调事件的sData 是错误代码 0是正常 ，sPeer是CallSend的最后一个参数
 * 新增函数 AudioCtrlVolume 控制sPeer 的扬声器和麦克风是否播放或采集声音数据，sPeer为空时

 * Updata 2016/12/30 v9
 * 1、升级产品版本规则，版本号前3位是中间件版本，后一位是SDK版本
 * 2、升级打包规则，不同平台分别打包
 * 3、updata增加一些视音频操作函数，节点操作函数 ，Reset 函数 等
 * 4、增加音频初始化选项
 *
 * Updata 2016/12/30 v10
 * 1、升级AudioSpeech函数，增加一个参数，同时兼容之前的函数。
 */

/*
* Updata 2017/02/09 v12
* 优化心跳包发送顺序。
*
* */

/*
* Updata 2017/02/014 v13
* 继续优化心跳包。
* 删除会议模块在离线事件和离开会议后的主动清理视频的代码。
* 修复上报离线事件后再次连接上报同步消息。
* 修复主席端对同一节点反复上报离线消息。
* 修复反复上报VideoLost消息。
* 修改PG_PEER 的列表添加位置，由加入会议添加，离开会议删除，改为视频打开或收到请求添加，视频关闭删除
* 修改函数VideoOpen中对同一节点的Node和View，由新建改为继承。
* 修改Keep函数中的列表的遍历方式。
* 修改ServiceStart的执行位置，使得可以在SDK Initialze 后可以在之后任意位置VideoStart 和AudioStart
* 函数执行打印信息
*
* 开放定时器
*    相关接口TimerOut
*  相关函数：
*    TimerOutAdd  把接口TimerOut的实现加入定时器处理
*    TimerOutDel  把接口TimerOut的实现从定时器处理中删除
*    TimerStart  开始一个定时器处理
*    TimerStop  对定时时间长或者循环定时进行停止操作
*
*
* Updata 2017/02/014 v14
* 1、取消TimeOut事件的上报。
*      如 ：Act="TimeOut",sData = "MemberAdd",sPeer 等不再上报。
* 2 、取消利用临时用户登录代码
* 3、取消bOpened的使用
* 4、增加Config_Node函数，在初始化前配置初始化参数。输入参数为结构体PG_NODE_CFG,具体情况可查看该结构体的注释。
*
*
* */



public class pgLibConference {
    public static final int AUDIO_Speech = 0;
    public static final int AUDIO_NoSpeechSelf = 1;
    public static final int AUDIO_NoSpeechPeer = 2;
    public static final int AUDIO_NoSpeechSelfAndPeer = 3;
    private static final String LIB_VER = "14";
    public static class PG_NODE_CFG{
        public int Type=0;//节点类型。不建议修改
        public int Option=1;//不建议修改
        /*Option：本节点实例的选项，分别为以下的掩码组合：
            0x01：启用网络异常时自动重新尝试登录（客户端有效）
            0x02：启用集群模式的P2P穿透握手机制（服务器端有效）
            0x04：启用踢出重复登录的用户功能（服务器端有效）
            0x08：启用节点协助转发功能的握手功能（服务器端有效）*/
        public int MaxPeer=256;//节点对象的最大数目，取值范围：1 ~ 32768
        public int MaxGroup=32;//组对象的最大数目，取值范围：1 ~ 32768
        public int MaxObject=512;//对象的最大数目，取值范围：1 ~ 65534
        public int MaxMCast=512;//组播句柄的最大数目，取值范围：1 ~ 65534
        public int MaxHandle=256;//常驻接口事件队列的最大长度，取值范围：1 ~ 65534
        public int SKTBufSize0=128;//消息流的Socket队列长度（报文个数），取值范围：1 ~ 32768
        public int SKTBufSize1=64;//音频流的Socket队列长度（报文个数），取值范围：1 ~ 32768
        public int SKTBufSize2=256;//视频流的Socket队列长度（报文个数），取值范围：1 ~ 32768
        public int SKTBufSize3 = 64;//文件流的Socket队列长度（报文个数），取值范围：1 ~ 32768

public PG_NODE_CFG(){
            Type=0;
            Option=1;
            MaxPeer=256;
            MaxGroup=32;
            MaxObject=512;
            MaxMCast=512;
            MaxHandle=256;
            SKTBufSize0=128;
            SKTBufSize1=64;
            SKTBufSize2=256;
            SKTBufSize3 = 64;
        }
    }
    private String m_sConfig_Node= "Type=0;Option=1;MaxPeer=256;MaxGroup=32;MaxObject=512;MaxMCast=512;MaxHandle=256;SKTBufSize0=128;SKTBufSize1=64;SKTBufSize2=256;SKTBufSize3=64";
    // Randomer.
    private java.util.Random m_Random = new java.util.Random();

    // Event listen interface object.
    private OnEventListener m_eventListener = null;
    private pgLibJNINode m_Node = null;
    private pgLibNodeProc m_NodeProc = null;
    private String m_sInitSvrName = "pgConnectSvr";
    private String m_sInitSvrAddr = "";

    private String m_sObjSvr = "";
    private String m_sObjSelf = "";
    private String m_sObjChair = "";

    private String m_sName = "";
    private String m_sChair = "";
    private String m_sUser = "";
    private String m_sPass = "";
    private String m_sSvrAddr = "";
    private String m_sRelayAddr = "";
    private String m_sVideoParam = "";

    private String m_sObjG = "";
    private String m_sObjD = "";
    private String m_sObjV = "";
    private String m_sObjLV = "";
    private String m_sObjA = "";

    //Video 默认参数
    private int m_iVideoCode = 0;
    private int m_iVideoMode = 0;
    private int m_iVideoFrmRate = 0;

    private int m_iLVideoCode = 0;
    private int m_iLVideoMode = 0;
    private int m_iLVideoFrmRate = 0;

    private int m_iVideoBitRate = 0;
    private int m_bVideoPortrait = 0;
    private int m_bVideoRotate = 0;
    private int m_iCameraNo = 0;

    //Audio 默认参数
    private int m_iAudioSpeechDisable = 0;
    private boolean m_bChairman = false;
    private boolean m_bInitialized = false;
    private boolean m_bLogined = false;

    private boolean m_bServiceStart = false;
    private boolean m_bApiVideoStart = false;
    private boolean m_bApiAudioStart = false;

    ///------------------------------------------------------------------------
    private int m_iExpire = 5;
    //打开视频
    private int m_iActiveStamp = 0;

    //同步
    private int m_iKeepStamp = 0;
    private int m_iKeepChainmanStamp = 0;

    private boolean m_bEventEnable = true;
    private int m_iVideoInitFlag = 0;

    //
    private class PG_PEER {

        String sPeer = "";
        int iStamp = 0;
        int iHandle = 0;
        //防止重复加入会议
//        boolean bOpened = false;
        //保证Video关闭前退出会议
        boolean bRequest = false;
        boolean bLarge = false;

        int iActStamp = 0;
        Boolean bVideoLost = false;

        pgLibJNINode Node = null;
        SurfaceView View = null;

        PG_PEER(String sPeer1) {
            sPeer = sPeer1;
        }

        //清理Video相关的数据和状态
        void Restore() {
            if (Node != null) {
                if (View != null) {
                    View = null;
                    Node.WndDelete();
                }
                Node = null;
            }
            iHandle = 0;
            bRequest = false;
            bLarge = false;
//            bOpened = false;
            iActStamp = m_iActiveStamp;
            bVideoLost = false;
        }
    }

    private ArrayList<PG_PEER> m_listVideoPeer = new ArrayList<PG_PEER>();

    private class PG_SYNC {
        String sPeer = "";
        int iKeepStamp = 0;

        PG_SYNC(String sPeer, int iKeepStamp) {
            this.sPeer = sPeer;
            this.iKeepStamp = iKeepStamp;
        }
    }

    private ArrayList<PG_SYNC> m_listSyncPeer = new ArrayList<>();

    //搜索加入会议的节点
    private PG_SYNC SyncPeerSearch(String sPeer) {
        try {
            if (sPeer.equals("")) {
                OutString("VideoPeerSearch can't Search Start");
                return null;
            }
            for (int i = 0; i < m_listSyncPeer.size(); i++) {
                if (m_listSyncPeer.get(i).sPeer.equals(sPeer)) {
                    return m_listSyncPeer.get(i);
                }
            }
        } catch (Exception ex) {
            OutString("GroupSearch: ex=" + ex.toString());
        }
        return null;
    }

    // PG Node callback class.
    private class pgLibNodeProc extends pgLibJNINodeProc {
        pgLibNodeProc() {
            super();
        }

        public int OnReply(String sObj, int uErrCode, String sData, String sParam) {
            return NodeOnReply(sObj, uErrCode, sData, sParam);
        }

        public int OnExtRequest(String sObj, int uMeth, String sData, int uHandle, String sPeer) {
            return NodeOnExtRequest(sObj, uMeth, sData, uHandle, sPeer);
        }
    }

    /**
     *  描述：设置消息接收回调接口。
     *  阻塞方式：非阻塞，立即返回
     *  eventListener：[IN] 实现了OnEventListner接口的对象，必须定义event函数。
     */

    public interface OnEventListener {
        void event(String sAct, String sData, String sPeer);
    }

    /**
     *  描述：获取自身的P2P节点名
     *  阻塞方式：非阻塞，立即返回。
     *  返回值：自身的P2P节点名
     *  作用：扩展时利用此类，进行底层操作。
     */
    public pgLibJNINode GetNode() {
        return m_Node;
    }

    /**
     *  描述：获取自身的P2P节点名
     *  阻塞方式：非阻塞，立即返回。
     *  返回值：自身的P2P节点名
     */
    public String GetSelfPeer() {
        return m_sObjSelf;
    }

    /**
     *  描述：设置心跳间隔。
     *  阻塞方式：非阻塞，立即返回
     *  iExpire：[IN] 心跳间隔。
     */
    public void SetExpire(int iExpire) {
        if (iExpire < 0) {
            return;
        }
        m_iExpire = iExpire;
    }

    /**
     *  描述：设置消息接收回调接口。
     *  阻塞方式：非阻塞，立即返回
     *  eventListener：[IN] 实现了OnEventListner接口的对象，必须定义event函数。
     */
    public void SetEventListener(OnEventListener eventListener) {
        m_eventListener = eventListener;
    }

//  sConfig_Node 参数示例："Type=0;Option=1;MaxPeer=256;MaxGroup=32;MaxObject=512;MaxMCast=512;MaxHandle=256;SKTBufSize0=128;SKTBufSize1=64;SKTBufSize2=256;SKTBufSize3=64";

    public boolean ConfigNode(PG_NODE_CFG mNodeCfg){
        if(mNodeCfg==null){
            return false;
        }


        m_sConfig_Node = "Type="+mNodeCfg.Type+";Option="+mNodeCfg.Option+
                ";MaxPeer="+mNodeCfg.MaxPeer+";MaxGroup="+mNodeCfg.MaxGroup+
                ";MaxObject="+mNodeCfg.MaxObject+";MaxMCast="+mNodeCfg.MaxMCast+";MaxHandle="+mNodeCfg.MaxHandle+
                ";SKTBufSize0="+mNodeCfg.SKTBufSize0+";SKTBufSize1="+mNodeCfg.SKTBufSize1+";SKTBufSize2="+mNodeCfg.SKTBufSize2+";SKTBufSize3="+mNodeCfg.SKTBufSize3;
        return true;
//        boolean[] bConfigs = {false,false,false,false,false,false,false,false,false,false,false};
//        String [] sConfigs = sConfig_Node.split(";");
//        for(int i=0;i<sConfigs.length;i++){
//            if(sConfigs[i].indexOf("Type")==0){
//                bConfigs[0]=true;
//            } else if(sConfigs[i].indexOf("Option")==0){
//                bConfigs[1]=true;
//            }else if(sConfigs[i].indexOf("MaxPeer")==0){
//                bConfigs[2]=true;
//            }else if(sConfigs[i].indexOf("MaxGroup")==0){
//                bConfigs[3]=true;
//            }else if(sConfigs[i].indexOf("MaxObject")==0){
//                bConfigs[4]=true;
//            }else if(sConfigs[i].indexOf("MaxMCast")==0){
//                bConfigs[5]=true;
//            }else if(sConfigs[i].indexOf("MaxHandle")==0){
//                bConfigs[6]=true;
//            }else if(sConfigs[i].indexOf("SKTBufSize0")==0){
//                bConfigs[7]=true;
//            }else if(sConfigs[i].indexOf("SKTBufSize1")==0){
//                bConfigs[8]=true;
//            }else if(sConfigs[i].indexOf("SKTBufSize2")==0){
//                bConfigs[9]=true;
//            }else if(sConfigs[i].indexOf("SKTBufSize3")==0){
//                bConfigs[10]=true;
//            }
//        }
//        for (int i=0;i<bConfigs.length;i++){
//            if(bConfigs[i]==true){
//               switch (i){
//                   case 0:{
//                       sConfig_Node = "Type=0;"+sConfig_Node;
//                       break;
//                   }
//               }
//            }
//        }


    }
    /**
     *  描述：P2P会议对象初始化函数
     *  阻塞方式：非阻塞，立即返回。
     *  sName：[IN] 会议名称
     *  sChair：[IN] 主席端设备ID
     *  sUser：[IN] 登录用户名，自身的设备ID
     *  sPass：[IN] 登录密码
     *  sSvrAddr：[IN] 登录服务器地址和端口，格式：x.x.x.x:x
     *  sRelayAddr：[IN] 转发服务器地址和端口，格式：x.x.x.x:x。
     *                 如果传入空字符串，则使用登录服务器的IP地址加上443端口构成转发服务器地址。
     *  sVideoParam：[IN] 视频参数，格式为：(Code){3}(Mode){2}(Rate){40}(LCode){3}(LMode){2}
     *                 (LRate){40}(CameraNo){0}(Portrait){1}(BitRate){400}
     *                 Code: 视频压缩编码类型：1为MJPEG、2为VP8、3为H264。
     *                 Mode: 视频图像的分辨率（尺寸），有效数值如下：
     *                        0: 80x60, 1: 160x120, 2: 320x240, 3: 640x480,
     *                        4: 800x600, 5: 1024x768, 6: 176x144, 7: 352x288,
     *                        8: 704x576, 9: 854x480, 10: 1280x720, 11: 1920x1080
     *                 FrmRate: 视频的帧间间隔（毫秒）。例如40毫秒的帧率为：1000/40 = 25 fps
     *                 LCode: 不同流视频压缩编码类型：1为MJPEG、2为VP8、3为H264。
     *                 LMode: 不同流视频图像的分辨率（尺寸），有效数值如下：
     *                        0: 80x60, 1: 160x120, 2: 320x240, 3: 640x480,
     *                        4: 800x600, 5: 1024x768, 6: 176x144, 7: 352x288,
     *                        8: 704x576, 9: 854x480, 10: 1280x720, 11: 1920x1080
     *                 LFrmRate: 不同流视频的帧间间隔（毫秒）。例如40毫秒的帧率为：1000/40 = 25 fps
     *                 VideoInExternal: 使能视频的外部采集
     *                 CameraNo: 摄像头编号，CameraInfo.facing的值。
     *                 Portrait: 采集图像的方向。0为横屏，1为竖屏。
     *                 BitRate: 视频压缩后的码率。单位为 Kbps
     *                 AudioSpeechDisable:音频的默认打开状态，0或者不设置为默认打开，1为默认不发送自己的音频给他人，2为默认不接收他人音频，3为默认既不发送给他人，也不接收
     *                            通过AudioSpeech函数改变音频打开状态
     *  oCtx： Android程序的上下文对象
     *  返回值：true 成功， false 失败
     */
    public boolean Initialize(String sName, String sChair, String sUser, String sPass, String sSvrAddr,
            String sRelayAddr, String sVideoParam, Context oCtx) {
        OutString("->Initialize start");
        try {
            if (m_bInitialized) {
                OutString("->Initialize :Initialized = true");
                return true;
            }
            if (sName.equals("") || sName.length() > 64) {
                return false;
            }
            if (sUser.equals("") || sUser.length() > 64) {
                return false;
            }
            if (sChair.equals("") || sChair.length() > 64) {
                return false;
            }
            // Check video parameters.
            if (sVideoParam.equals("")) {
                OutString("Initialize: Invalid VideoParam" + sVideoParam);
                return false;
            }
            // Init JNI lib.
            if (!pgLibJNINode.Initialize(oCtx)) {
                return false;
            }
            // Create Timer message handler.
            if (!TimerInit()) {
                Clean();
                return false;
            }
            TimerOutAdd(timerOut);
            //m_IsJoin = false;
            m_Random = new java.util.Random();
            // Create Node objects.
            m_Node = new pgLibJNINode();
            m_NodeProc = new pgLibNodeProc();

            // Init status
            m_sInitSvrName = "pgConnectSvr";
            m_sInitSvrAddr = sSvrAddr;

            m_sObjSelf = "_DEV_" + sUser;
            m_sObjChair = "_DEV_" + sChair;

            // Store parameters.

            m_sName = sName;
            m_sChair = sChair;
            m_sUser = sUser;
            m_sPass = sPass;

            m_sRelayAddr = sRelayAddr;
            m_sVideoParam = sVideoParam;

            m_iVideoCode = ParseInt(m_Node.omlGetContent(sVideoParam, "Code"), 3);
            m_iVideoMode = ParseInt(m_Node.omlGetContent(sVideoParam, "Mode"), 2);
            m_iVideoFrmRate = ParseInt(m_Node.omlGetContent(sVideoParam, "FrmRate"), 40);

            m_iLVideoCode = ParseInt(m_Node.omlGetContent(sVideoParam, "LCode"), 3);
            m_iLVideoMode = ParseInt(m_Node.omlGetContent(sVideoParam, "LMode"), 2);
            m_iLVideoFrmRate = ParseInt(m_Node.omlGetContent(sVideoParam, "LFrmRate"), 40);

            m_iVideoBitRate = ParseInt(m_Node.omlGetContent(sVideoParam, "BitRate"), 400);
            m_bVideoPortrait = ParseInt(m_Node.omlGetContent(sVideoParam, "Portrait"), 0);
            m_bVideoRotate = ParseInt(m_Node.omlGetContent(sVideoParam, "Rotate"), 0);
            m_iCameraNo = ParseInt(m_Node.omlGetContent(sVideoParam, "CameraNo"), 0);

            m_iAudioSpeechDisable = ParseInt(m_Node.omlGetContent(sVideoParam, "AudioSpeechDisable"), 0);
            if (m_iAudioSpeechDisable == 0) {
                m_iAudioSpeechDisable = ParseInt(m_Node.omlGetContent(sVideoParam, "AudioSpeech"), 0);
            }
            //初始化标记
            m_bLogined = false;
            m_bServiceStart = false;
            m_bApiVideoStart    =   false;
            m_bApiAudioStart    =   false;

            m_bChairman = m_sChair.equals(sUser);

            m_sObjG = "_G_" + m_sName;
            m_sObjD = "_D_" + m_sName;
            m_sObjV = "_V_" + m_sName;
            m_sObjLV = "_LV_" + m_sName;
            m_sObjA = "_A_" + m_sName;

            m_iExpire = 10;
            m_iActiveStamp = 0;
            m_iKeepStamp = 0;
            m_iKeepChainmanStamp = 0;

            m_bEventEnable = true;
            m_iVideoInitFlag = 0;


            m_listSyncPeer.clear();
            m_listVideoPeer.clear();

            if (!NodeStart()) {
                OutString("Initialize: Node start failed.");
                Clean();
                return false;
            }
        } catch (Exception ex) {
            OutString("Initialize: ex=" + ex.toString());
            Clean();
            return false;
        }
        m_bInitialized = true;

        return true;
    }

    /**
     *  描述：P2P会议对象清理函数
     *  阻塞方式：非阻塞，立即返回。
     */
    public void Clean() {
        OutString("->Clean");
        try {
            NodeStop();
            TimerOutDel(timerOut);
            TimerClean();

            m_sObjSvr = "";
            m_sObjSelf = "";

            m_Node = null;
            m_NodeProc = null;

            //pgLibJNINode.Clean();
            m_bInitialized = false;
        } catch (Exception ex) {
            OutString("Clean: ex=" + ex.toString());
        }
    }

    // Create preview for node.
    /**
     *  描述：创建播放窗口对象
     *  阻塞方式：阻塞
     *  iX：[IN] 窗口水平位置
     *  iY：[IN] 窗口垂直位置
     *  iW：[IN] 窗口宽度
     *  iH：[IN] 窗口高度
     *  返回值：SurfaceView对象，可加入到程序主View中
     */
    public SurfaceView PreviewCreate(int iW, int iH) {
        try {
            if (m_Node != null) {
                return (SurfaceView) m_Node.WndNew(0, 0, iW, iH);
            }
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     *  描述：销毁播放窗口对象
     *  阻塞方式：阻塞
     *  返回值：true 成功  false 失败
     */
    public void PreviewDestroy() {
        try {
            if (m_Node != null) {
                m_Node.WndDelete();
            }
        } catch (Exception ex) {
            OutString("PreviewDestroy Exception" + ex.toString());
        }
    }

    /*
    * 描述：通过节点名与其他节点建立联系 （节点名在我们P2P网络的功能类似英特网的IP地址）
    * 阻塞方式：非阻塞。
    * sPeer: 对端的节点名（用户名）
    */
    public boolean PeerAdd(String sPeer) {
        if (sPeer.equals("")) {
            return false;
        }

        String sPeerTemp = sPeer;
        if (sPeerTemp.indexOf("_DEV_") != 0) {
            sPeerTemp = ("_DEV_" + sPeer);
        }

        if (m_Node == null) {
            return false;
        }

        String sClass = m_Node.ObjectGetClass(sPeerTemp);
        if (sClass.equals("PG_CLASS_Peer")) {
            return true;
        }

        if (!sClass.equals("")) {
            m_Node.ObjectDelete(sPeerTemp);
        }

        return m_Node.ObjectAdd(sPeerTemp, "PG_CLASS_Peer", "", 0x10000);
    }

    ///// Sdk扩展运用之添加通信节点，  使用之后会产生PeerSync事件
    // 删除节点连接。（一般不用主动删除节点，因为如果没有通信，节点连接会自动老化。）
    // sPeer: 对端的节点名（用户名）
    public void PeerDelete(String sPeer) {
        if (sPeer.equals("")) {
            return;
        }

        String sPeerTemp = sPeer;
        if (sPeerTemp.indexOf("_DEV_") != 0) {
            sPeerTemp = ("_DEV_" + sPeer);
        }

        if (m_Node == null) {
            return;
        }

        m_Node.ObjectDelete(sPeerTemp);
    }

    /**
     *  描述：添加成员（主席端）
     *  阻塞方式：非阻塞，立即返回
     *  sMember：[IN] 成员名
     *  返回值： true 操作成功，false 操作失败
     */
    public boolean MemberAdd(String sMember) {
        try {
            OutString("MemberAdd");
            if (!m_bChairman) {
                OutString("MemberAdd: no Chairman");
                return false;
            }

            if (!m_bServiceStart) {
                OutString("MemberAdd:  no start");
                return false;
            }

            if (sMember.equals("")) {
                OutString("No Group or sMember name");
                return false;
            }

            if (sMember.indexOf("_DEV_") != 0) {
                sMember = "_DEV_" + sMember;
            }
            int uMask = 0x0200; // Tell all.
            String sDataMdf = "(Action){1}(PeerList){(" + sMember + "){" + uMask + "}}";
            int iErr = m_Node.ObjectRequest(m_sObjG, 32, sDataMdf, "");
            if (iErr > 0) {
                OutString("MemberAdd: Add group member failed err=" + iErr);
                return false;
            }
//            String sParam = "(Act){MemberAdd:" + sMember + "}";
//            TaskAdd("MemberAdd", sMember);
//            TimerStart(sParam, 1, false);

        } catch (Exception ex) {
            OutString("MemberAdd: ex=" + ex.toString());
            return false;
        }
        return true;
    }

    /**
     *  描述：删除成员（主席端）
     *  sMember：[IN] 成员名
     *  阻塞方式：非阻塞，立即返回
     */
    public void MemberDel(String sMember) {
        OutString("->MemberDel");
        try {
            OutString("MemberDel");

            if (!m_bChairman) {
                OutString("MemberDel: no Chairman");
                return;
            }
            if (!m_bServiceStart) {
                OutString("MemberDel:  no start");
                return;
            }
            if (sMember.equals("")) {
                OutString("No Group or sMember name");
                return;
            }
            if (sMember.indexOf("_DEV_") != 0) {
                sMember = "_DEV_" + sMember;
            }

            String sDataMdf = "(Action){0}(PeerList){(" + sMember + "){}}";

            int iErr = this.m_Node.ObjectRequest(this.m_sObjG, 32, sDataMdf, "");
            if (iErr > 0) {
                OutString("MemberDel: Add group member failed err=" + iErr);
            }

//            String sParam = "(Act){MemberDel:" + sMember + "}";
//            TaskAdd("MemberDel", sMember);
//            TimerStart(sParam, 1, false);

        } catch (Exception ex) {
            OutString("MemberDel: ex=" + ex.toString());
        }
    }

    //private boolean m_IsJoin = false;
    /**
     *  描述：请求加入会议
     *  阻塞方式：非阻塞，立即返回
     */
    public boolean Join() {
        try {
            if (m_Node == null) {
                OutString("pgLibConference.Join: Not initialize");
                return false;
            }
            if (!m_bServiceStart) {
                OutString("Start: Service no start!");
                return false;
            }
            if (m_sObjChair.equals(m_sObjSelf)) {
                OutString("you are chairman can't do this");
                return false;
            }

            //            if(m_IsJoin){
            //                return true;
            //            }
            String sData = "Join?" + this.m_sObjSelf;
            int iErr = this.m_Node.ObjectRequest(this.m_sObjChair, 36, sData, "pgLibConference.Join");
            if (iErr > 0) {
                OutString("pgLibConference.Join:ObjectRequest Err=" + iErr);
                return false;
            }
        } catch (Exception ex) {
            OutString("Join: ex=" + ex.toString());
        }
        return true;
    }

    /**
     *  描述：离开会议
     *  阻塞方式：非阻塞，立即返回
     */
    public void Leave() {
        OutString("->Leave");
        try {
            if (m_Node == null) {
                OutString("Leave Not initialize");
                return;
            }
            if (!m_bServiceStart) {
                OutString("Leave: Service no start!");
                return;
            }

            String sDataMdf = "(Action){0}(PeerList){(" + m_sObjSelf + "){}}";
            int iErr = this.m_Node.ObjectRequest(this.m_sObjG, 32, sDataMdf, "");
            if (iErr > 0) {
                OutString("Leave: Leave group member failed err=" + iErr);
            }

//            String sParam = "(Act){Leave}";
//            TaskAdd("Leave", "");
//            TimerStart(sParam, 1, false);
        } catch (Exception ex) {
            OutString("Leave: ex=" + ex.toString());
        }
    }

    /*
    *
    *  描述：切换会议和主席
    *  成员：sName 会议名称，sChair 主席ID
    *  阻塞方式：非阻塞，立即返回
    *   返回值： true 操作成功，false 操作失败
    *
    * */
    public boolean Reset(String sName, String sChair) {
        OutString("->Reset");
        try {

            if (m_Node == null) {
                OutString("pgLibConference.Reset: Not initialize");
                return false;
            }

            if (sName.equals("")) {
                sName = m_sName;
            }
            if (sChair.equals("")) {
                sChair = m_sChair;
            }
            ServiceStop();
            m_bChairman = sChair.equals(m_sUser);
            m_sObjChair = "_DEV_" + sChair;
            m_sObjG = "_G_" + sName;
            m_sObjD = "_D_" + sName;
            m_sObjV = "_V_" + sName;
            m_sObjLV = "_LV_" + sName;
            m_sObjA = "_A_" + sName;

            if (ServiceStart()) {
                return true;
            }
        } catch (Exception ex) {
            OutString("Reset :ex=" + ex.toString());
            return false;
        }
        return false;
    }

    /**
     *  描述：初始化视频设置
     *  成员：iFlag[in] 参考1）静态成员定义：
     *  阻塞方式：非阻塞，立即返回
     *   返回值： true 操作成功，false 操作失败
     */
    public boolean VideoStart(int iFlag) {
        if (m_Node == null) {
            OutString("VideoStart: Not initialize");
            return false;
        }
        if (!this.m_bServiceStart) {
            return false;
        }

        if (this.m_bApiVideoStart) {
            return true;
        }

        if (!VideoInit(iFlag)) {
            return false;
        }

        this.m_bApiVideoStart = true;
        return true;
    }

    /**
     *  描述：停止播放和采集视频
     *  阻塞方式：非阻塞，立即返回
     */
    public boolean VideoStop() {
        if (m_Node == null) {
            OutString("VideoStop: Not initialize");
            return false;
        }
        if (!this.m_bServiceStart) {
            return false;
        }

        if (this.m_bApiVideoStart) {
            this.VideoClean();
            this.m_bApiVideoStart = false;
        }
        return true;
    }

    private SurfaceView VideoOpen(String sPeer, int iW, int iH, boolean bLarge) {
        OutString("VideoOpen :sPeer=" + sPeer + "; iW=" + iW + "; iH=" + iH);
        PG_PEER oPeer = null;
        try {
            if (m_Node == null) {
                OutString("VideoOpen: Not initialize");
                return null;
            }
            if (sPeer.equals("")) {
                OutString("sPeer no chars");
                return null;
            }

            if (!this.m_bApiVideoStart) {
                OutString("Video not init!");
                return null;
            }
            if (sPeer.indexOf("_DEV_") != 0) {
                sPeer = "_DEV_" + sPeer;
            }

            oPeer = VideoPeerSearch(sPeer);
            if (oPeer == null) {
                oPeer = new PG_PEER(sPeer);
                m_listVideoPeer.add(oPeer);
            }

            int iErr = 0;
            String sData;

            // Create the node and view.专门用来显示视频的node
            if (oPeer.Node == null) {
                oPeer.Node = new pgLibJNINode();
                if (oPeer.View == null) {
                    oPeer.View = (SurfaceView) oPeer.Node.WndNew(0, 0, iW, iH);
                }
            }

            //
            if (oPeer.View != null) {
                sData = "(Peer){" + m_Node.omlEncode(sPeer) + "}(Wnd){" + oPeer.Node.utilGetWndRect() + "}";

                OutString("VideoOpen: sData=" + sData);
            } else {
                iErr = 13;
                sData = "";
                OutString("pgLibConference.VideoOpen: New node wnd failed!");
            }

            String sObjV;
            boolean bJoinRes = false;
            if (oPeer.bRequest) {
                // Join reply.

                if (oPeer.bLarge) {
                    sObjV = m_sObjLV;
                } else {
                    sObjV = m_sObjV;
                }
                OutString("Video open Relay iHandle=" + oPeer.iHandle);
                int iErrTemp = this.m_Node.ObjectExtReply(sObjV, iErr, sData, oPeer.iHandle);
                if (iErrTemp <= 0) {
                    if (iErr == 0) {
                        bJoinRes = true;
                    }

                } else {
                    OutString("pgLibConference.VideoOpen: Reply, iErr=" + iErrTemp);
                }
            } else {

                if (bLarge) {
                    sObjV = m_sObjLV;
                } else {
                    sObjV = m_sObjV;
                }
                oPeer.bLarge = bLarge;
//                if (oPeer.bOpened)
//                    return oPeer.View;
                String sParamTmp = "VideoOpen:" + sPeer;
                int iErrTemp = m_Node.ObjectRequest(sObjV, 35, sData, sParamTmp);
                if (iErrTemp <= 0) {
                    bJoinRes = true;
//                    oPeer.bOpened = true;
                } else {
                    OutString("pgLibConference.VideoOpen: Request, iErr=" + iErrTemp);
                }
            }
            if (bJoinRes) {
                oPeer.iActStamp = m_iActiveStamp;
            }

            // Reset request status.

            oPeer.iStamp = 0;
            oPeer.iHandle = 0;
            oPeer.bRequest = false;
            OutString("VideoOpen: scussce");
            return oPeer.View;
        } catch (Exception ex) {
            OutString("VideoOpen: ex=" + ex.toString());
            if (oPeer != null) {
                VideoClose(oPeer);
                oPeer.Restore();
            }
            return null;
        }
    }

 /**
     *  描述：打开某一成员的视频
     *  阻塞方式：非阻塞，立即返回
     *   返回值： true 操作成功，false 操作失败
     *   sPeer:成员节点名
     *   iW: 窗口宽度
     *   iH: 窗口高度
     */
    public SurfaceView VideoOpen(String sPeer, int iW, int iH) {
        return VideoOpen(sPeer, iW, iH, false);
    }

    /**
     *  描述：以不同流打开某一成员的视频（请求端有效）
     *  阻塞方式：非阻塞，立即返回
     *   返回值： true 操作成功，false 操作失败
     *   sPeer:成员节点名
     *   iW: 窗口宽度
     *   iH: 窗口高度
     */
    public SurfaceView VideoOpenL(String sPeer, int iW, int iH) {
        return VideoOpen(sPeer, iW, iH, true);
    }

    /**
     *  描述：拒绝打开某一成员的视频
     *  阻塞方式：非阻塞，立即返回
     *   返回值： true 操作成功，false 操作失败
     *   sPeer:成员节点名
     */
    public void VideoReject(String sPeer) {
        if (m_Node == null) {
            OutString("VideoReject: Not initialize");
            return;
        }
        if (sPeer.equals("")) {
            OutString("sPeer no chars");
            return;
        }

        if (!this.m_bApiVideoStart) {
            OutString("Video not init!");
            return;
        }
        if (sPeer.indexOf("_DEV_") != 0) {
            sPeer = "_DEV_" + sPeer;
        }

        PG_PEER oPeer = VideoPeerSearch(sPeer);
        if (oPeer == null) {
            return;
        }

        String sObjV;
        if (oPeer.bRequest) {
            // Join reply.

            if (oPeer.bLarge) {
                sObjV = m_sObjLV;
            } else {
                sObjV = m_sObjV;
            }
            OutString("Video open Relay iHandle=" + oPeer.iHandle);
            int iErrTemp = this.m_Node.ObjectExtReply(sObjV, 13, "", oPeer.iHandle);
            if (iErrTemp > 0) {
                OutString("pgLibConference.VideoReject: Reply, iErr=" + iErrTemp);
            }

            oPeer.Restore();
            m_listVideoPeer.remove(oPeer);
        }
    }
    /**
     *  描述：关闭某一成员视频
     *  阻塞方式：非阻塞，立即返回
     */
    public void VideoClose(String sPeer) {
        OutString("->VideoClose");
        try {
            if (m_Node == null) {
                OutString("VideoClose: Not initialize");
                return;
            }
            if (!m_bServiceStart) {
                OutString("VideoClose:m_bServiceStart false!");
                return;
            }

            if (!this.m_bApiVideoStart) {
                OutString("VideoClose: m_bApiVideoStart false!");
                return;
            }
            if (sPeer.indexOf("_DEV_") != 0) {
                sPeer = "_DEV_" + sPeer;
            }

            PG_PEER oPeer = VideoPeerSearch(sPeer);
            VideoClose(oPeer);
        } catch (Exception ex) {
            OutString("VideoClose: ex=" + ex.toString());
        }
    }

    /**
     *  描述：获取已打开成员视频的View
     *  阻塞方式：非阻塞，立即返回
     *   返回值： true 操作成功，false 操作失败
     */

    public SurfaceView VideoGetView(String sPeer) {
        SurfaceView view = null;
        try {
            if (m_Node == null) {
                OutString("pgLibConference.VideoGetView: Not initialize");
                return null;
            }
            if (!m_bServiceStart) {
                OutString("VideoGetView: Service no start!");
                return null;
            }

            if (!this.m_bApiVideoStart) {
                OutString("VideoClose: Video no start!");
                return null;
            }
            if (sPeer.indexOf("_DEV_") != 0) {
                sPeer = "_DEV_" + sPeer;
            }

            PG_PEER oCtrl = VideoPeerSearch(sPeer);
            if (oCtrl == null) {
                return null;
            }
            view = oCtrl.View;
        } catch (Exception ex) {
            OutString("VideoGetView: ex=" + ex.toString());
        }
        return view;
    }

    /**
     *  描述：摄像头切换
     *  阻塞方式：非阻塞，立即返回
     *  iCameraNo：摄像头编号
     */
    public boolean VideoSource(int iCameraNo) {
        if (m_Node == null) {
            OutString("VideoSource: Not initialize");
            return false;
        }
        if (!m_bApiVideoStart) {
            return false;
        }

        if (m_Node.ObjectAdd("_vTemp_1", "PG_CLASS_Video", "", 0x2)) {
            m_Node.ObjectRequest("_vTemp_1", 2, "(Item){0}(Value){" + iCameraNo + "}", "");
            m_Node.ObjectDelete("_vTemp_1");
            return true;
        }
        return false;
    }

    /*
    * 描述:采集图像角度切换
    * 阻塞方式：非阻塞，立即返回
    * iAngle:角度
    *
    * */

    public void VideoSetRotate(int iAngle) {
        if (m_Node != null) {
            if (m_Node.ObjectAdd("_vTemp", "PG_CLASS_Video", "", 0)) {
                m_Node.ObjectRequest("_vTemp", 2, "(Item){2}(Value){" + iAngle + "}", "");
                m_Node.ObjectDelete("_vTemp");
            }
        }
    }

    /**
     *  描述：控制成员的视频流
     *  阻塞方式：非阻塞，立即返回
     */

    public boolean VideoControl(String sPeer, boolean bEnable) {
        try {
            if (m_Node == null) {
                OutString("VideoControl: Not initialize");
                return false;
            }
            if (!m_bServiceStart) {
                OutString("VideoControl: Service no start");
                return false;
            }

            if (!this.m_bApiVideoStart) {
                this.OutString("VideoControl: Service no start");
                return false;
            }

            if (sPeer.indexOf("_DEV_") != 0) {
                sPeer = "_DEV_" + sPeer;
            }

            int iFlag = bEnable ? 1 : 0;
            if (sPeer.equals("")) {
                return false;
            }

            String sIn = "(Peer){" + sPeer + "}(Local){" + iFlag + "}(Remote){" + iFlag + "}";
            m_Node.ObjectRequest(m_sObjLV, 39, sIn, "VideoControl");
            m_Node.ObjectRequest(m_sObjV, 39, sIn, "VideoControl");
            return true;
        } catch (Exception ex) {
            OutString("ConfVideoControl: ex=" + ex.toString());
            return false;
        }
    }

    /*
    * 描述：抓拍 sPeer 节点的图片
    * 阻塞方式：非阻塞，立即返回
    * 参数：sPeer 节点名  sPath 路径
    *
    *
    * */
    public boolean VideoCamera(String sPeer, String sPath) {
        try {
            if (m_Node == null) {
                OutString("pgLibConference.VideoCamera: Not initialize");
                return false;
            }
            if (!m_bServiceStart) {
                OutString("VideoCamera: Service no start");
                return false;
            }
            if (sPeer.indexOf("_DEV_") != 0) {
                sPeer = "_DEV_" + sPeer;
            }
            String sPathTemp = sPath;
            if (sPathTemp.lastIndexOf(".jpg") < 0 && sPathTemp.lastIndexOf(".JPG") < 0) {
                sPathTemp += ".jpg";
            }

            String sObjV;
            PG_PEER oPeer = VideoPeerSearch(sPeer);
            if (oPeer == null) {
                OutString("VideoCamera:this Peer Video not open!");
                return false;
            }

            if (oPeer.bLarge) {
                sObjV = m_sObjLV;
            } else {
                sObjV = m_sObjV;
            }
            String sIn = "(Peer){" + sPeer + "}(Path){" + m_Node.omlEncode(sPathTemp) + "}";
            int iErr = m_Node.ObjectRequest(sObjV, 37, sIn, "VideoCamera:" + sPeer);
            if (iErr != 0) {
                OutString("VideoCamera Error  = " + iErr);
                return false;
            }
        } catch (Exception ex) {
            OutString("VideoCamera: ex=" + ex.toString());
            return false;
        }
        return true;
    }

    

    /*
       * 描述：开始录制 sPeer 节点的视频
       * 阻塞方式：非阻塞，立即返回
       * 参数：sPeer 节点名  sPath 路径
       *
       * */
    public boolean VideoRecordStart(String sPeer, String sPath) {
        return VideoRecord(sPeer, sPath);
    }

    /*
      * 描述：停止录制 sPeer 节点的视频
      * 阻塞方式：非阻塞，立即返回
      * 参数：sPeer 节点名
      *
      * */
    public boolean VideoRecordStop(String sPeer) {
        return VideoRecord(sPeer, "");
    }

    public boolean VideoRecord(String sPeer, String sPath) {
        try {
            if (m_Node == null) {
                OutString("pgLibConference.VideoRecord: Not initialize");
                return false;
            }
            if (!m_bServiceStart) {
                OutString("VideoRecord: Service no start");
                return false;
            }
            if (sPeer.indexOf("_DEV_") != 0) {
                sPeer = "_DEV_" + sPeer;
            }
            String sPathTemp = sPath;
            if (sPathTemp.lastIndexOf(".avi") < 0 && sPathTemp.lastIndexOf(".AVI") < 0) {
                sPathTemp += ".avi";
            }

            String sObjV;
            PG_PEER oPeer = VideoPeerSearch(sPeer);
            if (oPeer == null) {
                OutString("VideoRecord:this Peer Video not open!");
                return false;
            }

            if (oPeer.bLarge) {
                sObjV = m_sObjLV;
            } else {
                sObjV = m_sObjV;
            }
            String sIn = "(Peer){" + sPeer + "}(Path){" + m_Node.omlEncode(sPathTemp) + "}";
            int iErr = m_Node.ObjectRequest(sObjV, 38, sIn, "VideoRecord:" + sPeer);
            if (iErr > 0) {
                OutString("VideoRecord Error  = " + iErr);
                return false;
            }
        } catch (Exception ex) {
            OutString("VideoRecord: ex=" + ex.toString());
            return false;
        }
        return true;
    }

    // Start and stop audio

    /**
     *  描述：开始播放或采集音频
     *  阻塞方式：非阻塞，立即返回
     *   返回值： true 操作成功，false 操作失败
     */

    public boolean AudioStart() {
        if (m_Node == null) {
            OutString("AudioStart: Not initialize");
            return false;
        }
        if (!this.m_bServiceStart) {
            return false;
        }

        if (this.m_bApiAudioStart) {
            return true;
        }

        if (!this.AudioInit()) {
            return false;
        }

        this.m_bApiAudioStart = true;
        return true;
    }

    /**
     *  描述：停止播放或采集音频
     *  阻塞方式：非阻塞，立即返回
     */
    public void AudioStop() {
        if (m_Node == null) {
            OutString("pgLibConference.AudioStop: Not initialize");
            return;
        }
        if (!this.m_bServiceStart) {
            return;
        }

        if (this.m_bApiAudioStart) {
            this.AudioClean();
            this.m_bApiAudioStart = false;
        }
    }

    /*
    * 描述：AudioCtrlVolume控制自身的扬声器和麦克风是否播放或采集声音数据
    * 阻塞方式：非阻塞，立即返回
    * sPeer 节点名 （在麦克风下为空则表示控制本端的麦克风音量。 ）
    * iMode 0表示扬声器 1表示麦克风
    * iVolume 表示音量的百分比
    *
    * */
    public boolean AudioCtrlVolume(String sPeer, int iType, int iVolume) {

        if (!this.m_bApiAudioStart) {
            OutString("Audio not init");
            return false;
        }
        if ((!sPeer.equals("")) && sPeer.indexOf("_DEV_") != 0) {
            sPeer = "_DEV_" + sPeer;
        }

        iType = iType > 0 ? 1 : 0;

        iVolume = iVolume < 0 ? 0 : iVolume;//iVolume防止参数小于0
        iVolume = iVolume > 100 ? 100 : iVolume;//大于100 取100
        String sData = "(Peer){}(Action){1}(Type){" + iType + "}(Volume){" + m_Node.omlEncode(iVolume + "")
                + "}(Max){0}(Min){0}";
        int iErr = m_Node.ObjectRequest(m_sObjA, 34, sData, "AudioCtrlVolume");
        if (iErr > 0) {
            OutString("AudioCtrlVolume:set Volume, iErr=" + iErr);
        }

        return true;

    }
   
    public void AudioSpeechDisable(int iDisableMode) {
        m_iAudioSpeechDisable = iDisableMode;
    }
     //使指定peer端不播放本端的音频
    /**
     *  描述：控制某个节点是否能播放本节点的音频，本节点能播放对方的音频
     *  阻塞方式：非阻塞，立即返回
     *  sPeer：节点名
     *  bSendEnable: true接收 ，false不接收
     *  返回值： true 操作成功，false 操作失败
     */
    public boolean AudioSpeech(String sPeer, boolean bSendEnable) {
        return AudioSpeech(sPeer, bSendEnable, true);
    }

    /**
     *  描述：控制某个节点是否能播放本节点的音频，本节点能否播放对方的音频
     *  阻塞方式：非阻塞，立即返回
     *  sPeer：节点名
     *  bSendEnable: true接收 ，false不接收
     *  返回值： true 操作成功，false 操作失败
     */
    public boolean AudioSpeech(String sPeer, boolean bSendEnable, boolean bRecvEnable) {
        try {
            if (m_Node == null) {
                OutString("SvrRequest: Not initialize");
                return false;
            }

            if (!this.m_bApiAudioStart) {
                OutString("Audio not init");
                return false;
            }

            if (sPeer.indexOf("_DEV_") != 0) {
                sPeer = "_DEV_" + sPeer;
            }
            boolean bRet = false;
            int iSendEnable = bSendEnable ? 1 : 0;
            int iRecvEnable = bRecvEnable ? 1 : 0;
            String sData = "(Peer){" + sPeer + "}(ActSelf){" + iSendEnable + "}(ActPeer){" + iRecvEnable + "}";
            int iErr = m_Node.ObjectRequest(m_sObjA, 36, sData, "Speech");
            if (iErr > 0) {
                OutString("Speech: Set Speech, iErr=" + iErr);
            }
            
            return true;
        } catch (Exception ex) {
            OutString("Speech: ex=" + ex.toString());
            return false;
        }
    }

    public void AudioSetSampleRate(int iRate) {
        if (m_Node != null) {
            // Set microphone sample rate
            if (m_Node.ObjectAdd("_AudioTemp", "PG_CLASS_Audio", "", 0)) {
                m_Node.ObjectRequest("_AudioTemp", 2, "(Item){2}(Value){" + iRate + "}", "");
                m_Node.ObjectDelete("_AudioTemp");
            }
        }
    }

    /*
           * 描述：开始录制 sPeer 节点的音频
           * 阻塞方式：非阻塞，立即返回
           * 参数：sPeer 节点名  sPath 路径
           *
           * */
    public boolean AudioRecordStart(String sPeer, String sPath) {
        return AudioRecord(sPeer, sPath);
    }

    /*
         * 描述：开始录制 sPeer 节点的音频
         * 阻塞方式：非阻塞，立即返回
         * 参数：sPeer 节点名  sPath 路径
         *
         * */
    public boolean AudioRecordStop(String sPeer) {
        return AudioRecord(sPeer, "");
    }

    public boolean AudioRecord(String sPeer, String sPath) {
        try {
            if (m_Node == null) {
                OutString("pgLibConference.AudioRecord: Not initialize");
                return false;
            }
            if (!m_bServiceStart) {
                OutString("AudioRecord: Service no start");
                return false;
            }

            if (!m_bApiAudioStart) {
                OutString("Audio Not Start!");
                return false;
            }
            if (sPeer.indexOf("_DEV_") != 0) {
                sPeer = "_DEV_" + sPeer;
            }
            String sPathTemp = sPath;
            if (sPathTemp.lastIndexOf(".avi") < 0 && sPathTemp.lastIndexOf(".AVI") < 0) {
                sPathTemp += ".avi";
            }

            String sIn = "(Peer){" + sPeer + "}(Path){" + m_Node.omlEncode(sPathTemp) + "}";
            int iErr = m_Node.ObjectRequest(m_sObjA, 37, sIn, "AudioRecord:" + sPeer);
            if (iErr > 0) {
                OutString("AudioRecord Error  = " + iErr);
                return false;
            }

        } catch (Exception ex) {
            OutString("VideoRecord: ex=" + ex.toString());
            return false;
        }
        return true;
    }

    /**
     *  描述：摄像头控制。
     *  阻塞方式：非阻塞，立即返回
     *  返回值： true 操作成功，false 操作失败
     */
    public boolean CameraSwitch(boolean bEnable) {
        try {
            if (m_Node == null) {
                OutString("CameraSwitch: Not initialize");
                return false;
            }
            if (!m_bServiceStart) {
                OutString("CameraSwitch: Service no start");
                return false;
            }
            boolean bRet = false;
            if (m_Node.ObjectAdd("_vSwitch", "PG_CLASS_Video", "", 0)) {

                int iEnable = bEnable ? 1 : 0;
                String sData = "(Item){9}(Value){" + iEnable + "}";
                int iErr = m_Node.ObjectRequest("_vSwitch", 2, sData, "SetOption");
                if (iErr > 0) {
                    OutString("CameraSwitch: Set option, iErr=" + iErr);
                } else {
                    bRet = true;
                }

                m_Node.ObjectDelete("_vSwitch");
            }

            return bRet;
        } catch (Exception ex) {
            OutString("CameraSwitch: ex=" + ex.toString());
            return false;
        }
    }

    /**
     *  描述：给指定节点发送消息
     *  阻塞方式：非阻塞，立即返回
     *  sMsg：[IN] 消息内容
     *  sPeer：[IN]节点名称
     *  返回值： true 操作成功，false 操作失败
     */
    public boolean MessageSend(String sMsg, String sPeer) {
        try {
            if (m_Node == null) {
                OutString("MessageSend: Not initialize");
                return false;
            }
            if (!m_bServiceStart) {
                OutString("MessageSend: Service no start");
                return false;
            }
            if (sPeer.indexOf("_DEV_") != 0) {
                sPeer = "_DEV_" + sPeer;
            }
            String sData = "Msg?" + sMsg;
            int iErr = m_Node.ObjectRequest(sPeer, 36, sData, "MessageSend:" + m_sObjSelf);
            if (iErr > 0) {
                OutString("MessageSend: iErr=" + iErr);
                return false;
            }

            return true;
        } catch (Exception ex) {
            OutString("MessageSend: ex=" + ex.toString());
            return false;
        }
    }

    /**
     *  描述：给指定节点发送消息
     *  阻塞方式：非阻塞，立即返回
     *  sMsg：[IN] 消息内容
     *  sPeer：[IN]节点名称
     *  返回值： true 操作成功，false 操作失败
     */
    public boolean CallSend(String sMsg, String sPeer, String sSession) {
        try {
            if (m_Node == null) {
                OutString("CallSend: Not initialize");
                return false;
            }
            if (!m_bServiceStart) {
                OutString("CallSend: Service no start");
                return false;
            }
            if (sPeer.indexOf("_DEV_") != 0) {
                sPeer = "_DEV_" + sPeer;
            }
            String sData = "Msg?" + sMsg;
            int iErr = m_Node.ObjectRequest(sPeer, 35, sData, "CallSend?" + sSession);
            if (iErr > 0) {
                OutString("CallSend: iErr=" + iErr);
                return false;
            }

            return true;
        } catch (Exception ex) {
            OutString("MessageSend: ex=" + ex.toString());
            return false;
        }
    }

    /**
     *  描述：给其他所有成员节点节点发送消息
     *  阻塞方式：非阻塞，立即返回
     *  sMsg：[IN] 消息内容
     *  返回值： true 操作成功，false 操作失败
     */
    public boolean NotifySend(String sData) {
        try {
            if (!m_bServiceStart) {
                OutString("NotifySend: Service no start");
                return false;
            }
            if (sData.indexOf("") != 0) {
                return false;
            }

            int iErr = m_Node.ObjectRequest(m_sObjD, 32, sData, "NotifySend:" + m_sObjSelf);
            if (iErr > 0) {
                OutString("NotifySend: iErr=" + iErr);
                return false;
            }

            return true;
        } catch (Exception ex) {
            OutString("MessageSend: ex=" + ex.toString());
            return false;
        }
    }

    /**
     *  描述：给服务器发送消息。
     *  阻塞方式：非阻塞，立即返回
     *  返回值： true 操作成功，false 操作失败
     */
    public boolean SvrRequest(String sData) {
        if (m_Node == null) {
            OutString("SvrRequest: Not initialize");
            return false;
        }

        int iErr = m_Node.ObjectRequest(m_sObjSvr, 35, ("1024:" + sData), "SvrRequest");
        if (iErr > 0) {
            OutString("SvrRequest: iErr=" + iErr);
            return false;
        }

        return true;
    }

    // /**
    //  *  描述：指定客户端与P2P服务器的连网方式。
    //  *      在手机系统上使用P2P时，如果手机休眠，则网络切换到“只使用Relay转发)”方式连接，
    //  *      可增强手机APP在休眠状态下的在线能力。
    //  *  阻塞方式：非阻塞，立即返回
    //  *  iMode：[IN] 连网模式。0为自动选择，1为只用P2P穿透，2为只用Relay转发。
    //  *  返回值： true 操作成功，false 操作失败
    //  */
    // public boolean ServerNetMode(int iMode) {
    //     if (iMode > 2) {
    //         return false;
    //     }
    //     if (m_Node == null) {
    //         return false;
    //     }
    //     String sCls = m_Node.ObjectGetClass(m_sObjSvr);
    //     if (sCls.equals("")) {
    //         OutString("ServerNetMode: Server peer object is invalid");
    //         return false;
    //     }
    //     int iErr = m_Node.ObjectRequest(m_sObjSvr, 2, "(Item){4}(Value){" + iMode + "}", "");
    //     if (iErr > 0) {
    //         OutString("ServerNetMode: iErr=" + iErr);
    //         return false;
    //     }
    //     return true;
    // }

    //    public void TimerStart(String sAct,int iDelay,boolean  bRepeat, String sSession)
    //    {
    //        String sParam="(Act){"+sAct+":"+sSession+"}";
    //        TimerStart(sParam,iDelay,bRepeat);
    //    }

    ///------------------------------------------------------------------------
    // Static function
    //处理int
    private static int ParseInt(String sInt, int iDef) {
        try {
            if (sInt.equals("")) {
                return 0;
            }
            return Integer.parseInt(sInt);
        } catch (Exception ex) {
            return iDef;
        }
    }

    //log 打印
    private static void OutString(String sOut) {
        Log.d("pgLibConference", sOut);
    }

    //定时器处理程序
    private TimerOut timerOut = new TimerOut() {
        @Override
        public void TimerProc(String sParam) {
            String sAct = m_Node.omlGetContent(sParam, "Act");
            if (sAct.equals("Keep")) {
                Keep();
                return;
            } else if (sAct.equals("TimerActive")) {
                TimerActive();
                return;
            } else if (sAct.equals("ChairmanAdd")) {
                ChairmanAdd();
                return;
            } else if (sAct.equals("Relogin")) {
                NodeLogin();
                return;
            }

        }
    };

    //事件下发程序
    private void EventProc(String sAct, String sData, String sPeer) {
        try {
            if (m_eventListener != null && m_bEventEnable) {
                OutString("EventProc: sAct=" + sAct + ", sData=" + sData + ", sPeer=" + sPeer);
                m_eventListener.event(sAct, sData, sPeer);
            }
        } catch (Exception ex) {
            OutString("->EventProc ex= " + ex.toString());
        }
    }

    // Set capture extend option.
    //摄像头参数设置
    private void VideoOption() {
        OutString("->NodeStart");

        try {
            if (m_Node.ObjectAdd("_vTemp", "PG_CLASS_Video", "", 0)) {
                if (m_iVideoFrmRate != 0) {
                    m_Node.ObjectRequest("_vTemp", 2, "(Item){4}(Value){" + m_iVideoFrmRate + "}", "");

                    String sParam = "(FrmRate){" + m_iVideoFrmRate + "}(KeyFrmRate){4000}";
                    m_Node.ObjectRequest("_vTemp", 2, "(Item){5}(Value){" + m_Node.omlEncode(sParam) + "}", "");
                }
                if (m_iVideoBitRate != 0) {
                    String sParam = "(BitRate){" + m_iVideoBitRate + "}";
                    m_Node.ObjectRequest("_vTemp", 2, "(Item){5}(Value){" + m_Node.omlEncode(sParam) + "}", "");
                }
                if (m_bVideoPortrait != 0) {
                    int angle = m_bVideoPortrait * 90;
                    m_Node.ObjectRequest("_vTemp", 2, "(Item){2}(Value){" + 90 + "}", "");
                } else if (m_bVideoRotate != 0) {
                    m_Node.ObjectRequest("_vTemp", 2, "(Item){2}(Value){" + m_bVideoRotate + "}", "");
                }
                if (m_iCameraNo == Camera.CameraInfo.CAMERA_FACING_FRONT
                        || m_iCameraNo == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    m_Node.ObjectRequest("_vTemp", 2, "(Item){0}(Value){" + m_iCameraNo + "}", "");
                }
                m_Node.ObjectDelete("_vTemp");
            }
        } catch (Exception ex) {
            OutString("VideoOption. ex=" + ex.toString());
        }
    }

    //外部采集设置
    private void NodeVideoExter() {
        OutString("->NodeStart");
        if (m_Node == null) {
            return;
        }

        int iVideoInExternal = ParseInt(m_Node.omlGetContent(m_sVideoParam, "VideoInExternal"), 0);
        int iInputExternal = ParseInt(m_Node.omlGetContent(m_sVideoParam, "InputExternal"), 0);
        int iOutputExternal = ParseInt(m_Node.omlGetContent(m_sVideoParam, "OutputExternal"), 0);
        int iOutputExtCmp = ParseInt(m_Node.omlGetContent(m_sVideoParam, "OutputExtCmp"), 0);

        if (iVideoInExternal != 0 || iInputExternal != 0 || iOutputExternal != 0 || iOutputExtCmp != 0) {
            if (m_Node.ObjectAdd("_vTemp", "PG_CLASS_Video", "", 0)) {

                if (iVideoInExternal != 0 || iInputExternal != 0) {
                    m_Node.ObjectRequest("_vTemp", 2, "(Item){8}(Value){1}", "");
                }

                if (iOutputExtCmp != 0) {
                    m_Node.ObjectRequest("_vTemp", 2, "(Item){13}(Value){1}", "");
                } else if (iOutputExternal != 0) {
                    m_Node.ObjectRequest("_vTemp", 2, "(Item){11}(Value){1}", "");
                }

                m_Node.ObjectDelete("_vTemp");
            }
        }
    }

    //设置Node 上线参数
    private boolean NodeStart() {
        OutString("->NodeStart");
        if (m_Node == null) {
            return false;
        }

        m_sObjSvr = m_sInitSvrName;
        m_sSvrAddr = m_sInitSvrAddr;
        // Config jni node.
        m_Node.Control = "Type=1;LogLevel0=1;LogLevel1=1";
        m_Node.Node = m_sConfig_Node;
        m_Node.Class = "PG_CLASS_Data:128;PG_CLASS_Video:128;PG_CLASS_Audio:128";
        m_Node.Local = "Addr=0:0:0:127.0.0.1:0:0";
        m_Node.Server = "Name=" + m_sObjSvr + ";Addr=" + m_sSvrAddr + ";Digest=1";
        m_Node.NodeProc = m_NodeProc;
        if (!m_sRelayAddr.equals("")) {
            m_Node.Relay = "(Relay0){(Type){0}(Load){0}(Addr){" + m_sRelayAddr + "}}";
        } else {
            int iInd = m_sSvrAddr.lastIndexOf(':');
            if (iInd > 0) {
                String sSvrIP = m_sSvrAddr.substring(0, iInd);
                m_Node.Relay = "(Relay0){(Type){0}(Load){0}(Addr){" + sSvrIP + ":443}}";
            }
        }

        // Start atx node.
        if (!m_Node.Start(0)) {
            OutString("NodeStart: Start node failed.");
            return false;
        }

        m_bEventEnable = true;
        // Enable video input external
        NodeVideoExter();
        // Login to server.
        if (!NodeLogin()) {
            OutString("NodeStart: login failed.");
            NodeStop();
            return false;
        }

        if (!ServiceStart()) {
            OutString("ServiceStart: login failed.");
            ServiceStop();
            NodeStop();
            return false;
        }

        if (m_bApiVideoStart) {
            VideoInit(m_iVideoInitFlag);
        }

        if (m_bApiAudioStart) {
            AudioInit();
        }
        //ServiceStart();
        // Set video option.
        return true;
    }

    //停止Node联网
    private void NodeStop() {
        OutString("->NodeStop");

        if (m_Node == null) {
            return;
        }

        ServiceStop();
        NodeLogout();

        m_bEventEnable = false;
        m_Node.Stop();
    }

    //节点 登录
    private boolean NodeLogin() {
        OutString("->NodeLogin");

        if (m_Node == null) {
            return false;
        }
        String sVersion = "";
        String sVerTemp = m_Node.omlGetContent(m_Node.utilCmd("Version", ""), "Version");
        if (sVerTemp.length() > 1) {
            sVersion = sVerTemp.substring(1);
        }

        String sParamTemp = "(Ver){" + sVersion + "." + LIB_VER + "}";
        String sData = "(User){" + m_sObjSelf + "}(Pass){" + m_sPass + "}(Param){" + m_Node.omlEncode(sParamTemp) + "}";
        int iErr = m_Node.ObjectRequest(m_sObjSvr, 32, sData, "NodeLogin");
        if (iErr > 0) {
            OutString("NodeLogin: Login failed. iErr=" + iErr);
            return false;
        }
       
        return true;
    }

    //节点下线
    private void NodeLogout() {
        OutString("->NodeLogout");
        if (m_Node == null) {
            return;
        }
        m_Node.ObjectRequest(m_sObjSvr, 33, "", "NodeLogout");
        if (m_bLogined) {
            EventProc("Logout", "", "");
        }

        m_bLogined = false;

    }

    //节点重新登录
    private void NodeRelogin(int iDelay) {
        OutString("->NodeRelogin!");

        NodeLogout();
        TimerStart("(Act){Relogin}", iDelay, false);
        
    }

    //重新配置节点信息
    private void NodeRedirect(String sRedirect) {

        if (m_Node == null) {
            return;
        }

        NodeLogout();

        String sSvrName = m_Node.omlGetContent(sRedirect, "SvrName");
        if (!sSvrName.equals("") && !sSvrName.equals(m_sObjSvr)) {
            m_Node.ObjectDelete(m_sObjSvr);
            if (!m_Node.ObjectAdd(sSvrName, "PG_CLASS_Peer", "", (0x10000 | 0x2))) {
                OutString("pgLibConference.NodeRedirect: Add server object failed");
                return;
            }
            m_sObjSvr = sSvrName;
            m_sSvrAddr = "";
        }
        String sSvrAddr = m_Node.omlGetContent(sRedirect, "SvrAddr");
        if (!sSvrAddr.equals("") && !sSvrAddr.equals(m_sSvrAddr)) {
            String sData = "(Addr){" + sSvrAddr + "}(Proxy){}";
            int iErr = m_Node.ObjectRequest(m_sObjSvr, 37, sData, "pgLibConference.NodeRedirect");
            if (iErr > 0) {
                OutString("pgLibConference.NodeRedirect: Set server address. iErr=" + iErr);
                return;
            }
            m_sSvrAddr = sSvrAddr;
        }

        OutString("NodeRedirect: sSvrName=" + sSvrName + ", sSvrAddr=" + sSvrAddr);

        TimerStart("(Act){Relogin}", 1, false);
    }

    private void NodeRedirectReset(int iDelay) {
        if (!m_sSvrAddr.equals(m_sInitSvrAddr)) {
            String sRedirect = "(SvrName){" + m_sInitSvrName + "}(SvrAddr){" + m_sInitSvrAddr + "}";
            NodeRedirect(sRedirect);
        } else {
            if (iDelay != 0) {
                NodeRelogin(iDelay);
            }
        }

    }

    //登录回复信息
    private int NodeLoginReply(int iErr, String sData) {
        OutString(" ->NodeLoginReply ");
        try {
            if (m_Node == null) {
                return 1;
            }

            if (iErr != 0) {
                OutString("pgLibLive.NodeLoginReply: Login failed. uErr=" + iErr);

                EventProc("Login", String.valueOf(iErr), "");
                if (iErr == 11 || iErr == 12 || iErr == 14) {
                    NodeRedirectReset(10);
                }

                return 1;
            }

            // Process redirect.
            String sParam = m_Node.omlGetContent(sData, "Param");
            String sRedirect = m_Node.omlGetEle(sParam, "Redirect.", 10, 0);
            if (!sRedirect.equals("")) {
                NodeRedirect(sRedirect);
                return 1;
            }

            m_bLogined = true;
            EventProc("Login", "0", m_sObjSvr);

        } catch (Exception ex) {
            OutString("->NodeLoginReply ex = " + ex.toString());
        }
        return 1;
    }

    //添加主席节点  使之能在加入会议前与主席通信，发送Join信号
    private void ChairmanAdd() {
        OutString(" ->ChairmanAdd ");
        try {
            if (m_Node.ObjectGetClass(m_sObjChair).equals("PG_CLASS_Peer")) {
                PeerSync(m_sObjChair, "", 1);
            } else {
                if (!this.m_Node.ObjectAdd(this.m_sObjChair, "PG_CLASS_Peer", "", (0x10000))) {
                    OutString("ChairmanAdd:  failed.");
                    return;
                }
            }
        } catch (Exception ex) {
            OutString("->ChairmanAdd ex = " + ex.toString());
        }
    }

    private void PeerSync(String sObject, String sPeer, int uAction) {
        OutString(" ->PeerSync Act=" + uAction);
        if (m_Node == null) {
            return;
        }
        uAction = (uAction <= 0) ? 0 : 1;
        try {
            m_Node.ObjectSync(sObject, sPeer, uAction);
        } catch (Exception ex) {
            OutString("->PeerSync ex = " + ex.toString());
        }
    }

    //删除主席节点  使能在添加主席节点失败后能重新添加
    private void ChairmanDel() {
        OutString(" ->ChairmanDel ");
        try {
            this.m_Node.ObjectDelete(this.m_sObjChair);
        } catch (Exception ex) {
            OutString("->ChairmanDel ex = " + ex.toString());
        }
    }

    // 建立通讯组 视音频通讯类
    private boolean ServiceStart() {
        OutString(" ->ServiceStart ");
        try {
            do {
                if (m_sObjChair.equals(m_sObjSelf)) {
                    if (!m_Node.ObjectAdd(m_sObjG, "PG_CLASS_Group", "", (0x10000 | 0x10 | 0x4 | 0x1))) {
                        OutString("ServiceStart: Add group object failed");
                        break;
                    }
                    int iMask = 0x0200; // Tell all.
                    String sDataMdf = "(Action){1}(PeerList){(" + m_sObjSelf + "){" + iMask + "}}";
                    int iErr = m_Node.ObjectRequest(m_sObjG, 32, sDataMdf, "");
                    if (iErr > 0) {
                        OutString("ServiceStart: Add group Chairman failed");
                        break;
                    }
                } else {
                    if (!m_Node.ObjectAdd(m_sObjG, "PG_CLASS_Group", m_sObjChair, (0x10000 | 0x10 | 0x1))) {
                        OutString("ServiceStart: Add group object failed");
                        break;
                    }
                    ChairmanAdd();
                }

                if (!m_Node.ObjectAdd(m_sObjD, "PG_CLASS_Data", m_sObjG, 0)) {
                    OutString("ServiceStart: Add  Data object failed");
                    break;
                }

                //开始发送心跳包
                if (TimerStart("(Act){TimerActive}", 10, false) < 0) {
                    break;
                }
                m_iActiveStamp = 0;

                if (TimerStart("(Act){Keep}", m_iExpire, false) < 0) {
                    break;
                }
                m_iKeepChainmanStamp = 0;
                m_iKeepStamp = 0;
                m_bServiceStart = true;
                return true;
            } while (false);
            ServiceStop();
        } catch (Exception ex) {
            OutString("->ServiceStart ex = " + ex.toString());
        }
        return false;
    }

    //视音频去同步 会议去同步
    private void ServiceStop() {
        OutString(" ->ServiceStop");
        try {
            if (m_Node == null) {
                return;
            }
            m_bServiceStart = false;
            //停止心跳包发送

            if (this.m_bApiVideoStart) {
                this.VideoClean();

            }
            if (this.m_bApiAudioStart) {
                this.AudioClean();

            }
            this.m_bApiVideoStart = false;
            this.m_bApiAudioStart = false;

            String sDataMdf = "(Action){0}(PeerList){(" + m_Node.omlEncode(m_sObjSelf) + "){0}}";
            m_Node.ObjectRequest(m_sObjG, 32, sDataMdf, "");

            m_Node.ObjectDelete(m_sObjD);
            m_Node.ObjectDelete(m_sObjG);
            if (!m_bChairman) {
                ChairmanDel();
            }
        } catch (Exception ex) {
            OutString("->ServiceStop ex = " + ex.toString());
        }
    }

    //视频开始后的心跳检测可发送
    private void TimerActive() {
        OutString(" ->TimerActive TimeOut");
        try {
            if (m_Node == null) {
                return;
            }

            if (!m_bServiceStart) {
                m_iActiveStamp = 0;
                return;
            }

            m_iActiveStamp += 10;
            TimerStart("(Act){TimerActive}", 10, false);

            if (m_listVideoPeer == null) {
                return;
            }
            ArrayList<PG_PEER> listVideoPeer = (ArrayList<PG_PEER>) m_listVideoPeer.clone();
            for (int i = 0; i < listVideoPeer.size(); i++) {
                PG_PEER oPeer = listVideoPeer.get(i);
                if ((!oPeer.sPeer.equals(m_sObjSelf)) && (oPeer.Node != null)) {
                    //检测心跳超时
                    if ((m_iActiveStamp - oPeer.iActStamp) > 30 && (!oPeer.bVideoLost)) {
                        EventProc("VideoLost", "", oPeer.sPeer);
                        oPeer.bVideoLost = true;
                    }

                    //视频打开发送心跳
                    //给各连接的节点发送心跳
                    m_Node.ObjectRequest(oPeer.sPeer, 36, "Active?", "pgLibConference.MessageSend");
                }
            }
        } catch (Exception ex) {
            OutString("TimerActive: ex=" + ex.toString());
        }

    }

    private void KeepAdd(String sPeer) {
        // 添加
        OutString("->KeepAdd");
        if (SyncPeerSearch(sPeer) == null) {
            m_listSyncPeer.add(new PG_SYNC(sPeer, m_iKeepStamp));
        }
        m_Node.ObjectRequest(sPeer, 36, "Keep?", "pgLibConference.MessageSend");
    }

    private void KeepDel(String sPeer) {
        //作为成员端只接受主席端心跳 删除
        OutString("->KeepDel");
        PG_SYNC oSync = SyncPeerSearch(sPeer);
        if (oSync != null) {
            m_listSyncPeer.remove(oSync);
        }
    }

    //收到Keep
    private void KeepRecv(String sPeer) {
        OutString("pgLibConference ->KeepRecv sPeer=" + sPeer);

        if (m_bServiceStart) {
            if (m_bChairman) {
                PG_SYNC oSync = SyncPeerSearch(sPeer);
                if (oSync != null) {
                    oSync.iKeepStamp = m_iKeepStamp;
                } else {
                    KeepAdd(sPeer);
                    EventProc("PeerSync", "reason=1", sPeer);
                }
            } else {
                m_Node.ObjectRequest(sPeer, 36, "Keep?", "pgLibConference.MessageSend");
                m_iKeepChainmanStamp = m_iKeepStamp;
            }
        }
    }

    //成员端登录后与主席端连接保存
    private void Keep() {
        OutString("->Keep TimeOut");

        try {
            if (m_Node == null) {
                return;
            }

            if (!m_bServiceStart) {
                m_iKeepStamp = 0;
                m_iKeepChainmanStamp = 0;
                m_listSyncPeer.clear();
                return;
            }

            m_iKeepStamp += m_iExpire;

            TimerStart("(Act){Keep}", m_iExpire, false);

            if (m_bChairman) {

                //如果是主席，主动给所有成员发心跳
                int i = 0;
                while (i < m_listSyncPeer.size()) {
                    PG_SYNC oSync = m_listSyncPeer.get(i);

                    if ((m_iKeepStamp - oSync.iKeepStamp) > m_iExpire * 2) {
                        //超时
                        //                        PeerSync(oSync.sPeer,"",0);
                        EventProc("PeerOffline", "reason=1", oSync.sPeer);
                        PeerDelete(oSync.sPeer);
                        m_listSyncPeer.remove(i);
                        continue;
                    }

                    m_Node.ObjectRequest(oSync.sPeer, 36, "Keep?", "pgLibConference.MessageSend");
                    i++;
                }
            } else {
                if ((m_iKeepStamp - m_iKeepChainmanStamp) > m_iExpire * 2) {

                    ChairmanAdd();
                }
            }
        } catch (Exception ex) {
            OutString("Keep: ex=" + ex.toString());
        }

    }

    //视频相关初始化
    private boolean VideoInit(int iFlag) {
        OutString("->VideoInit iFlag = " + iFlag);
        
        this.VideoOption();
        m_iVideoInitFlag = iFlag;
        int uFlag = 0x10000 | 0x1 | 0x10 | 0x20;
        switch (iFlag) {
        case pgVideoPutMode.OnlyInput: {
            uFlag = uFlag | 0x4;
            break;
        }
        case pgVideoPutMode.OnlyOutput: {
            uFlag = uFlag | 0x8;
            break;
        }
        case pgVideoPutMode.Normal:
        default:
        }

        //预览
        this.m_Node.ObjectAdd("Prvw", "PG_CLASS_Video", "", 0x2);
        String sWndRect = "(Code){0}(Mode){2}(Rate){40}(Wnd){}";
        int iErr = this.m_Node.ObjectRequest("Prvw", 32, sWndRect, "pgLibConference.PrvwStart");
        if (iErr > 0) {
            OutString("pgLibConference.m_VideoInit: Open Prvw failed. iErr=" + iErr);
            return false;
        }

        if (!this.m_Node.ObjectAdd(this.m_sObjV, "PG_CLASS_Video", this.m_sObjG, uFlag)) {
            OutString("pgLibConference.m_VideoInit: Add 'Video' failed.");
            return false;
        }

        String sData = "(Code){" + this.m_iVideoCode + "}(Mode){" + this.m_iVideoMode + "}(Rate){"
                + this.m_iVideoFrmRate + "}";

        iErr = this.m_Node.ObjectRequest(this.m_sObjV, 32, sData, "pgLibConference.VideoStart");
        if (iErr > 0) {
            OutString("pgLibConference.m_VideoInit: Open live failed. iErr=" + iErr);
            return false;
        }

        if (!this.m_Node.ObjectAdd(this.m_sObjLV, "PG_CLASS_Video", this.m_sObjG, uFlag)) {
            OutString("pgLibConference.m_VideoInit: Add 'Video' failed.");
            return false;
        }

        sData = "(Code){" + this.m_iLVideoCode + "}(Mode){" + this.m_iLVideoMode + "}(Rate){" + this.m_iLVideoFrmRate
                + "}";

        iErr = this.m_Node.ObjectRequest(this.m_sObjLV, 32, sData, "pgLibConference.VideoStart");
        if (iErr > 0) {
            OutString("pgLibConference.m_VideoInit: Open live failed. iErr=" + iErr);
            return false;
        }

        return true;
    }

    private boolean VideoClose(PG_PEER oPeer) {
        OutString("->VideoClose : oPeer.sPeer" + oPeer.sPeer);
        try {
            if (oPeer == null)
                return false;
            if (oPeer.Node != null || oPeer.iHandle > 0) {
                String sObjV;
                if (oPeer.bLarge) {
                    sObjV = m_sObjLV;
                } else {
                    sObjV = m_sObjV;
                }
                String sData = "(Peer){" + this.m_Node.omlEncode(oPeer.sPeer) + "}";
                int iErr = this.m_Node.ObjectRequest(sObjV, 36, sData, "VideoClose:" + oPeer.sPeer);
                if (iErr != 0) {
                    return false;
                }
            }
            oPeer.Restore();
            m_listVideoPeer.remove(oPeer);
        } catch (Exception ex) {
            OutString("VideoClose PG_PEER: " + ex.toString());
        }
        return true;
    }

    //视频相关清理
    private void VideoClean() {
        OutString("->VideoClean");
        try {

            this.m_Node.ObjectRequest(this.m_sObjLV, 33, "", "pgLibConference.VideoCleanL");
            this.m_Node.ObjectDelete(this.m_sObjLV);

            this.m_Node.ObjectRequest(this.m_sObjV, 33, "", "pgLibConference.VideoClean");
            this.m_Node.ObjectDelete(this.m_sObjV);

            this.m_Node.ObjectRequest("Prvw", 33, "", "pgLibConference.PrvwClean");
            this.m_Node.ObjectDelete("Prvw");

        } catch (Exception ex) {
            OutString("VideoClean: " + ex.toString());
        }
    }

    //音频相关初始化
    private boolean AudioInit() {
        OutString("->AudioInit");
        int uFlag = 0x10000 | 0x01;
        switch (m_iAudioSpeechDisable) {
        case 1:
            uFlag = uFlag | 0x0020;
            break;
        case 2:
            uFlag = uFlag | 0x0040;
            break;
        case 3:
            uFlag = uFlag | 0x0020 | 0x0040;
            break;
        case 0:
        default:
            break;
        }

        if (!this.m_Node.ObjectAdd(this.m_sObjA, "PG_CLASS_Audio", this.m_sObjG, uFlag)) {
            OutString("pgLibConference.m_AudioInit: Add Audio failed.");
            return false;
        }

        int iErr = this.m_Node.ObjectRequest(this.m_sObjA, 32, "(Code){1}(Mode){0}", "pgLibConference.AudioInit");
        if (iErr > 0) {
            OutString("pgLibConference.AudioInit: Open audio failed. iErr=" + iErr);
            return false;
        }
        return true;
    }

    //音频相关清理
    private void AudioClean() {
        OutString("->AudioClean");

        this.m_Node.ObjectRequest(this.m_sObjA, 33, "", "pgLibConference.AudioClean");
        this.m_Node.ObjectDelete(this.m_sObjA);
    }

    //自身登录事件处理
    private void SelfSync(String sData, String sPeer) {
        OutString("->SelfSync");

        String sAct = this.m_Node.omlGetContent(sData, "Action");
        if (!sAct.equals("1")) {
            if (sPeer.equals(this.m_sObjSvr)) {
                this.NodeRelogin(10);
            }
        }
    }

    private int SelfCall(String sData, String sPeer, int iHandle) {
        OutString("->SelfCall");
        try {
            String sCmd = "";
            String sParam = "";
            int iInd = sData.indexOf('?');
            if (iInd > 0) {
                sCmd = sData.substring(0, iInd);
                sParam = sData.substring(iInd + 1);
            } else {
                sParam = sData;
            }
            if (sCmd.equals("Msg")) {
                this.EventProc("Message", sParam, sPeer);
            }
            m_Node.ObjectExtReply(sPeer, 0, "", iHandle);
        } catch (Exception ex) {
            OutString("SelfMessage" + ex.toString());
        }
        return 1;
    }

    //自身消息处理
    private int SelfMessage(String sData, String sPeer) {
        OutString("->SelfMessage");

        try {
            String sCmd = "";
            String sParam = "";
            int iInd = sData.indexOf('?');
            if (iInd > 0) {
                sCmd = sData.substring(0, iInd);
                sParam = sData.substring(iInd + 1);
            } else {
                sParam = sData;
            }

            if (sCmd.equals("Join")) {
                this.EventProc("AskJoin", "", sPeer);
            } else if (sCmd.equals("Leave")) {
                this.EventProc("AskLeave", "", sPeer);
            } else if (sCmd.equals("Msg")) {
                this.EventProc("Message", sParam, sPeer);
            } else if (sCmd.equals("Active")) {
                if (m_bServiceStart) {
                    PG_PEER oPeer = VideoPeerSearch(sPeer);
                    if (oPeer != null) {
                        oPeer.iActStamp = m_iActiveStamp;
                        oPeer.bVideoLost = false;
                    }
                }
                return 0;
           
            } else if (sCmd.equals("Keep")) {
                KeepRecv(sPeer);
            }
        } catch (Exception ex) {
            OutString("SelfMessage" + ex.toString());
        }
        return 0;
    }

    //服务器消息处理
    private int ServerMessage(String sData, String sPeer) {
        OutString("->ServerMessage");
        String sCmd = "";
        String sParam = "";
        int iInd = sData.indexOf('?');
        if (iInd > 0) {
            sCmd = sData.substring(0, iInd);
            sParam = sData.substring(iInd + 1);
        } else {
            sParam = sData;
        }

        if (sCmd.equals("UserExtend")) {
            this.EventProc("SvrNotify", sParam, sPeer);
        } else if (sCmd.equals("Restart")) {
            if (sParam.contains("redirect=1")) {
                NodeRedirectReset(3);
            }
        }

        return 0;
    }

    //服务器错误处理
    private void ServerError(String sData) {
        OutString("->ServerError");

        String sMeth = m_Node.omlGetContent(sData, "Meth");
        if (sMeth.equals("32")) {
            String sError = m_Node.omlGetContent(sData, "Error");
            if (sError.equals("10")) {
                NodeRelogin(3);
            } else if (sError.equals("11") || sError.equals("12") || sError.equals("14")) {
                NodeRedirectReset(0);
            }
//            if (sError.equals("8")) {
//                int iReloginDelay;
//                String sObjTemp ="_TMP_"+m_sUser;
//                if (!sObjTemp.equals("")) {
//                    m_sObjSelf = sObjTemp;
//                    OutString("NodeLoginReply: Change to templete user, sObjTemp="+sObjTemp);
//                    iReloginDelay = 1;
//                }
//                else {
//                    iReloginDelay = 30;
//                }
//
//                NodeLogout();
//                TimerStart("(Act){Relogin}",iReloginDelay,false);
//            }
        }


    }

    private void ServerRelogin(String sData) {
        String sError = m_Node.omlGetContent(sData, "ErrCode");
        if (sError.equals("0")) {
            String sParam = m_Node.omlGetContent(sData, "Param");
            String sRedirect = m_Node.omlGetEle(sParam, "Redirect.", 10, 0);
            if (!sRedirect.equals("")) {
                NodeRedirect(sRedirect);
                return;
            }

            m_bLogined = true;
            EventProc("Login", "0", m_sObjSvr);
        }
    }

    //会议成员更新   每加入一个新成员 其他成员获得他的信息  新成员获得其他所有成员的信息
    private void GroupUpdate(String sData) {
        OutString("->GroupUpdate");
        try {
            String sAct = this.m_Node.omlGetContent(sData, "Action");
            String sPeerList = this.m_Node.omlGetEle(sData, "PeerList.", 256, 0);
            OutString("GroupUpdate: sAct=" + sAct + " sPeerList=" + sPeerList);
            int iInd = 0;
            while (true) {
                String sEle = this.m_Node.omlGetEle(sPeerList, "", 1, iInd);
                if (sEle.equals("")) {
                    break;
                }

                String sPeerTemp = this.m_Node.omlGetName(sEle, "");
                OutString("GroupUpdate: sAct=" + sAct + " sPeer=" + sPeerTemp);
                if (sPeerTemp.indexOf("_DEV_") == 0) {

                    if (sAct.equals("1")) {
                        EventProc("Join", "", sPeerTemp);
                    } else {
                        EventProc("Leave", "", sPeerTemp);

                    }
                }

                iInd++;
            }
        } catch (Exception ex) {
            OutString("GroupUpdate: ex=" + ex.toString());
        }
    }

    //搜索加入会议的节点
    private PG_PEER VideoPeerSearch(String sPeer) {
        OutString("->VideoPeerSearch");
        try {
            if (sPeer.equals("")) {
                OutString("VideoPeerSearch can't Search Start");
                return null;
            }
            for (int i = 0; i < m_listVideoPeer.size(); i++) {
                if (m_listVideoPeer.get(i).sPeer.equals(sPeer)) {
                    return m_listVideoPeer.get(i);
                }
            }
        } catch (Exception ex) {
            OutString("VideoPeerSearch: ex=" + ex.toString());
        }
        return null;
    }

    //保存对端视频请求句柄
    private void VideoJoin(String sObj, String sData, int iHandle, String sPeer, String sAct) {
        OutString("->VideoJoin");

        PG_PEER oCtrl = VideoPeerSearch(sPeer);
        if (oCtrl == null) {
            oCtrl = new PG_PEER(sPeer);
            m_listVideoPeer.add(oCtrl);
        }

        oCtrl.iStamp = m_iCurStamp;
        oCtrl.iHandle = iHandle;
        oCtrl.bRequest = true;
        if (sObj.indexOf("_LV_") == 0) {
            oCtrl.bLarge = true;
        }
        OutString("Video _VideoJoin iHandle=" + oCtrl.iHandle);
        EventProc(sAct, sData, sPeer);
    }

    //初始化节点
    private void VideoLeave(String sObj, String sData, int iHandle, String sPeer, String sAct) {
        OutString("->VideoLeave");
        EventProc(sAct, sData, sPeer);

        PG_PEER oCtrl = VideoPeerSearch(sPeer);
        if (oCtrl != null) {
            oCtrl.Restore();
            m_listVideoPeer.remove(oCtrl);
        }
    }

    //peer离线
    private void PeerOffline(String sPeer, String sError) {
        OutString("->PeerOffline");
        try {
            String sAct;
            if (sPeer.equals(m_sObjChair)) {
                sAct = "ChairmanOffline";
            } else {
                sAct = "PeerOffline";
            }
            this.EventProc(sAct, sError, sPeer);
        } catch (Exception ex) {
            OutString("PeerOffline :" + ex.toString());
        }
    }

    //上报发送视频帧信息
    private void VideoFrameStat(String sData, String sAct) {

        String sPeerTemp = m_Node.omlGetContent(sData, "Peer");
        String sFrmTotal = m_Node.omlGetContent(sData, "Total");
        String sFrmDrop = m_Node.omlGetContent(sData, "Drop");

        EventProc(sAct, (sFrmTotal + ":" + sFrmDrop), sPeerTemp);
    }

    private void VideoCameraReply(String sData) {
        if (m_Node == null) {
            return;
        }
        if (!m_bApiVideoStart) {
            return;
        }
        String sPeer = m_Node.omlGetContent(sData, "Peer");
        String sPath = m_Node.omlGetContent(sData, "Path");
        EventProc("VideoCamera", sPath, sPeer);
    }

    private void VideoRecordReply(String sData) {
        if (m_Node == null) {
            return;
        }
        if (!m_bApiVideoStart) {
            return;
        }
        String sPeer = m_Node.omlGetContent(sData, "Peer");
        String sPath = m_Node.omlGetContent(sData, "Path");
        EventProc("VideoRecord", sPath, sPeer);
    }

    //服务器下发数据
    private void SvrReply(int iErr, String sData) {
        if (iErr != 0) {
            EventProc("SvrReplyError", iErr + "", "");
        } else {
            EventProc("SvrReply", sData, "");
        }
    }

    private int NodeOnExtRequest(String sObj, int uMeth, String sData, int iHandle, String sPeer) {
        OutString("NodeOnExtRequest: " + sObj + ", " + uMeth + ", " + sData + ", " + sPeer);
        try {
            //Peer类相关
            if (sObj.equals(m_sObjSvr)) {
                if (uMeth == 0) {
                    String sAct = this.m_Node.omlGetContent(sData, "Action");
                    if (!sAct.equals("1") && this.m_sObjSvr.equals("")) {
                        this.NodeRelogin(10);
                    }
                } else if (uMeth == 1) {
                    ServerError(sData);
                } else if (uMeth == 46) {
                    ServerRelogin(sData);
                }
                return 0;
            } else if (sObj.equals(m_sObjSelf)) {
                if (uMeth == 0) {
                    SelfSync(sData, sPeer);
                }
                else if (uMeth == 35) {
                    return this.SelfCall(sData, sPeer, iHandle);
                } else if (uMeth == 36) {
                    if (sPeer.equals(this.m_sObjSvr)) {
                        return this.ServerMessage(sData, sPeer);
                    } else {
                        return this.SelfMessage(sData, sPeer);
                    }
                } else if (uMeth == 47) {
                    //ID冲突 被踢下线了
                    EventProc("Logout", "47", "");
                } 
                return 0;
            } else if (sObj.equals(this.m_sObjChair)) {

                if (uMeth == 0) {
                    String sAct = this.m_Node.omlGetContent(sData, "Action");
                    if (sAct.equals("1")) {
                        KeepAdd(sObj);
                        this.EventProc("ChairmanSync", sAct, sObj);
                    }
                } else if (uMeth == 1) {
                    String sMeth = this.m_Node.omlGetContent(sData, "Meth");
                    if (sMeth.equals("34")) {
                        String sError = this.m_Node.omlGetContent(sData, "Error");
                        
                        PeerOffline(sObj, sError);
                        KeepDel(sObj);
                    }
                }
                return 0;
            } else if (this.m_Node.ObjectGetClass(sObj).equals("PG_CLASS_Peer")) {
                if (uMeth == 0) {
                    String sAct = this.m_Node.omlGetContent(sData, "Action");
                    if (sAct.equals("1")) {

                        //心跳包列表 添加
                        if (m_bChairman) {
                            KeepAdd(sObj);
                        }
                        this.EventProc("PeerSync", sAct, sObj);
                    }
                } else if (uMeth == 1) {
                    String sMeth = this.m_Node.omlGetContent(sData, "Meth");
                    if (sMeth.equals("34")) {
                        String sError = this.m_Node.omlGetContent(sData, "Error");

                        //心跳包列表 删除
                        if (m_bChairman) {
                            KeepDel(sObj);
                        }
                        PeerOffline(sObj, sError);
                    }
                }
                return 0;
            }

            //通讯组类相关
            if (sObj.equals(m_sObjG)) {
                if (uMeth == 33) {
                    //成员有更新
                    //加入列表，
                    this.GroupUpdate(sData);

                }
            }

            //DData类相关
            if (sObj.equals(this.m_sObjD)) {
                if (uMeth == 32) {
                    this.EventProc("Notify", sData, sPeer);
                }
                return 0;
            }
            //接收视频类方法
            if (sObj.equals(m_sObjV)) {
                if (uMeth == 0) {
                    String sAct = this.m_Node.omlGetContent(sData, "Action");
                    if (sAct.equals("1")) {
                        EventProc("VideoSync", "", sPeer);
                    }

                } else if (uMeth == 35) {
                    VideoJoin(sObj, sData, iHandle, sPeer, "VideoOpen");
                    return -1;
                } else if (uMeth == 36) {
                    VideoLeave(sObj, sData, iHandle, sPeer, "VideoClose");
                } else if (uMeth == 40) {
                    VideoFrameStat(sData, "VideoFrameStat");
                }
                return 0;
            }
            if (sObj.equals(m_sObjLV)) {
                if (uMeth == 0) {
                    String sAct = this.m_Node.omlGetContent(sData, "Action");
                    if (sAct.equals("1")) {
                        EventProc("VideoSyncL", "", sPeer);
                    }
                } else if (uMeth == 35) {
                    VideoJoin(sObj, sData, iHandle, sPeer, "VideoOpenL");
                    return -1;
                } else if (uMeth == 36) {
                    VideoLeave(sObj, sData, iHandle, sPeer, "VideoCloseL");
                } else if (uMeth == 40) {
                    VideoFrameStat(sData, "VideoFrameStatL");
                }
                return 0;
            }
            //音频类相关
            if (sObj.equals(m_sObjA)) {
                if (uMeth == 0) {
                    String sAct = this.m_Node.omlGetContent(sData, "Action");
                    if (sAct.equals("1")) {
                        EventProc("AudioSync", "", sPeer);
                    }
                }
            }
        } catch (Exception ex) {
            OutString("NodeOnExtRequest ex=" + ex.toString());
        }
        return 0;
    }

    private int NodeOnReply(String sObj, int iErr, String sData, String sParam) {
        OutString("NodeOnReply: " + sObj + ", " + iErr + ", " + sData + ", " + sParam);
        try {

            if (sObj.equals(m_sObjSvr)) {
                if (sParam.equals("NodeLogin")) {
                    NodeLoginReply(iErr, sData);
                } 
                else if (sParam.equals("SvrRequest")) {
                    SvrReply(iErr, sData);
                }

                return 1;
            }
            if (sParam.indexOf("CallSend") == 0) {
                String sSession = "";
                int iInd = sParam.indexOf(':');
                sSession = sData.substring(9);
                EventProc("CallSend", sSession + ":" + iErr, sObj);
                return 1;
            }
            if (sParam.indexOf("VideoOpen") == 0) {
                //视频加入通知
                this.EventProc("VideoJoin", "" + iErr, sParam.substring(10));
                return 1;
            }
            if (sParam.indexOf("VideoCamera") == 0) {
                VideoCameraReply(sData);
                return 1;
            }
            if (sParam.indexOf("VideoRecord") == 0) {
                VideoRecordReply(sData);
                return 1;
            }

            if (sObj.equals(m_sObjA)) {
                if (sParam.equals("AudioCtrlVolume")) { // Cancel file
                    EventProc("AudioCtrlVolume", Integer.valueOf(iErr).toString(), sObj);
                }
            }
        } catch (Exception ex) {
            OutString("->NodeOnReply ex=" + ex);
        }
        return 1;
    }

    //VideoOpen 超时清理
    private void DropPeerHelper(String sPeer) {

        if (m_bApiVideoStart) {
            EventProc("VideoClose", "", sPeer);
        }

        if (!sPeer.equals(m_sObjSelf)) {
            VideoClose(sPeer);
        }
    }

    // 定时器相关

    public interface TimerOut {
        void TimerProc(String sParam);
    }

    ArrayList<TimerOut> m_listTimerOut = new ArrayList<>();

    /**
     * 描述:将TimerOut接口添加到超时处理列表中
     * 阻塞方式：非阻塞
     * @param timerOut
     */
    public void TimerOutAdd(TimerOut timerOut) {
        if (m_listTimerOut != null) {
            m_listTimerOut.add(timerOut);
        }
    }

    /**
     * 描述:将TimerOut接口从超时处理列表中删除
     * 阻塞方式：非阻塞
     * @param timerOut
     */
    public void TimerOutDel(TimerOut timerOut) {
        if (m_listTimerOut != null) {
            m_listTimerOut.remove(timerOut);
        }
    }

    private class TimerItem {

        int iCookie = 0;
        String sParam = "";
        boolean bRepeat = false;
        int iTimeoutVal = 0;
        int iTimeCount = 0;

        TimerItem() {
        }
    }

    // Timer class.
    private class pgTimerTask extends TimerTask {

        pgTimerTask() {
            super();
        }

        public void run() {
            try {
                if (m_timerHandler != null) {
                    Message oMsg = m_timerHandler.obtainMessage(0, null);
                    m_timerHandler.sendMessage(oMsg);
                } else {
                    TimerProc();
                }
            } catch (Exception ex) {
                OutString("pgTimerTask.run, ex=" + ex.toString());
            }
        }
    }

    private int m_iCurStamp = 0;
    private Timer m_timer = null;
    private pgTimerTask m_timerTask = null;
    private Handler m_timerHandler = null;
    private ArrayList<TimerItem> s_timerList = new ArrayList<TimerItem>();

    private void TimerProc() {

        m_iCurStamp++;

        for (int i = 0; i < m_listVideoPeer.size(); i++) {
            PG_PEER oCtrl = m_listVideoPeer.get(i);
            if (oCtrl.bRequest && (m_iCurStamp - oCtrl.iStamp) > 20) {
                DropPeerHelper(oCtrl.sPeer);
            }
        }

        for (int i = 0; i < s_timerList.size(); i++) {

            TimerItem oItem = s_timerList.get(i);
            if (oItem.iTimeoutVal == 0) {
                continue;
            }

            oItem.iTimeCount++;
            if (oItem.iTimeCount < oItem.iTimeoutVal) {
                continue;
            }

            try {
                ArrayList<TimerOut> list = (ArrayList<TimerOut>) m_listTimerOut.clone();
                for (TimerOut timerOut : list) {
                    timerOut.TimerProc(oItem.sParam);
                }
            } catch (Exception ex) {
                OutString("TimerProc : " + ex.toString());
            }
            //            TimerProc(oItem.sParam);

            if (!oItem.bRepeat) {
                oItem.iTimeoutVal = 0;
            }

            oItem.iTimeCount = 0;
        }
    }

    // Create Timer message handler.
    private boolean TimerInit() {
        try {
            m_listTimerOut.clear();
            m_timerHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    TimerProc();
                }
            };

            m_timer = new Timer();
            pgTimerTask m_timerTask = new pgTimerTask();
            m_timer.schedule(m_timerTask, 1000, 1000);

            return true;
        } catch (Exception ex) {
            OutString("TimerInit: ex=" + ex.toString());
            m_listTimerOut = null;
            m_timerHandler = null;
            m_timerTask = null;
            m_timer = null;
            return false;
        }
    }

    private void TimerClean() {
        try {
            if (m_timer != null) {
                m_timer.cancel();
                m_timer = null;
            }
            m_listTimerOut.clear();

            m_timerTask = null;
            m_timerHandler = null;

            for (int i = 0; i < s_timerList.size(); i++) {
                TimerItem oItem = s_timerList.get(i);
                oItem.iCookie = 0;
                oItem.sParam = "";
                oItem.bRepeat = false;
                oItem.iTimeoutVal = 0;
                oItem.iTimeCount = 0;
            }
        } catch (Exception ex) {
            OutString("TimerClean, ex=" + ex.toString());
        }
    }

    /**
     * 描述：开启一个定时器
     * 阻塞方式：非阻塞
     * @param sParam  : 超时处理接口收到的参数
     * @param iTimeout :超时时间
     * @param bRepeat : 是否循环
     * @return  : 定时器实例ID
     */
    public int TimerStart(String sParam, int iTimeout, boolean bRepeat) {

        try {
            int iItem = -1;
            for (int i = 0; i < s_timerList.size(); i++) {
                if (s_timerList.get(i).iTimeoutVal == 0) {
                    iItem = i;
                    break;
                }
            }
            if (iItem < 0) {
                s_timerList.add(new TimerItem());
                iItem = s_timerList.size() - 1;
            }

            int iCookie = (m_Random.nextInt() & 0xffff);

            TimerItem oItem = s_timerList.get(iItem);
            oItem.iCookie = iCookie;
            oItem.sParam = sParam;
            oItem.bRepeat = bRepeat;
            oItem.iTimeoutVal = iTimeout;
            oItem.iTimeCount = 0;

            return (((iItem << 16) & 0xffff0000) | iCookie);
        } catch (Exception ex) {
            OutString("Add, ex=" + ex.toString());
            return -1;
        }
    }

    /**
     * 描述：关闭一个定时器
     * 阻塞方式：非阻塞
     * @param iTimerID : 定时器实例ID
     */
    public void TimerStop(int iTimerID) {

        try {
            int iCookie = iTimerID & 0xffff;
            int iItem = (iTimerID >> 16) & 0xffff;

            if (iItem >= s_timerList.size()) {
                return;
            }

            TimerItem oItem = s_timerList.get(iItem);
            if (oItem.iCookie != iCookie) {
                return;
            }

            oItem.iCookie = (m_Random.nextInt() & 0xffff);
            oItem.sParam = "";
            oItem.bRepeat = false;
            oItem.iTimeoutVal = 0;
            oItem.iTimeCount = 0;
        } catch (Exception ex) {
            OutString("TimerStop, ex=" + ex.toString());
        }
    }
}
