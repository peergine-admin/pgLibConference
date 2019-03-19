package com.peergine.android.conference;

/**
 * Copyright (C) 2014-2017, Peergine, All rights reserved.
 * www.peergine.com, www.pptun.com
 * com.peergine.android.conference
 *
 * @author ctkj
 * @date 2018/3/12.
 */

/**
 * 描述：设置消息接收回调接口。
 * 阻塞方式：非阻塞，立即返回
 * eventListener：[IN] 实现了OnEventListner接口的对象，必须定义event函数。
 */
public interface OnEventListener {
    /**
     * 上报事件回调
     * @param sAct 上报事件名称 Action
     * @param sData 上报事件数据，默认为空
     * @param sPeer 上报事件对端节点, 默认为空
     * @param sGroup 上报事件相关会议名称，默认为空
     * @param sEventParam 上报事件额外参数,默认为空
     *
     */
    void event(String sAct, String sData, String sPeer,String sGroup,String sEventParam);
}