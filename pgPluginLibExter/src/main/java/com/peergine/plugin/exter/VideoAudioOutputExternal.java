package com.peergine.plugin.exter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.LinearLayout;

import com.peergine.plugin.android.pgDevVideoOut;

import java.nio.ByteBuffer;

/**
 * Created by ctkj on 2017/7/19.
 */

public class VideoAudioOutputExternal {
    private final VideoPlayView m_wndPlay;

    VideoAudioOutputExternal(LinearLayout m_View , Context context){
        pgDevVideoOut.SetCallback(m_oVideoOutCB);
        Log.d("pgLiveRanExter", "pgDevVideoOut Set callback");


        m_wndPlay = new VideoPlayView(context);
        m_View.addView(m_wndPlay);
        m_wndPlay.setVisibility(View.VISIBLE);
    }



    public pgDevVideoOut.OnCallback m_oVideoOutCB = new pgDevVideoOut.OnCallback() {

        @Override
        public int Open(int iDevNO) {
            // TODO Auto-generated method stub
            Log.d("RenExter", "pgDevVideoOut.Open: iDevNO=" + iDevNO);

            return 1234;
        }

        @Override
        public void Close(int iDevID) {
            // TODO Auto-generated method stub
            Log.d("RenExter", "pgDevVideoOut.Close: iDevID=" + iDevID);
        }

        @Override
        public void Image(int iDevID, byte[] byData, int iFormat, int iFlag,
                          int iPosX, int iPosY, int iWidth, int iHeight, int iFillMode, int iRotate)
        {
            // TODO Auto-generated method stub
            if (iFormat == pgDevVideoOut.PG_DEV_VIDEO_OUT_FMT_RGB24) {
                m_wndPlay.DrawBitmap(byData, iPosX, iPosY, iWidth, iHeight, iFillMode);
            }
            else {
                // Need to decode data, and then play.
            }
        }

        @Override
        public void Clean(int iDevID) {
            // TODO Auto-generated method stub
            Log.d("RenExter", "pgDevVideoOut.Clean: iDevID=" + iDevID);

            m_wndPlay.DrawClean();
        }
    };
}

class VideoPlayView extends SurfaceView implements SurfaceHolder.Callback
{
    // Videp bitmap mode
    private static final int VIDEO_BITMAP_DstInSrc = 0;
    private static final int VIDEO_BITMAP_SrcInDst = 1;
    private static final int VIDEO_BITMAP_SrcFitDst = 2;

    // Board member.
    private int m_iWndWidth = 0;
    private int m_iWndHeight = 0;

    private int m_iWidth = 0;
    private int m_iHeight = 0;
    int[] m_iImgData = null;

    private int m_iVideoFillMode = 0;
    private int m_iVideoFillCount = 0;
    private Paint m_PaintVideo = null;

    public VideoPlayView(Context ctx) {
        super(ctx);

        try {
            m_PaintVideo = new Paint();
            m_PaintVideo.setAntiAlias(true);
            m_PaintVideo.setFilterBitmap(true);

            SurfaceHolder holder = getHolder();
            holder.addCallback(this);

            setFocusable(true);
        }
        catch (Exception ex) {
            Log.d("pgnpp", "pgSysWnd: ex=" + ex.toString());
        }
    }

