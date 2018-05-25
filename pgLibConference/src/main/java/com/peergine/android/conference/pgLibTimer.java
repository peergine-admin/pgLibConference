package com.peergine.android.conference;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;

import java.util.ArrayList;
import java.util.Random;
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
    private Random mRandom = new Random();

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
        public ScheduledExecutorService timer = null;
        public pgTimerTask timerTask = null;

        public TimerItem(String sParam1) {
            sParam = sParam1;
        }
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
                target.run();
            } finally {
                int c = counter.getAndDecrement();
                System.out.println("pgLibTimer.terminate no " + c + " Threads");
            }
        }
    }


    // Timer class.
    class pgTimerTask extends TimerTask {

        int m_iTimeID = -1;

        public pgTimerTask(int iTimerID) {
            super();
            m_iTimeID = iTimerID;
        }

        @Override
        public void run() {
            try {
                if (m_TimerHandler != null) {
                    Message oMsg = m_TimerHandler.obtainMessage(0, Integer.valueOf(m_iTimeID));
                    m_TimerHandler.sendMessage(oMsg);
                }
            }
            catch (Exception ex) {
                outString("pgLibTimer.pgTimerTask.run, ex=" + ex.toString());
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
        try {
            this.onTimeOut = onTimeOut;
            m_TimerHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {

                    int iTimerID = (Integer) msg.obj;
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
                        item.timer.shutdown();
                    }
                    if (item.timerTask != null) {
                        item.timerTask.cancel();
                    }
                    item.sParam = "";
                    item.timerTask = null;
                    item.timer = null;
                    item.iCookie = 0;

                    // Timer callback.
                    _OnTimeout(sParam);
                }
            };

            return true;
        }
        catch (Exception ex) {
            outString("pgLibTimer.TimerInit: ex=" + ex.toString());
            return false;
        }
    }


    /**
     * 内部使用
     */
    public void timerClean() {
        if (m_TimerHandler != null) {
            for (int i = 0; i < m_TimeList.size(); i++) {
                try {
                    if (m_TimeList.get(i).timer != null) {
                        m_TimeList.get(i).timer.shutdown();
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
                    outString("pgLibTimer.TimerClean, ex=" + ex.toString());
                }
            }
            m_TimerHandler = null;
        }
    }

    /**
     * 开始一个定时器
     * @param sParam 定时器参数，传到定时器回调中。
     * @param iTimeout 超时时间
     * @return TimerID 用来结束定时器
     */
    public int timerStart(String sParam, int iTimeout) {

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

            int iCookie = (mRandom.nextInt() & 0xffff);
            int iTimerID = (((iItem << 16) & 0xffff0000) | iCookie);

            ScheduledExecutorService timer = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
                private AtomicInteger count = new AtomicInteger();
                @Override
                public Thread newThread(Runnable r) {
                    int c = count.incrementAndGet();
                    System.out.println("pgLibTimer.create no " + c + " Threads");
                    return new WorkThread(r,count);
                }
            });
            pgTimerTask timerTask = new pgTimerTask(iTimerID);

            TimerItem item = m_TimeList.get(iItem);
            item.sParam = sParam;

            item.timer = timer;
            item.timerTask = timerTask;
            item.iCookie = iCookie;
            timer.schedule(timerTask, (iTimeout * 1000), TimeUnit.MILLISECONDS);

            return iTimerID;
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

        int iCookie = iTimerID & 0xffff;
        int iItem = (iTimerID >> 16) & 0xffff;
        if (iItem >= m_TimeList.size()
                || m_TimeList.get(iItem).iCookie != iCookie)
        {
            return;
        }

        try {
            if (m_TimeList.get(iItem).timer != null) {
                m_TimeList.get(iItem).timer.shutdown();
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
            outString("pgLibTimer.TimerStop, ex=" + ex.toString());
        }
    }
}
