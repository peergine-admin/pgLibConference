package com.peergine.android.conference;

import android.view.SurfaceView;

public class VideoPeerList {

    private static final int   VIDEO_PEER_MODE_Leave  =0;
    private static final int    VIDEO_PEER_MODE_Request = 1;
    private static final int    VIDEO_PEER_MODE_Response = 2;
    private static final int   VIDEO_PEER_MODE_Join = 3;
    private static final int VIDEO_RESPONSE_TIMEOUT = 30;
    private class VideoPeer {

        String sObjPeer = "";

        //------------

        int iActStamp = 0;
        int iRequestStamp = 0;
        //------------
        Boolean bVideoLost = false;

        //------------
        //
        int iOnVideoJoinStamp = 0;

        int iHandle = 0;

        SurfaceView View = null;

        int bMode = VIDEO_PEER_MODE_Leave;
        //------------
        int iOnVideoJoinStampL = 0;

        int iHandleL = 0;

        SurfaceView ViewL = null;

        int bModeL = VIDEO_PEER_MODE_Leave;

        //------------

        VideoPeer(String sObjPeer1) {
            sObjPeer = sObjPeer1;
        }

        String GetWndEle(int iW,int iH){
            // Create the node and view.专门用来显示视频的node

            if (View == null) {
                View = (SurfaceView) pgLibView.Get("v1" + sObjPeer);
            }

            if(View!=null){
                return pgLibView.GetNodeByView(View).utilGetWndRect();
            }
            return "";
        }

        String GetWndEleL(int iW,int iH){
            // Create the node and view.专门用来显示视频的node

            if ( ViewL == null) {
                ViewL = (SurfaceView) pgLibView.Get("v2" + sObjPeer);
            }

            if(ViewL!=null){
                return  pgLibView.GetNodeByView(ViewL).utilGetWndRect();
            }
            return "";
        }

        void VideoJoin(int iHandle, int iStamp){
            this.iHandle = iHandle;
            iOnVideoJoinStamp = iStamp;
            this.bMode = VIDEO_PEER_MODE_Response;
        }

        void VideoLeave(){
            iHandle = 0;
            iOnVideoJoinStamp = 0;
            this.bMode = VIDEO_PEER_MODE_Leave;
        }

        void VideoJoinL(int iHandle, int iStamp){
            iHandleL = iHandle;
            iOnVideoJoinStampL = iStamp;
            this.bModeL = VIDEO_PEER_MODE_Response;
        }

        void VideoLeaveL(){
            iHandleL = 0;
            iOnVideoJoinStampL = 0;
            this.bModeL = VIDEO_PEER_MODE_Leave;
        }

        void Release(){
            if (View != null) {
                pgLibView.Release(View);
                View = null;
            }
            if (ViewL != null) {
                pgLibView.Release(ViewL);
                ViewL = null;
            }
        }

    }

    public void cheackTimeout(){
        for (int i = 0; i < m_listVideoPeer.size(); i++) {
            PG_PEER oCtrl = m_listVideoPeer.get(i);
            if (oCtrl.bMode == VIDEO_PEER_MODE_Response && (m_iCurStamp - oCtrl.iOnVideoJoinStamp) > VIDEO_RESPONSE_TIMEOUT) {
                VideoJoinResponse(m_Group.sObjV,PG_ERR_Timeout,"",oCtrl.iHandle);
                oCtrl.VideoLeave();
            }
            if (oCtrl.bModeL == VIDEO_PEER_MODE_Response && (m_iCurStamp - oCtrl.iOnVideoJoinStampL) > VIDEO_RESPONSE_TIMEOUT) {
                VideoJoinResponse(m_Group.sObjLV,PG_ERR_Timeout,"",oCtrl.iHandleL);
                oCtrl.VideoLeaveL();
            }
        }
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

    private PG_PEER _VideoPeerAdd(String sObjPeer){
        PG_PEER oPeer = null;
        try {
            oPeer = new PG_PEER(sObjPeer);
            m_listVideoPeer.add(oPeer);
        } catch (Exception ex) {
            return null;
        }
        return oPeer;
    }

    private void  _VideoPeerDelete(String sObjPeer){
        PG_PEER oPeer = _VideoPeerSearch(sObjPeer);
        _VideoPeerDelete(oPeer);
    }

    private void  _VideoPeerDelete(PG_PEER oPeer){
        if(oPeer!=null){
            oPeer.Release();
            m_listVideoPeer.remove(oPeer);
        }
    }

    private void _VideoPeerClean(){
        for (PG_PEER oPeer : m_listVideoPeer){
            oPeer.Release();
        }
        m_listVideoPeer.clear();
    }
}