    public void DrawBitmap(byte[] byData, int iPosX, int iPosY, int iWidth, int iHeight, int iFillMode) {

        SurfaceHolder holder = getHolder();
        Canvas canvas = holder.lockCanvas();
        if (canvas == null) {
            return;
        }

        try {

            int iSizeXY = iWidth * iHeight;
            if (iWidth != m_iWidth || iHeight != m_iHeight) {
                m_iImgData = new int[iSizeXY];
                m_iWidth = iWidth;
                m_iHeight = iHeight;
                m_iVideoFillCount = 4;
            }

            int iPos = 0;
            for (int i = 0; i < iSizeXY; i++, iPos += 3) {
                m_iImgData[i] = (((byData[iPos] << 16) & 0x00ff0000)
                        | ((byData[iPos + 1] << 8) & 0x0000ff00)
                        | ((byData[iPos + 2]) & 0x000000ff));
            }

            int iPosX1 = 0, iPosY1 = 0;
            float fScaleX = 0.0f, fScaleY = 0.0f;

            if (m_iVideoFillMode != iFillMode) {
                m_iVideoFillMode = iFillMode;
                m_iVideoFillCount = 4;
            }
            if (m_iVideoFillCount > 0) {
                canvas.drawColor(Color.BLACK);
                m_iVideoFillCount--;
            }

            if (iFillMode == VIDEO_BITMAP_DstInSrc) {
                if (((m_iWndWidth << 3) / m_iWndHeight) > ((iWidth << 3) / iHeight)) {
                    fScaleX = (float)m_iWndWidth / (float)iWidth;
                    fScaleY = fScaleX;
                    int iHeight1 = (int)(((float)iWidth * (float)m_iWndHeight) / (float)m_iWndWidth);
                    iPosY1 = -((iHeight - iHeight1) / 2);
                }
                else if (((m_iWndWidth << 3) / m_iWndHeight) < ((iWidth << 3) / iHeight)) {
                    fScaleY = (float)m_iWndHeight / (float)iHeight;
                    fScaleX = fScaleY;
                    int iWidth1 = (int)(((float)iHeight * (float)m_iWndWidth) / (float)m_iWndHeight);
                    iPosX1 = -((iWidth - iWidth1) / 2);
                }
                else {
                    fScaleX = (float)m_iWndWidth / (float)iWidth;
                    fScaleY = (float)m_iWndHeight / (float)iHeight;
                }
            }
            else if (iFillMode == VIDEO_BITMAP_SrcInDst) {
                if (((m_iWndWidth << 3) / m_iWndHeight) > ((iWidth << 3) / iHeight)) {
                    fScaleX = (float)m_iWndHeight / (float)iHeight;
                    fScaleY = fScaleX;
                    int iWidth1 = (int)(((float)iHeight * (float)m_iWndWidth) / (float)m_iWndHeight);
                    iPosX1 = (iWidth1 - iWidth) / 2;
                }
                else if (((m_iWndWidth << 3) / m_iWndHeight) < ((iWidth << 3) / iHeight)) {
                    fScaleY = (float)m_iWndWidth / (float)iWidth;
                    fScaleX = fScaleY;
                    int iHeight1 = (int)(((float)iWidth * (float)m_iWndHeight) / (float)m_iWndWidth);
                    iPosY1 = (iHeight1 - iHeight) / 2;
                }
                else {
                    fScaleX = (float)m_iWndWidth / (float)iWidth;
                    fScaleY = (float)m_iWndHeight / (float)iHeight;
                }
            }
            else { // (iFillMode == VIDEO_BITMAP_SrcFitDst)
                fScaleX = (float)m_iWndWidth / (float)iWidth;
                fScaleY = (float)m_iWndHeight / (float)iHeight;
            }

            canvas.scale(fScaleX, fScaleY);
            canvas.drawBitmap(m_iImgData, 0, iWidth, iPosX1, iPosY1, iWidth, iHeight, false, m_PaintVideo);
        }
        catch (Exception ex) {
            Log.d("pgLive", "DrawBitmap: ex=" + ex.toString());
        }

        holder.unlockCanvasAndPost(canvas);
    }

    public void DrawClean() {

        SurfaceHolder holder = getHolder();
        Canvas canvas = holder.lockCanvas();
        if (canvas == null) {
            return;
        }

        try {
            canvas.drawColor(Color.BLACK);
        }
        catch (Exception ex) {
            Log.d("pgLive", "DrawClean: ex=" + ex.toString());
        }

        holder.unlockCanvasAndPost(canvas);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        m_iWndWidth = width;
        m_iWndHeight = height;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }
}


class VideoPlayViewGL extends GLSurfaceView {

    private int m_iWidth = 0;
    private int m_iHeight = 0;

    private MyRenderer m_Renderer = null;

    public VideoPlayViewGL(Context ctx) {
        super(ctx);

        Log.d("plugin out Exter", "VideoPlayViewGL.VideoPlayViewGL");

        this.setFocusableInTouchMode(true);

        this.setEGLContextClientVersion(2);

        this.setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        this.setDebugFlags(DEBUG_CHECK_GL_ERROR);

        m_Renderer = new MyRenderer();
        this.setRenderer(m_Renderer);
        this.setRenderMode(RENDERMODE_WHEN_DIRTY);


        Log.d("plugin out Exter", "VideoPlayViewGL.VideoPlayViewGL end");
    }

    public void DrawBitmap(byte[] byData, int iPosX, int iPosY, int iWidth, int iHeight, int iFillMode) {
        m_Renderer.DrawBitmap(byData, iPosX, iPosY, iWidth, iHeight, iFillMode);
        requestRender();
    }

    public void DrawClean() {
        m_Renderer.DrawClean();
        requestRender();
    }
}


///-------------------------------------------------------------------------------------
// Renderer.
class MyRenderer implements GLSurfaceView.Renderer {

    // Videp bitmap mode
    private static final int VIDEO_BITMAP_DstInSrc = 0;
    private static final int VIDEO_BITMAP_SrcInDst = 1;
    private static final int VIDEO_BITMAP_SrcFitDst = 2;

    // Board member.
    private int m_iWndWidth = 0;
    private int m_iWndHeight = 0;

