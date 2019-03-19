package com.peergine.android.conference;

import java.util.ArrayList;

public class SyncPeerList {
    public class SyncPeer {
        String sObjPeer = "";
        int iKeepStamp = 0;
        int iRequestStamp = 0;

        SyncPeer(String sObjPeer, int iCurrentStamp) {
            this.sObjPeer = sObjPeer;
            this.iKeepStamp = iCurrentStamp;
        }
    }

    private final ArrayList<SyncPeer> m_listSyncPeer = new ArrayList<>();

    //搜索加入会议的节点
    public SyncPeer _SyncPeerSearch(String sObjPeer) {
        SyncPeer oSync = null;
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

    public boolean _SyncPeerAdd(String sObjPeer,int iCurrentStamp){
        SyncPeer oPeer = null;
        try {
            oPeer = new SyncPeer(sObjPeer,iCurrentStamp);
            m_listSyncPeer.add(oPeer);
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    public void  _SyncPeerDelete(String sObjPeer){
        SyncPeer oPeer = _SyncPeerSearch(sObjPeer);
        _SyncPeerDelete(oPeer);
    }
    public void  _SyncPeerDelete(SyncPeer oPeer ){
        if(oPeer!=null){
            m_listSyncPeer.remove(oPeer);
        }
    }
    public void _SyncPeerClean(){
        m_listSyncPeer.clear();
    }
}
