package com.peergine.android.conference;

import android.view.SurfaceView;

import java.util.ArrayList;


/**
 * TODO 添加检查请求打开视频的超时关闭机制
 */
public class VideoPeerList {

    private final ArrayList<VideoPeer> m_listVideoPeer = new ArrayList<>();

    //搜索加入会议的节点
    public VideoPeer _VideoPeerSearch(String sObjPeer) {
        VideoPeer oPeer = null;
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

    public VideoPeer _VideoPeerAdd(String sObjPeer){
        VideoPeer oPeer = _VideoPeerSearch(sObjPeer);
        if(oPeer != null){
            return oPeer;
        }
        try {
            oPeer = new VideoPeer(sObjPeer);
            m_listVideoPeer.add(oPeer);
        } catch (Exception ex) {
            return null;
        }
        return oPeer;
    }

    public void  _VideoPeerDelete(String sObjPeer){
        VideoPeer oPeer = _VideoPeerSearch(sObjPeer);
        _VideoPeerDelete(oPeer);
    }

    public void  _VideoPeerDelete(VideoPeer oPeer){
        if(oPeer!=null){
            oPeer.Release();
            m_listVideoPeer.remove(oPeer);
        }
    }

    public void _VideoPeerClean(){
        for (VideoPeer oPeer : m_listVideoPeer){
            oPeer.Release();
        }
        m_listVideoPeer.clear();
    }
}
