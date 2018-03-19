/*************************************************************************
  copyright   : Copyright (C) 2014-2017, Peergine, All rights reserved.
              : www.peergine.com, www.pptun.com
              :
  filename    : pgLibError.java
  discription : 
  modify      : create, chenbichao, 2017/04/05

*************************************************************************/

package com.peergine.android.conference;

/**
 * @author ctkj
 * 错误码常量的定义
 */
public final class pgLibError {

	/** 成功*/
	public static final int PG_ERR_Normal = 0;
	/** 系统错误*/
	public static final int PG_ERR_System = 1;
	/** 参数错误*/
	public static final int PG_ERR_BadParam = 2;
	/** 错误的通信对象类*/
	public static final int PG_ERR_BadClass = 3;
	/** 错误的通信对象类方法*/
	public static final int PG_ERR_BadMethod = 4;
	/** 错误的通信对象*/
	public static final int PG_ERR_BadObject = 5;
	/** 错误的状态*/
	public static final int PG_ERR_BadStatus = 6;
	/** 错误的文件*/
	public static final int PG_ERR_BadFile = 7;
	/** 错误的用户*/
	public static final int PG_ERR_BadUser = 8;
	/** 错误的密码*/
	public static final int PG_ERR_BadPass = 9;
	/** 还没有登录*/
	public static final int PG_ERR_NoLogin = 10;
	/** 网络错误*/
	public static final int PG_ERR_Network = 11;
	/** 操作超时*/
	public static final int PG_ERR_Timeout = 12;
	/** 拒绝访问*/
	public static final int PG_ERR_Reject = 13;
	/** 系统正忙*/
	public static final int PG_ERR_Busy = 14;
	/** 已经打开*/
	public static final int PG_ERR_Opened = 15;
	/** 已经关闭*/
	public static final int PG_ERR_Closed = 16;
	/** 对象或资源已经存在*/
	public static final int PG_ERR_Exist = 17;
	/** 对象或资源不存在*/
	public static final int PG_ERR_NoExist = 18;
	/** 没有空间了*/
	public static final int PG_ERR_NoSpace = 19;
	/** 错误的类型*/
	public static final int PG_ERR_BadType = 20;
	/** 检查Peer对象信息和状态错误*/
	public static final int PG_ERR_CheckErr = 21;
	/** 错误的服务器*/
	public static final int PG_ERR_BadServer = 22;
	/** 错误的域*/
	public static final int PG_ERR_BadDomain = 23;
	/** 没有数据*/
	public static final int PG_ERR_NoData = 24;
	/** 未知错误*/
	public static final int PG_ERR_Unknown = 0xff;
	
	public pgLibError() {
	}
}
