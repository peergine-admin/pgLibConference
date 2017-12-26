
/* ***********************************************************************
 copyright   : Copyright (C) 2016, ctkj, All rights reserved.
 : www.peergine.com, www.pptun.com
 :
 filename    : pgLibConference.java
 discription :
 modify      : create, ctkj, 2016/07/24

 ************************************************************************ */

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

import static android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT;
import static com.peergine.android.conference.pgLibConferenceEvent.*;
import static com.peergine.android.conference.pgLibConferenceEvent.EVENT_LOGOUT;
import static com.peergine.android.conference.pgLibConferenceEvent.EVENT_VIDEO_LOST;
import static com.peergine.android.conference.pgLibError.*;
/**
 * Copyright (C) 2014-2017, Peergine, All rights reserved.
 * www.peergine.com, www.pptun.com
 * ${PACKAGE_NAME}
 * @author ctkj-004
 * @date 2016/8/16.
 */
public class pgLibConference {

    //===================================================
    /**
     * 录像的模式
     */
    public static final int PG_RECORD_NORMAL = 0;
    public static final int PG_RECORD_ONLYVIDEO = 1;
    public static final int PG_RECORD_ONLYAUDIO = 2;
    /**
     * 初始化音频控制正常对讲
     */
    public static final int AUDIO_SPEECH = 0;
    /**
     * 初始化音频控制自己静音
     */
    public static final int AUDIO_NO_SPEECH_SELF = 1;
    /**
     * 初始化音频控制静音其他成员
     */
    public static final int AUDIO_NO_SPEECH_PEER = 2;
    /**
     * 初始化音频控制不接收音频也不发送音频
     */
    public static final int AUDIO_NO_SPEECH_SELF_AND_PEER = 3;

    /**
     * 初始化视频正常
     */
    public static final int VIDEO_NORMAL = 0;
    /**
     * 初始化视频只接收视频不发送视频
     */
    public static final int VIDEO_ONLY_INPUT = 1;
    /**
     * 初始化视频只发送视频不接收视频
     */
    public static final int VIDEO_ONLY_OUTPUT = 2;

    //======================================================================

    private static final String ID_PREFIX = "_DEV_";
    private static final String LIB_VER = "20";
    private static final int KEEP_TIMER_INTERVAL = 2;
    private static final int ACTIVE_TIMER_INTERVAL = 2;


    /**
     * @author ctkj
     * 中间件初始化参数结构类
     */
    public static class PG_NODE_CFG {
        /**
         * 节点类型。不建议修改
         */
        public int Type = 0;
        /**
         * 不建议修改
         * Option：本节点实例的选项，分别为以下的掩码组合：
         * 0x01：启用网络异常时自动重新尝试登录（客户端有效）
         * 0x02：启用集群模式的P2P穿透握手机制（服务器端有效）
         * 0x04：启用踢出重复登录的用户功能（服务器端有效）
         * 0x08：启用节点协助转发功能的握手功能（服务器端有效）
         */
        public int Option = 1;
        /**
         * 节点对象的最大数目，取值范围：1 ~ 32768
         */
        public int MaxPeer = 256;
        /**
         * 组对象的最大数目，取值范围：1 ~ 32768
         */
        public int MaxGroup = 32;
        /**
         * 对象的最大数目，取值范围：1 ~ 65534
         */
        public int MaxObject = 512;
        /**
         * 组播句柄的最大数目，取值范围：1 ~ 65534
         */
        public int MaxMCast = 512;
        /**
         * 常驻接口事件队列的最大长度，取值范围：1 ~ 65534
         */
        public int MaxHandle = 256;
        /**
         * 消息流的Socket队列长度（报文个数），取值范围：1 ~ 32768
         */
        public int SKTBufSize0 = 128;
        /**
         * 音频流的Socket队列长度（报文个数），取值范围：1 ~ 32768
         */
        public int SKTBufSize1 = 64;
        /**
         * 视频流的Socket队列长度（报文个数），取值范围：1 ~ 32768
         */
        public int SKTBufSize2 = 256;
        /**
         * 文件流的Socket队列长度（报文个数），取值范围：1 ~ 32768
         */
        public int SKTBufSize3 = 64;
        /**
         * P2P尝试时间。
         */
        public int P2PTryTime = 1;

        public PG_NODE_CFG() {
            Type = 0;
            Option = 1;
            MaxPeer = 256;
            MaxGroup = 32;
            MaxObject = 512;
            MaxMCast = 512;
            MaxHandle = 256;
            SKTBufSize0 = 128;
            SKTBufSize1 = 64;
            SKTBufSize2 = 256;
            SKTBufSize3 = 64;
        }
    }


    ///------------------------------------------------------
    // Peergine event hook interface.

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
     * eventListener：[IN] 实现了OnEventListner接口的对象，必须定义event函数。
     */

    public interface OnEventListener {
        void event(String sAct, String sData, String sPeer);
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
        return m_Self.sObjSelf;
    }

    /**
     * 描述：设置心跳间隔。
     * 阻塞方式：非阻塞，立即返回
     * iExpire：[IN] 心跳间隔。
     */
    public void SetExpire(int iExpire) {
        if (iExpire < (KEEP_TIMER_INTERVAL * 2)) {
            m_Stamp.iExpire = 0;
        } else {
            m_Stamp.iExpire = iExpire;
        }
    }

    /**
     * 描述：设置消息接收回调接口。
     * 阻塞方式：非阻塞，立即返回
     * @param eventListener：[IN] 实现了OnEventListner接口的对象，必须定义event函数。
     */
    public void SetEventListener(OnEventListener eventListener) {
        m_eventListener = eventListener;
    }

    /**
     * 初始化前可以设置的 中间件 Node的配置参数，一般不用配置。
     * @param mNodeCfg 参数结构体。
     * @return true 成功，false 失败
     */
    public boolean ConfigNode(PG_NODE_CFG mNodeCfg) {
        if (mNodeCfg == null) {
            return false;
        }
        msConfigNode = "Type=" + mNodeCfg.Type +
                ";Option=" + mNodeCfg.Option +
                ";MaxPeer=" + mNodeCfg.MaxPeer +
                ";MaxGroup=" + mNodeCfg.MaxGroup +
                ";MaxObject=" + mNodeCfg.MaxObject +
                ";MaxMCast=" + mNodeCfg.MaxMCast +
                ";MaxHandle=" + mNodeCfg.MaxHandle +
                ";SKTBufSize0=" + mNodeCfg.SKTBufSize0 +
                ";SKTBufSize1=" + mNodeCfg.SKTBufSize1 +
                ";SKTBufSize2=" + mNodeCfg.SKTBufSize2 +
                ";SKTBufSize3=" + mNodeCfg.SKTBufSize3 +
                ";P2PTryTime=" + mNodeCfg.P2PTryTime;
        return true;
    }

    /**
     * 描述：P2P会议对象初始化函数
     * 阻塞方式：非阻塞，立即返回。
     * @param sName：[IN] 会议名称
     * @param sChair：[IN] 主席端设备ID
     * @param sUser：[IN] 登录用户名，自身的设备ID
     * @param sPass：[IN] 登录密码
     * @param sSvrAddr：[IN] 登录服务器地址和端口，格式：x.x.x.x:x
     * @param sRelayAddr：[IN] 转发服务器地址和端口，格式：x.x.x.x:x。
     * 如果传入空字符串，则使用登录服务器的IP地址加上443端口构成转发服务器地址。
     * @param sVideoParam：[IN] 视频参数，格式为：(Code){3}(Mode){2}(Rate){40}(LCode){3}(LMode){2}
     *                       (LRate){40}(CameraNo){0}(Portrait){1}(BitRate){400}
     *                        Code: 视频压缩编码类型：1为MJPEG、2为VP8、3为H264。
     *                        Mode: 视频图像的分辨率（尺寸），有效数值如下：
     *                           0: 80x60, 1: 160x120, 2: 320x240, 3: 640x480,
     *                           4: 800x600, 5: 1024x768, 6: 176x144, 7: 352x288,
     *                            8: 704x576, 9: 854x480, 10: 1280x720, 11: 1920x1080
     *                       FrmRate: 视频的帧间间隔（毫秒）。例如40毫秒的帧率为：1000/40 = 25 fps
     *                       LCode: 不同流视频压缩编码类型：1为MJPEG、2为VP8、3为H264。
     *                       LMode: 不同流视频图像的分辨率（尺寸），有效数值如下：
     *                          0: 80x60, 1: 160x120, 2: 320x240, 3: 640x480,
     *                          4: 800x600, 5: 1024x768, 6: 176x144, 7: 352x288,
     *                          8: 704x576, 9: 854x480, 10: 1280x720, 11: 1920x1080
     *                       LFrmRate: 不同流视频的帧间间隔（毫秒）。例如40毫秒的帧率为：1000/40 = 25 fps
     *                       VideoInExternal: 使能视频的外部采集
     *                       CameraNo: 摄像头编号，CameraInfo.facing的值。
     *                       Portrait: 采集图像的方向。0为横屏，1为竖屏。
     *                       BitRate: 视频压缩后的码率。单位为 Kbps
     *                         AudioSpeechDisable:音频的默认打开状态，0或者不设置为默认打开，1为默认不发送自己的音频给他人，2为默认不接收他人音频，3为默认既不发送给他人，也不接收
     *                       通过AudioSpeech函数改变音频打开状态
     *                      oCtx： Android程序的上下文对象
     * @return true 成功， false 失败
     */

    /**
     * 初始化后需手动加入会议。
     */
    public boolean Initialize(String sUser, String sPass, String sSvrAddr,
                              String sRelayAddr, String sVideoParam, Context oCtx) {
        return Initialize("", "", sUser, sPass, sSvrAddr, sRelayAddr, sVideoParam, oCtx);
    }

