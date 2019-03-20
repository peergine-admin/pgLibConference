package com.peergine.android.conference;

import static com.peergine.android.conference.pgLibConference2._AudioBuildObject;
import static com.peergine.android.conference.pgLibConference2._DataBuildObject;
import static com.peergine.android.conference.pgLibConference2._GroupBuildObject;
import static com.peergine.android.conference.pgLibConference2._ObjPeerBuild;
import static com.peergine.android.conference.pgLibConference2._VideoBuildObject;
import static com.peergine.android.conference.pgLibConference2._VideoLBuildObject;

public class Group {
    String sConfName = "";
    String sChair = "";
    String sUser = "";

    int iKeepTimer = -1;
    int iActiveTimer = -1;

    String sObjChair = "";

    String sObjG = "";
    String sObjD = "";
    String sObjV = "";
    String sObjLV = "";
    String sObjA = "";

    String sVideoParamDef;
    String sVideoParamLarge;
    String sAudioParam;

    boolean bServiceStart = false;
    boolean bApiVideoStart = false;
    boolean bApiAudioStart = false;

    // 视频连接状态检测
    int videoHeartbeatExpire = 10;
    int videoHeartbeatStamp = 0;

    // 节点连接状态检测
    int peerHeartbeatExpire = 10;
    int peerHeartbeatStamp = 0;

    /**
     * 收到主席端消息刷新时戳
     */
    int responseChainmanStamp = 0;
    /**
     * 给主席端发送心跳消息刷新
     */
    int requestChainmanStamp = 0;

    final VideoPeerList videoPeerList = new VideoPeerList();
    final SyncPeerList syncPeerList = new SyncPeerList();

    public Group(String sConfName, String sChair, String sUser) {

        this.sConfName = sConfName;
        this.sChair = sChair;
        this.sUser = sUser;

        iKeepTimer = -1;
        iActiveTimer = -1;


        sObjChair = _ObjPeerBuild(sChair);
        sObjG = _GroupBuildObject(sConfName);
        sObjD = _DataBuildObject(sConfName);
        sObjV = _VideoBuildObject(sConfName);
        sObjLV = _VideoLBuildObject(sConfName);
        sObjA = _AudioBuildObject(sConfName);

        restoreStamp();
    }

    public void restoreStamp() {
        videoHeartbeatStamp = 0;

        peerHeartbeatStamp = 0;

        responseChainmanStamp = 0;
        requestChainmanStamp = 0;
    }


    boolean isChairman() {
        return this.sChair.equals(this.sUser);
    }

    boolean _ObjChairmanIs(String sObj){
        return this.sObjChair.equals(sObj);
    }

    String _VideoObjectGet(int iStreamMode){
        return iStreamMode == 0 ?sObjV:sObjLV;
    }


//    public void cheackTimeout(){
//        for (int i = 0; i < m_listVideoPeer.size(); i++) {
//            VideoPeer oCtrl = m_listVideoPeer.get(i);
//            if (oCtrl.bMode == VIDEO_PEER_MODE_Response && (m_iCurStamp - oCtrl.iOnVideoJoinStamp) > VIDEO_RESPONSE_TIMEOUT) {
//                VideoJoinResponse(m_Group.sObjV,PG_ERR_Timeout,"",oCtrl.iHandle);
//                oCtrl.VideoLeave();
//            }
//            if (oCtrl.bModeL == VIDEO_PEER_MODE_Response && (m_iCurStamp - oCtrl.iOnVideoJoinStampL) > VIDEO_RESPONSE_TIMEOUT) {
//                VideoJoinResponse(m_Group.sObjLV,PG_ERR_Timeout,"",oCtrl.iHandleL);
//                oCtrl.VideoLeaveL();
//            }
//        }
//    }
}
