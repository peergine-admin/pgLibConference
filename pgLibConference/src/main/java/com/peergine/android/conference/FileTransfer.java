package com.peergine.android.conference;

import com.peergine.plugin.lib.pgLibJNINode;

import static com.peergine.android.conference.OnEventConst.*;
import static com.peergine.android.conference.pgLibConference2._ObjPeerBuild;
import static com.peergine.android.conference.pgLibConference2._OutString;
import static com.peergine.android.conference.pgLibConference2._ParseInt;
import static com.peergine.android.conference.pgLibError.PG_ERR_BadStatus;
import static com.peergine.android.conference.pgLibError.PG_ERR_Normal;
import static com.peergine.android.conference.pgLibError.PG_ERR_Opened;
import static com.peergine.android.conference.pgLibNode.PG_METH_FILE_Cancel;
import static com.peergine.android.conference.pgLibNode.PG_METH_FILE_Get;
import static com.peergine.android.conference.pgLibNode.PG_METH_FILE_Put;
import static com.peergine.android.conference.pgLibNode.PG_METH_FILE_Status;

public class FileTransfer {
    private static final String PARAM_FILE_GET_REQUEST = "FileGetRequest";
    private static final String PARAM_FILE_PUT_REQUEST = "FilePutRequest";

    //------------------------------------------------------
    // File handles.

    private pgLibJNINode m_Node = null;
    private String m_sListFile = "";

    private void _OnEvent(String sAct,String sData, String sPeer){

    }

    private String _FileBuildObject(String sChairID,String sID) {
        String sObjFile = ("File_" + sChairID + "\n" + sID);
        if (sObjFile.length() > 127) {
            _OutString("_FileBuildObject: '" + sObjFile + "' to long !");
        }
        return sObjFile;
    }

    private boolean _FileObjectIs(String sObject) {
        return (sObject.indexOf("File_") == 0);
    }

    private String _FileObjectParseChairID(String sObject) {

        String sCapRender = "";
        if (sObject.indexOf("File_") == 0) {
            sCapRender = sObject.substring(5);
        }
        int iInd = sCapRender.indexOf("\n");
        if (iInd > 0) {
            return sCapRender.substring(0, iInd);
        }
        return "";

    }
    private String _FileObjectParseMemberID(String sObject) {

        String sCapRender = "";
        if (sObject.indexOf("File_") == 0) {
            sCapRender = sObject.substring(5);
        }
        int iInd = sCapRender.indexOf("\n");
        if (iInd > 0) {
            return sCapRender.substring(iInd+1);
        }
        return "";

    }

    private String _FileListSearch(String sChairID,String sID) {
        return m_Node.omlGetEle(m_sListFile, ("\n*" + sChairID + "_" + sID), 1, 0);
    }

    private boolean _FileListAdd(String sChairID,String sID,boolean bChairman) {
        String sFile = _FileListSearch(sChairID,sID);
        if ("".equals(sFile)) {
            m_sListFile += "(" + m_Node.omlEncode(sChairID + "_" + sID) + "){(Status){0}(Handle){0}}";
        }

        String sObjFile = _FileBuildObject(sChairID , sID);

        if (!"PG_CLASS_File".equals(m_Node.ObjectGetClass(sObjFile))) {
            String sObj = bChairman?_ObjPeerBuild(sID):_ObjPeerBuild(sChairID);
            if (!m_Node.ObjectAdd(sObjFile, "PG_CLASS_File", sObj, 0x10000)) {
                _OutString("_FileListAdd: Add '" + sObjFile + "' failed!");
                return false;
            }
        }

        return true;
    }

    private boolean _FileListDelete(String sChairID,String sID) {
        String sObjFile = _FileBuildObject(sChairID,sID);

        m_Node.ObjectRequest(sObjFile, 35, "", "");
        m_Node.ObjectDelete(sObjFile);

        String sFile = _FileListSearch(sChairID,sID);
        if (!"".equals(sFile)) {
            m_sListFile = m_Node.omlDeleteEle(m_sListFile, ("\n*" + sChairID + "_" + sID), 1, 0);
            return true;
        }

        return false;
    }

