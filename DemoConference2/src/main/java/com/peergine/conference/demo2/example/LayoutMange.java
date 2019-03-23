package com.peergine.conference.demo2.example;

import android.widget.LinearLayout;
import android.widget.TextView;

import com.peergine.conference.democonference2.R;

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

    public LinearLayout Alloc(String Info){
        if(m_listLinearIdle.size()<=0){
            return null;
        }

        LinearLayout layout = m_listLinearIdle.get(0);
        if(layout == null) {
            return null;
        }
        LinearLayout viewlayout = layout.findViewById(R.id.videoView);
        TextView textView = layout.findViewById(R.id.textView);
        if(viewlayout != null && textView != null) {
            textView.setText(Info);
            m_listLinearIdle.remove(layout);
            m_listLinearUsed.add(layout);
        }
        return viewlayout;
    }

    public void Free(LinearLayout videolayout){
        LinearLayout layout = (LinearLayout) videolayout.getParent();
        if(layout != null) {
            TextView textView = layout.findViewById(R.id.textView);
            textView.setText("");
            m_listLinearUsed.remove(layout);
            m_listLinearIdle.add(layout);
        }
    }

    public void Clean(){
        m_listLinearIdle.clear();
        m_listLinearUsed.clear();
    }
}
