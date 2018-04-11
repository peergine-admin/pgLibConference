package com.peergine.android.conference;

/**
 * Copyright (C) 2014-2017, Peergine, All rights reserved.
 * www.peergine.com, www.pptun.com
 * com.peergine.android.conference
 *
 * @author ctkj
 */

public class pgLibConferenceEvent {
    /**
     * 登录事件
     */
    public static final String EVENT_LOGIN = "Login";
    /**
     * 登出事件
     */
    public static final String EVENT_LOGOUT = "Logout";
    /**
     * 视频丢失事件
     */
    public static final String EVENT_VIDEO_LOST = "VideoLost";
    /**
     * 音频通道同步事件
     */
    public static final String EVENT_AUDIO_SYNC = "AudioSync";
    /**
     * 音频控制声音事件
     */
    public static final String EVENT_AUDIO_CTRL_VOLUME = "AudioCtrlVolume";
    /**
     * 视频通道同步事件
     */
    public static final String EVENT_VIDEO_SYNC = "VideoSync";
    /**
     * 第二个视频通道同步事件
     */
    public static final String EVENT_VIDEO_SYNC_1 = "VideoSyncL";
    /**
     * 请求视频通话
     */
    public static final String EVENT_VIDEO_OPEN = "VideoOpen";
    /**
     * 第二个视频通道请求视频通话
     */
    public static final String EVENT_VIDEO_OPEN_1 = "VideoOpenL";
    /**
     * 视频关闭事件
     */
    public static final String EVENT_VIDEO_CLOSE = "VideoClose";
    /**
     * 第二个视频关闭事件
     */
    public static final String EVENT_VIDEO_CLOSE_1 = "VideoCloseL";
    /**
     * 视频状态信息上报 ,
     * Peer：指定上报视频统计的节点。
     * Total：总发送的视频帧数
     * Drop：丢弃的视频帧数
     */
    public static final String EVENT_VIDEO_FRAME_STAT = "VideoFrameStat";
    /**
     * 视频状态信息上报2
     * Peer：指定上报视频统计的节点。
     * Total：总发送的视频帧数
     * Drop：丢弃的视频帧数
     */
    public static final String EVENT_VIDEO_FRAME_STAT_1 = "VideoFrameStatL";

    /**
     * 请求视频通话结果上报事件
     */
    public static final String EVENT_VIDEO_JOIN = "VideoJoin";
    /**
     * 拍照结果事件
     */
    public static final String EVENT_VIDEO_CAMERA = "VideoCamera";
    /**
     * 视频录像结果事件
     */
    public static final String EVENT_VIDEO_RECORD = "VideoRecord";
    /**
     * 主席端同步
     */
    public static final String EVENT_CHAIRMAN_SYNC = "ChairmanSync";
    /**
     * 主席端离线消息
     */
    public static final String EVENT_CHAIRMAN_OFFLINE = "ChairmanOffline";
    /**
     * 节点同步
     */
    public static final String EVENT_PEER_SYNC = "PeerSync";
    /**
     * 节点离线消息
     */
    public static final String EVENT_PEER_OFFLINE = "PeerOffline";
    /**
     * 成员端请求加入会议事件（主席端上报）
     */
    public static final String EVENT_ASK_JOIN = "AskJoin";
    /**
     * 成员请求离开会议事件(主席端上报)
     */
    public static final String EVENT_ASK_LEAVE = "AskLeave";
    /**
     * 成员加入组事件
     */
    public static final String EVENT_JOIN = "Join";
    /**
     * 成员离开会议事件
     */
    public static final String EVENT_LEAVE = "Leave";
    /**
     * 节点消息事件
     */
    public static final String EVENT_MESSAGE = "Message";
    /**
     * 广播消息事件
     */
    public static final String EVENT_NOTIFY = "Notify";
    /**
     * 服务器下发消息事件
     */
    public static final String EVENT_SVR_NOTIFY = "SvrNotify";
    /**
     * 服务器回复消息错误事件
     */
    public static final String EVENT_SVR_REPLYR_ERROR = "SvrReplyError";
    /**
     * 服务器回复消息事件
     */
    public static final String EVENT_SVR_RELAY = "SvrReply";
    /**
     * 上报CallSend的结果
     */
    public static final String EVENT_CALLSEND_RESULT = "CallSend";
    /**
     * 上报局域网节点信息
     */
    public static final String EVENT_LAN_SCAN_RESULT = "LanScanResult";

    /**
     * 文件上传请求 sObjPeer 为 ID
     */
    public static final String EVENT_FILE_PUT_REQUEST = "FilePutRequest";
    /**
     * 文件下载请求 sObjPeer 为 成员ID。
     */
    public static final String EVENT_FILE_GET_REQUEST = "FileGetRequest";
    /**
     * 文件传输进度
     */
    public static final String EVENT_FILE_PROGRESS = "FileProgress";
    /**
     * 文件传输结束
     */
    public static final String EVENT_FILE_FINISH = "FileFinish";
    /**
     * 文件传输中断
     */
    public static final String EVENT_FILE_ABORT = "FileAbort";
    /**
     * 文件传输被拒绝
     */
    public static final String EVENT_FILE_REJECT = "FileReject";
    /**
     * 文件传输请求被接受
     */
    public static final String EVENT_FILE_ACCEPT = "FileAccept";
}
