package com.peergine.android.conference;

import java.util.ArrayList;

import static com.peergine.android.conference.pgLibConference2._AudioBuildObject;
import static com.peergine.android.conference.pgLibConference2._DataBuildObject;
import static com.peergine.android.conference.pgLibConference2._GroupBuildObject;
import static com.peergine.android.conference.pgLibConference2._ObjPeerBuild;
import static com.peergine.android.conference.pgLibConference2._OutString;
import static com.peergine.android.conference.pgLibConference2._VideoBuildObject;
import static com.peergine.android.conference.pgLibConference2._VideoLBuildObject;

public class GroupList {
    public class Group {

        String sConfName = "";
        String sChair = "";
        String sUser = "";


        int iKeepTimer = -1;
        int iActiveTimer = -1;



        boolean bChairman = false;
        String sObjChair = "";

        String sObjG = "";
        String sObjD = "";
        String sObjV = "";
        String sObjLV = "";
        String sObjA = "";

        boolean bServiceStart = false;
        boolean bApiVideoStart = false;
        boolean bApiAudioStart = false;

        // 视频连接状态检测
        private int iActiveExpire = 10;
        private int iActiveStamp = 0;

        // 节点连接状态检测
        private int iExpire = 10;
        private int iKeepStamp = 0;
        private int iKeepChainmanStamp = 0;
        private int iRequestChainmanStamp = 0;

        Group(String sConfName, String sChair, String sUser) {

            this.sConfName = sConfName;
            this.sChair = sChair;
            this.sUser = sUser;

            iKeepTimer = -1;
            iActiveTimer = -1;


            sObjChair = _ObjPeerBuild(sChair);
            sObjG = _GroupBuildObject(sConfName);
            sObjD = _DataBuildObject(sConfName);
            sObjV = _VideoBuildObject(sConfName);
            sObjLV = _VideoLBuildObject(sConfName);
            sObjA = _AudioBuildObject(sConfName);

            restoreStamp();
        }
        void restoreStamp() {
            iActiveStamp = 0;
            iKeepStamp = 0;
            iKeepChainmanStamp = 0;
            iRequestChainmanStamp = 0;
        }


        public boolean isChairman() {
            return this.sChair.equals(this.sUser);
        }

        private boolean _ObjChairmanIs(String sObj){
            return this.sObjChair.equals(sObj);
        }

        private String _VideoObjectGet(int iStreamMode){
            return iStreamMode == 0 ?sObjV:sObjLV;
        }
    }

    private final ArrayList<Group> m_listGroup = new ArrayList<>();
    //搜索加入会议的节点
    public Group _GroupSearch(String sConfName) {
        for (int i = 0;i< m_listGroup.size();i++){
            if(m_listGroup.get(i).sConfName.equals(sConfName)){
                return m_listGroup.get(i);
            };
        }
        return null;
    }
    public void _GroupAdd2(String sConfName, String sChair, String sUser) {
        Group group = new Group(sConfName,sChair,sUser);
        _GroupAdd(group);
    }
    public void _GroupAdd(Group group){
        try {
            if(_GroupSearch(group.sConfName)==null){
                m_listGroup.add(group);
            }

        } catch (Exception ex) {
            _OutString("_LostPeerAdd ex = " + ex.toString());
        }
    }

    public void  _GroupDelete2(String sConfName){
        Group group = _GroupSearch(sConfName);
        m_listGroup.remove(group);

    } public void  _GroupDelete(Group group){
        m_listGroup.remove(group);

    }
    public void _GroupClean(){
        m_listGroup.clear();
    }

}
