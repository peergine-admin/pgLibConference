package com.peergine.conference.demo.example;

import android.util.Log;
import android.view.SurfaceView;
import android.widget.LinearLayout;

import java.util.ArrayList;

public class ConferencePeerList {
    ArrayList<ConferencePeer> m_listConferencePeer = new ArrayList<>();

    //搜索成员列表
    ConferencePeer _ConferencePeerSearch(String sPeer) {

        try {
            if ("".equals(sPeer)) {
                Log.d("", "Search can't Search Start");
                return null;
            }
            for (int i = 0; i < m_listConferencePeer.size(); i++) {

                if (m_listConferencePeer.get(i).sPeer.equals(sPeer)) {
                    return m_listConferencePeer.get(i);
                }
            }

            for (int i = 0; i < m_listConferencePeer.size(); i++) {
                if ("".equals(m_listConferencePeer.get(i).sPeer)) {
                    return m_listConferencePeer.get(i);
                }
            }
        } catch (Exception ex) {
            Log.d("", "VideoOption. ex=" + ex.toString());
        }
        return null;
    }
}
