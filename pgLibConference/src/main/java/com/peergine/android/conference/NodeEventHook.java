package com.peergine.android.conference;

/**
 * Copyright (C) 2014-2017, Peergine, All rights reserved.
 * www.peergine.com, www.pptun.com
 * com.peergine.android.conference
 *
 * @author ctkj
 * @date 2018/3/12.
 */

public interface NodeEventHook {
    /**
     * 钩子结构回调接口，不建议使用，使用需要对中间件编程有足够的了解。
     */

    int OnExtRequest(String sObj, int uMeth, String sData, int uHandle, String sPeer);
    int OnReply(String sObj, int uErr, String sData, String sParam);

}
