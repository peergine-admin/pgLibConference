/****************************************************************
  copyright   : Copyright (C) 2012-2012, chenbichao,
                All rights reserved.
  filename    : pgLibJNINodeProc.java
  discription : 
  modify      : create, chenbichao, 2012/1/8

*****************************************************************/

package com.peergine.plugin.lib;


public class pgLibJNINodeProc extends Object {
	public pgLibJNINodeProc() {
	}
	public int OnReply(String sObj, int uErrCode, String sData, String sParam) {
		return -1;
	}
	public int OnExtRequest(String sObj, int uMeth, String sData, int uHandle, String sPeer) {
		return -1;
	}	
}