    private boolean _FileListSet(String sChairID,String sID, String sItem, String sValue) {
        String sFile = _FileListSearch(sChairID,sID);
        if (!"".equals(sFile)) {
            String sPath = "\n*" +  sChairID + "_" + sID + "*" + sItem;
            m_sListFile = m_Node.omlSetContent(m_sListFile, sPath, sValue);
            return true;
        }
        return false;
    }

    private String _FileListGet(String sChairID,String sID, String sItem) {
        String sPath = "\n*" + sChairID + "_" + sID + "*" + sItem;
        return m_Node.omlGetContent(m_sListFile, sPath);
    }

    private int _FileRequest(String sChairID,String sID, String sPath, String sPeerPath, int iMethod) {
        if ("1".equals(_FileListGet(sChairID,sID, "Status"))) {
            return PG_ERR_Opened;
        }

        String sData = "(Path){" + m_Node.omlEncode(sPath) + "}(PeerPath){"
                + m_Node.omlEncode(sPeerPath) + "}(TimerVal){1}(Offset){0}(Size){0}";

        String sParam = (iMethod == 32) ? "FilePutRequest" : "FileGetRequest";

        String sObjFile = _FileBuildObject(sChairID,sID);
        int iErr =  m_Node.ObjectRequest(sObjFile, iMethod, sData, sParam);
        if (iErr > PG_ERR_Normal) {
            _OutString("_FileRequest: iMethod=" + iMethod + ", iErr=" + iErr);
            return iErr;
        }

        _FileListSet(sChairID,sID, "Status", "1");
        return iErr;
    }

    private int _FileReply(int iErrReply, String sChairID,String sID, String sPath) {

        String sData = "";
        if (iErrReply != PG_ERR_Normal) {
            _FileListSet(sChairID,sID, "Status", "0");
        }
        else {
            _FileListSet(sChairID,sID, "Status", "1");
            sData = "(Path){" + m_Node.omlEncode(sPath) + "}(TimerVal){1}";
        }

        _OutString("_FileReply: iErrReply=" + iErrReply + ", sChairID=" + sChairID + ", sData=" + sData);

        String sHandle = _FileListGet(sChairID,sID, "Handle");
        _OutString("_FileReply: sHandle=" + sHandle);

        int iHandle = _ParseInt(sHandle, 0);
        if (iHandle == 0) {
            _FileListSet(sChairID, sID, "Status", "0");
            return PG_ERR_BadStatus;
        }


        String sObjFile = _FileBuildObject(sChairID,sID);
        int iErr = m_Node.ObjectExtReply(sObjFile, iErrReply, sData, iHandle);
        if (iErr <= PG_ERR_Normal) {
            _FileListSet(sChairID,sID, "Handle", "0");
        }

        _OutString("_FileReply: iErr=" + iErr);
        return iErr;
    }

    private int _FileCancel(String sChairID,String sID) {

        String sObjFile = _FileBuildObject(sChairID,sID);
        int iErr = m_Node.ObjectRequest(sObjFile, 35, "", "FileCancel");
        if (iErr <= PG_ERR_Normal) {
            _FileListSet(sChairID,sID, "Status", "0");
        }

        return iErr;
    }

    private int _OnFileRequest(String sObj, int iMethod, String sData, int iHandle) {
        String sChairID = _FileObjectParseChairID(sObj);
        String sID = _FileObjectParseMemberID(sObj);

        if ("1".equals(_FileListGet(sChairID, sID, "Status"))) {
            return PG_ERR_BadStatus;
        }

        _FileListSet(sChairID,sID,"Handle", (iHandle + ""));
        _FileListSet(sChairID,sID, "Status", "1");

        _OutString("_OnFileRequest: sData=" + sData);

        String sPeerPath = m_Node.omlGetContent(sData, "PeerPath");
        String sParam = "peerpath=" + sPeerPath;

        if (iMethod == 32) {
            _OnEvent(EVENT_FILE_PUT_REQUEST, sParam, sID);
        }
        else if (iMethod == 33) {
            _OnEvent(EVENT_FILE_GET_REQUEST, sParam, sID);
        }

        return -1; // Async reply
    }