    /**
      *  初始化后已经自动加入会议。
      */
    public boolean Initialize(String sName, String sChair, String sUser, String sPass, String sSvrAddr,
                              String sRelayAddr, String sVideoParam, Context oCtx) {
        if (!m_Status.bInitialized) {
            if ((!"".equals(sUser)) && sUser.length() < 100) {
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
                    m_NodeProc = new pgLibNodeProc();
                } catch (Exception ex) {
                    m_Node = null;
                    m_NodeProc = null;
                    TimerOutDel(timerOut);
                    TimerClean();
                    return false;
                }

                // Init status
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
            }
        } else {
            _OutString("->Initialize :Initialized = true");
        }
        m_Status.bInitialized = true;
        return true;
    }

    /**
     * 描述：P2P会议对象清理函数
     * 阻塞方式：非阻塞，立即返回。
     */
    public void Clean() {
        try {
            _NodeDispClean();
            _NodeStop();
            TimerOutDel(timerOut);
            TimerClean();
            //pgLibJNINode.Clean();

            if (m_bJNILibInited) {
                pgLibNode.NodeLibClean();
                m_bJNILibInited = false;
            }
            m_Status.restore();
        }catch (Exception ex){
            _OutString("pgLibConfernece.Clean : ex = " + ex.toString());
        }
    }

    // Create preview for node.

    /**
     * 描述：创建播放窗口对象
     * 阻塞方式：阻塞
     * @param iW：[IN] 窗口宽度
     * @param iH：[IN] 窗口高度
     * 返回值：SurfaceView对象，可加入到程序主View中
     */
    public SurfaceView PreviewCreate(int iW, int iH) {
        if (m_Node != null) {
            return (SurfaceView) m_Node.WndNew(0, 0, iW, iH);
        }
        return null;
    }

    /**
     * 描述：销毁播放窗口对象
     * 阻塞方式：阻塞
     */
    public void PreviewDestroy() {
        if (m_Node != null) {
            m_Node.WndDelete();
        }
    }

    /**
     *  描述：开始会议，初始化视音频等会议相关数据。
     *  阻塞方式：非阻塞
     *  @return ：true 成功  false 失败
     */
    public boolean Start(String sName, String sChair) {
        m_Group.Init(sName, sChair, m_Self.sUser);
        m_Stamp.restore();
        return !m_Group.bEmpty && (_ServiceStart()!=PG_ERR_Normal);
    }

    /**
     *  描述：停止会议，初始化视音频等会议相关数据。
     *  阻塞方式：非阻塞
     *  返回值：true 成功  false 失败
     */
    public void Stop() {
        _ServiceStop();
        m_Group.bEmpty = true;
    }

    /**
     * 描述：通过节点名与其他节点建立联系 （节点名在我们P2P网络的功能类似英特网的IP地址）
     * 阻塞方式：非阻塞。
     * @param sPeer: 对端的节点名（用户名 ID）
     * @return true 成功，失败
    */
    public boolean PeerAdd(String sPeer) {
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
    public void PeerDelete(String sPeer) {
        if (m_Node != null) {
            if (!"".equals(sPeer)) {
                String sObjPeer = _ObjPeerBuild(sPeer);

                m_Node.ObjectDelete(sObjPeer);
            }
        }
    }

    /**
     * 描述：添加成员（主席端）
     * 阻塞方式：非阻塞，立即返回
     * @param sMember ：[IN] 成员名
     * 返回值： true 操作成功，false 操作失败
     */
    public boolean MemberAdd(String sMember) {
        boolean bRet = false;
        if (m_Status.bServiceStart && m_Group.bChairman) {

            if ("".equals(sMember)) {
                _OutString("No Group or sMember name");
                return false;
            }
            String sObjMember = _ObjPeerBuild(sMember);

            int uMask = 0x0200; // Tell all.
            String sDataMdf = "(Action){1}(PeerList){(" + m_Node.omlEncode(sObjMember) + "){" + uMask + "}}";
            int iErr = m_Node.ObjectRequest(m_Group.sObjG, 32, sDataMdf, "");
            if (iErr > 0) {
                _OutString("MemberAdd: Add group member failed err=" + iErr);
            } else {
                bRet = true;
            }
        }
        return bRet;
    }

    /**
     * 描述：删除成员（主席端）
     * @param sMember ：[IN] 成员名
     * 阻塞方式：非阻塞，立即返回
     */
    public void MemberDel(String sMember) {
        if (m_Status.bServiceStart && m_Group.bChairman) {
            if ("".equals(sMember)) {
                _OutString("No Group or sMember name");
                return;
            }
            String sObjMember = _ObjPeerBuild(sMember);

            String sDataMdf = "(Action){0}(PeerList){(" + m_Node.omlEncode(sObjMember) + "){}}";

            int iErr = this.m_Node.ObjectRequest(m_Group.sObjG, 32, sDataMdf, "");
            if (iErr > 0) {
                _OutString("MemberDel: Add group member failed err=" + iErr);
            }
        }
    }

    //private boolean m_IsJoin = false;

    /**
     * 描述：请求加入会议
     * 阻塞方式：非阻塞，立即返回
     */
    public boolean Join() {
        if (m_Status.bServiceStart && !m_Group.bChairman) {

            String sData = "Join?" + m_Self.sObjSelf;
            int iErr = this.m_Node.ObjectRequest(m_Group.sObjChair, 36, sData, "pgLibConference.Join");
            if (iErr > 0) {
                _OutString("pgLibConference.Join:ObjectRequest Err=" + iErr);
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * 描述：离开会议
     * 阻塞方式：非阻塞，立即返回
     */
    public void Leave() {
        if (m_Status.bServiceStart) {

            String sDataMdf = "(Action){0}(PeerList){(" + m_Self.sObjSelf + "){}}";
            int iErr = this.m_Node.ObjectRequest(m_Group.sObjG, 32, sDataMdf, "");
            if (iErr > 0) {
                _OutString("Leave: Leave group member failed err=" + iErr);
            }
        }
    }

    /**
     *
     *  描述：切换会议和主席
     *  @param sName 会议名称
     *  @param sChair 主席ID
     *  阻塞方式：非阻塞，立即返回
     *   返回值： true 操作成功，false 操作失败
     */
    public boolean Reset(String sName, String sChair) {

        if (m_Status.bServiceStart) {
            _ServiceStop();
        }
        m_Group.Init(sName, sChair, m_Self.sUser);
        m_Stamp.restore();
        if (!m_Group.bEmpty) {
            if (_ServiceStart()!=PG_ERR_Normal) {
                return true;
            }
        }
        return false;
    }

    /**
     * 描述：初始化视频设置
     * @param iFlag [in] 参考1）静态成员定义：
     * 阻塞方式：非阻塞，立即返回
     * @return  true 操作成功，false 操作失败
     */
    public boolean VideoStart(int iFlag) {
        if (!m_Status.bServiceStart) {
            _OutString("VideoStart: Not initialize");
            return false;
        }

        if (!m_Status.bApiVideoStart) {
            if (!_VideoInit(iFlag)) {
                return false;
            }
            m_Status.bApiVideoStart = true;
        }
        return true;
    }

    /**
     * 描述：停止播放和采集视频
     * 阻塞方式：非阻塞，立即返回
     */
    public boolean VideoStop() {
        _OutString("->VideoStop");
        if (m_Status.bApiVideoStart) {
            this._VideoClean();
            m_Status.bApiVideoStart = false;
        }
        return true;
    }

    /**
     * 描述：打开某一成员的视频
     * 阻塞方式：非阻塞，立即返回
     * 返回值： true 操作成功，false 操作失败
     * sObjPeer:成员节点名
     * iW: 窗口宽度
     * iH: 窗口高度
     */
    public SurfaceView VideoOpen(String sPeer, int iW, int iH) {
        return VideoOpen(sPeer, iW, iH, false);
    }

    /**
     * 描述：以不同流打开某一成员的视频（请求端有效）
     * 阻塞方式：非阻塞，立即返回
     * 返回值： true 操作成功，false 操作失败
     * sObjPeer:成员节点名
     * iW: 窗口宽度
     * iH: 窗口高度
     */
    public SurfaceView VideoOpenL(String sPeer, int iW, int iH) {
        return VideoOpen(sPeer, iW, iH, true);
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
                    try {
                        oPeer = new PG_PEER(sObjPeer);
                    } catch (Exception ex) {
                        break;
                    }
                    m_listVideoPeer.add(oPeer);
                }

                int iErr = 0;
                String sData;

                // Create the node and view.专门用来显示视频的node
                if (oPeer.Node == null) {
                    try {
                        oPeer.Node = new pgLibJNINode();
                    } catch (Exception ex) {
                        oPeer.Node = null;
                        m_listVideoPeer.remove(oPeer);
                        break;
                    }
                }
                if (oPeer.View == null) {
                    oPeer.View = (SurfaceView) oPeer.Node.WndNew(0, 0, iW, iH);
                }
                //
                if (oPeer.View != null) {
                    sData = "(Peer){" + m_Node.omlEncode(sObjPeer) + "}(Wnd){" + oPeer.Node.utilGetWndRect() + "}";
                    _OutString("VideoOpen: sData=" + sData);
                } else {
                    iErr = 13;
                    sData = "";
                    _OutString("pgLibConference.VideoOpen: New node wnd failed!");
                }

                String sObjV;
                boolean bJoinRes = false;
                if (oPeer.iHandle > 0) {
                    if (oPeer.bLarge) {
                        sObjV = m_Group.sObjLV;
                    } else {
                        sObjV = m_Group.sObjV;
                    }
                    _OutString("Video open Relay iHandle=" + oPeer.iHandle);
                    int iErrTemp = this.m_Node.ObjectExtReply(sObjV, iErr, sData, oPeer.iHandle);
                    if (iErrTemp <= 0) {
                        if (iErr == 0) {
                            bJoinRes = true;
                        }
                    } else {
                        _OutString("pgLibConference.VideoOpen: Reply, iErr=" + iErrTemp);
                    }
                } else {

                    if (bLarge) {
                        sObjV = m_Group.sObjLV;
                    } else {
                        sObjV = m_Group.sObjV;
                    }
                    oPeer.bLarge = bLarge;
                    String sParamTmp = "VideoOpen:" + sPeer;
                    int iErrTemp = m_Node.ObjectRequest(sObjV, 35, sData, sParamTmp);
                    if (iErrTemp <= 0) {
                        bJoinRes = true;
                    } else {
                        _OutString("pgLibConference.VideoOpen: Request, iErr=" + iErrTemp);
                    }
                }
                if (bJoinRes) {
                    oPeer.iActStamp = m_Stamp.iActiveStamp;
                    // Reset request status.
                    oPeer.iStamp = 0;
                    oPeer.iHandle = 0;
                    retView = oPeer.View;
                    _OutString("VideoOpen: scussce");
                } else {
                    oPeer.Node.WndDelete();
                    m_listVideoPeer.remove(oPeer);
                }
            } while (false);
        }
        return retView;

    }


    /**
     * 描述：拒绝打开某一成员的视频
     * 阻塞方式：非阻塞，立即返回
     * 返回值： true 操作成功，false 操作失败
     * @param sPeer 成员节点名
     */
    public void VideoReject(String sPeer) {
        _OutString("->VideoReject");
        if (m_Status.bApiVideoStart) {

            if ("".equals(sPeer)) {
                _OutString("sObjPeer no chars");
                return;
            }

            if (!m_Status.bApiVideoStart) {
                _OutString("Video not init!");
                return;
            }
            String sObjPeer = _ObjPeerBuild(sPeer);

            PG_PEER oPeer = _VideoPeerSearch(sObjPeer);
            if (oPeer == null) {
                return;
            }

            String sObjV;
            if (oPeer.iHandle > 0) {
                // Join reply.
                if (oPeer.bLarge) {
                    sObjV = m_Group.sObjLV;
                } else {
                    sObjV = m_Group.sObjV;
                }
                _OutString("Video open Relay iHandle=" + oPeer.iHandle);
                int iErrTemp = this.m_Node.ObjectExtReply(sObjV, 13, "", oPeer.iHandle);
                if (iErrTemp > 0) {
                    _OutString("pgLibConference.VideoReject: Reply, iErr=" + iErrTemp);
                }

                oPeer.restore(m_Stamp.iActiveStamp);
                m_listVideoPeer.remove(oPeer);
            }
        }
    }

    /**
     * 描述：关闭某一成员视频
     * 阻塞方式：非阻塞，立即返回
     * @param sPeer 节点ID或对象
     */
    public void VideoClose(String sPeer) {

        if (m_Status.bApiVideoStart) {
            String sObjPeer = _ObjPeerBuild(sPeer);
            PG_PEER oPeer = _VideoPeerSearch(sObjPeer);
            _VideoClose(oPeer);
        } else {
            _OutString("VideoClose: m_Status.bApiVideoStart false!");
        }
    }

    /**
     * 描述：获取已打开成员视频的View
     * 阻塞方式：非阻塞，立即返回
     * @param sPeer 节点ID或对象
     * 返回值： true 操作成功，false 操作失败
     */
    public SurfaceView VideoGetView(String sPeer) {
        SurfaceView view = null;
        if (m_Status.bApiVideoStart) {
            String sObjPeer = _ObjPeerBuild(sPeer);

            PG_PEER oCtrl = _VideoPeerSearch(sObjPeer);
            if (oCtrl != null) {
                view = oCtrl.View;
            }
        } else {
            _OutString("VideoClose: Video no start!");
        }
        return view;
    }

    /**
     * 描述：摄像头切换
     * 阻塞方式：非阻塞，立即返回
     *
     * @param iCameraNo：摄像头编号
     * @return true成功，false失败
     */
    public boolean VideoSource(int iCameraNo) {
        if (m_Node != null) {
            if (m_Node.ObjectAdd("_vTemp_1", "PG_CLASS_Video", "", 0x2)) {
                m_Node.ObjectRequest("_vTemp_1", 2, "(Item){0}(Value){" + iCameraNo + "}", "");
                m_Node.ObjectDelete("_vTemp_1");
                return true;
            }
        }
        return false;
    }

    /**
     * 描述:采集图像角度切换
     * 阻塞方式：非阻塞，立即返回
     * @param  iAngle:角度
     */
    public void VideoSetRotate(int iAngle) {
        if (m_Node != null) {
            if (m_Node.ObjectAdd("_vTemp", "PG_CLASS_Video", "", 0)) {
                m_Node.ObjectRequest("_vTemp", 2, "(Item){2}(Value){" + iAngle + "}", "");
                m_Node.ObjectDelete("_vTemp");
            }
        }
    }

    /**
     * 描述：控制成员的视频流
     * 阻塞方式：非阻塞，立即返回
     * @param sPeer 节点ID或对象
     * @param bEnable 是否接收和发送视频流
     */
    public boolean VideoControl(String sPeer, boolean bEnable) {

        if (m_Status.bApiVideoStart) {
            if ("".equals(sPeer)) {
                return false;
            }
            String sObjPeer = _ObjPeerBuild(sPeer);

            int iFlag = bEnable ? 1 : 0;


            String sIn = "(Peer){" + m_Node.omlEncode(sObjPeer) + "}(Local){" + iFlag + "}(Remote){" + iFlag + "}";
            m_Node.ObjectRequest(m_Group.sObjLV, 39, sIn, "VideoControl");
            m_Node.ObjectRequest(m_Group.sObjV, 39, sIn, "VideoControl");
            return true;
        } else {
            _OutString("VideoControl -> Video Not Start");
        }
        return false;
    }

    /**
     * 描述：抓拍 sObjPeer 节点的图片
     * 阻塞方式：非阻塞，立即返回
     * @param sPeer 节点名
     * @param sPath 路径
     * @return true 成功 false 失败
     */
    public boolean VideoCamera(String sPeer, String sPath) {
        boolean bRet = false;
        if (m_Status.bApiVideoStart) {
            String sObjPeer = _ObjPeerBuild(sPeer);

            String sPathTemp = sPath;
            if (sPathTemp.lastIndexOf(".jpg") < 0 && sPathTemp.lastIndexOf(".JPG") < 0) {
                sPathTemp += ".jpg";
            }

            String sObjV;
            PG_PEER oPeer = _VideoPeerSearch(sObjPeer);
            if (oPeer != null) {

                if (oPeer.bLarge) {
                    sObjV = m_Group.sObjLV;
                } else {
                    sObjV = m_Group.sObjV;
                }
                String sIn = "(Peer){" + m_Node.omlEncode(sObjPeer) + "}(Path){" + m_Node.omlEncode(sPathTemp) + "}";
                int iErr = m_Node.ObjectRequest(sObjV, 37, sIn, "VideoCamera:" + sPeer);
                if (iErr != 0) {
                    _OutString("VideoCamera Error  = " + iErr);
                } else {
                    bRet = true;
                }
            } else {
                _OutString("VideoCamera:this Peer Video not open!");
            }
        }
        return bRet;
    }

    // Start and stop audio

    /**
     * 描述：开始播放或采集音频
     * 阻塞方式：非阻塞，立即返回
     * @return true 操作成功，false 操作失败
     */
    public boolean AudioStart() {
        if (!m_Status.bApiAudioStart) {
            if (!this._AudioInit()) {
                return false;
            }
        }
        m_Status.bApiAudioStart = true;
        return true;
    }

    /**
     * 描述：停止播放或采集音频
     * 阻塞方式：非阻塞，立即返回
     */
    public void AudioStop() {
        if (m_Status.bApiAudioStart) {
            this._AudioClean();
            m_Status.bApiAudioStart = false;
        }
    }

    /**
     * 描述：AudioCtrlVolume控制自身的扬声器和麦克风是否播放或采集声音数据
     * 阻塞方式：非阻塞，立即返回
     *
     * @param sPeer   节点名 （在麦克风下为空则表示控制本端的麦克风音量。 ）
     * @param iType   0表示扬声器 1表示麦克风
     * @param iVolume 表示音量的百分比
     */
    public boolean AudioCtrlVolume(String sPeer, int iType, int iVolume) {

        if (m_Status.bApiAudioStart) {
            String sObjPeer = _ObjPeerBuild(sPeer);

            iType = iType > 0 ? 1 : 0;

            iVolume = iVolume < 0 ? 0 : iVolume;//iVolume防止参数小于0
            iVolume = iVolume > 100 ? 100 : iVolume;//大于100 取100
            String sData = "(Peer){" + m_Node.omlEncode(sObjPeer) + "}(Action){1}(Type){" + iType + "}(Volume){" + m_Node.omlEncode(iVolume + "")
                    + "}(Max){0}(Min){0}";
            int iErr = m_Node.ObjectRequest(m_Group.sObjA, 34, sData, "AudioCtrlVolume");
            if (iErr > 0) {
                _OutString("AudioCtrlVolume:set Volume, iErr=" + iErr);
            }
        } else {
            _OutString("Audio not init");
            return false;
        }
        return true;

    }

    public void AudioSpeechDisable(int iDisableMode) {
        m_Self.iAudioSpeechDisable = iDisableMode;
    }
    //使指定peer端不播放本端的音频

    /**
     * 描述：控制某个节点是否能播放本节点的音频，本节点能播放对方的音频
     * 阻塞方式：非阻塞，立即返回
     *
     * @param sPeer：节点名
     * @param bSendEnable: true接收 ，false不接收
     *                     返回值： true 操作成功，false 操作失败
     */
    public boolean AudioSpeech(String sPeer, boolean bSendEnable) {
        return AudioSpeech(sPeer, bSendEnable, true);
    }

    /**
     * 描述：控制某个节点是否能播放本节点的音频，本节点能否播放对方的音频
     * 阻塞方式：非阻塞，立即返回
     *
     * @param sPeer：节点名
     * @param bSendEnable: true接收 ，false不接收
     * @param bRecvEnable  返回值： true 操作成功，false 操作失败
     */
    public boolean AudioSpeech(String sPeer, boolean bSendEnable, boolean bRecvEnable) {

        if (m_Status.bApiAudioStart) {
            String sObjPeer = _ObjPeerBuild(sPeer);

            boolean bRet = false;
            int iSendEnable = bSendEnable ? 1 : 0;
            int iRecvEnable = bRecvEnable ? 1 : 0;
            String sData = "(Peer){" + m_Node.omlEncode(sObjPeer) + "}(ActSelf){" + iSendEnable + "}(ActPeer){" + iRecvEnable + "}";
            int iErr = m_Node.ObjectRequest(m_Group.sObjA, 36, sData, "Speech");
            if (iErr > 0) {
                _OutString("Speech: Set Speech, iErr=" + iErr);
            }

            return true;
        } else {
            _OutString("Audio not init");
        }
        return false;
    }

    /**
     * 设置音频采样率
     * @param iRate 采样率大小
     */
    public void AudioSetSampleRate(int iRate) {
        if (m_Node != null) {
            // Set microphone sample rate
            if (m_Node.ObjectAdd("_AudioTemp", "PG_CLASS_Audio", "", 0)) {
                m_Node.ObjectRequest("_AudioTemp", 2, "(Item){2}(Value){" + iRate + "}", "");
                m_Node.ObjectDelete("_AudioTemp");
            }
        }
    }

    //------------------------------------------------------
    // Record handle functions.

    /**
     * 开始录制
     * @param sID 录制端ID ID为本身则录制本端视频，要求：视频通话正在进行。
     * @param sAviPath 视频保存路径
     * @param iMode 录制模式，0 同时录制视音频；1 只录制视频；2 只录制音频
     * @return 错误码 @link pgLibError
     */
    public int RecordStart( String sID, String sAviPath,int iMode) {
        if (!m_Status.bServiceStart) {
            _OutString("RecordStart: Not initialize");
            return PG_ERR_BadStatus;
        }

        if (sAviPath.lastIndexOf(".avi") <= 0
                && sAviPath.lastIndexOf(".mov") <= 0
                && sAviPath.lastIndexOf(".mp4") <= 0)
        {
            _OutString("RecordStart: invalid avi path. sAviPath=" + sAviPath);
            return PG_ERR_BadParam;
        }

        String sRec = _RecordListSearch(iMode,sID);
        if (!"".equals(sRec)) {
            return PG_ERR_Normal;
        }
        String sObjPeer = _ObjPeerBuild(sID);
        String sObjVideo = m_Group.sObjV;
        PG_PEER oPeer = _VideoPeerSearch(sObjPeer);
        if (oPeer != null) {
            if (oPeer.bLarge) {
                sObjVideo = m_Group.sObjLV;
            } else {
                sObjVideo = m_Group.sObjV;
            }
        }

        String sObjAudio = m_Group.sObjA;
        int iHas = (iMode == PG_RECORD_NORMAL)?1:0;


        boolean bRecord = false;
        if (iMode == PG_RECORD_NORMAL ||iMode == PG_RECORD_ONLYVIDEO) {
            String sIn = "(Peer){" + m_Node.omlEncode(sObjPeer) + "}(Path){" + m_Node.omlEncode(sAviPath) + "}(HasAudio){" + iHas + "}";
            int iErr = m_Node.ObjectRequest(sObjVideo,
                    38, sIn, "RecordStartVideo");
            if (iErr > PG_ERR_Normal) {
                _OutString("RecordStartVideo: iErr=" + iErr);
                return iErr;
            }
            bRecord = true;
        }

        if (iMode == PG_RECORD_NORMAL ||iMode == PG_RECORD_ONLYAUDIO) {

            String sIn = "(Peer){" + m_Node.omlEncode(sObjPeer) + "}(Path){" + m_Node.omlEncode(sAviPath) + "}(HasVideo){" + iHas + "}";

            int iErr = m_Node.ObjectRequest(sObjAudio,
                    37, sIn, "RecordStartAudio");
            if (iErr > PG_ERR_Normal) {
                m_Node.ObjectRequest(sObjVideo, 36, "(Path){}", "RecordStopVideo");
                _OutString("RecordStartAudio: iErr=" + iErr);
                return iErr;
            }
            bRecord = true;
        }

        if (bRecord) {
            _RecordListAdd(iMode,sID);
        }

        return PG_ERR_Normal;
    }

    /**
     * 停止录制
     * @param sID 录制ID ,本端录制填本端ID。
     * @param iMode 录制模式
     */
    public void RecordStop(String sID,int iMode) {
        if (!m_Status.bServiceStart) {
            _OutString("RecordStop: Not initialize");
            return;
        }

        String sRec = _RecordListSearch(iMode,sID);
        if ("".equals(sRec)) {
            return;
        }

        String sObjPeer = _ObjPeerBuild(sID);


        if (iMode == PG_RECORD_NORMAL ||iMode == PG_RECORD_ONLYVIDEO) {
            String sObjVideo = m_Group.sObjV;
            PG_PEER oPeer = _VideoPeerSearch(sObjPeer);
            if (oPeer != null) {
                if (oPeer.bLarge) {
                    sObjVideo = m_Group.sObjLV;
                } else {
                    sObjVideo = m_Group.sObjV;
                }
            }

            int iErr = m_Node.ObjectRequest(sObjVideo,
                    38, "(Peer){" + m_Node.omlEncode(sObjPeer) + "}(Path){}", "RecordStopVideo");
            if (iErr > PG_ERR_Normal) {
                _OutString("RecordStopVideo: iErr=" + iErr);
            }
        }

        if (iMode == PG_RECORD_NORMAL ||iMode == PG_RECORD_ONLYAUDIO) {
            String sObjAudio = m_Group.sObjA;
            int iErr = m_Node.ObjectRequest(sObjAudio,
                    37, "(Peer){" + m_Node.omlEncode(sObjPeer) + "}(Path){}",
                    "RecordStopAudio");
            if (iErr > PG_ERR_Normal) {
                _OutString("RecordStopAudio: iErr=" + iErr);
            }
        }

        _RecordListDelete(iMode,sID);
    }


    /**
     * 描述：摄像头控制。
     * 阻塞方式：非阻塞，立即返回
     *
     * @param bEnable
     * @return true 操作成功，false 操作失败
     */
    public boolean CameraSwitch(boolean bEnable) {

        boolean bRet = false;
        if (m_Node != null) {
            if (m_Node.ObjectAdd("_vSwitch", "PG_CLASS_Video", "", 0)) {

                int iEnable = bEnable ? 1 : 0;
                String sData = "(Item){9}(Value){" + iEnable + "}";
                int iErr = m_Node.ObjectRequest("_vSwitch", 2, sData, "SetOption");
                if (iErr > 0) {
                    _OutString("CameraSwitch: Set option, iErr=" + iErr);
                } else {
                    bRet = true;
                }

                m_Node.ObjectDelete("_vSwitch");
            }
        }
        return bRet;
    }

    /**
     * 描述：给指定节点发送消息
     * 阻塞方式：非阻塞，立即返回
     *
     * @param sMsg：[IN]      消息内容
     * @param sPeer：[IN]节点名称 返回值： true 操作成功，false 操作失败
     */
    public boolean MessageSend(String sMsg, String sPeer) {
        boolean bRet = false;
        if (m_Node != null) {
            String sObjPeer = _ObjPeerBuild(sPeer);
            String sData = "Msg?" + sMsg;
            int iErr = m_Node.ObjectRequest(sObjPeer, 36, sData, "");
            if (iErr > 0) {
                if (iErr == 5 && !m_Group.bEmpty && !m_Group.bChairman) {
                    _ChairPeerCheck();
                }
                _OutString("MessageSend: iErr=" + iErr);

            } else {
                bRet = true;
            }
        }
        return bRet;
    }

    /**
     * 描述：给指定节点发送消息
     * 阻塞方式：非阻塞，立即返回
     *
     * @param sMsg：[IN] 消息内容
     * @param sPeer：[IN]节点名称
     * @param sSession:[IN]可以为空，发送成功后可以收到CallSend事件，sSession 为sData = sSession+":"+错误码 0表示正常成功
     *                                                       返回值： true 操作成功，false 操作失败
     */
    public boolean CallSend(String sMsg, String sPeer, String sSession) {
        boolean bRet = false;
        if (m_Node != null) {

            String sObjPeer = _ObjPeerBuild(sPeer);

            String sData = "Msg?" + sMsg;
            int iErr = m_Node.ObjectRequest(sObjPeer, 35, sData, "CallSend:" + sSession);
            if (iErr > 0) {
                _OutString("CallSend: iErr=" + iErr);
            } else {
                bRet = true;
            }
        }
        return bRet;
    }

    /**
     * 描述：给其他所有成员节点节点发送消息
     * 阻塞方式：非阻塞，立即返回
     *
     * @param sData：[IN] 消息内容
     * @return true 操作成功，false 操作失败
     */
    public boolean NotifySend(String sData) {
        boolean bRet = false;
        if (m_Status.bServiceStart) {
            int iErr = m_Node.ObjectRequest(m_Group.sObjD, 32, sData, "NotifySend:" + m_Self.sObjSelf);
            if (iErr > 0) {
                _OutString("NotifySend: iErr=" + iErr);
            } else {
                bRet = true;
            }
        }
        return bRet;

    }

    /**
     * 描述：给服务器发送消息。
     * 阻塞方式：非阻塞，立即返回
     * @param sData 发送到服务器的消息内容
     * @return true 操作成功，false 操作失败
     */
    public boolean SvrRequest(String sData) {
        boolean bRet = false;
        if (m_Node != null) {

            int iErr = m_Node.ObjectRequest(m_Svr.sSvrName, 35, ("1024:" + sData), "SvrRequest");
            if (iErr > 0) {
                _OutString("SvrRequest: iErr=" + iErr);
            } else {
                bRet = true;
            }
        }
        return bRet;
    }


    // Scan the captures in the same lan.
    public boolean LanScanStart() {
        boolean bRet = false;
        if (m_Node != null) {

            if (!m_LanScan.bApiLanScan) {
                int iErr = m_Node.ObjectRequest(m_Svr.sSvrName, 42, "(Timeout){5}", "LanScan");
                if (iErr > 0) {
                    _OutString("LanScanStart: iErr=" + iErr);
                } else {
                    m_LanScan.bApiLanScan = true;
                    bRet = true;
                }
            } else {
                bRet = true;
            }
        }
        return bRet;
    }

    //------------------------------------------------------
    // File transfer functions.

    public int FilePutRequest(String sChairID,String sID, String sPath, String sPeerPath) {
        if (!m_Status.bInitialized) {
            _OutString("FilePutRequest: Not initialize");
            return PG_ERR_BadStatus;
        }

        return _FileRequest(sChairID, sID, sPath, sPeerPath, 32);
    }

    public int FileGetRequest(String sChairID,String sID, String sPath, String sPeerPath) {
        if (!m_Status.bInitialized) {
            _OutString("FileGetRequest: Not initialize");
            return PG_ERR_BadStatus;
        }

        return _FileRequest(sChairID,sID, sPath, sPeerPath, 33);
    }

    public int FileAccept(String sChairID,String sID, String sPath) {
        if (!m_Status.bInitialized) {
            _OutString("FileAccept: Not initialize");
            return PG_ERR_BadStatus;
        }

        return _FileReply(PG_ERR_Normal, sChairID,sID, sPath);
    }

    public int FileReject(String sChairID,String sID, int iErrCode) {
        if (!m_Status.bInitialized) {
            _OutString("FileReject: Not initialize");
            return PG_ERR_BadStatus;
        }

        int iErrTemp = (iErrCode > PG_ERR_Normal) ? iErrCode : PG_ERR_Reject;
        return _FileReply(iErrTemp, sChairID,sID, "");
    }

    public int FileCancel(String sChairID,String sID) {
        if (!m_Status.bInitialized) {
            _OutString("FileCancel: Not initialize");
            return PG_ERR_BadStatus;
        }

        return _FileCancel(sChairID,sID);
    }

    public String Version() {
        if (!m_Status.bInitialized) {
            return "";
        }

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

    ///------------------------------------------------------------------------
    // Static function
    //处理int
    private static int _ParseInt(String sInt, int iDef) {
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
    private static void _OutString(String sOut) {
        if (BuildConfig.DEBUG) {
            Log.d("pgLibConference", sOut);
        }
    }

    //定时器处理程序
    private final TimerOut timerOut = new TimerOut() {
        @Override
        public void TimerProc(String sParam) {
            String sAct = m_Node.omlGetContent(sParam, "Act");
            if ("Keep".equals(sAct)) {
                _Keep();
            } else if ("TimerActive".equals(sAct)) {
                _TimerActive();
            } else if ("ChairPeerCheck".equals(sAct)) {
                _ChairPeerCheckTimeout();
            } else if ("ChairmanAdd".equals(sAct)) {
                _ChairmanAdd();
            } else if ("Relogin".equals(sAct)) {
                _NodeLogin();
            }

        }
    };

    //事件下发程序
    private void _EventProc(String sAct, String sData, String sPeer) {
        if (m_eventListener != null && m_Status.bEventEnable) {
            //OutString("EventProc: sAct=" + sAct + ", sData=" + sData + ", sObjPeer=" + sObjPeer);
            m_eventListener.event(sAct, sData, sPeer);
        }
    }

    // Set capture extend option.
    //摄像头参数设置
    private void _VideoOption() {
        if (m_Node != null) {

            if (m_Node.ObjectAdd("_vTemp", "PG_CLASS_Video", "", 0)) {
                if (m_Self.iVideoFrmRate != 0) {
                    m_Node.ObjectRequest("_vTemp", 2, "(Item){4}(Value){" + m_Self.iVideoFrmRate + "}", "");

                    String sParam = "(FrmRate){" + m_Self.iVideoFrmRate + "}(KeyFrmRate){4000}";
                    m_Node.ObjectRequest("_vTemp", 2, "(Item){5}(Value){" + m_Node.omlEncode(sParam) + "}", "");
                }
                if (m_Self.iVideoBitRate != 0) {
                    String sParam = "(BitRate){" + m_Self.iVideoBitRate + "}";
                    m_Node.ObjectRequest("_vTemp", 2, "(Item){5}(Value){" + m_Node.omlEncode(sParam) + "}", "");
                }
                if (m_Self.bVideoPortrait != 0) {
                    int angle = m_Self.bVideoPortrait * 90;
                    m_Node.ObjectRequest("_vTemp", 2, "(Item){2}(Value){" + angle + "}", "");
                } else if (m_Self.bVideoRotate != 0) {
                    m_Node.ObjectRequest("_vTemp", 2, "(Item){2}(Value){" + m_Self.bVideoRotate + "}", "");
                }
                if (m_Self.iCameraNo == CAMERA_FACING_FRONT
                        || m_Self.iCameraNo == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    m_Node.ObjectRequest("_vTemp", 2, "(Item){0}(Value){" + m_Self.iCameraNo + "}", "");
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
            m_Node.Local = "Addr=0:0:0:127.0.0.1:0:0";
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
            if (!_NodeLogin()) {
                _OutString("NodeStart: failed.");
                _NodeStop();
                return false;
            }

            // Enable LAN scan.
            String sValue = "(Enable){1}(Peer){" + m_Node.omlEncode(m_Svr.sSvrName) + "}(Label){pgConf}";
            String sData = "(Item){1}(Value){" + m_Node.omlEncode(sValue) + "}";
            int iErr = m_Node.ObjectRequest(m_Svr.sSvrName, 2, sData, "EnableLanScan");
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

    //节点 登录
    private boolean _NodeLogin() {
        _OutString("->NodeLogin");
        boolean bRet = false;
        if (m_Node != null) {

            String sVersion = "";
            String sVerTemp = m_Node.omlGetContent(m_Node.utilCmd("Version", ""), "Version");
            if (sVerTemp.length() > 1) {
                sVersion = sVerTemp.substring(1);
            }

            String sParamTemp = "(Ver){" + sVersion + "." + LIB_VER + "}";
            String sData = "(User){" + m_Self.sObjSelf + "}(Pass){" + m_Self.sPass + "}(Param){" + m_Node.omlEncode(sParamTemp) + "}";
            int iErr = m_Node.ObjectRequest(m_Svr.sSvrName, 32, sData, "NodeLogin");
            if (iErr > 0) {
                _OutString("NodeLogin: Login failed. iErr=" + iErr);
            } else {
                bRet = true;
            }
        }
        return bRet;
    }

    //节点下线
    private void _NodeLogout() {
        _OutString("->NodeLogout");
        if (m_Node != null) {
            m_Node.ObjectRequest(m_Svr.sSvrName, 33, "", "NodeLogout");
            if (m_Status.bLogined) {
                _EventProc(EVENT_LOGOUT, "", "");
            }
            m_Status.bLogined = false;
        }
    }

    //节点重新登录
    private void _NodeRelogin(int iDelay) {
        _OutString("->NodeRelogin!");
        _NodeLogout();
        TimerStart("(Act){Relogin}", iDelay, false);
    }

    //重新配置节点信息
    private void _NodeRedirect(String sRedirect) {

        if (m_Node != null) {
            _NodeLogout();

            String sSvrName = m_Node.omlGetContent(sRedirect, "SvrName");
            if (!"".equals(sSvrName) && !sSvrName.equals(m_Svr.sSvrName)) {
                m_Node.ObjectDelete(m_Svr.sSvrName);
                if (!m_Node.ObjectAdd(sSvrName, "PG_CLASS_Peer", "", (0x10000 | 0x2))) {
                    _OutString("pgLibConference.NodeRedirect: Add server object failed");
                    return;
                }
                m_Svr.sSvrName = sSvrName;
                m_Svr.sSvrAddr = "";
            }
            String sSvrAddr = m_Node.omlGetContent(sRedirect, "SvrAddr");
            if (!"".equals(sSvrAddr) && !sSvrAddr.equals(m_Svr.sSvrAddr)) {
                String sData = "(Addr){" + sSvrAddr + "}(Proxy){}";
                int iErr = m_Node.ObjectRequest(m_Svr.sSvrName, 37, sData, "pgLibConference.NodeRedirect");
                if (iErr > 0) {
                    _OutString("pgLibConference.NodeRedirect: Set server address. iErr=" + iErr);
                    return;
                }
                m_Svr.sSvrAddr = sSvrAddr;
            }

            _OutString("NodeRedirect: sSvrName=" + sSvrName + ", sSvrAddr=" + sSvrAddr);

            TimerStart("(Act){Relogin}", 1, false);
        } else {
            _OutString("->NodeRedirect! NODE = null");
        }
    }

    private void _NodeRedirectReset(int iDelay) {
        if (!m_Svr.sSvrAddr.equals(m_InitSvr.sSvrAddr)) {
            String sRedirect = "(SvrName){" + m_InitSvr.sSvrName + "}(SvrAddr){" + m_InitSvr.sSvrAddr + "}";
            _NodeRedirect(sRedirect);
        } else {
            if (iDelay != 0) {
                _NodeRelogin(iDelay);
            }
        }
    }

    //登录回复信息
    private int _NodeLoginReply(int iErr, String sData) {
        int iRet = 0;
        if (m_Node != null) {
            if (iErr != 0) {
                _OutString("NodeLoginReply: Login failed. uErr=" + iErr);

                _EventProc(EVENT_LOGIN, String.valueOf(iErr), "");
                if (iErr == 11 || iErr == 12 || iErr == 14) {
                    _NodeRedirectReset(10);
                }
            } else {
                // Process redirect.
                String sParam = m_Node.omlGetContent(sData, "Param");
                String sRedirect = m_Node.omlGetEle(sParam, "Redirect.", 10, 0);
                if (!"".equals(sRedirect)) {
                    _NodeRedirect(sRedirect);
                    return 1;
                }

                m_Status.bLogined = true;
                _ChairPeerCheck();
                _EventProc(EVENT_LOGIN, "0", m_Svr.sSvrName);
            }
        }

        return iRet;
    }

    //添加主席节点  使之能在加入会议前与主席通信，发送Join信号
    private void _ChairmanAdd() {
        if (!m_Group.bEmpty) {
            if ("PG_CLASS_Peer".equals(m_Node.ObjectGetClass(m_Group.sObjChair))) {
                _PeerSync(m_Group.sObjChair, "", 1);
                _ChairPeerCheck();
            } else {
                if (!this.m_Node.ObjectAdd(this.m_Group.sObjChair, "PG_CLASS_Peer", "", (0x10000))) {
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

    // 建立通讯组 视音频通讯类
    private int _ServiceStart() {
        if (m_Node != null && !m_Group.bEmpty) {
            do {
                m_listSyncPeer.clear();
                m_listVideoPeer.clear();
                if (m_Group.bChairman) {
                    if (!m_Node.ObjectAdd(m_Group.sObjG, "PG_CLASS_Group", "", (0x10000 | 0x10 | 0x4 | 0x1))) {
                        _OutString("ServiceStart: Add group object failed");
                        break;
                    }

                    int iMask = 0x0200; // Tell all.
                    String sDataMdf = "(Action){1}(PeerList){(" + m_Self.sObjSelf + "){" + iMask + "}}";
                    int iErr = m_Node.ObjectRequest(m_Group.sObjG, 32, sDataMdf, "");
                    if (iErr > 0) {
                        _OutString("ServiceStart: Add group Chairman failed");
                        break;
                    }
                } else {
                    if (!m_Node.ObjectAdd(m_Group.sObjG, "PG_CLASS_Group", m_Group.sObjChair, (0x10000 | 0x10 | 0x1))) {
                        _OutString("ServiceStart: Add group object failed");
                        break;
                    }
                    _ChairmanAdd();
                }

                if (!m_Node.ObjectAdd(m_Group.sObjD, "PG_CLASS_Data", m_Group.sObjG, 0)) {
                    _OutString("ServiceStart: Add  Data object failed");
                    break;
                }

                // 开始节点连接状态检测定时器。
                m_Group.iKeepTimer = TimerStart("(Act){Keep}", KEEP_TIMER_INTERVAL, false);
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
            m_Node.ObjectRequest(m_Group.sObjG, 32, sDataMdf, "");

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
        m_Group.iActiveTimer = TimerStart("(Act){TimerActive}", ACTIVE_TIMER_INTERVAL, false);

        m_listLostPeer.clear();

        for (int i = 0; i < m_listVideoPeer.size(); i++) {

            PG_PEER oPeer = m_listVideoPeer.get(i);
            if ((!oPeer.sObjPeer.equals(m_Self.sObjSelf)) && (oPeer.Node != null)) {

                // 超过3倍心跳周期，没有接收到对端的心跳应答，说明与对端之间连接断开了
                if ((m_Stamp.iActiveStamp - oPeer.iActStamp) > (m_Stamp.iActiveExpire * 3) && (!oPeer.bVideoLost)) {
                    m_listLostPeer.add(oPeer.sObjPeer);
                    oPeer.bVideoLost = true;
                }

                // 每个心跳周期发送一个心跳请求给对端
                if ((m_Stamp.iActiveStamp - oPeer.iRequestStamp) >= m_Stamp.iActiveExpire) {
                    m_Node.ObjectRequest(oPeer.sObjPeer, 36, "Active?", "pgLibConference.MessageSend");
                    oPeer.iRequestStamp = m_Stamp.iActiveStamp;
                }
            }
        }
        int i = 0;
        while (i < m_listLostPeer.size()) {
            _EventProc(EVENT_VIDEO_LOST, "", m_listLostPeer.get(i));
            i++;
        }
    }

    private boolean _KeepAdd(String sObjPeer) {
        // 添加
        _OutString("->KeepAdd");
        PG_SYNC oSync = _SyncPeerSearch(sObjPeer);
        if (oSync == null) {
            try {
                oSync = new PG_SYNC(sObjPeer, m_Stamp.iKeepStamp);
            } catch (Exception ex) {
                return false;
            }
            m_listSyncPeer.add(oSync);
        }
        m_Node.ObjectRequest(sObjPeer, 36, "Keep?", "pgLibConference.MessageSend");
        return true;
    }

    private void _KeepDel(String sObjPeer) {
        //作为成员端只接受主席端心跳 删除
        _OutString("->KeepDel");
        PG_SYNC oSync = _SyncPeerSearch(sObjPeer);
        if (oSync != null) {
            m_listSyncPeer.remove(oSync);
        }
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
                    _EventProc(EVENT_PEER_SYNC, "reason=1", sObjPeer);
                }
            } else {
                m_Node.ObjectRequest(sObjPeer, 36, "Keep?", "pgLibConference.MessageSend");
                m_Stamp.iKeepChainmanStamp = m_Stamp.iKeepStamp;
            }

        }
    }

    //成员端登录后与主席端连接保存
    private void _Keep() {
        //OutString("->Keep TimeOut");

        if (m_Node != null) {

            if (!m_Status.bServiceStart || m_Stamp.iExpire == 0 || m_Group.bEmpty) {
                m_Stamp.iKeepStamp = 0;
                m_Stamp.iKeepChainmanStamp = 0;
                m_Stamp.iRequestChainmanStamp = 0;
                m_listSyncPeer.clear();
                return;
            }

            m_Stamp.iKeepStamp += KEEP_TIMER_INTERVAL;
            m_Group.iKeepTimer = TimerStart("(Act){Keep}", KEEP_TIMER_INTERVAL, false);

            //取消心跳的接收和发送
            if (m_Group.bChairman) {

                //如果是主席，主动给所有成员发心跳
                int i = 0;
                while (i < m_listSyncPeer.size()) {
                    PG_SYNC oSync = m_listSyncPeer.get(i);

                    // 超过3倍心跳周期，没有接收到成员端的心跳应答，说明成员端之间连接断开了
                    if ((m_Stamp.iKeepStamp - oSync.iKeepStamp) > (m_Stamp.iExpire * 3)) {
                        _EventProc(EVENT_PEER_OFFLINE, "reason=1", oSync.sObjPeer);
                        PeerDelete(oSync.sObjPeer);
                        m_listSyncPeer.remove(i);
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

    private String _ObjPeerBuild(String sPeer) {
        if (sPeer.indexOf(ID_PREFIX) != 0) {
            return ID_PREFIX + sPeer;
        }
        return sPeer;
    }

    private String _ObjPeerParsePeer(String sObjPeer) {
        int ind = sObjPeer.indexOf(ID_PREFIX);
        if (ind == 0) {
            return sObjPeer.substring(5);
        }
        return sObjPeer;
    }
    private final ArrayList<PG_PEER> m_listVideoPeer = new ArrayList<>();

    //搜索加入会议的节点
    private PG_PEER _VideoPeerSearch(String sObjPeer) {
        PG_PEER oPeer = null;
        int i = 0;
        while (i < m_listVideoPeer.size()) {
            if (m_listVideoPeer.get(i).sObjPeer.equals(sObjPeer)) {
                oPeer = m_listVideoPeer.get(i);
                break;
            }
            i++;
        }
        return oPeer;
    }


    private final ArrayList<String> m_listLostPeer = new ArrayList<>();

    private class PG_SYNC {
        String sObjPeer = "";
        int iKeepStamp = 0;
        int iRequestStamp = 0;

        PG_SYNC(String sObjPeer, int iCurrentStamp) {
            this.sObjPeer = sObjPeer;
            this.iKeepStamp = iCurrentStamp;
        }
    }

    private final ArrayList<PG_SYNC> m_listSyncPeer = new ArrayList<>();

    //搜索加入会议的节点
    private PG_SYNC _SyncPeerSearch(String sObjPeer) {
        PG_SYNC oSync = null;
        int i = 0;
        while (i < m_listSyncPeer.size()) {
            if (m_listSyncPeer.get(i).sObjPeer.equals(sObjPeer)) {
                oSync = m_listSyncPeer.get(i);
                break;
            }
            i++;
        }
        return oSync;
    }
    //
    private String _PrvwBuild() {
        return "Prvw";
    }

    //视频相关初始化
    private boolean _VideoInit(int iFlag) {
        _OutString("->VideoInit iFlag = " + iFlag);

        this._VideoOption();
        if (m_Status.bServiceStart) {


            if (!m_Group.bChairman) {
                _ChairPeerCheck();
            }

            m_Status.iVideoInitFlag = iFlag;
            int uFlag = 0x10000 | 0x1 | 0x10 | 0x20;
            switch (iFlag) {
                case VIDEO_ONLY_INPUT: {
                    uFlag = uFlag | 0x4;
                    break;
                }
                case VIDEO_ONLY_OUTPUT: {
                    uFlag = uFlag | 0x8;
                    break;
                }
                case VIDEO_NORMAL:
                default:
            }

            //预览
            this.m_Node.ObjectAdd(_PrvwBuild(), "PG_CLASS_Video", "", 0x2);
            String sWndRect = "(Code){" + m_Self.iVideoCode + "}(Mode){2}(Rate){40}(Wnd){}";
            int iErr = this.m_Node.ObjectRequest(_PrvwBuild(), 32, sWndRect, "pgLibConference.PrvwStart");
            if (iErr > 0) {
                _OutString("pgLibConference.m_VideoInit: Open Prvw failed. iErr=" + iErr);
                return false;
            }

            if (!this.m_Node.ObjectAdd(this.m_Group.sObjV, "PG_CLASS_Video", this.m_Group.sObjG, uFlag)) {
                _OutString("pgLibConference.VideoInit: Add 'Video' failed.");
                return false;
            }

            String sData = "(Code){" + m_Self.iVideoCode + "}(Mode){" + m_Self.iVideoMode + "}(Rate){"
                    + m_Self.iVideoFrmRate + "}";

            _OutString("VideoInit -> sObjV sData = " + sData);

            iErr = this.m_Node.ObjectRequest(this.m_Group.sObjV, 32, sData, "pgLibConference.VideoStart");
            if (iErr > 0) {
                _OutString("pgLibConference.VideoInit: Open live failed. iErr=" + iErr);
                return false;
            }

            if (!this.m_Node.ObjectAdd(this.m_Group.sObjLV, "PG_CLASS_Video", this.m_Group.sObjG, uFlag)) {
                _OutString("pgLibConference.VideoInit: Add 'Video' failed.");
                return false;
            }

            sData = "(Code){" + m_Self.iLVideoCode + "}(Mode){" + m_Self.iLVideoMode + "}(Rate){" + m_Self.iLVideoFrmRate
                    + "}";
            _OutString("VideoInit -> sObjLV sData = " + sData);
            iErr = this.m_Node.ObjectRequest(this.m_Group.sObjLV, 32, sData, "pgLibConference.VideoStart");
            if (iErr > 0) {
                _OutString("pgLibConference.VideoInit: Open live failed. iErr=" + iErr);
                return false;
            }

            // 开始视频连接状态检测定时器
            m_Group.iActiveTimer = TimerStart("(Act){TimerActive}", ACTIVE_TIMER_INTERVAL, false);
            m_Stamp.iActiveStamp = 0;
            return true;
        }
        return false;
    }

    private boolean _VideoClose(PG_PEER oPeer) {
        boolean bRet = false;
        if (oPeer != null && m_Status.bApiVideoStart) {
            if (oPeer.Node != null || oPeer.iHandle > 0) {
                String sObjV;
                if (oPeer.bLarge) {
                    sObjV = m_Group.sObjLV;
                } else {
                    sObjV = m_Group.sObjV;
                }
                String sData = "(Peer){" + this.m_Node.omlEncode(oPeer.sObjPeer) + "}";
                int iErr = this.m_Node.ObjectRequest(sObjV, 36, sData, "VideoClose:" + oPeer.sObjPeer);
                if (iErr != 0) {
                    return false;
                }
            }
            oPeer.restore(m_Stamp.iActiveStamp);
            m_listVideoPeer.remove(oPeer);
        }
        return bRet;
    }

    //视频相关清理
    private void _VideoClean() {
        _OutString("->VideoClean");
        if (m_Status.bApiVideoStart) {
            if (m_Group.iActiveTimer > 0) {
                TimerStop(m_Group.iActiveTimer);
            }
            this.m_Node.ObjectRequest(this.m_Group.sObjLV, 33, "", "pgLibConference.VideoCleanL");
            this.m_Node.ObjectDelete(this.m_Group.sObjLV);

            this.m_Node.ObjectRequest(this.m_Group.sObjV, 33, "", "pgLibConference.VideoClean");
            this.m_Node.ObjectDelete(this.m_Group.sObjV);

            this.m_Node.ObjectRequest(_PrvwBuild(), 33, "", "pgLibConference.PrvwClean");
            this.m_Node.ObjectDelete(_PrvwBuild());

        }
        m_Status.bApiVideoStart = false;
    }

    //音频相关初始化
    private boolean _AudioInit() {
        _OutString("->AudioInit");
        int uFlag = 0x10000 | 0x01;
        switch (m_Self.iAudioSpeechDisable) {
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

        if (!this.m_Node.ObjectAdd(this.m_Group.sObjA, "PG_CLASS_Audio", this.m_Group.sObjG, uFlag)) {
            _OutString("pgLibConference.m_AudioInit: Add Audio failed.");
            return false;
        }

        int iErr = this.m_Node.ObjectRequest(this.m_Group.sObjA, 32, "(Code){1}(Mode){0}", "pgLibConference.AudioInit");
        if (iErr > 0) {
            _OutString("pgLibConference.AudioInit: Open audio failed. iErr=" + iErr);
            return false;
        }
        return true;
    }

    //音频相关清理
    private void _AudioClean() {
        _OutString("->AudioClean");

        this.m_Node.ObjectRequest(this.m_Group.sObjA, 33, "", "pgLibConference.AudioClean");
        this.m_Node.ObjectDelete(this.m_Group.sObjA);
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

    //自身登录事件处理
    private void _SelfSync(String sData, String sPeer) {
        _OutString("->SelfSync");

        String sAct = this.m_Node.omlGetContent(sData, "Action");
        if (!"1".equals(sAct)) {
            if (sPeer.equals(this.m_Svr.sSvrName)) {
                this._NodeRelogin(10);
            }
        }
    }

    private int _SelfCall(String sData, String sObjPeer, int iHandle) {
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
            String sPeer = _ObjPeerParsePeer(sObjPeer);
            this._EventProc(EVENT_MESSAGE, sParam, sPeer);
        }
        m_Node.ObjectExtReply(sObjPeer, 0, "", iHandle);

        return 1;
    }

    //自身消息处理
    private int _SelfMessage(String sData, String sObjPeer) {
        _OutString("->SelfMessage");
        String sPeer = _ObjPeerParsePeer(sObjPeer);

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
            this._EventProc(EVENT_ASK_JOIN, "", sPeer);
        } else if ("Leave".equals(sCmd)) {
            this._EventProc(EVENT_ASK_LEAVE, "", sPeer);
        } else if ("Msg".equals(sCmd)) {
            this._EventProc(EVENT_MESSAGE, sParam, sPeer);
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
    private int _ServerMessage(String sData, String sObjPeer) {
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
            this._EventProc(EVENT_SVR_NOTIFY, sParam, sObjPeer);
        } else if ("Restart".equals(sCmd)) {
            if (sParam.contains("redirect=1")) {
                _NodeRedirectReset(3);
            }
        }

        return 0;
    }

    //服务器错误处理
    private void _ServerError(String sData) {
        _OutString("->ServerError");

        String sMeth = m_Node.omlGetContent(sData, "Meth");
        if ("32".equals(sMeth)) {
            String sError = m_Node.omlGetContent(sData, "Error");
            if ("10".equals(sError)) {
                _NodeRelogin(3);
            } else if ("11".equals(sError) || "12".equals(sError) || "14".equals(sError)) {
                _NodeRedirectReset(0);
            }
        }
    }

    private void _ServerRelogin(String sData) {
        _OutString("->ServerRelogin!");

        String sError = m_Node.omlGetContent(sData, "ErrCode");
        if ("0".equals(sError)) {
            String sParam = m_Node.omlGetContent(sData, "Param");
            String sRedirect = m_Node.omlGetEle(sParam, "Redirect.", 10, 0);
            if (!"".equals(sRedirect)) {
                _NodeRedirect(sRedirect);
                return;
            }

            m_Status.bLogined = true;
            _ChairPeerCheck();
            _EventProc(EVENT_LOGIN, "0", m_Svr.sSvrName);
        }

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
                        _EventProc(EVENT_JOIN, "", sPeer);
                    } else {
                        _FileListDelete(m_Group.sChair,sPeer);
                        _EventProc(EVENT_LEAVE, "", sPeer);
                    }
                }

                iInd++;
            }
        }
    }


    //保存对端视频请求句柄
    private void _VideoJoin(String sObj, String sData, int iHandle, String sObjPeer, String sAct) {
        _OutString("->VideoJoin");

        PG_PEER oCtrl = _VideoPeerSearch(sObjPeer);
        if (oCtrl == null) {
            try {
                oCtrl = new PG_PEER(sObjPeer);
            } catch (Exception ex) {
                _OutString("->VideoJoin ex = " + ex.toString());
                return;
            }
            m_listVideoPeer.add(oCtrl);
        }

        oCtrl.iStamp = m_iCurStamp;
        oCtrl.iHandle = iHandle;
        if (sObj.indexOf("_LV_") == 0) {
            oCtrl.bLarge = true;
        }
        String sPeer = _ObjPeerParsePeer(sObjPeer);
        _EventProc(sAct, sData, sPeer);
    }

    //初始化节点
    private void _VideoLeave(String sObj, String sData, int iHandle, String sPeer, String sAct) {
        _OutString("->VideoLeave");
        _EventProc(sAct, sData, sPeer);

        PG_PEER oCtrl = _VideoPeerSearch(sPeer);
        if (oCtrl != null) {
            oCtrl.restore(m_Stamp.iActiveStamp);
            m_listVideoPeer.remove(oCtrl);
        }
    }

    //peer离线
    private void _PeerOffline(String sObjPeer, String sError) {
        _OutString("->PeerOffline");

        String sPeer = _ObjPeerParsePeer(sObjPeer);
        String sAct;
        if (!m_Group.bEmpty && sObjPeer.equals(m_Group.sObjChair)) {
            sAct = EVENT_CHAIRMAN_OFFLINE;
            this._EventProc(sAct, sError, sPeer);
            _ChairPeerStatic();
            if (!m_LanScan.bPeerCheckTimer) {
                if (TimerStart("(Act){CapPeerCheck}", 3, false) >= 0) {
                    m_LanScan.bPeerCheckTimer = true;
                }
            }
        } else {
            sAct = EVENT_PEER_OFFLINE;
            this._EventProc(sAct, sError, sPeer);
        }
    }

    //上报发送视频帧信息
    private void _VideoFrameStat(String sData, String sAct) {
        //OutString("->VideoFrameStat");
        String sPeerTemp = m_Node.omlGetContent(sData, "Peer");
        String sFrmTotal = m_Node.omlGetContent(sData, "Total");
        String sFrmDrop = m_Node.omlGetContent(sData, "Drop");

        _EventProc(sAct, ("total=" + sFrmTotal + "&drop=" + sFrmDrop), sPeerTemp);
    }

    private void _VideoCameraReply(String sData) {
        if (!m_Status.bApiVideoStart) {
            return;
        }
        String sObjPeer = m_Node.omlGetContent(sData, "Peer");
        String sPath = m_Node.omlGetContent(sData, "Path");
        String sPeer = _ObjPeerParsePeer(sObjPeer);
        _EventProc(EVENT_VIDEO_CAMERA, sPath, sPeer);
    }

    private void _VideoRecordReply(String sData) {
        if (m_Status.bApiVideoStart) {
            String sObjPeer = m_Node.omlGetContent(sData, "Peer");
            String sPath = m_Node.omlGetContent(sData, "Path");
            String sPeer = _ObjPeerParsePeer(sObjPeer);
            _EventProc(EVENT_VIDEO_RECORD, sPath, sPeer);
        }
    }

    //服务器下发数据
    private void _SvrReply(int iErr, String sData) {
        if (iErr != 0) {
            _EventProc(EVENT_SVR_REPLYR_ERROR, "" + iErr, m_Svr.sSvrName);
        } else {
            _EventProc(EVENT_SVR_RELAY, sData, m_Svr.sSvrName);
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
            TimerStart("(Act){ChairPeerCheck}", 5, false);
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
                    _EventProc(EVENT_LAN_SCAN_RESULT, sDataTemp, "");
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
            _EventProc("FilePutRequest", sParam, sID);
        }
        else if (iMethod == 33) {
            _EventProc("FileGetRequest", sParam, sID);
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
            _EventProc("FileProgress", sParam, sChairID);
        }
        else { // Stop
            _FileListSet(sChairID,sID, "Status", "0");

            String sPath = m_Node.omlGetContent(sData, "Path");
            String sReqSize = m_Node.omlGetContent(sData, "ReqSize");
            String sCurSize = m_Node.omlGetContent(sData, "CurSize");

            String sParam = "path=" + sPath + "&total=" + sReqSize + "&position=" + sCurSize;
            _EventProc("FileProgress", sParam, sChairID);

            int iCurSize = _ParseInt(sCurSize, 0);
            int iReqSize = _ParseInt(sReqSize, 0);
            if (iCurSize >= iReqSize && iReqSize > 0) {
                _EventProc("FileFinish", sParam, sChairID);
            }
            else {
                _EventProc("FileAbort", sParam, sChairID);
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
        _EventProc("FileAbort", "", sRenID);
    }

    private int _NodeOnExtRequest(String sObj, int uMeth, String sData, int iHandle, String sObjPeer) {
        if (!((!m_Group.bEmpty) && uMeth == 40 && (sObj.equals(m_Group.sObjV) || sObj.equals(m_Group.sObjLV)))) {
            _OutString("NodeOnExtRequest: " + sObj + ", " + uMeth + ", " + sData + ", " + sObjPeer);
        }

        if (m_eventHook != null) {
            int iErr = m_eventHook.OnExtRequest(sObj, uMeth, sData, iHandle, sObjPeer);
            if (iErr != PG_ERR_Unknown) {
                return iErr;
            }
        }

        if (m_Node != null) {
            //Peer类相关
            if (sObj.equals(m_Svr.sSvrName)) {
                if (uMeth == 0) {
                    String sAct = this.m_Node.omlGetContent(sData, "Action");
                    if (!"1".equals(sAct) && "".equals(this.m_Svr.sSvrName)) {
                        this._NodeRelogin(10);
                    }
                } else if (uMeth == 1) {
                    this._ServerError(sData);
                } else if (uMeth == 46) {
                    this._ServerRelogin(sData);
                }
                return 0;
            } else if (sObj.equals(m_Self.sObjSelf)) {
                if (uMeth == 0) {
                    _SelfSync(sData, sObjPeer);
                } else if (uMeth == 35) {
                    return this._SelfCall(sData, sObjPeer, iHandle);
                } else if (uMeth == 36) {
                    if (sObjPeer.equals(m_Svr.sSvrName)) {
                        return this._ServerMessage(sData, sObjPeer);
                    } else {
                        return this._SelfMessage(sData, sObjPeer);
                    }
                } else if (uMeth == 47) {
                    //ID冲突 被踢下线了
                    _EventProc(EVENT_LOGOUT, "47", "");
                }
                return 0;
            } else if (!m_Group.bEmpty && this.m_Group.sObjChair.equals(sObj)) {

                if (uMeth == 0) {
                    String sAct = this.m_Node.omlGetContent(sData, "Action");
                    if ("1".equals(sAct)) {
                        _KeepAdd(sObj);
                        m_LanScan.bPeerCheckTimer = false;
                        this._EventProc(EVENT_CHAIRMAN_SYNC, sAct, sObj);
                    }
                } else if (uMeth == 1) {
                    String sMeth = this.m_Node.omlGetContent(sData, "Meth");
                    if ("34".equals(sMeth)) {
                        String sError = this.m_Node.omlGetContent(sData, "Error");

                        _PeerOffline(sObj, sError);
                        _KeepDel(sObj);
                    }
                }
                return 0;
            } else if ("PG_CLASS_Peer".equals(this.m_Node.ObjectGetClass(sObj))) {
                if (uMeth == 0) {
                    String sAct = this.m_Node.omlGetContent(sData, "Action");
                    if ("1".equals(sAct)) {

                        //心跳包列表 添加
                        if (!m_Group.bEmpty && m_Group.bChairman) {
                            _KeepAdd(sObj);
                        }
                        this._EventProc(EVENT_PEER_SYNC, sAct, sObj);
                    }
                } else if (uMeth == 1) {
                    String sMeth = this.m_Node.omlGetContent(sData, "Meth");
                    if ("34".equals(sMeth)) {
                        String sError = this.m_Node.omlGetContent(sData, "Error");

                        //心跳包列表 删除
                        if (!m_Group.bEmpty && m_Group.bChairman) {
                            _KeepDel(sObj);
                        }
                        _PeerOffline(sObj, sError);
                    }
                }
                return 0;
            }

            if (m_Group.bEmpty) {
                _OutString("Group Not Init");
                return 0;
            }

            //通讯组类相关
            if (!m_Group.bEmpty && sObj.equals(m_Group.sObjG)) {
                if (uMeth == 33) {
                    //成员有更新
                    //加入列表，
                    this._GroupUpdate(sData);

                }
            }

            //DData类相关
            if (!m_Group.bEmpty && sObj.equals(this.m_Group.sObjD)) {
                if (uMeth == 32) {
                    this._EventProc(EVENT_NOTIFY, sData, sObjPeer);
                }
                return 0;
            }
            //接收视频类方法
            if (!m_Group.bEmpty && sObj.equals(m_Group.sObjV)) {
                if (uMeth == 0) {
                    String sAct = this.m_Node.omlGetContent(sData, "Action");
                    if ("1".equals(sAct)) {
                        _EventProc(EVENT_VIDEO_SYNC, "", sObjPeer);
                    }

                } else if (uMeth == 35) {
                    _VideoJoin(sObj, sData, iHandle, sObjPeer, EVENT_VIDEO_OPEN);
                    return -1;
                } else if (uMeth == 36) {
                    _VideoLeave(sObj, sData, iHandle, sObjPeer, EVENT_VIDEO_CLOSE);
                } else if (uMeth == 40) {
                    _VideoFrameStat(sData, EVENT_VIDEO_FRAME_STAT);

                }
                return 0;
            }
            if (!m_Group.bEmpty && sObj.equals(m_Group.sObjLV)) {
                if (uMeth == 0) {
                    String sAct = this.m_Node.omlGetContent(sData, "Action");
                    if ("1".equals(sAct)) {
                        _EventProc(EVENT_VIDEO_SYNC_1, "", sObjPeer);
                    }
                } else if (uMeth == 35) {
                    _VideoJoin(sObj, sData, iHandle, sObjPeer, EVENT_VIDEO_OPEN_1);
                    return -1;
                } else if (uMeth == 36) {
                    _VideoLeave(sObj, sData, iHandle, sObjPeer, EVENT_VIDEO_CLOSE_1);
                } else if (uMeth == 40) {
                    _VideoFrameStat(sData, EVENT_VIDEO_FRAME_STAT_1);
                }
                return 0;
            }
            //音频类相关
            if (!m_Group.bEmpty && sObj.equals(m_Group.sObjA)) {
                if (uMeth == 0) {
                    String sAct = this.m_Node.omlGetContent(sData, "Action");
                    if ("1".equals(sAct)) {
                        _EventProc(EVENT_AUDIO_SYNC, "", sObjPeer);
                    }
                }
            }

            if (_FileObjectIs(sObj)) {
                if (uMeth == 32) {
                    // put file request
                    return _OnFileRequest(sObj, uMeth, sData, iHandle);
                }

                if (uMeth == 33) {
                    // get file request
                    return _OnFileRequest(sObj, uMeth, sData, iHandle);
                }

                if (uMeth == 34) {
                    // File transfer status report.
                    _OnFileStatus(sObj, sData);
                    return 0;
                }

                if (uMeth == 35) {
                    // Cancel file request
                    _OnFileCancel(sObj);
                    return 0;
                }

                return 0;
            }

        }
        return 0;
    }

    private int _NodeOnReply(String sObj, int iErr, String sData, String sParam) {
        _OutString("NodeOnReply: " + sObj + ", " + iErr + ", " + sData + ", " + sParam);

        if (m_eventHook != null) {
            int uErr = m_eventHook.OnReply(sObj, iErr, sData, sParam);
            if (uErr >= 0) {
                return iErr;
            }
        }

        if (m_Node != null) {

            if (sObj.equals(m_Svr.sSvrName)) {
                if ("NodeLogin".equals(sParam)) {
                    _NodeLoginReply(iErr, sData);
                } else if ("LanScan".equals(sParam)) {
                    _LanScanResult(sData);
                } else if ("SvrRequest".equals(sParam)) {
                    _SvrReply(iErr, sData);
                }

                return 1;
            }
            if (sParam.indexOf("CallSend") == 0) {
                String sSession;
                sSession = sParam.substring(9);
                _EventProc(EVENT_CALLSEND_RESULT, sSession + ":" + iErr, sObj);
                return 1;
            }

            if (m_Group.bEmpty) {
                _OutString("NodeOnReply: Group Init faile");
                return 1;
            }

            if (sParam.indexOf("VideoOpen") == 0) {
                //视频加入通知
                this._EventProc(EVENT_VIDEO_JOIN, "" + iErr, sParam.substring(10));
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

            if (!m_Group.bEmpty && sObj.equals(m_Group.sObjA)) {
                if ("AudioCtrlVolume".equals(sParam)) { // Cancel file
                    _EventProc(EVENT_AUDIO_CTRL_VOLUME, Integer.valueOf(iErr).toString(), sObj);
                }
            }

            if (_FileObjectIs(sObj)) {
                if ("FileGetRequest".equals(sParam)
                        || "FilePutRequest".equals(sParam))
                {
                    String sRenID = _FileObjectParseChairID(sObj);
                    String sID = "";
                    if (iErr != PG_ERR_Normal) {
                        _FileListSet(sRenID,sID, "Status", "0");
                        _EventProc("FileReject", (iErr + ""), sRenID);
                        return 1;
                    }
                    else {
                        _FileListSet(sRenID,sID, "Status", "1");
                        _EventProc("FileAccept", "0" , sRenID);
                        return 1;
                    }
                }

                return 1;
            }
        }
        return 1;
    }

    // PG Node callback class.
    private class pgLibNodeProc extends pgLibJNINodeProc {
        pgLibNodeProc() {
            super();
        }

        @Override
        public int OnReply(String sObj, int uErrCode, String sData, String sParam) {
            int iErr = 0;
            try {
                if (m_Status.bInitialized && !m_bThreadExit && m_dispItem != null && m_handlerDisp != null) {
                    synchronized(m_dispItem) {
                        m_dispItem.iType = 0;
                        m_dispItem.sObject = sObj;
                        m_dispItem.sParam0 = sData;
                        m_dispItem.sParam1 = sParam;
                        m_dispItem.iParam0 = uErrCode;
                        m_dispItem.iParam1 = 0;
                        if (m_handlerDisp.post(m_RunnableNodeProc)) {
                            m_dispItem.wait();
                            if (m_dispItem != null) {
                                iErr = m_dispItem.iReturn;
                            }
                        }
                        else {
                            _OutString("OnReply: Post run failed");
                        }
                    }
                }
            }
            catch (Exception ex) {
                _OutString("OnReply: ex=" + ex.toString());
            }
            return iErr;
        }

        @Override
        public int OnExtRequest(String sObj, int uMeth, String sData, int uHandle, String sPeer) {
            int iErr = PG_ERR_System;
            try {
                if (m_Status.bInitialized && !m_bThreadExit && m_dispItem != null && m_handlerDisp != null) {
                    synchronized(m_dispItem) {
                        m_dispItem.iType = 1;
                        m_dispItem.sObject = sObj;
                        m_dispItem.sParam0 = sData;
                        m_dispItem.sParam1 = sPeer;
                        m_dispItem.iParam0 = uMeth;
                        m_dispItem.iParam1 = uHandle;
                        if (m_handlerDisp.post(m_RunnableNodeProc)) {
                            m_dispItem.wait();
                            if (m_dispItem != null) {
                                iErr = m_dispItem.iReturn;
                            }
                        }
                        else {
                            _OutString("OnExtRequest: Post run failed");
                        }
                    }
                }
            }
            catch (Exception ex) {
                _OutString("OnExtRequest: ex=" + ex.toString());
            }
            return iErr;
        }
    }

    /**
     * 本节点和自身有关的参数类
     *
     * @author ctkj
     */
    private class PG_SELF {
        /**
         * 自身对象名
         */
        String sObjSelf = "";

        /**
         * 自身ID
         */
        String sUser = "";
        /**
         * 自身密码
         */
        String sPass = "";
        /**
         * 视频参数保存
         */
        String sVideoParam = "";
        /**
         * 视频帧格式类型
         */
        int iVideoCode = 0;
        /**
         * 视频常见分辨率类型
         */
        int iVideoMode = 0;
        /**
         * 视频期望帧率
         */
        int iVideoFrmRate = 0;
        /**
         * 视频帧格式类型2
         */
        int iLVideoCode = 0;
        /**
         * 视频常见分辨率类型2
         */
        int iLVideoMode = 0;
        /**
         * 视频期望帧率2
         */
        int iLVideoFrmRate = 0;

        /**
         * 视频比特率
         */
        int iVideoBitRate = 0;
        /**
         * 视频横向或正向
         */
        int bVideoPortrait = 0;
        /**
         * 视频旋转角度
         */
        int bVideoRotate = 0;
        /**
         * 视频摄像头编号
         */
        int iCameraNo = 0;

        /**
         * 音频初始化参数
         */
        int iAudioSpeechDisable = 0;

        void init(String sUser, String sPass, String sVideoParam, pgLibJNINode mNode) {
            this.sUser = sUser;
            this.sPass = sPass;

            this.sVideoParam = sVideoParam;

            iVideoCode = _ParseInt(mNode.omlGetContent(sVideoParam, "Code"), 3);
            iVideoMode = _ParseInt(mNode.omlGetContent(sVideoParam, "Mode"), 2);
            iVideoFrmRate = _ParseInt(mNode.omlGetContent(sVideoParam, "FrmRate"), 40);

            iLVideoCode = _ParseInt(mNode.omlGetContent(sVideoParam, "LCode"), 3);
            iLVideoMode = _ParseInt(mNode.omlGetContent(sVideoParam, "LMode"), 2);
            iLVideoFrmRate = _ParseInt(mNode.omlGetContent(sVideoParam, "LFrmRate"), 40);

            iVideoBitRate = _ParseInt(mNode.omlGetContent(sVideoParam, "BitRate"), 400);
            bVideoPortrait = _ParseInt(mNode.omlGetContent(sVideoParam, "Portrait"), 0);
            bVideoRotate = _ParseInt(mNode.omlGetContent(sVideoParam, "Rotate"), 0);
            iCameraNo = _ParseInt(mNode.omlGetContent(sVideoParam, "CameraNo"), 0);

            iAudioSpeechDisable = _ParseInt(mNode.omlGetContent(sVideoParam, "AudioSpeechDisable"), 0);
            if (iAudioSpeechDisable == 0) {
                iAudioSpeechDisable = _ParseInt(mNode.omlGetContent(sVideoParam, "AudioSpeech"), 0);
            }

            this.sObjSelf = ID_PREFIX + sUser;
        }
    }

    /**
     *
     */
    private class PG_SVR {
        String sSvrName = "";
        String sSvrAddr = "";
        String sRelayAddr = "";

        void init(String sSvrName, String sSvrAddr, String sRelayAddr) {
            this.sSvrName = sSvrName;
            this.sSvrAddr = sSvrAddr;
            this.sRelayAddr = sRelayAddr;
        }
    }

    private class PG_GROUP {
        boolean bEmpty = true;

        int iKeepTimer = -1;
        int iActiveTimer = -1;

        String sName = "";
        String sChair = "";
        String sUser = "";

        boolean bChairman = false;
        String sObjChair = "";

        String sObjG = "";
        String sObjD = "";
        String sObjV = "";
        String sObjLV = "";
        String sObjA = "";

        void Init(String sName, String sChair, String sUser) {
            this.sName = sName;
            this.sChair = sChair;
            this.sUser = sUser;
            if ("".equals(this.sName) || "".equals(this.sChair)) {
                bEmpty = true;
            } else {
                bEmpty = false;

                iKeepTimer = -1;
                iActiveTimer = -1;

                bChairman = this.sChair.equals(this.sUser);
                sObjChair = ID_PREFIX + sChair;
                sObjG = "_G_" + sName;
                sObjD = "_D_" + sName;
                sObjV = "_V_" + sName;
                sObjLV = "_LV_" + sName;
                sObjA = "_A_" + sName;
            }
        }
    }

    private class PG_STATUS {
        boolean bInitialized = false;
        boolean bLogined = false;
        boolean bServiceStart = false;
        boolean bApiVideoStart = false;
        boolean bApiAudioStart = false;
        boolean bEventEnable = true;
        int iVideoInitFlag = 0;

        void restore() {
            this.bInitialized = false;
            this.bLogined = false;
            this.bServiceStart = false;
            this.bApiVideoStart = false;
            this.bApiAudioStart = false;
            this.bEventEnable = true;
            this.iVideoInitFlag = 0;
        }
    }

    private class PG_STAMP {
        // 视频连接状态检测
        private int iActiveExpire = 10;
        private int iActiveStamp = 0;

        // 节点连接状态检测
        private int iExpire = 10;
        private int iKeepStamp = 0;
        private int iKeepChainmanStamp = 0;
        private int iRequestChainmanStamp = 0;

        void restore() {
            iActiveStamp = 0;
            iKeepStamp = 0;
            iKeepChainmanStamp = 0;
            iRequestChainmanStamp = 0;
        }
    }

    private class PG_LANSCAN {
        boolean bApiLanScan = false;
        String sLanScanRes = "";
        String sLanAddr;
        boolean bPeerCheckTimer = false;

        PG_LANSCAN() {
            this.bApiLanScan = false;
            this.sLanScanRes = "";
            this.sLanAddr = "";
            this.bPeerCheckTimer = false;
        }
    }

    private String msIni = "Type=1;LogLevel0=1;LogLevel1=1";
    private String msConfigNode = "Type=0;Option=1;MaxPeer=256;MaxGroup=32;MaxObject=512;MaxMCast=512;MaxHandle=256;SKTBufSize0=128;SKTBufSize1=64;SKTBufSize2=256;SKTBufSize3=64;P2PTryTime=1";
    // Randomer.
    private java.util.Random m_Random = new java.util.Random();
    private boolean m_bJNILibInited = false;
    // Event listen interface object.
    // Node event hook interface object.
    private NodeEventHook m_eventHook = null;
    private OnEventListener m_eventListener = null;
    private pgLibJNINode m_Node = null;
    private pgLibNodeProc m_NodeProc = null;
    private final PG_SELF m_Self = new PG_SELF();
    private final PG_SVR m_InitSvr = new PG_SVR();
    private final PG_SVR m_Svr = new PG_SVR();
    private final PG_GROUP m_InitGroup = new PG_GROUP();
    private final PG_GROUP m_Group = new PG_GROUP();
    private final PG_STATUS m_Status = new PG_STATUS();
    private final PG_STAMP m_Stamp = new PG_STAMP();
    private final PG_LANSCAN m_LanScan = new PG_LANSCAN();

    //
    private class PG_PEER {

        String sObjPeer = "";
        int iStamp = 0;
        int iHandle = 0;
        //保证Video关闭前退出会议
        //boolean bRequest = false;
        boolean bLarge = false;

        int iActStamp = 0;
        int iRequestStamp = 0;
        Boolean bVideoLost = false;

        pgLibJNINode Node = null;
        SurfaceView View = null;

        PG_PEER(String sObjPeer1) {
            sObjPeer = sObjPeer1;
        }

        //清理Video相关的数据和状态
        void restore(int iActiveStamp) {
            if (Node != null) {
                if (View != null) {
                    View = null;
                    Node.WndDelete();
                }
                Node = null;
            }
            iHandle = 0;
            //bRequest = false;
            bLarge = false;
            iActStamp = iActiveStamp;
            bVideoLost = false;
        }
    }


    ///-------------------------------------------------------------------------
    // Node dispatch handles

    class DispItem {
        public int iType = 0;
        public String sObject = "";
        public String sParam0 = "";
        public String sParam1 = "";
        public int iParam0 = 0;
        public int iParam1 = 0;
        public int iReturn = 0;

        public DispItem() {
        }
    }

    private Handler m_handlerDisp = null;
    private DispItem m_dispItem = null;

    private Thread m_thread = null;
    private boolean m_bThreadExit = false;

    class DispThread extends Thread {
        @Override
        public void run() {
            _ThreadProc();
        }
    }

    private void _ThreadProc() {
        if (m_Node != null) {
            while (true) {
                if (m_Node.PumpMessage(0)) {
                    if (m_bThreadExit) {
                        break;
                    }
                }
            }
        }
    }

    private Runnable m_RunnableNodeProc = new Runnable() {
        @Override
        public void run() {
            if (m_dispItem != null) {
                synchronized(m_dispItem) {
                    switch (m_dispItem.iType) {
                        case 0:
                            m_dispItem.iReturn = _NodeOnReply(m_dispItem.sObject,
                                    m_dispItem.iParam0, m_dispItem.sParam0,
                                    m_dispItem.sParam1);
                            m_dispItem.notify();
                            break;

                        case 1:
                            m_dispItem.iReturn = _NodeOnExtRequest(m_dispItem.sObject,
                                    m_dispItem.iParam0, m_dispItem.sParam0,
                                    m_dispItem.iParam1, m_dispItem.sParam1);
                            m_dispItem.notify();
                            break;
                    }
                }
            }
        }
    };

    private boolean _NodeDispInit() {
        try {
            m_dispItem = new DispItem();
            m_handlerDisp = new Handler();

            m_bThreadExit = false;
            m_thread = new DispThread();
            m_thread.start();

            return true;
        }
        catch (Exception ex) {
            _OutString("pgLibLiveMultiCapture._NodeDispInit: ex=" + ex.toString());
            _NodeDispClean();
            return false;
        }
    }

    private void _NodeDispClean() {
        try {
            m_bThreadExit = true;
            if (m_handlerDisp != null) {
                m_handlerDisp.removeCallbacks(m_RunnableNodeProc);
            }
            if (m_Node != null) {
                m_Node.PostMessage("__exit");
            }
            if (m_dispItem != null) {
                synchronized(m_dispItem) {
                    m_dispItem.notify();
                }
            }
            if (m_thread != null) {
                m_thread.join();
                m_thread = null;
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        m_handlerDisp = null;
        m_dispItem = null;
    }


    //=======================================================

    //VideoOpen 超时清理
    private void _DropPeerHelper(String sPeer) {
        _OutString("->DropPeerHelper");
        VideoReject(sPeer);
    }

    // 定时器相关

    public interface TimerOut {
        void TimerProc(String sParam);
    }

    private ArrayList<TimerOut> m_listTimerOut = new ArrayList<>();

    /**
     * 描述:将TimerOut接口添加到超时处理列表中
     * 阻塞方式：非阻塞
     *
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
     *
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

        @Override
        public void run() {
            try {
                if (m_timerHandler != null) {
                    Message oMsg = m_timerHandler.obtainMessage(0, null);
                    m_timerHandler.sendMessage(oMsg);
                } else {
                    TimerProc();
                }
            } catch (Exception ex) {
                _OutString("pgTimerTask.run, ex=" + ex.toString());
            }
        }
    }

    private int m_iCurStamp = 0;
    private Timer m_timer = null;
    private pgTimerTask m_timerTask = null;
    private Handler m_timerHandler = null;
    private ArrayList<TimerItem> s_timerList = new ArrayList<>();

    private void TimerProc() {

        m_iCurStamp++;

        for (int i = 0; i < m_listVideoPeer.size(); i++) {
            PG_PEER oCtrl = m_listVideoPeer.get(i);
            if (oCtrl.iHandle > 0 && (m_iCurStamp - oCtrl.iStamp) > 20) {
                _DropPeerHelper(oCtrl.sObjPeer);
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
                _OutString("TimerProc : " + ex.toString());
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
            _OutString("TimerInit: ex=" + ex.toString());
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
            _OutString("TimerClean, ex=" + ex.toString());
        }
    }

    /**
     * 描述：开启一个定时器
     * 阻塞方式：非阻塞
     *
     * @param sParam   : 超时处理接口收到的参数
     * @param iTimeout :超时时间
     * @param bRepeat  : 是否循环
     * @return : 定时器实例ID
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
            _OutString("Add, ex=" + ex.toString());
            return -1;
        }
    }

    /**
     * 描述：关闭一个定时器
     * 阻塞方式：非阻塞
     *
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
            _OutString("TimerStop, ex=" + ex.toString());
        }
    }
}
