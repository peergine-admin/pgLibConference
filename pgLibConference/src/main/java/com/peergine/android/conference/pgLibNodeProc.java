package com.peergine.android.conference;


/**
 * 上报到应用成的回调接口，接收中间件消息
 */
public interface pgLibNodeProc {
    //------

    /**
     * 回调到会议模块
     * @param sObj 对象名称
     * @param uMeth 方法
     * @param sData 内容
     * @param uHandle 句柄
     * @param sObjPeer 对端对象名称
     * @return 错误码
     */
    public abstract int _NodeOnExtRequest(String sObj, int uMeth, String sData, int uHandle, String sObjPeer);

    /**
     * 回调到上层
     * @param sObj 对象名称
     * @param uErrCode 错误码
     * @param sData 内容
     * @param sParam 参数
     * @return 0
     */
    public abstract int _NodeOnReply(String sObj, int uErrCode, String sData, String sParam);

}
