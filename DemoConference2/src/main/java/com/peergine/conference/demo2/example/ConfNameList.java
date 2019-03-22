package com.peergine.conference.demo2.example;

import java.util.ArrayList;

public class ConfNameList {
    public final ArrayList<String> m_listConfName = new ArrayList<>();

    public boolean _Search(String sConfName) {
        for (int i = 0; i< m_listConfName.size();i++){
            if(sConfName.equals(m_listConfName.get(i))){
                return true;
            }
        }
        return false;
    }

    public void _Add(String sConfName) {
        m_listConfName.add(sConfName);
    }

    public void _Delete(String sConfName){
        m_listConfName.remove(sConfName);
    }
    public void _Clean(){
        m_listConfName.clear();
    }

}
