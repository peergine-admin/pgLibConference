package com.peergine.android.conference;

import android.view.SurfaceView;

import static com.peergine.android.conference.pgLibConference2._isEmpty;

public class VideoPeer {
    private static final int VIDEO_PEER_MODE_Leave  =0;
    private static final int VIDEO_PEER_MODE_Request = 1;
    private static final int VIDEO_PEER_MODE_Response = 2;
    private static final int VIDEO_PEER_MODE_Join = 3;
    private static final int VIDEO_RESPONSE_TIMEOUT = 30;

    String sObjPeer = "";
    VideoPeer(String sObjPeer) {
        this.sObjPeer = sObjPeer;
    }
    //------------
    int videoHeartbeatStamp = 0;
    boolean videoHeartbeatLost = false;
    //------------

    String smallVideoWndEle = "";

    int smallVideoRequestStamp = 0;
    int smallOnVideoRequestStamp = 0;
    int smallVideoRequestHandle = 0;
    int smallVideoMode = VIDEO_PEER_MODE_Leave;
    //------------

    String largeVideoWndEle = "";

    int largeVideoRequestStamp = 0;
    int largeOnVideoRequestStamp = 0;
    int largeVideoRequestHandle = 0;
    int largeVideoMode = VIDEO_PEER_MODE_Leave;


    void VideoJoin(int iStreamMode, int iStamp ,String sWndEle){
        if(iStreamMode > 0 ) {
            largeVideoRequestStamp = iStamp;
            largeVideoMode = VIDEO_PEER_MODE_Request;
            largeVideoWndEle = sWndEle;
        }else{
            smallVideoRequestStamp = iStamp;
            smallVideoMode = VIDEO_PEER_MODE_Request;
            smallVideoWndEle = sWndEle;
        }
    }
    boolean VideoJoinCheck(int iStreamMode,int iStamp){
        int iVideoRequestStamp = iStreamMode > 0 ? largeVideoRequestStamp : smallVideoRequestStamp;
        return iVideoRequestStamp > 0 && iStamp - iVideoRequestStamp > 60 ;
    }
    void OnVideoJoin(int iStreamMode, int iStamp,int iHandle){
        if(iStreamMode > 0 ) {
            largeOnVideoRequestStamp = iStamp;
            largeVideoRequestHandle = iStamp;
            largeVideoMode = VIDEO_PEER_MODE_Request;
        }else{
            smallOnVideoRequestStamp = iStamp;
            smallVideoRequestHandle = iStamp;
            smallVideoMode = VIDEO_PEER_MODE_Request;
        }
    }
    boolean OnVideoJoinCheck(int iStreamMode,int iStamp){
        int iOnVideoRequestStamp = iStreamMode > 0 ? largeOnVideoRequestStamp : smallOnVideoRequestStamp;
        return iOnVideoRequestStamp > 0 && iStamp - iOnVideoRequestStamp > 60 ;
    }

    void VideoJoined(int iStreamMode,int iStamp,String sWndEle){
        videoHeartbeatStamp = iStamp;

        if(iStreamMode > 0){
            largeVideoRequestStamp = 0;
            largeOnVideoRequestStamp = 0;
            largeVideoRequestHandle = 0;
            if(!_isEmpty(sWndEle)) {
                largeVideoWndEle = sWndEle;
            }
            largeVideoMode = VIDEO_PEER_MODE_Join;
        }else{
            smallVideoRequestStamp = 0;
            smallOnVideoRequestStamp = 0;
            smallVideoRequestHandle = 0;
            if(!_isEmpty(sWndEle)) {
                smallVideoWndEle = sWndEle;
            }
            smallVideoMode = VIDEO_PEER_MODE_Join;
        }

    }
    void VideoLeave(int iStreamMode){
        if(iStreamMode > 0) {
            largeVideoRequestStamp = 0;
            largeOnVideoRequestStamp = 0;
            largeVideoRequestHandle = 0;
            largeVideoMode = VIDEO_PEER_MODE_Leave;
        }else{
            smallVideoRequestStamp = 0;
            smallOnVideoRequestStamp = 0;
            smallVideoRequestHandle = 0;
            smallVideoMode = VIDEO_PEER_MODE_Leave;
        }

    }

    void Release(){
        VideoLeave(0);
        VideoLeave(1);
        smallVideoWndEle = "";
        largeVideoWndEle = "";
    }
}
