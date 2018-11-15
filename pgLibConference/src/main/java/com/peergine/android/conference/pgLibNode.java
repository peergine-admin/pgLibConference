/*************************************************************************
  copyright   : Copyright (C) 2014-2017, Peergine, All rights reserved.
              : www.peergine.com, www.pptun.com
              :
  filename    : pgLibNode.java
  discription : 
  modify      : create, chenbichao, 2017/08/23

*************************************************************************/

package com.peergine.android.conference;


import android.content.Context;

import com.peergine.plugin.lib.pgLibJNINode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author ctkj
 * Node 对象初始化管理类
 */
public final class pgLibNode {

	public static final String PG_CLASS_Peer = "PG_CLASS_Peer";
	public static final String PG_CLASS_Group = "PG_CLASS_Group";
	public static final String PG_CLASS_Data = "PG_CLASS_Data";
	public static final String PG_CLASS_File = "PG_CLASS_File";
	public static final String PG_CLASS_Audio = "PG_CLASS_Audio";
	public static final String PG_CLASS_Video = "PG_CLASS_Video";

	public static final int PG_ADD_COMMON_Sync = 0x10000;
	public static final int PG_ADD_COMMON_Error = 0x20000;
	public static final int PG_ADD_COMMON_Encrypt = 0x40000;
	public static final int PG_ADD_COMMON_Compress = 0x80000;


	public static final int PG_ADD_PEER_Self = 0x1;
	public static final int PG_ADD_PEER_Server = 0x2;
	public static final int PG_ADD_PEER_Static = 0x4;
	public static final int PG_ADD_PEER_Digest = 0x8;
	public static final int PG_ADD_PEER_Disable = 0x10;

	public static final int PG_ADD_GROUP_Master =0x1;
	public static final int PG_ADD_GROUP_Refered = 0x2;
	public static final int PG_ADD_GROUP_NearPeer = 0x4;
	public static final int PG_ADD_GROUP_Modify = 0x8;
	public static final int PG_ADD_GROUP_Index = 0x10;
	public static final int PG_ADD_GROUP_Offline = 0x20;
	public static final int PG_ADD_GROUP_HearOnly = 0x40;

	public static final int PG_ADD_FILE_TcpSock = 0x1;
	public static final int PG_ADD_FILE_Flush = 0x2;
	public static final int PG_ADD_FILE_PeerStop = 0x4;

	public static final int PG_ADD_AUDIO_Conference = 0x1;
	public static final int PG_ADD_AUDIO_ShowVolume = 0x2;
	public static final int PG_ADD_AUDIO_OnlyInput =0x4;
	public static final int PG_ADD_AUDIO_OnlyOutput = 0x8;
	public static final int PG_ADD_AUDIO_SendReliable = 0x10;
	public static final int PG_ADD_AUDIO_NoSpeechSelf = 0x20;
	public static final int PG_ADD_AUDIO_NoSpeechPeer = 0x40;
	public static final int PG_ADD_AUDIO_MuteInput = 0x80;
	public static final int PG_ADD_AUDIO_MuteOutput = 0x100;

	public static final int PG_ADD_VIDEO_Conference = 0x1;
	public static final int PG_ADD_VIDEO_Preview = 0x2;
	public static final int PG_ADD_VIDEO_OnlyInput = 0x4;
	public static final int PG_ADD_VIDEO_OnlyOutput = 0x8;
	public static final int PG_ADD_VIDEO_FrameStat = 0x10;
	public static final int PG_ADD_VIDEO_DrawThread = 0x20 ;
	public static final int PG_ADD_VIDEO_OutputExternal = 0x40;
	public static final int PG_ADD_VIDEO_OutputExtCmp = 0x80;
	public static final int PG_ADD_VIDEO_FilterDecode = 0x100;

	public static final int  PG_METH_COMMON_Sync = 0;
	public static final int  PG_METH_COMMON_Error = 1;
	public static final int  PG_METH_COMMON_SetOption = 2;
	public static final int  PG_METH_COMMON_GetOption = 3;

