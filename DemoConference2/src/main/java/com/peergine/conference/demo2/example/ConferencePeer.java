package com.peergine.conference.demo2.example;

import android.view.SurfaceView;
import android.widget.LinearLayout;

public class ConferencePeer {
    String sConfName = "";
    String sPeer = "";
    SurfaceView pView = null;
    LinearLayout pLayout = null;
    public ConferencePeer(String sConfName,String sPeer){
        this.sConfName = sConfName;
        this.sPeer = sPeer;
    }
}