    private int m_iVideoFillMode = 0;
    private int m_iVideoFillCount = 0;

    private int m_iTexture = -1;

    private Object m_sDraw = new Object();
    private ByteBuffer m_byBuf = null;
    private int m_iDrawPosX = 0;
    private int m_iDrawPosY = 0;
    private int m_iDrawWidth = 0;
    private int m_iDrawHeight = 0;
    private int m_iDrawFillMode = 0;

    public MyRenderer() {
        super();
        Log.d("plugin out Exter", "MyRenderer.MyRenderer");
    }

    public void DrawBitmap(byte[] byData, int iPosX, int iPosY, int iWidth, int iHeight, int iFillMode) {
        try {
            synchronized(m_sDraw) {
                m_byBuf = ByteBuffer.wrap(byData);
                m_iDrawPosX = iPosX;
                m_iDrawPosY = iPosY;
                m_iDrawWidth = iWidth;
                m_iDrawHeight = iHeight;
                m_iDrawFillMode = iFillMode;
            }
        }
        catch (Exception ex) {
            Log.d("plugin out Exter", "MyRenderer.DrawBitmap, ex=" + ex.toString());
        }
    }

    public void DrawClean() {
        try {
            synchronized(m_sDraw) {
                m_byBuf = null;
            }
        }
        catch (Exception ex) {
            Log.d("plugin out Exter", "MyRenderer.DrawClean, ex=" + ex.toString());
        }
    }

    @Override
    public void onDrawFrame(javax.microedition.khronos.opengles.GL10 gl) {

        try {
            synchronized(m_sDraw) {

                int iErr;
                if (m_byBuf != null) {
                    if (m_iTexture < 0) {
                        int iTexture[] = new int[1];
                        GLES20.glGenTextures(1, iTexture, 0);
                        if ((iErr = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
                            Log.d("plugin out Exter", "MyRenderer.onDrawFrame: glGenTextures, iErr=" + iErr);
                        }
                        m_iTexture = iTexture[0];
                        Log.d("plugin out Exter", "MyRenderer.onDrawFrame, m_iTexture=" + m_iTexture);

                    }

                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
                    if ((iErr = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
                        Log.d("plugin out Exter", "MyRenderer.onDrawFrame: glClear, iErr=" + iErr);
                    }

                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, m_iTexture);
                    if ((iErr = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
                        Log.d("plugin out Exter", "MyRenderer.onDrawFrame: glBindTexture, iErr=" + iErr);
                    }

                    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
                    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
                    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
                    if ((iErr = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
                        Log.d("plugin out Exter", "MyRenderer.onDrawFrame: glTexParameteri, iErr=" + iErr);
                    }

                    GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, 256,
                            256, 0, GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE, m_byBuf);
                    if ((iErr = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
                        Log.d("plugin out Exter", "MyRenderer.onDrawFrame: glTexImage2D, iErr=" + iErr);
                    }

                    Log.d("plugin out Exter", "MyRenderer.onDrawFrame draw, datasize=" + m_byBuf.limit());
                }
                else {
                    GLES20.glClearColor(0.0f, 0.0f, 1.0f, 1.0f);
                    if ((iErr = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
                        Log.d("plugin out Exter", "MyRenderer.onDrawFrame: glClearColor, iErr=" + iErr);
                    }

                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

                    Log.d("plugin out Exter", "MyRenderer.onDrawFrame clear");
                }
            }
        }
        catch (Exception ex) {
            Log.d("plugin out Exter", "MyRenderer.onDrawFrame, ex=" + ex.toString());
        }
    }

    @Override
    public void onSurfaceChanged(javax.microedition.khronos.opengles.GL10 gl, int width, int height) {
        Log.d("plugin out Exter", "MyRenderer.onSurfaceChanged");

        int iErr;

        GLES20.glViewport(0, 0, width, height);
        if ((iErr = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.d("plugin out Exter", "MyRenderer.onSurfaceChanged: glViewport, iErr=" + iErr);
        }

        m_iWndWidth = width;
        m_iWndHeight = height;
    }

    @Override
    public void onSurfaceCreated(javax.microedition.khronos.opengles.GL10 arg0,
                                 javax.microedition.khronos.egl.EGLConfig arg1)
    {
        // TODO Auto-generated method stub
        Log.d("plugin out Exter", "MyRenderer.onSurfaceCreated");

        int iErr;

        GLES20.glEnable(GLES20.GL_TEXTURE_2D);
        if ((iErr = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.d("plugin out Exter", "MyRenderer.onSurfaceCreated: glEnable, iErr=" + iErr);
        }

        // Active the texture unit 0
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        if ((iErr = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.d("plugin out Exter", "MyRenderer.onSurfaceCreated: glActiveTexture, iErr=" + iErr);
        }
    }
}