	public static final int PG_METH_PEER_Login = 32;
	public static final int PG_METH_PEER_Logout=33;
	public static final int PG_METH_PEER_Status=34;
	public static final int PG_METH_PEER_Call=35;
	public static final int PG_METH_PEER_Message=36;
	public static final int PG_METH_PEER_SetAddr=37;
	public static final int PG_METH_PEER_GetAddr=38;
	public static final int PG_METH_PEER_DigGen=39;
	public static final int PG_METH_PEER_DigVerify=40;
	public static final int PG_METH_PEER_CheckInfo=41;
	public static final int PG_METH_PEER_LanScan = 42;
	public static final int PG_METH_PEER_AgentLogin = 43;
	public static final int PG_METH_PEER_AgentLogout = 44;
	public static final int PG_METH_PEER_AgentMessage = 45;
	public static final int PG_METH_PEER_ReloginReply = 46;
	public static final int PG_METH_PEER_KickOut = 47;
	public static final int PG_METH_PEER_AccessCtrl = 48;
	public static final int PG_METH_PEER_PushOption = 49;


	public static final int PG_METH_GROUP_Modify=32;
	public static final int PG_METH_GROUP_Update=33;
	public static final int PG_METH_GROUP_Master=34;

	public static final int PG_METH_DATA_Message = 32;

	public static final int PG_METH_FILE_Put = 32;
	public static final int PG_METH_FILE_Get = 33;
	public static final int PG_METH_FILE_Status = 34;
	public static final int PG_METH_FILE_Cancel = 35;

	public static final int PG_METH_AUDIO_Open = 32;
	public static final int PG_METH_AUDIO_Close = 33;
	public static final int PG_METH_AUDIO_CtrlVolume = 34;
	public static final int PG_METH_AUDIO_ShowVolume = 35;
	public static final int PG_METH_AUDIO_Speech = 36;
	public static final int PG_METH_AUDIO_Record = 37;

	public static final int PG_METH_VIDEO_Open = 32;
	public static final int PG_METH_VIDEO_Close =33;
	public static final int PG_METH_VIDEO_Move = 34;
	public static final int PG_METH_VIDEO_Join = 35;
	public static final int PG_METH_VIDEO_Leave = 36;
	public static final int PG_METH_VIDEO_Camera =37;
	public static final int PG_METH_VIDEO_Record =38;
	public static final int PG_METH_VIDEO_Transfer = 39;
	public static final int PG_METH_VIDEO_FrameStat = 40;

	public pgLibNode() {
	}

	/**
	 * 通过上下文初始化Node JNI对象。如果没有一个APP没有这个操作，Node对象的方法将无效。
	 * @param oCtx 上下文
	 * @return true 成功，false 失败
	 */
	public static boolean NodeLibInit(Context oCtx) {
		try {
			boolean bResult = false;
			synchronized(s_iNodeLibInitCount) {
				if (s_iNodeLibInitCount.get() > 0) {
					s_iNodeLibInitCount.getAndIncrement();
					bResult = true;
				}
				else {
					if (pgLibJNINode.Initialize(oCtx)) {
						pgLibView.Clean();
						s_iNodeLibInitCount.getAndIncrement();
						bResult = true;
					}
				}
			}
			return bResult;
		}
		catch (Exception e) {
			outString("pgLibNode.NodeLibInit: e=" + e.toString());
			return false;
		}
	}

	/**
	 * 清除初始化信息，如果s_iNodeLibInitCount 为0，Node对象的方法将无效。
	 */
	public static void NodeLibClean() {
		try {
			synchronized(s_iNodeLibInitCount) {
				if (s_iNodeLibInitCount.get() > 0) {
					s_iNodeLibInitCount.getAndDecrement();
					if (s_iNodeLibInitCount.get() == 0) {
						pgLibJNINode.Clean();
						pgLibView.Clean();
					}
				}
			}
		}
		catch (Exception e) {
			outString("pgLibNode.NodeLibClean: e=" + e.toString());
		}		
	}

	// Node lib init count.
	private static AtomicInteger s_iNodeLibInitCount = new AtomicInteger();

