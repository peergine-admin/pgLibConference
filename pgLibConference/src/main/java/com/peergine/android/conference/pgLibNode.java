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
public class pgLibNode {

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
						s_iNodeLibInitCount.getAndIncrement();
						bResult = true;
					}
				}
			}
			return bResult;
		}
		catch (Exception e) {
			_OutString("pgLibNode.NodeLibInit: e=" + e.toString());
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
					}
				}
			}
		}
		catch (Exception e) {
			_OutString("pgLibNode.NodeLibClean: e=" + e.toString());
		}		
	}

	// Node lib init count.
	private static AtomicInteger s_iNodeLibInitCount = new AtomicInteger();

	/**
	 * 打印日志
	 * @param sOut 日志内容
	 */
	public static void _OutString(String sOut) {
		System.out.println(sOut);
	}

	/**
	 * 字符转int
	 * @param sVal 字符String
	 * @param idefVal 默认值 转换失败使用。
	 * @return 转换后的int
	 */
	public static int _ParseInt(String sVal, int idefVal) {
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
	public static String _AddrToReadable(String sAddr) {
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
