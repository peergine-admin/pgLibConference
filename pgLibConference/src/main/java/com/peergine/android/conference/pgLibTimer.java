package com.peergine.android.conference;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;

import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.peergine.android.conference.pgLibNode.outString;

/**
 * Copyright (C) 2014-2017, Peergine, All rights reserved.
 * www.peergine.com, www.pptun.com
 * com.peergine.lib.share
 *
 * @author ctkj
 */

public class pgLibTimer {
    private Random m_Random = new Random();

    private static void  _OutString(String sOut){
        System.out.println("pgLibTimer : " + sOut);
    }

    private long m_Stamp = 0;

    public interface OnTimeOut{
        void onTimeOut(String sParam);
    }

    private OnTimeOut onTimeOut = null;

    private void _OnTimeout(String sParam) {
        if(onTimeOut!=null){
            onTimeOut.onTimeOut(sParam);
        }
    }

    // Timer list.
    class TimerItem {
        public String sParam = "";

        public int iCookie = 0;
        public Timer timer = null;
        public pgTimerTask timerTask = null;

        public TimerItem(String sParam1) {
            sParam = sParam1;
        }
    }

    private Handler m_TimerHandler = null;
    private ArrayList<TimerItem> m_TimeList = new ArrayList<TimerItem>();

    // Timer class.
    class pgTimerTask extends TimerTask {
        int m_iTimeID = -1;

        public pgTimerTask(int iTimerID) {
            super();
            m_iTimeID = iTimerID;
        }

        public void run() {
            try {
                if (m_TimerHandler != null) {
                    Message oMsg = m_TimerHandler.obtainMessage(0, Integer.valueOf(m_iTimeID));
                    m_TimerHandler.sendMessage(oMsg);
                }
            }
            catch (Exception ex) {
                _OutString("pgLibLiveMultiRender.pgTimerTask.run, ex=" + ex.toString());
            }
        }
    }


    /**
     * 定时器初始化 -- pgLibShare内部使用
     * @param onTimeOut 定时器回调
     * @return true成功 false 失败
     */
    @SuppressLint("HandlerLeak")
    public boolean timerInit(OnTimeOut onTimeOut) {
        this.onTimeOut = onTimeOut;
        return _TimerInit();
    }
    /**
     * 内部使用
     */
    public void timerClean() {
       _TimerClean();
        this.onTimeOut = null;
    }

    public long timerStamp(){
        return m_Stamp;
    }

    /**
     * 开始一个定时器
     * @param sParam 定时器参数，传到定时器回调中。
     * @param iTimeout 超时时间
     * @return TimerID 用来结束定时器
     */
    public int timerStart(String sParam, int iTimeout,boolean bre) {
        return _TimerStart( sParam,iTimeout);
    }
    public int timerStart(String sParam, int iTimeout) {
        return _TimerStart( sParam,iTimeout);
    }

    /**
     * 结束定时器
     * @param iTimerID 定时器ID
     */
    public void timerStop(int iTimerID) {
        _TimerStop(iTimerID);
    }

    // Create Timer message handler.
    private boolean _TimerInit() {
        try {
            m_TimerHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {

                    int iTimerID = ((Integer)msg.obj).intValue();
                    int iCookie = iTimerID & 0xffff;
                    int iItem = (iTimerID >> 16) & 0xffff;

                    if (iItem >= m_TimeList.size()
                            || m_TimeList.get(iItem).iCookie != iCookie)
                    {
                        return;
                    }

                    TimerItem item = m_TimeList.get(iItem);
                    String sParam = item.sParam;

                    // Timer clean.
                    if (item.timer != null) {
                        item.timer.cancel();
                    }
                    if (item.timerTask != null) {
                        item.timerTask.cancel();
                    }
                    item.sParam = "";
                    item.timerTask = null;
                    item.timer = null;
                    item.iCookie = 0;

                    // Timer callback.
                    _OutString("stamp ");
                    m_Stamp ++;
                    _OnTimeout(sParam);
                }
            };

            return true;
        }
        catch (Exception ex) {
            _OutString("pgLibLiveMultiRender.TimerInit: ex=" + ex.toString());
            return false;
        }
    }

    private void _TimerClean() {
        if (m_TimerHandler != null) {
            for (int i = 0; i < m_TimeList.size(); i++) {
                try {
                    if (m_TimeList.get(i).timer != null) {
                        m_TimeList.get(i).timer.cancel();
                    }
                    if (m_TimeList.get(i).timerTask != null) {
                        m_TimeList.get(i).timerTask.cancel();
                    }

                    m_TimeList.get(i).sParam = "";
                    m_TimeList.get(i).timerTask = null;
                    m_TimeList.get(i).timer = null;
                    m_TimeList.get(i).iCookie = 0;
                }
                catch (Exception ex) {
                    _OutString("pgLibLiveMultiRender.TimerClean, ex=" + ex.toString());
                }
            }
            m_TimerHandler = null;
        }
    }

    private int _TimerStart(String sParam, int iTimeout) {

        try {
            int iItem = -1;
            for (int i = 0; i < m_TimeList.size(); i++) {
                if (m_TimeList.get(i).timer == null) {
                    iItem = i;
                    break;
                }
            }
            if (iItem < 0) {
                m_TimeList.add(new TimerItem(sParam));
                iItem = m_TimeList.size() - 1;
            }

            int iCookie = (m_Random.nextInt() & 0xffff);
            int iTimerID = (((iItem << 16) & 0xffff0000) | iCookie);

            Timer timer = new Timer();
            pgTimerTask timerTask = new pgTimerTask(iTimerID);

            TimerItem item = m_TimeList.get(iItem);
            item.sParam = sParam;

            item.timer = timer;
            item.timerTask = timerTask;
            item.iCookie = iCookie;
            timer.schedule(timerTask, (iTimeout * 1000));

            return iTimerID;
        }
        catch (Exception ex) {
            _OutString("pgLibLiveMultiRender.Add, ex=" + ex.toString());
            return -1;
        }
    }

    private void _TimerStop(int iTimerID) {

        int iCookie = iTimerID & 0xffff;
        int iItem = (iTimerID >> 16) & 0xffff;
        if (iItem >= m_TimeList.size()
                || m_TimeList.get(iItem).iCookie != iCookie)
        {
            return;
        }

        try {
            if (m_TimeList.get(iItem).timer != null) {
                m_TimeList.get(iItem).timer.cancel();
            }
            if (m_TimeList.get(iItem).timerTask != null) {
                m_TimeList.get(iItem).timerTask.cancel();
            }

            m_TimeList.get(iItem).sParam = "";
            m_TimeList.get(iItem).timerTask = null;
            m_TimeList.get(iItem).timer = null;
            m_TimeList.get(iItem).iCookie = 0;
        }
        catch (Exception ex) {
            _OutString("pgLibLiveMultiRender.TimerStop, ex=" + ex.toString());
        }
    }
}
