package com.peergine.android.conference;

import android.util.Log;

import com.peergine.plugin.lib.pgLibJNINode;

import java.util.ArrayList;

import static android.text.TextUtils.isEmpty;
import static com.peergine.android.conference.OnEventConst.EVENT_PEER_OFFLINE;
import static com.peergine.android.conference.pgLibConference2._ParseInt;
import static com.peergine.android.conference.pgLibError.PG_ERR_Normal;
import static com.peergine.android.conference.pgLibNode.PG_ADD_COMMON_Sync;
import static com.peergine.android.conference.pgLibNode.PG_CLASS_Peer;
import static com.peergine.android.conference.pgLibNode.PG_METH_PEER_Message;

/**
 * Todo 发送心跳
 */
public class HeartBeartPeerList {
    interface OnHeartBeartEvent{
        void event(String sAct,String sData , String sObjPeer,String sConfName, String sEventParam);
    }


    pgLibJNINode m_Node = null;
    String sAction = "";
    String sVersion = "";
    int peerHeartbeatExpire = 10;

    OnHeartBeartEvent onHeartBeartEvent = null;

    private int m_iStamp = 0;

    private int m_iTimerID = -1;
    final pgLibTimer m_Timer = new pgLibTimer();
    final pgLibTimer.OnTimeOut timerOut = new pgLibTimer.OnTimeOut() {
        @Override
        public void onTimeOut(String sParam) {
            _TimerOut(sParam);
        }
    };


    private class HeartBeartPeer{
        int iConut = 0;
        String sObjPeer = "";
        String sVersion = "";
        int iMaster = 0;
        boolean bOffline = false;
        int peerHeartbeatExpire = 10;

        int peerHeartbeatStartStamp = 0;
        /**
         * 收到主席端消息刷新时戳
         */
        int responseStamp = 0;
        /**
         * 给主席端发送心跳消息刷新
         */
        int requestStamp = 0;

        HeartBeartPeer(String sObjPeer ,String sVersion,int iExpire, int iCurStamp, int iMaster){
            this.sObjPeer = sObjPeer;
            this.peerHeartbeatStartStamp = iCurStamp;
            this.sVersion = sVersion;
            this.peerHeartbeatExpire = iExpire;
            this.iMaster = iMaster;
        }

    }

    private ArrayList<HeartBeartPeer> m_listHeartBeartPeer = new ArrayList<>();

    private void _OutString(String sOut) {
        Log.d("HeartBeartPeerList",sOut);
    }

    private void _OnEvent(String sAtc, String sData, String sObjPeer, String sConfName, String sEventParam) {
        if(onHeartBeartEvent != null){
            onHeartBeartEvent.event(sAtc,sData,sObjPeer,sConfName,sEventParam);
        }
    }


    public int Initialize(pgLibJNINode Node, String sAction, String sVersion, int peerHeartbeatExpire, OnHeartBeartEvent onHeartBeartEvent){
        m_listHeartBeartPeer.clear();
        this.m_Node = Node;
        this.sAction = sAction;
        this.sVersion = sVersion;
        this.peerHeartbeatExpire = peerHeartbeatExpire;
        this.onHeartBeartEvent = onHeartBeartEvent;
        m_Timer.timerInit(timerOut);

        TimerStart();
        return PG_ERR_Normal;
    }

    public int Clean(){
        TimerStop();
        m_Timer.timerClean();
        m_listHeartBeartPeer.clear();
        m_Node = null;
        return PG_ERR_Normal;
    }

    private HeartBeartPeer _Search(String sObjPeer){
        if(isEmpty(sObjPeer)){
            return null;
        }

        for (int i = 0; i < m_listHeartBeartPeer.size(); i++) {
            HeartBeartPeer peer = m_listHeartBeartPeer.get(i);
            if (peer.sObjPeer.equals(sObjPeer)) {
                return peer;
            }
        }
        return null;
    }

