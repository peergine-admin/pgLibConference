package com.peergine.conference.demo.example;

public class Conference {

    /**
     * 打开视频时完成窗口和相关数据的改变
     *
     * @param sPeer 对象ID
     * @return
     */
    private boolean pgVideoOpen(String sPeer) {

        MEMBER MembTmp = memberSearch(sPeer);
        //没有窗口了
        if (MembTmp == null) {
            mConf.VideoReject(sPeer);
            return false;
        }
        if ("".equals(MembTmp.sPeer)) {
            MembTmp.sPeer = sPeer;
        }

        MembTmp.pView = mConf.VideoOpen(sPeer, 160, 120);
        if (MembTmp.pView != null) {
            MembTmp.pLayout.removeAllViews();
            LinearLayout parent = (LinearLayout) MembTmp.pView.getParent();
            if(parent!= null){
                parent.removeAllViews();
            }
            MembTmp.pLayout.addView(MembTmp.pView);
        }

        return true;

    }

    /**
     * 重置节点的显示窗口
     *
     * @param sPeer 重置节点
     */
    private void pgVideoRestore(String sPeer) {
        MEMBER membTmp = memberSearch(sPeer);
        if (membTmp != null) {
            if (membTmp.sPeer.equals(sPeer)) {
                membTmp.pLayout.removeView(membTmp.pView);
                membTmp.pView = null;
                membTmp.sPeer = "";
                membTmp.bJoin = false;
                membTmp.bVideoSync = false;
            }
        }
    }

    //清理窗口数据和关闭视频
    private void pgVideoClose(String sPeer) {
        mConf.VideoClose(sPeer);
        pgVideoRestore(sPeer);
    }

    private void pgRecordStartNew(){
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String sDate = formatter.format(currentTime);
        sPath = getSDCardDir() + "/test/record" + sDate + ".avi";
        int iErr = mConf.RecordStart(msChair,sPath,PG_RECORD_ONLYVIDEO_HASAUDIO,false);
        if(iErr!=0){
            Toast.makeText(getContext(), "录像失败。 已经关闭 Err = " + iErr, Toast.LENGTH_SHORT).show();
        }
        //boolean iErr = mConf.RecordStart(msChair, sPath);
//        if ((!iErr)) {
//            Toast.makeText(getContext(), "录像失败。 已经关闭", Toast.LENGTH_SHORT).show();
//            mConf.RecordStop(msChair,PG_RECORD_NORMAL);
//
//        }else{l
//
//        }
        int iErr1 = RecordAudioBothStart(sPath);
        if(iErr1!=0){
            Toast.makeText(getContext(), "录音失败。 已经关闭 Err = " + iErr1, Toast.LENGTH_SHORT).show();
        }

    }

    private void pgRecordStopNew(){
        mConf.RecordStop(msChair,PG_RECORD_ONLYVIDEO_HASAUDIO,false);
        int iErr = RecordAudioBothStop(sPath);
        if(iErr!=0) {
            Toast.makeText(getContext(), "RecordAudioBothStop 录音停止。 iErr = "+iErr, Toast.LENGTH_SHORT).show();
        }

    }

    private void pgRecordStart(){
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String sDate = formatter.format(currentTime);
        String sPath = getSDCardDir() + "/test/record" + sDate + ".avi";
        boolean iErr = mConf.RecordStart(msChair, sPath);
        if ((iErr == false)) {
            Toast.makeText(getContext(), "录像失败。 已经关闭", Toast.LENGTH_SHORT).show();
            mConf.RecordStop(msChair,PG_RECORD_NORMAL);

        }
    }

    private void pgRecordStop(){
        mConf.RecordStop(msChair);
    }

    //给所有加入会议的成员发送消息
    private boolean pgNotifySend() {
        String sData = mEdittextNotify.getText().toString();
        if ("".equals(sData)) {
            return false;
        }
        if(mConf.NotifySend(sData) == false){
            showInfo( "NotifySend 失败。");
        }

        return true;
    }

    private boolean m_bSpeechEnable = true;

    /**
     * 选择自己的声音是否在对端播放
     */
    private void Speech() {
        if (!mConf.AudioSpeech(mSmemb, m_bSpeechEnable)) {
            Log.d("pgRobotClient", "Enable speech failed");
        } else {
            m_bSpeechEnable = !m_bSpeechEnable;
        }
    }


    public void SetCameraRate(int iCameraRate) {
        pgLibJNINode Node = mConf.GetNode();
        if (Node != null) {
            if (Node.ObjectAdd("_aTemp", "PG_CLASS_Video", "", 0)) {

                String sData = "(Item){4}(Value){"+ iCameraRate +"}";

                Node.ObjectRequest("_aTemp", 2, sData, "");
                Node.ObjectDelete("_aTemp");
            }
        }
    }
    //
    public int RecordAudioBothStart(String sAviPath) {
        pgLibJNINode Node = mConf.GetNode();
        if (Node != null) {
            if (Node.ObjectAdd("_vTemp", "PG_CLASS_Audio", "", 0)) {
                String sData = "(Path){" + Node.omlEncode(sAviPath) + "}(Action){1}(MicNo){65535}(SpeakerNo){65535}(HasVideo){1}";
                /*String sData = "(Path){" + Node.omlEncode(sAviPath) + "}(Action){1}(MicNo){1}(SpeakerNo){65535}(HasVideo){1}";*/
                int iErr = Node.ObjectRequest("_vTemp", 38, sData, "");
                Log.d("pgLiveCapture", "RecordAudioBothStart, iErr=" + iErr);
                Node.ObjectDelete("_vTemp");
                return iErr;
            }
        }
        return 1;
    }


    ///
    // 停止录制双方对讲的音频数据到一个avi文件。
    //     sAviPath：保存音频数据的*.avi文件，必须与RecordAudioBothStart传入的sAviPath参数相同。
    //
    public int RecordAudioBothStop(String sAviPath) {
        pgLibJNINode Node = mConf.GetNode();
        if (Node != null) {
            if (Node.ObjectAdd("_vTemp", "PG_CLASS_Audio", "", 0)) {
                String sData = "(Path){" + Node.omlEncode(sAviPath) + "}(Action){0}";
                int iErr = Node.ObjectRequest("_vTemp", 38, sData, "");
                Log.d("pgLiveCapture", "RecordAudioBothStop, iErr=" + iErr);
                Node.ObjectDelete("_vTemp");
                return iErr;
            }
        }
        return 1;
    }


}
