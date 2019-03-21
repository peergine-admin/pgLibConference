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

public final class pgLibTimer {
    private Random m_Random = new Random();
    private pgTimerTask m_timerTask = null;
    private ScheduledExecutorService m_timer = null;
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
        public int iCookie = 0;
        public String sParam = "";

        boolean bRepeat = false;
        int iTimeoutVal = 0;
        int iTimeCount = 0;
    }

    private Handler m_TimerHandler = null;
    private ArrayList<TimerItem> m_TimeList = new ArrayList<TimerItem>();

    private class WorkThread extends Thread {

        private Runnable target;
        private AtomicInteger counter;

        WorkThread(Runnable target, AtomicInteger counter) {
            this.target = target;
            this.counter = counter;
        }

        @Override
        public void run() {
            try {
                if(target!=null){
                    target.run();
                }
            } finally {
                int c = counter.getAndDecrement();
                System.out.println("pgLibTimer.terminate no " + c + " Threads");
            }
        }
    }


    // Timer class.
    class pgTimerTask extends TimerTask {
        public pgTimerTask() {
            super();
        }

        @Override
        public void run() {
            try {
                if (m_TimerHandler != null) {
                    Message oMsg = m_TimerHandler.obtainMessage(0, null);
                    m_TimerHandler.sendMessage(oMsg);
                }
            }
            catch (Exception ex) {
                outString("pgLibTimer.pgTimerTask.run, ex=" + ex.toString());
            }
        }
    }

    private void TimerProc() {
        m_Stamp ++ ;
        for (int i = 0; i < m_TimeList.size(); i++) {

            TimerItem oItem = m_TimeList.get(i);
            if (oItem.iTimeoutVal == 0) {
                continue;
            }

            oItem.iTimeCount++;
            if (oItem.iTimeCount < oItem.iTimeoutVal) {
                continue;
            }

            try {
                _OnTimeout(oItem.sParam);
            } catch (Exception ex) {
                outString("TimerProc : " + ex.toString());
            }
            //            TimerProc(oItem.sParam);

            if (!oItem.bRepeat) {
                oItem.iTimeoutVal = 0;
            }
            oItem.iTimeCount = 0;
        }
    }


    /**
     * 定时器初始化 -- pgLibShare内部使用
     * @param onTimeOut 定时器回调
     * @return true成功 false 失败
     */
    @SuppressLint("HandlerLeak")
    public boolean timerInit(OnTimeOut onTimeOut) {
        try {
            this.onTimeOut = onTimeOut;

            m_TimerHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    TimerProc();
                }
            };
            m_timer = new ScheduledThreadPoolExecutor(4, new ThreadFactory() {
                private AtomicInteger count = new AtomicInteger();
                @Override
                public Thread newThread(Runnable r) {
                    int c = count.incrementAndGet();
                    System.out.println("pgLibTimer.create no " + c + " Threads");
                    return new WorkThread(r,count);
                }
            });

            m_timerTask = new pgTimerTask();
            m_timer.schedule(m_timerTask,  1000 , TimeUnit.MILLISECONDS);

            return true;
        }
        catch (Exception ex) {
            outString("pgLibTimer.TimerInit: ex=" + ex.toString());
            m_TimerHandler = null;
            m_timerTask = null;
            m_timer = null;
            return false;
        }
    }
    /**
     * 内部使用
     */
    public void timerClean() {
        try {
            if (m_timer != null) {
                m_timer.shutdown();
                m_timer = null;
            }

            if(m_timerTask != null){
                m_timerTask.cancel();
                m_timerTask = null;
            }
            m_TimerHandler = null;
            for (int i = 0; i < m_TimeList.size(); i++) {
                TimerItem oItem = m_TimeList.get(i);
                oItem.iCookie = 0;
                oItem.sParam = "";
                oItem.bRepeat = false;
                oItem.iTimeoutVal = 0;
                oItem.iTimeCount = 0;
            }
        }
        catch (Exception ex) {
            outString("pgLibTimer.TimerClean, ex=" + ex.toString());
        }

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
    public int timerStart(String sParam, int iTimeout, boolean bRepeat) {

        try {
            int iItem = -1;
            for (int i = 0; i < m_TimeList.size(); i++) {
                if (m_TimeList.get(i).iTimeoutVal == 0) {
                    iItem = i;
                    break;
                }
            }
            if (iItem < 0) {
                m_TimeList.add(new TimerItem());
                iItem = m_TimeList.size() - 1;
            }

            int iCookie = (m_Random.nextInt() & 0xffff);

            TimerItem oItem = m_TimeList.get(iItem);
            oItem.iCookie = iCookie;
            oItem.sParam = sParam;
            oItem.bRepeat = bRepeat;
            oItem.iTimeoutVal = iTimeout;
            oItem.iTimeCount = 0;

            return (((iItem << 16) & 0xffff0000) | iCookie);
        }
        catch (Exception ex) {
            outString("pgLibTimer.Add, ex=" + ex.toString());
            return -1;
        }
    }

    /**
     * 结束定时器
     * @param iTimerID 定时器ID
     */
    public void timerStop(int iTimerID) {

        try {
            int iCookie = iTimerID & 0xffff;
            int iItem = (iTimerID >> 16) & 0xffff;

            if (iItem >= m_TimeList.size()) {
                return;
            }

            TimerItem oItem = m_TimeList.get(iItem);
            if (oItem.iCookie != iCookie) {
                return;
            }

            oItem.iCookie = (m_Random.nextInt() & 0xffff);
            oItem.sParam = "";
            oItem.bRepeat = false;
            oItem.iTimeoutVal = 0;
            oItem.iTimeCount = 0;
        }
        catch (Exception ex) {
            outString("pgLibTimer.TimerStop, ex=" + ex.toString());
        }
    }
}