    //添加主席节点  使之能在加入会议前与主席通信，发送Join信号
    public HeartBeartPeer _Add(String sObjPeer,String sVer,int iExpire,int iMaster) {
        if(m_Node == null){
            return null;
        }

        if (PG_CLASS_Peer.equals(m_Node.ObjectGetClass(sObjPeer))) {
            _PeerSync(sObjPeer, "", 1);
        } else {
            if (!this.m_Node.ObjectAdd(sObjPeer, PG_CLASS_Peer, "", PG_ADD_COMMON_Sync )) {
                _OutString("._Add .PeerAdd Object  failed.");
            }
        }

        HeartBeartPeer heartBeartPeer = _Search(sObjPeer);
        if(heartBeartPeer == null){
            heartBeartPeer = new HeartBeartPeer(sObjPeer,sVer,iExpire, m_iStamp,iMaster);
            m_listHeartBeartPeer.add(heartBeartPeer);
        }
        heartBeartPeer.iConut ++;
        _HeartBeatSendReq(heartBeartPeer);
        return heartBeartPeer;
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
    public void _Delete(String sObjPeer) {
        HeartBeartPeer heartBeartPeer = _Search(sObjPeer);
        if(heartBeartPeer != null){
            heartBeartPeer.iConut --;
            if(heartBeartPeer.iConut <= 0 ){
                m_listHeartBeartPeer.remove(heartBeartPeer);
            }

        }
    }

    private void TimerStart() {
        m_iTimerID = m_Timer.timerStart("", 1,false);
    }

    private void TimerStop(){
        m_Timer.timerStop(m_iTimerID);
    }

    private void _TimerOut(String sParam) {
        if (m_Node == null) {
            return;
        }

        m_iStamp++;
        TimerStart();

        if (m_iStamp % 2 == 0) {
            ArrayList<HeartBeartPeer> beartPeerArrayList = null;
            try{
                beartPeerArrayList = (ArrayList<HeartBeartPeer>) m_listHeartBeartPeer.clone();
            }catch (Exception ex){
                _OutString("_TimerOut:listHeartBeartPeer.clone faile.");
            }
            if(beartPeerArrayList == null){
                return;
            }
            //如果是主席，主动给所有成员发心跳
            int i = 0;
            while (i < beartPeerArrayList.size()) {
                HeartBeartPeer heartBeartPeer = beartPeerArrayList.get(i);
                if(!heartBeartPeer.bOffline) {
                    if (heartBeartPeer.iMaster > 0) {
                        // 超过3倍心跳周期，没有接收到成员端的心跳应答，说明成员端之间连接断开了
                        if ((m_iStamp - heartBeartPeer.responseStamp) > (peerHeartbeatExpire * 3)) {
                            _OnEvent(EVENT_PEER_OFFLINE, "reason=1", heartBeartPeer.sObjPeer, "", "");
                            beartPeerArrayList.remove(heartBeartPeer);
                            //标记为不在线
                            HeartBeartPeer oPeer = _Search(heartBeartPeer.sObjPeer);
                            if(oPeer !=null){
                                oPeer.bOffline = true;
                            }
                            continue;
                        }

                        // 每个心跳周期发送一个心跳请求给Salve端
                        if ((m_iStamp - heartBeartPeer.requestStamp) >= peerHeartbeatExpire) {
                            _HeartBeatSendReq(heartBeartPeer);
                        }
                    } else {
                        // 超过1.5倍心跳周期，没有接收到Master端的心跳请求，尝试主动给Master端发送心跳
                        if ((m_iStamp - heartBeartPeer.responseStamp) > (heartBeartPeer.peerHeartbeatExpire * 3 / 2)) {

                            if ((m_iStamp - heartBeartPeer.requestStamp) >= peerHeartbeatExpire) {
                                _HeartBeatSendReq(heartBeartPeer);
                            }
                        }

                        // 超过3倍心跳周期，没有接收到主席端的心跳请求，说明间 连接断开了
                        if ((m_iStamp - heartBeartPeer.responseStamp) > (heartBeartPeer.peerHeartbeatExpire * 3 / 2)) {

                            _OnEvent(EVENT_PEER_OFFLINE, "reason=1", heartBeartPeer.sObjPeer, "", "");
                            beartPeerArrayList.remove(heartBeartPeer);
                            _Delete(heartBeartPeer.sObjPeer);
                            continue;
                        }
                    }
                }else{
                    tryRelink(heartBeartPeer);
                }
                i++;
            }
        }
    }


    private void _HeartBeatSendReq(HeartBeartPeer heartBeartPeer){

        if(heartBeartPeer == null){
            return;
        }


        String sVer = heartBeartPeer.iMaster > 0 ? sVersion : heartBeartPeer.sVersion;
        int iExpire = heartBeartPeer.iMaster > 0 ? peerHeartbeatExpire : heartBeartPeer.peerHeartbeatExpire;
        String sData = sAction + "?(Version){" + sVer + "}(Expire){" + iExpire + "}(Master){" + heartBeartPeer.iMaster + "}" ;
        _OutString("_HeartBeatSendReq sObjPeer=" + heartBeartPeer.sObjPeer + " sData = " + sData);
        m_Node.ObjectRequest(heartBeartPeer.sObjPeer, PG_METH_PEER_Message, sData, "HBeat");
        heartBeartPeer.requestStamp = m_iStamp;
    }

        //收到Keep 处理
    public void _OnHeartBeatRecv(String sObjPeer,String sParam) {
        _OutString("_OnHeartBeatRecv sObjPeer=" + sObjPeer + " sParam = " + sParam);
        int iExpire = _ParseInt(m_Node.omlGetContent(sParam,"Expire"),10);
        int iMaster = _ParseInt(m_Node.omlGetContent(sParam,"Master"),0);
        String sVersion = m_Node.omlGetContent(sParam,"Version");

        HeartBeartPeer heartBeartPeer = _Search(sObjPeer);
        if(heartBeartPeer == null){
            if(iMaster == 1) {
                heartBeartPeer = _Add(sObjPeer, sVersion, iExpire, 0);
            }
        }

        if(heartBeartPeer == null) {
            return;
        }
        heartBeartPeer.bOffline = false;
        heartBeartPeer.responseStamp = m_iStamp;
        if(heartBeartPeer.iMaster == 0){
            _HeartBeatSendReq(heartBeartPeer);
        }
    }

    /**
     * 尝试重新连接
     * @param heartBeartPeer 对象
     */
    private void tryRelink(HeartBeartPeer heartBeartPeer){
        if(heartBeartPeer ==null || isEmpty(heartBeartPeer.sObjPeer)){
            return;
        }
        if(m_Node == null){
            return;
        }

        if (PG_CLASS_Peer.equals(m_Node.ObjectGetClass(heartBeartPeer.sObjPeer))) {
            _PeerSync(heartBeartPeer.sObjPeer, "", 1);
        } else {
            if (!this.m_Node.ObjectAdd(heartBeartPeer.sObjPeer, PG_CLASS_Peer, "", PG_ADD_COMMON_Sync )) {
                _OutString("._Add .PeerAdd Object  failed.");
            }
        }
        _HeartBeatSendReq(heartBeartPeer);
    }

}