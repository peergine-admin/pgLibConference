package com.peergine.android.conference;

import com.peergine.plugin.lib.pgLibJNINode;

import static com.peergine.android.conference.pgLibNode.PG_ADD_COMMON_Sync;
import static com.peergine.android.conference.pgLibNode.PG_CLASS_Peer;

/**
 * Todo 发送心跳
 */
public class PeerHeartBeartList {

    pgLibJNINode m_Node = null;

    void _OutString(String sOut) {

    }

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
//            _ChairPeerCheck();
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


}
