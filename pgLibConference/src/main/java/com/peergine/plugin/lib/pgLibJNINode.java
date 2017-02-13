/****************************************************************
  copyright   : Copyright (C) 2012-2012, chenbichao,
                All rights reserved.
  filename    : pgLibJNINode.java
  discription : 
  modify      : create, chenbichao, 2012/1/8
              :
              : modify, chenbichao, 2016/12/1
              : Fix the return value BUG of PostMessage()
              :
*****************************************************************/

package com.peergine.plugin.lib;


import android.content.Context;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;


public class pgLibJNINode {

	private static Context sAppCtx = null;

	private static Class<?> cls = null;
	private static Field[] fields = null;
	private static Method[] methods = null;

	private static int[] FieldInd = new int[7];
	private static String[] FieldName = new String[7];

	private static int[] MethInd = new int[32];
	private static String[] MethName = new String[32];
	
	private static boolean LoadJNIClass(Context appCtx) {
		try {
			// Load class from this package.
			try {
				cls = appCtx.getClassLoader().loadClass("com.peergine.plugin.pgJNINode");
			}
			catch (Exception ex) {
				Log.d("peergine lib", "Load 'pgJNINode' class from this package failed");
				cls = null;
			}

			// Load class from package 'com.peergine.plugin'.
			if (cls == null) {
				try {
					Context pgCtx = appCtx.createPackageContext("com.peergine.plugin", 
						(Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY));
					cls = pgCtx.getClassLoader().loadClass("com.peergine.plugin.pgJNINode");
				}
				catch (Exception e) {
					e.printStackTrace();
					Log.d("peergine lib", "Load 'pgJNINode' class from package 'com.peergine.plugin' failed");
					cls = null;
					return false;
				}				
			}

			// Get fields and methos from class.
			fields = cls.getFields();
			methods = cls.getMethods();

			// Init field name
			FieldName[0] = "Control";
			FieldName[1] = "Node";
			FieldName[2] = "Class";
			FieldName[3] = "Server";
			FieldName[4] = "Local";
			FieldName[5] = "Relay";
			FieldName[6] = "NodeProc";
			
			// Init method name.
			MethName[0] = "omlEncode";
			MethName[1] = "omlDecode";
			MethName[2] = "omlSetName";
			MethName[3] = "omlSetClass";
			MethName[4] = "omlSetContent";
			MethName[5] = "omlNewEle";
			MethName[6] = "omlGetEle";
			MethName[7] = "omlDeleteEle";
			MethName[8] = "omlGetName";
			MethName[9] = "omlGetClass";
			MethName[10] = "omlGetContent";
			MethName[11] = "ObjectAdd";
			MethName[12] = "ObjectDelete";
			MethName[13] = "ObjectEnum";
			MethName[14] = "ObjectGetClass";
			MethName[15] = "ObjectSetGroup";
			MethName[16] = "ObjectGetGroup";
			MethName[17] = "ObjectSync";
			MethName[18] = "ObjectRequest";
			MethName[19] = "ObjectExtReply";
			MethName[20] = "utilCmd";
			MethName[21] = "utilGetWndRect";
			MethName[22] = "WndNew";
			MethName[23] = "WndDelete";
			MethName[24] = "WndSetParam";
			MethName[25] = "Start";
			MethName[26] = "Stop";
			MethName[27] = "PostMessage";
			MethName[28] = "PumpMessage";
			MethName[29] = "Quit";
			MethName[30] = "Initialize";
			MethName[31] = "Clean";

			// Mapping field index.
			for (int i = 0; i < fields.length; i++) {
				for (int j = 0; j < FieldName.length; j++) {
					if (FieldName[j].equals(fields[i].getName())) {
						FieldInd[j] = i;
						break;
					}
				}
			}

			// Mapping method index.
			for (int i = 0; i < methods.length; i++) {
				for (int j = 0; j < MethName.length; j++) {
					if (MethName[j].equals(methods[i].getName())) {
						MethInd[j] = i;
						break;
					}
				}
			}

			sAppCtx = appCtx;
			return true;
		}
		catch (Exception e) {
			e.printStackTrace();
			Log.d("peergine lib", "LoadJNIClass failed");
			return false;
		}
	}

	public static Context getAppCtx() {
		return sAppCtx;
	}

	// Node object.
	private Object objNode = null;

	// Set config parameters
	public String Control = "";
	public String Class = "";
	public String Server = "";
	public String Local = "";
	public String Relay = "";
	public String Node = "";
	public pgLibJNINodeProc NodeProc = null;

	public pgLibJNINode() {
		try {
			objNode = cls.newInstance();
		}
		catch (Exception e) {
			e.printStackTrace();
			Log.d("peergine lib", "Init pgLibJNINode failed");			
		}
	}

