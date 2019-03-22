package com.peergine.android.conference;

import android.os.Handler;
import android.util.Log;

import com.peergine.plugin.lib.pgLibJNINode;
import com.peergine.plugin.lib.pgLibJNINodeProc;

import java.util.concurrent.atomic.AtomicInteger;

import static com.peergine.android.conference.pgLibError.PG_ERR_BadStatus;
import static com.peergine.android.conference.pgLibError.PG_ERR_System;

/**
 * 中间件回调Post到另外的线程上报到应用层
 */
public class pgLibNodeThreadProc extends pgLibJNINodeProc implements pgLibNodeProc {
    private pgLibJNINode m_Node = null;
    pgLibNodeProc m_NodeProc = null;
//    pgLibNodeThreadProc(){
//        super();
//    }
//    pgLibNodeThreadProc(pgLibJNINode Node){
//        super();
//        m_Node = Node;
//    }
    pgLibNodeThreadProc(pgLibJNINode Node, pgLibNodeProc NodeProc) {
        super();
        m_Node = Node;
        m_NodeProc = NodeProc;
    }

    public static void _OutString(String sOut) {
        //if (BuildConfig.DEBUG) {
        Log.d("pgLibNodeThreadProc", sOut);
        //}
    }
    ///-------------------------------------------------------------------------
    // Node dispatch handles 从中间件回调
    @Override
    public int OnReply(String sObj, int uErrCode, String sData, String sParam) {
        return _NodeDispPost(0, sObj, sData, sParam, uErrCode, 0);
    }

    @Override
    public int OnExtRequest(String sObj, int uMeth, String sData, int uHandle, String sPeer) {
        return _NodeDispPost(1, sObj, sData, sPeer, uMeth, uHandle);
    }

    //=============================================================================
    // 回调到应用
    @Override
    public int _NodeOnExtRequest(String sObj, int uMeth, String sData, int uHandle, String sObjPeer) {
        if(m_NodeProc!=null){
            m_NodeProc._NodeOnExtRequest( sObj,  uMeth,  sData,  uHandle,  sObjPeer);
        }
        return 0;
    }

    @Override
    public int _NodeOnReply(String sObj, int uErrCode, String sData, String sParam) {
        if(m_NodeProc!=null){
            m_NodeProc._NodeOnReply( sObj,  uErrCode,  sData,  sParam);
        }
        return 0;
    }


    class DispItem {
        public int iType = 0;
        public String sObject = "";
        public String sParam0 = "";
        public String sParam1 = "";
        public int iParam0 = 0;
        public int iParam1 = 0;
        public int iReturn = 0;

        public DispItem() {
        }
    }

    private final AtomicInteger m_dispAtomic = new AtomicInteger();
    private Handler m_handlerDisp = null;
    private DispItem m_dispItem = null;

    private Thread m_thread = null;
    private boolean m_bThreadExit = false;

    class DispThread extends Thread {
        @Override
        public void run() {
            _ThreadProc();
        }
    }

    private void _ThreadProc() {
        if (m_Node != null) {
            while (true) {
                if (m_Node.PumpMessage(0)) {
                    if (m_bThreadExit) {
                        break;
                    }
                }
            }
        }
    }

    private Runnable m_RunnableNodeProc = new Runnable() {
        @Override
        public void run() {
            synchronized(m_dispAtomic) {
                if (!m_bThreadExit && m_dispItem != null) {

                    int iReturn = (m_dispItem.iType == 0) ? 0 : PG_ERR_System;
                    try {
                        switch (m_dispItem.iType) {
                            case 0:
                                iReturn = _NodeOnReply(m_dispItem.sObject,
                                        m_dispItem.iParam0, m_dispItem.sParam0,
                                        m_dispItem.sParam1);
                                break;

                            case 1:
                                iReturn = _NodeOnExtRequest(m_dispItem.sObject,
                                        m_dispItem.iParam0, m_dispItem.sParam0,
                                        m_dispItem.iParam1, m_dispItem.sParam1);
                                break;

                            default:
                                _OutString("m_RunnableNodeProc.run: invalid type.");
                                break;
                        }
                    }
                    catch (Exception ex) {
                        _OutString("m_RunnableNodeProc.run: ex=" + ex.toString());
                    }

                    if (m_dispItem != null) {
                        m_dispItem.iReturn = iReturn;
                    }
                }

                m_dispAtomic.notify();
            }
        }
    };

    private int _NodeDispPost(int iType, String sObject, String sParam0, String sParam1, int iParam0, int iParam1) {
        int iErr = (iType == 0) ? 0 : PG_ERR_System;
        try {
            if (!m_bThreadExit) {
                synchronized(m_dispAtomic) {
                    if (m_dispItem != null && m_handlerDisp != null) {
                        m_dispItem.iType = iType;
                        m_dispItem.sObject = sObject;
                        m_dispItem.sParam0 = sParam0;
                        m_dispItem.sParam1 = sParam1;
                        m_dispItem.iParam0 = iParam0;
                        m_dispItem.iParam1 = iParam1;
                        m_dispItem.iReturn = PG_ERR_System;
                        if (m_handlerDisp.post(m_RunnableNodeProc)) {
                            m_dispAtomic.wait();
                            if (m_dispItem != null) {
                                iErr = m_dispItem.iReturn;
                            }
                        }
                        else {
                            _OutString("pgLibLiveMultiRender._NodeDispPost: Post run failed");
                        }
                    }
                    else {
                        iErr = (iType == 0) ? 0 : PG_ERR_BadStatus;
                    }
                }
            }
            else {
                iErr = (iType == 0) ? 0 : PG_ERR_BadStatus;
            }
        }
        catch (Exception ex) {
            _OutString("._NodeDispPost: ex=" + ex.toString());
        }
        return iErr;
    }

    public boolean _NodeDispInit() {
        try {
            m_dispItem = new DispItem();
            m_handlerDisp = new Handler();

            m_bThreadExit = false;
            m_thread = new DispThread();
            m_thread.start();

            return true;
        }
        catch (Exception ex) {
            _OutString("._NodeDispInit: ex=" + ex.toString());
            _NodeDispClean();
            return false;
        }
    }

    public void _NodeDispClean() {
        try {
            _OutString("._NodeDispClean: begin");

            m_bThreadExit = true;
            if (m_Node != null) {
                m_Node.PostMessage("__exit");
            }
            _OutString("._NodeDispClean: m_Node.PostMessage finish");
            synchronized(m_dispAtomic) {
                if (m_handlerDisp != null) {
                    m_handlerDisp.removeCallbacks(m_RunnableNodeProc);
                }
                if (m_dispItem != null) {
                    m_dispAtomic.notify();
                }
            }
            _OutString("._NodeDispClean: synchronized(m_dispAtomic) finish");
            if (m_thread != null) {
                m_thread.join();
                m_thread = null;
            }
            _OutString("._NodeDispClean: m_thread.join finish");
            synchronized(m_dispAtomic) {
                m_handlerDisp = null;
                m_dispItem = null;
            }

            m_Node = null;
            _OutString("._NodeDispClean: finish");
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
