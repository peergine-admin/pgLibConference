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
        _GroupDelete(group);
    }
    public void  _GroupDelete(Group group){
        if(group == null) {
            return;
        }
        m_listGroup.remove(group);

    }
    public void _GroupClean(){
        m_listGroup.clear();
    }

}