    private int _OnFileStatus(String sObj, String sData) {
        String sChairID = _FileObjectParseChairID(sObj);
        String sID = _FileObjectParseMemberID(sObj);

        String sStatus = m_Node.omlGetContent(sData, "Status");
        int iStatus = _ParseInt(sStatus, -1);
        if (iStatus != 3) {
            String sPath = m_Node.omlGetContent(sData, "Path");
            String sReqSize = m_Node.omlGetContent(sData, "ReqSize");
            String sCurSize = m_Node.omlGetContent(sData, "CurSize");
            String sParam = "path=" + sPath + "&total=" + sReqSize	+ "&position=" + sCurSize;
            _OnEvent(EVENT_FILE_PROGRESS, sParam, sChairID);
        }
        else { // Stop
            _FileListSet(sChairID,sID, "Status", "0");

            String sPath = m_Node.omlGetContent(sData, "Path");
            String sReqSize = m_Node.omlGetContent(sData, "ReqSize");
            String sCurSize = m_Node.omlGetContent(sData, "CurSize");

            String sParam = "path=" + sPath + "&total=" + sReqSize + "&position=" + sCurSize;
            _OnEvent(EVENT_FILE_PROGRESS, sParam, sChairID);

            int iCurSize = _ParseInt(sCurSize, 0);
            int iReqSize = _ParseInt(sReqSize, 0);
            if (iCurSize >= iReqSize && iReqSize > 0) {
                _OnEvent(EVENT_FILE_FINISH, sParam, sChairID);
            }
            else {
                _OnEvent(EVENT_FILE_ABORT, sParam, sChairID);
            }
        }

        return 0;
    }

    private void _OnFileCancel(String sObj) {
        String sRenID = _FileObjectParseChairID(sObj);
        String sID = _FileObjectParseMemberID(sObj);
        if ("".equals(sRenID)) {
            return;
        }

        _FileListSet(sRenID,sID, "Status", "0");
        _OnEvent(EVENT_FILE_ABORT , "", sRenID);
    }

    private int _NodeOnExtRequestFile(String sObj, int uMeth, String sData, int iHandle, String sObjPeer) {
        if (_FileObjectIs(sObj)) {
            if (uMeth == PG_METH_FILE_Put) {
                // put file request
                return _OnFileRequest(sObj, uMeth, sData, iHandle);
            }

            if (uMeth == PG_METH_FILE_Get) {
                // get file request
                return _OnFileRequest(sObj, uMeth, sData, iHandle);
            }

            if (uMeth == PG_METH_FILE_Status) {
                // File transfer status report.
                _OnFileStatus(sObj, sData);
                return 0;
            }

            if (uMeth == PG_METH_FILE_Cancel) {
                // Cancel file request
                _OnFileCancel(sObj);
                return 0;
            }

            return 0;
        }

        return 0;
    }

    private int _NodeOnReplyFile(String sObj, int iErr, String sData, String sParam) {
        if (_FileObjectIs(sObj)) {
            if (PARAM_FILE_GET_REQUEST.equals(sParam)
                    || PARAM_FILE_PUT_REQUEST.equals(sParam))
            {
                String sRenID = _FileObjectParseChairID(sObj);
                String sID = "";
                if (iErr != PG_ERR_Normal) {
                    _FileListSet(sRenID,sID, "Status", "0");
                    _OnEvent(EVENT_FILE_REJECT, (iErr + ""), sRenID);
                    return 1;
                }
                else {
                    _FileListSet(sRenID,sID, "Status", "1");
                    _OnEvent(EVENT_FILE_ACCEPT, "0" , sRenID);
                    return 1;
                }
            }

            return 1;
        }
        return 1;
    }
}
