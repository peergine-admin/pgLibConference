package com.peergine.android.conference;

public class OnEventConst {
    /**
     * 登录事件
     */
    public static final String EVENT_LOGIN = "Login";
    /**
     * 登出事件
     */
    public static final String EVENT_LOGOUT = "Logout";
    /**
     * 因为其他设备使用同一个ID登录，被服务器踢出
     */
    public static final String EVENT_KICK_OUT = "KickOut";

    public static final String EVENT_PING = "Ping";

    /**
     * 上报相对节点的信息。
     *        sData 上报信息格式： peer=xxx&through=xxx&proxy=xxx&addrlcl=xxx&addrrmt=xxx&tunnellcl=xxx&tunnelrmt=xxx&privatermt=xxx
     */
    public static final String EVENT_PEER_INFO = "PeerInfo";

    /**
     * 节点同步
     */
    public static final String EVENT_PEER_SYNC = "PeerSync";
    /**
     * 节点离线消息
     */
    public static final String EVENT_PEER_OFFLINE = "PeerOffline";

    /**
     * 节点消息事件
     */
    public static final String EVENT_MESSAGE = "Message";
    /**
     * 上报RpcRequest 消息；
     */
    public static final String EVENT_RPC_REQUEST = "RpcRequest";

    /**
     * 上报 RpcResponse 消息；;sEventParam 上报sParam + ":" + iErrCode
     */
    public static final String EVENT_RPC_RESPONSE = "RpcResponse";

    /**
     * 服务器下发消息事件
     */
    public static final String EVENT_SVR_NOTIFY = "SvrNotify";
    /**
     * 服务器回复消息错误事件
     */
    public static final String EVENT_SVR_REPLYR_ERROR = "SvrRequestReplyError";
    /**
     * 服务器回复消息事件
     */
    public static final String EVENT_SVR_RELAY = "SvrRequestReply";

    /**
     * 上报局域网节点信息
     */
    public static final String EVENT_LAN_SCAN_RESULT = "LanScanResult";



    /**
     * 成员端请求加入会议事件（主席端上报）
     */
    public static final String EVENT_JOIN_REQUEST = "JoinRequest";

    /**
     * 成员加入组事件
     *
     */
    public static final String EVENT_JOIN = "Join";
    /**
     * 成员离开会议事件
     */
    public static final String EVENT_LEAVE = "Leave";

    /**
     * 广播消息事件
     */
    public static final String EVENT_NOTIFY = "Notify";


    /**
     * 视频丢失事件
     */
    public static final String EVENT_VIDEO_LOST = "VideoLost";


    /**
     * 视频通道同步事件 ;sEventParam 上报VideoMode
     */
    public static final String EVENT_VIDEO_SYNC = "VideoSync";

    /**
     * 请求视频通话 ;sEventParam 上报VideoMode
     */
    public static final String EVENT_VIDEO_REQUEST = "VideoRequest";


    /**
     * 请求视频通话结果上报事件 ;sEventParam 上报VideoMode
     */
    public static final String EVENT_VIDEO_RESPONSE = "VideoResponse";


    /**
     * 视频关闭事件 ;sEventParam 上报VideoMode
     */
    public static final String EVENT_VIDEO_CLOSE = "VideoClose";

    /**
     * 视频状态信息上报 , ;sEventParam 上报VideoMode
     * Peer：指定上报视频统计的节点。
     * Total：总发送的视频帧数
     * Drop：丢弃的视频帧数
     */
    public static final String EVENT_VIDEO_FRAME_STAT = "VideoFrameStat";

    /**
     * 拍照结果事件 ;sEventParam 上报VideoMode
     */
    public static final String EVENT_VIDEO_CAMERA = "VideoCamera";

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
}
