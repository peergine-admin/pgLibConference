package com.peergine.conference.demo2.example;

import android.widget.LinearLayout;

import java.util.ArrayList;

/**
 * 管理视频窗口分配使用
 */
public class LayoutMange {
    private final ArrayList<LinearLayout> m_listLinearIdle = new ArrayList<>();
    private final ArrayList<LinearLayout> m_listLinearUsed = new ArrayList<>();

    public void Add(LinearLayout layout){
        m_listLinearIdle.add(layout);
    }

    public LinearLayout Alloc(){
        LinearLayout layout = m_listLinearIdle.get(0);
        if(layout == null) {
            return null;
        }
        m_listLinearIdle.remove(layout);
        m_listLinearUsed.add(layout);
        return layout;
    }

    public void Free(LinearLayout layout){
        if(m_listLinearUsed.remove(layout)){
            m_listLinearIdle.add(layout);
        }
    }

    public void Clean(){
        m_listLinearIdle.clear();
        m_listLinearUsed.clear();
    }
}