	// OML parser method.
	public String omlEncode(String strEle) {
		try {
			return (String)methods[MethInd[0]].invoke(objNode, strEle);
		}
		catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	public String omlDecode(String strEle) {
		try {
			return (String)methods[MethInd[1]].invoke(objNode, strEle);
		}
		catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	public String omlSetName(String strEle, String strPath, String strName) {
		try {
			return (String)methods[MethInd[2]].invoke(objNode, strEle, strPath, strName);
		}
		catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	public String omlSetClass(String strEle, String strPath, String strClass) {
		try {
			return (String)methods[MethInd[3]].invoke(objNode, strEle, strPath, strClass);
		}
		catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	public String omlSetContent(String strEle, String strPath, String strContent) {
		try {
			return (String)methods[MethInd[4]].invoke(objNode, strEle, strPath, strContent);
		}
		catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	public String omlNewEle(String strName, String strClass, String strContent) {
		try {
			return (String)methods[MethInd[5]].invoke(objNode, strName, strClass, strContent);
		}
		catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	public String omlGetEle(String strEle, String strPath, int uSize, int uPos) {
		try {
			return (String)methods[MethInd[6]].invoke(objNode, strEle, strPath, uSize, uPos);
		}
		catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	public String omlDeleteEle(String strEle, String strPath, int uSize, int uPos) {
		try {
			return (String)methods[MethInd[7]].invoke(objNode, strEle, strPath, uSize, uPos);
		}
		catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	public String omlGetName(String strEle, String strPath) {
		try {
			return (String)methods[MethInd[8]].invoke(objNode, strEle, strPath);
		}
		catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	public String omlGetClass(String strEle, String strPath) {
		try {
			return (String)methods[MethInd[9]].invoke(objNode, strEle, strPath);
		}
		catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	public String omlGetContent(String strEle, String strPath) {
		try {
			return (String)methods[MethInd[10]].invoke(objNode, strEle, strPath);
		}
		catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	// Peergine object methods
	public boolean ObjectAdd(String strName, String strClass, String strGroup, int uFlag) {
		try {
			int iRet = (Integer)methods[MethInd[11]].invoke(objNode, strName, strClass, strGroup, uFlag);
			return (iRet != 0);
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	public void ObjectDelete(String strObj) {
		try {
			methods[MethInd[12]].invoke(objNode, strObj);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	public String ObjectEnum(String strObject, String strClass) {
		try {
			return (String)methods[MethInd[13]].invoke(objNode, strObject, strClass);
		}
		catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	public String ObjectGetClass(String strObj) {
		try {
			return (String)methods[MethInd[14]].invoke(objNode, strObj);
		}
		catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	public boolean ObjectSetGroup(String strObj, String strGroup) {
		try {
			int iRet = (Integer)methods[MethInd[15]].invoke(objNode, strObj, strGroup);
			return (iRet != 0);
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	public String ObjectGetGroup(String strObj) {
		try {
			return (String)methods[MethInd[16]].invoke(objNode, strObj);
		}
		catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	public boolean ObjectSync(String strObj, String strPeer, int uAction) {
		try {
			int iRet = (Integer)methods[MethInd[17]].invoke(objNode, strObj, strPeer, uAction);
			return (iRet != 0);
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	public int ObjectRequest(String strObj, int uMethod, String strIn, String strParam) {
		try {
			return (Integer)methods[MethInd[18]].invoke(objNode, strObj, uMethod, strIn, strParam);
		}
		catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
	}
	public int ObjectExtReply(String strObj, int uErrCode, String strOut, int uHandle) {
		try {
			return (Integer)methods[MethInd[19]].invoke(objNode, strObj, uErrCode, strOut, uHandle);
		}
		catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
	}

	// Utilize methods
	public String utilCmd(String strCmd, String strParam) {
		try {
			return (String)methods[MethInd[20]].invoke(objNode, strCmd, strParam);
		}
		catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	public String utilGetWndRect() {
		try {
			return (String)methods[MethInd[21]].invoke(objNode);
		}
		catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	
	// New and delete plugin window
	public Object WndNew(int iLeft, int iTop, int iWidth, int iHeight) {
		try {
			return (Object)methods[MethInd[22]].invoke(objNode, iLeft, iTop, iWidth, iHeight);
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	public void WndDelete() {
		try {
			methods[MethInd[23]].invoke(objNode);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	public boolean WndSetParam(int iLeft, int iTop, int iWidth, int iHeight) {
		try {
			int iRet = (Integer)methods[MethInd[24]].invoke(objNode, iLeft, iTop, iWidth, iHeight);
			return (iRet != 0);
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	// System methods.
	public boolean Start(int uOption) {
		try {
			fields[FieldInd[0]].set(objNode, Control);
			fields[FieldInd[1]].set(objNode, Node);
			fields[FieldInd[2]].set(objNode, Class);
			fields[FieldInd[3]].set(objNode, Server);
			fields[FieldInd[4]].set(objNode, Local);
			fields[FieldInd[5]].set(objNode, Relay);
			fields[FieldInd[6]].set(objNode, NodeProc);
			int iRet = (Integer)methods[MethInd[25]].invoke(objNode, uOption);
			return (iRet != 0);
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	public void Stop() {
		try {
			methods[MethInd[26]].invoke(objNode);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	public boolean PostMessage(String sMsg) {
		try {
			int iRet = (Integer)methods[MethInd[27]].invoke(objNode, sMsg);
			return (iRet == 0);
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	public boolean PumpMessage(int uLoop) {
		try {
			int iRet = (Integer)methods[MethInd[28]].invoke(objNode, uLoop);
			return (iRet != 0);
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	public void Quit() {
		try {
			methods[MethInd[29]].invoke(objNode);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static boolean Initialize(Context appCtx) {
		try {
			if (LoadJNIClass(appCtx)) {
				int iRet = (Integer)methods[MethInd[30]].invoke(null, appCtx);
				return (iRet != 0);
			}
			return false;
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	public static void Clean() {
		try {
			if (sAppCtx != null) {
				methods[MethInd[31]].invoke(null);
				sAppCtx = null;
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
