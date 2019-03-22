package com.peergine.conference.demo2.example;

import android.util.Log;

import java.util.ArrayList;

public class ConferencePeerList {
    final ArrayList<ConferencePeer> m_listConferencePeer = new ArrayList<>();

    //搜索成员列表
    ConferencePeer _Search(String sConfName, String sPeer) {

        try {
            if ("".equals(sPeer)) {
                Log.d("", "Search can't Search Start");
                return null;
            }
            for (int i = 0; i < m_listConferencePeer.size(); i++) {
                ConferencePeer peer = m_listConferencePeer.get(i);
                if (peer.sPeer.equals(sPeer) && peer.sPeer.equals(sConfName)) {
                    return peer;
                }
            }

        } catch (Exception ex) {
            Log.d("", "VideoOption. ex=" + ex.toString());
        }
        return null;
    }

    ConferencePeer _Add(String sConfName , String sPeer){
        ConferencePeer peer = _Search(sConfName,sPeer);
        if(peer != null){
            return peer;
        }

        peer = new ConferencePeer(sConfName,sPeer);
        m_listConferencePeer.add(peer);
        return peer;
    }

    void _Delete(String sConfName , String sPeer){
        ConferencePeer peer = _Search(sConfName,sPeer);
        if(peer == null){
            return;
        }
        m_listConferencePeer.remove(peer);
    }
    void _Delete(ConferencePeer peer){
        if(peer == null){
            return;
        }
        m_listConferencePeer.remove(peer);
    }



}