	/**
	 * 打印日志
	 * @param sOut 日志内容
	 */
	public static void outString(String sOut) {
		System.out.println(sOut);
	}

	/**
	 * 字符转int
	 * @param sVal 字符String
	 * @param idefVal 默认值 转换失败使用。
	 * @return 转换后的int
	 */
	public static int parseInt(String sVal, int idefVal) {
		try {
			if ("".equals(sVal)) {
				return idefVal;
			}
			return Integer.parseInt(sVal);
		}
		catch (Exception ex) {
			return idefVal;
		}
	}

	/**
	 * 内部使用，地址转换函数
	 * @param sAddr 地址
	 * @return 地址
	 */
	public static String addrToReadable(String sAddr) {
		try {
			String[] sAddrSect = sAddr.split(":", 6);
			if (sAddrSect.length < 6) {
				return sAddr;
			}

			String sReadable = "";
			if ("0".equals(sAddrSect[0])
					&& "0".equals(sAddrSect[1])
					&& "0".equals(sAddrSect[2])
					&& !"0".equals(sAddrSect[3])
					&& !"1".equals(sAddrSect[3]))
			{
				long iIP = Long.parseLong(sAddrSect[3], 16);

				int iIP0 = (int)((iIP >> 24) & 0xff);
				int iIP1 = (int)((iIP >> 16) & 0xff);
				int iIP2 = (int)((iIP >> 8) & 0xff);
				int iIP3 = (int)(iIP & 0xff);

				sReadable = (iIP0 + "." + iIP1 + "." + iIP2 + "." + iIP3 + ":" + sAddrSect[4]);
			}
			else {
				long iIP0 = Long.parseLong(sAddrSect[0], 16);
				long iIP1 = Long.parseLong(sAddrSect[1], 16);
				long iIP2 = Long.parseLong(sAddrSect[2], 16);
				long iIP3 = Long.parseLong(sAddrSect[3], 16);

				int iWord0 = (int)((iIP0 >> 16) & 0xffff);
				int iWord1 = (int)(iIP0 & 0xffff);

				int iWord2 = (int)((iIP1 >> 16) & 0xffff);
				int iWord3 = (int)(iIP1 & 0xffff);

				int iWord4 = (int)((iIP2 >> 16) & 0xffff);
				int iWord5 = (int)(iIP2 & 0xffff);

				int iWord6 = (int)((iIP3 >> 16) & 0xffff);
				int iWord7 = (int)(iIP3 & 0xffff);

				sReadable = ("[" + Integer.toString(iWord0, 16) + ":" + Integer.toString(iWord1, 16)
						+ ":" + Integer.toString(iWord2, 16) + ":" + Integer.toString(iWord3, 16)
						+ ":" + Integer.toString(iWord4, 16) + ":" + Integer.toString(iWord5, 16)
						+ ":" + Integer.toString(iWord6, 16) + ":" + Integer.toString(iWord7, 16)
						+ "]:" + sAddrSect[4]);
			}

			return sReadable;
		}
		catch (Exception e) {
			return sAddr;
		}
	}

	/**
	 *  复制单个文件
	 *  @param  oldPath  String  原文件路径  如：c:/fqf.txt
	 *  @param  newPath  String  复制后路径  如：f:/fqf.txt
	 */
	public static void  copyFile(String  oldPath,  String  newPath)  {
		try  {
			int  bytesum  =  0;
			int  byteread  =  0;
			File oldfile  =  new  File(oldPath);
			if  (oldfile.exists())  {  //文件存在时
				InputStream inStream  =  new FileInputStream(oldPath);  //读入原文件
				FileOutputStream fs  =  new  FileOutputStream(newPath);
				byte[]  buffer  =  new  byte[1024*1024];
				int  length;
				while  (  (byteread  =  inStream.read(buffer))  !=  -1)  {
					bytesum  +=  byteread;  //字节数  文件大小
					System.out.println(bytesum);
					fs.write(buffer,  0,  byteread);
				}
				inStream.close();
			}
		}
		catch  (Exception  e)  {
			System.out.println("复制单个文件操作出错");
			e.printStackTrace();

		}

	}
}
