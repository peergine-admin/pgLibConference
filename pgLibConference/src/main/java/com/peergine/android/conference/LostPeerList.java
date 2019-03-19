package com.peergine.android.conference;

import java.util.ArrayList;

import static com.peergine.android.conference.pgLibConference2._OutString;

public class LostPeerList {
    private final ArrayList<String> m_listLostPeer = new ArrayList<>();
    //搜索加入会议的节点
    private boolean _LostPeerSearch(String sObjPeer) {
        boolean oSync = false;
        int i = 0;
        while (i < m_listLostPeer.size()) {
            if (m_listLostPeer.get(i).equals(sObjPeer)) {
                oSync = true;
                break;
            }
            i++;
        }
        return oSync;
    }

    private void _LostPeerAdd(String sObjPeer){
        try {
            m_listLostPeer.add(sObjPeer);
        } catch (Exception ex) {
            _OutString("_LostPeerAdd ex = " + ex.toString());
        }
    }

    private void  _LostPeerDelete(String sObjPeer){
        m_listLostPeer.remove(sObjPeer);

    }
    private void _LostPeerClean(){

        m_listLostPeer.clear();
    }

}
