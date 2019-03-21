package com.peergine.android.conference;

import com.peergine.plugin.lib.pgLibJNINode;

public class RecordList {
    //------------------------------------------------------
    // Record list handles
    private pgLibJNINode m_Node = null;
    public void SetNode(pgLibJNINode Node){
        m_Node = Node;
    }
    private String m_sListRecord = "";

    String _RecordListSearch(int iMode,String sID) {
        return m_Node.omlGetEle(m_sListRecord, ("\n*" + iMode + "_" + sID), 1, 0);
    }

    void _RecordListAdd(int iMode, String sID) {
        String sRec = _RecordListSearch(iMode,sID);
        if ("".equals(sRec)) {
            m_sListRecord += "(" + m_Node.omlEncode(iMode + "_" + sID)
                    + "){}";
        }
    }

    boolean _RecordListDelete(int iMode,String sID) {
        String sRec = _RecordListSearch(iMode,sID);
        if (!"".equals(sRec)) {
            m_sListRecord = m_Node.omlDeleteEle(m_sListRecord, ("\n*" + iMode + "_" + sID), 1, 0);
            return true;
        }
        return false;
    }

}
