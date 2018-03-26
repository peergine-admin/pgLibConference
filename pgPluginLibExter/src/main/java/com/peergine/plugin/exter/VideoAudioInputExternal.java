package com.peergine.plugin.exter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.LinearLayout;

import com.peergine.plugin.android.pgDevAudioIn;
import com.peergine.plugin.android.pgDevVideoIn;
import com.peergine.plugin.lib.pgLibJNINode;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Created by ctkj on 2017/7/19.
 */

public class VideoAudioInputExternal {

    private CameraView m_CameraView;
    private ExterAudioIn m_AudioIn;
    pgLibJNINode m_Node;
    LinearLayout m_View;
    int iVideoMode= -1;
    Context context;
    public VideoAudioInputExternal(pgLibJNINode m_Node, LinearLayout m_View,int iVideoMode, Context context){
        this.m_Node= m_Node;
        this.m_View = m_View;
        this.iVideoMode = iVideoMode;
        this.context = context;
    }

    public void VideoInputExternalEnable(){
        pgDevVideoIn.SetCallback(m_oVideoInCB);
        m_CameraView = new CameraView(context);
        m_CameraView.SetCurrentVideoMode(iVideoMode);
        m_CameraView.Initialize(true, true);
        m_View.addView(m_CameraView);
        m_CameraView.setVisibility(View.GONE);
        Log.d("plugin Exter", "VideoStart: initialize capture");
    }
    public void VideoInputExternalDisable(){
        if(m_CameraView!=null){
            if(m_View!=null){
                m_View.removeAllViews();
            }
            m_CameraView.Clean();
            m_CameraView = null;
        }
        Log.d("plugin Exter", "VideoStart: initialize capture");
    }
    public void AudioInputExternalEnable(){
        pgDevAudioIn.SetCallback(m_oAudioInCB);
        SetAudioInExter(m_Node);
        Log.d("plugin Exter", "AudioStart: Set callback");

        m_AudioIn = new ExterAudioIn();
        Log.d("plugin Exter", "AudioStart: new exter audio in");
    }

    public pgDevVideoIn.OnCallback m_oVideoInCB = new pgDevVideoIn.OnCallback() {

        @Override
        public int Open(int iDevNO, int iPixBytes, int iWidth, int iHeight,
                        int iBitRate, int iFrmRate, int iKeyFrmRate)
        {
            // TODO Auto-generated method stub
            Log.d("pgLiveCapExter", "pgDevVideoIn.OnCallback.Open");

            // The iDevID is '1234'.
            int iDevID = 1234;
            if (!m_CameraView.Start(iDevID, iDevNO, iWidth, iHeight, iBitRate, iFrmRate, iKeyFrmRate)) {
                return -1;
            }

            return iDevID;
        }

        @Override
        public void Close(int iDevID) {
            // TODO Auto-generated method stub

            m_CameraView.Stop();
            Log.d("pgLiveCapExter", "pgDevVideoIn.OnCallback.Close");
        }

        @Override
        public void Ctrl(int iDevID, int iCtrl, int iParam) {
            // TODO Auto-generated method stub
            Log.d("pgLiveCapExter", "pgDevVideoIn.OnCallback.Ctrl");

            m_CameraView.Ctrl(iDevID, iCtrl, iParam);
        }
    };


    public pgDevAudioIn.OnCallback m_oAudioInCB = new pgDevAudioIn.OnCallback() {

        @Override
        public int Open(int iDevNO, int iSampleBits, int iSampleRate, int iChannels, int iPackBytes) {
            // TODO Auto-generated method stub

            // The iDevID is '1234'.
            m_AudioIn.Open(1234, iSampleBits, iSampleRate, iChannels, iPackBytes, iDevNO);
            return 1234;
        }

        @Override
        public void Close(int iDevID) {
            // TODO Auto-generated method stub

            m_AudioIn.Close();
            Log.d("plugin Exter", "pgDevAudioIn.OnCallback.Close");
        }
    };

    private void SetAudioInExter(pgLibJNINode Node) {
        if (Node != null) {
            if (Node.ObjectAdd("_aTemp", "PG_CLASS_Audio", "", 0)) {
                Node.ObjectRequest("_aTemp", 2, "(Item){4}(Value){1}", "");
                Node.ObjectDelete("_aTemp");
            }
        }
    }

}


class CameraView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback
{
    private static final int H264NAL_TYPE_NAL = 0;
    private static final int H264NAL_TYPE_SLICE = 1;
    private static final int H264NAL_TYPE_SLICE_DPA = 2;
    private static final int H264NAL_TYPE_SLICE_DPB = 3;
    private static final int H264NAL_TYPE_SLICE_DPC = 4;
    private static final int H264NAL_TYPE_SLICE_IDR = 5;
    private static final int H264NAL_TYPE_SEI = 6;
    private static final int H264NAL_TYPE_SPS = 7;
    private static final int H264NAL_TYPE_PPS = 8;

    private boolean m_bPostRunnable = true;
    private boolean m_bHardwareEncode = false;
    private boolean m_bCombineSPS = true;
    private boolean m_bInputEnable = true;
    private int m_iRotate = 0;

    private boolean m_bStarted = false;
    private boolean m_bStoped = false;

    private Handler m_Handler = null;

    private Camera m_Camera = null;
    private SurfaceHolder m_Holder;

    private int m_iCameraWidth = 0;
    private int m_iCameraHeight = 0;
    private int m_iCameraFrmRate = 0;

    private MediaCodec m_MediaCodec = null;
    private int m_iDevID = -1;
    private int m_iCameraNo = -1;
    private int m_iCameraFormat = -1;

    private int m_iCameraOpenStatus = -1;
    private int m_iCameraCloseStatus = -1;

    private int m_iHWEncodeFormat = -1;
    private int m_iFrmCount = 0;
    private boolean m_bKeyFrame = false;
    private byte[] m_byBufSPS = null;
    private byte[] m_byBufVideo = null;
    private byte[] m_byBufOutData = null;
    private byte[] m_byBufConvert = null;
    private MediaCodec.BufferInfo m_bufferInfo = null;

    private int m_iCurrentVideoMode = -1;
    private int[][] m_iModeList = {
            {80, 60},
            {160, 120},
            {320, 240},
            {640, 480},
            {800, 600},
            {1024, 768},
            {176, 144},
            {352, 288},
            {704, 576},
            {854, 480},
            {1280, 720},
            {1920, 1080}
    };

    public CameraView(Context ctx) {
        super(ctx);
    }

    public boolean Initialize(boolean bHardwareEncode, boolean bCombineSPS) {
        try {
            Log.d("DevExtend", "CameraView.Initialize");

            m_bHardwareEncode = bHardwareEncode;
            m_bCombineSPS = bCombineSPS;

            m_Handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    try {
                    }
                    catch (Exception ex) {
                        Log.d("CameraView", "handleMessage Exception");
                    }
                }
            };

            m_Holder = getHolder();
            m_Holder.addCallback(this);

            return true;
        }
        catch (Exception ex) {
            Log.d("DevExtend", "CameraView.Start, ex=" + ex.toString());

            return false;
        }
    }

    public void Clean() {
        try {
            Log.d("DevExtend", "CameraView.Clean");

            Stop();
        }
        catch (Exception ex) {
            Log.d("DevExtend", "CameraView.Clean, ex=" + ex.toString());
        }
    }

    public void SetPostRunnable(boolean bEnable) {
        m_bPostRunnable = bEnable;
    }

    public void SetInputEnable(boolean bEnable) {
        m_bInputEnable = bEnable;
    }

    public void SetCurrentVideoMode(int iMode) {
        if (iMode < m_iModeList.length) {
            m_iCurrentVideoMode = iMode;
            Log.d("DevExtend", "SetCurrentVideoMode, iMode=" + iMode);
        }
    }

    public void SetVideoModeSize(int iMode, int iWidth, int iHeight) {
        if (iMode < m_iModeList.length) {
            m_iModeList[iMode][0] = iWidth;
            m_iModeList[iMode][1] = iHeight;
            Log.d("DevExtend", "SetVideoModeSize, iMode=" + iMode + ", iWidth=" + iWidth + ", iHeight=" + iHeight);
        }
    }

    public void SetRotate(int iRotate) {
        m_iRotate = iRotate;
    }

    public boolean Start(int iDevID, int iCameraNo, int iW, int iH, int iBitRate, int iFrmRate, int iKeyFrmRate) {
        try {
            Log.d("DevExtend", "CameraView.Start: iW=" + iW + ", iH=" + iH + ", iBitRate="
                    + iBitRate + ", iFrmRate=" + iFrmRate + ", iKeyFrmRate=" + iKeyFrmRate);

            if (m_bStarted) {
                return true;
            }

            if (m_iCurrentVideoMode < 0) {
                return false;
            }

            if (iW != m_iModeList[m_iCurrentVideoMode][0] || iH != m_iModeList[m_iCurrentVideoMode][1]) {
                Log.d("DevExtend", "CameraView.Start: iW=" + iW + ", iH=" + iH + ", Reject!");
                return false;
            }

            m_iDevID = iDevID;
            m_iCameraNo = iCameraNo;
            m_iCameraWidth = iW;
            m_iCameraHeight = iH;
            m_iCameraFrmRate = iFrmRate;

            if (m_bHardwareEncode) {
                m_byBufVideo = new byte[(m_iCameraWidth * m_iCameraHeight * 3) / 2];

                m_bufferInfo = new MediaCodec.BufferInfo();


                MediaCodecInfo codecInfo = null;
                int numCodecs = 0;

                numCodecs = MediaCodecList.getCodecCount();

                for(int i = 0; i < numCodecs && codecInfo == null ; i++){
                    MediaCodecInfo info = null;

                    info = MediaCodecList.getCodecInfoAt(i);


                    if(!info.isEncoder()){
                        continue;
                    }


                    String[] types = info.getSupportedTypes();
                    for (int j = 0; j < types.length; j++) {
                        if (types[j].equals("video/avc")) {
                            Log.d("DevExtend", "CameraView.Start, Support H264 encode!");
                            codecInfo = info;
                            break;
                        }
                    }
                    if (codecInfo != null) {
                        break;
                    }
                }
                if (codecInfo == null) {
                    Log.d("DevExtend", "CameraView.Start, Not support H264 encode!");
                    return false;
                }

                int colorFormat = -1;
                MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType("video/avc");
                for(int i = 0; i < capabilities.colorFormats.length; i++){
                    colorFormat = capabilities.colorFormats[i];
                    Log.d("DevExtend", "CameraView.Start, support encode format=" + colorFormat);
                    if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
                            || colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar)
                    {
                        break;
                    }
                }
                if (colorFormat != MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
                        && colorFormat != MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar)
                {
                    Log.d("DevExtend", "CameraView.Start, Not support YUV420SP or YUV420P encode format! colorFormat=" + colorFormat);
                    return false;
                }

                // YUV420SP or YUV420P.
                m_iHWEncodeFormat = colorFormat;

                m_MediaCodec = MediaCodec.createEncoderByType("video/avc");
                MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", m_iCameraWidth, m_iCameraHeight);
                mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, (iBitRate * 1024));
                mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, (1000 / iFrmRate));
                mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, m_iHWEncodeFormat);
                mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2); //(iKeyFrmRate / 1000));

                m_MediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                m_MediaCodec.start();

                Log.d("DevExtend", "CameraView.Start, Init H264 hardware encode success.");
            }

            if (m_bPostRunnable) {
                // Reset the open status.
                m_iCameraOpenStatus = -1;

                // Post the open camera handle to UI thread.
                m_Handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("DevExtend", "CameraView.Start, run PreviewOpen");
                        PreviewOpen();
                    }
                });

                // wait and check the open result, timeout 10 seconds.
                int i = 0;
                while (i < 100) {
                    if (m_iCameraOpenStatus >= 0) {
                        break;
                    }
                    Thread.sleep(100);
                    i++;
                }
                if (m_iCameraOpenStatus <= 0) {
                    return false;
                }
            }
            else {
                PreviewOpen();
            }

            m_bStarted = true;
            m_bStoped = false;

            return true;
        }
        catch (Exception ex) {
            Log.d("DevExtend", "CameraView.Start, ex=" + ex.toString());
            m_bStarted = false;
            return false;
        }
    }

    public void Ctrl(int iDevID, int iCtrl, int iParam) {
        if (!m_bStarted) {
            return;
        }

        if (iDevID != m_iDevID) {
            return;
        }

        if (iCtrl == pgDevVideoIn.PG_DEV_VIDEO_IN_CTRL_PULL_KEY_FRAME) {

            // *** Important: Let's the encoder to output a key frame immidiately.
            if (m_bHardwareEncode) {
                m_bKeyFrame = true;
            }
        }
    }

    public void Stop() {
        if (!m_bStarted) {
            return;
        }

        try {
            Log.d("DevExtend", "CameraView.Stop");

            m_bStoped = true;
            if (m_bPostRunnable) {
                // Reset the close status.
                m_iCameraCloseStatus = -1;

                // Post the close camera handle to UI thread.
                m_Handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("DevExtend", "CameraView.Stop, run PreviewClose");
                        PreviewClose();
                    }
                });

                // wait and check the close result
                int i = 0;
                while (i < 5) {
                    if (m_iCameraCloseStatus >= 0) {
                        break;
                    }
                    Thread.sleep(100);
                    i++;
                }
            }
            else {
                PreviewClose();
            }

            if (m_bHardwareEncode) {
                if (m_MediaCodec != null) {
                    m_MediaCodec.flush();
                    m_MediaCodec.stop();
                    m_MediaCodec.release();
                    m_MediaCodec = null;
                }
            }

            m_byBufSPS = null;
            m_byBufVideo = null;
            m_byBufConvert = null;
            m_bufferInfo = null;

            m_iFrmCount = 0;
            m_bKeyFrame = false;

            m_bStarted = false;
        }
        catch (Exception ex) {
            Log.d("DevExtend", "CameraView.Stop, ex=" + ex.toString());
            m_bStarted = false;
        }
    }

    public void PreviewOpen() {
        try {
            Log.d("DevExtend", "CameraView.PreviewOpen");

            getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

            if (getVisibility() != View.GONE) {
                setVisibility(View.GONE);
            }
            setVisibility(View.VISIBLE);
        }
        catch (Exception ex) {
            Log.d("DevExtend", "CameraView.PreviewOpen, ex=" + ex.toString());
        }
    }

    public void PreviewClose() {
        try {
            Log.d("DevExtend", "CameraView.PreviewClose");
            if (m_Camera != null) {
                if (getVisibility() != View.GONE) {
                    setVisibility(View.GONE);
                }
                getHolder().setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
                m_Camera = null;
            }
        }
        catch (Exception ex) {
            Log.d("DevExtend", "CameraView.PreviewClose, ex=" + ex.toString());
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceholder) {
        Log.d("DevExtend", "CameraView.surfaceCreated");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // TODO Auto-generated method stub
        try {
            if (m_Camera == null) {

                int iCameraInd = -1;
                int iCameraIndFront = -1;
                Camera cameraTemp = null;

                for (int iInd = 0; iInd < Camera.getNumberOfCameras(); iInd++) {
                    Camera.CameraInfo info = new Camera.CameraInfo();
                    Camera.getCameraInfo(iInd, info);
                    if (info.facing == m_iCameraNo) {
                        iCameraInd = iInd;
                        Log.d("DevExtend", "CameraView.surfaceChanged: Select iCameraNo=" + m_iCameraNo);
                        break;
                    }
                    if (iCameraIndFront < 0 && info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                        iCameraIndFront = iInd;
                    }
                }

                // If not 'iCameraNo' camera, select front camera.
                if (iCameraInd < 0 && iCameraIndFront >= 0) {
                    iCameraInd = iCameraIndFront;
                    Log.d("DevExtend", "CameraView.surfaceChanged: Select front camera.");
                }

                // Try to open the selected camera.
                if (iCameraInd >= 0) {
                    cameraTemp = Camera.open(iCameraInd);
                }

                if (cameraTemp == null) {
                    cameraTemp = Camera.open(0);
                    if (cameraTemp == null) {
                        Log.d("DevExtend", "CameraView.surfaceChanged, open camera failed.");
                        return;
                    }
                }

                m_Camera = cameraTemp;
            }
            else {
                m_Camera.stopPreview();
            }

            // Check support size.
            boolean bHasSizeMode = false;
            Camera.Parameters Param = m_Camera.getParameters();
            List<Camera.Size> sizeList = Param.getSupportedPreviewSizes();
            for (int i = 0; i < sizeList.size(); i++) {
                Camera.Size size = sizeList.get(i);
                if (size.width == m_iCameraWidth && size.height == m_iCameraHeight) {
                    bHasSizeMode = true;
                    break;
                }
                Log.d("DevExtend", "CameraView.surfaceChanged: Not match size: width=" + size.width + ", height=" + size.height);
            }
            if (!bHasSizeMode) {
                Log.d("DevExtend", "CameraView.surfaceChanged: Not find valid size mode");
                m_iCameraOpenStatus = 0;
                return;
            }

            // Set preview size
            Param.setPreviewSize(m_iCameraWidth, m_iCameraHeight);

            // List all preview format.
            List<Integer> fmtList = Param.getSupportedPreviewFormats();
            for (int i = 0; i < fmtList.size(); i++) {
                Log.d("DevExtend", "CameraView.surfaceChanged: Format=" + fmtList.get(i));
            }

            // Select an support format.
            int iFmtSel = -1;
            int[] iFmtList = new int[]{ImageFormat.NV21,
                    ImageFormat.NV16, ImageFormat.YUY2, ImageFormat.YV12};
            for (int i = 0; i < iFmtList.length; i++) {
                for (int j = 0; j < fmtList.size(); j++) {
                    if (iFmtList[i] == fmtList.get(j)) {
                        iFmtSel = iFmtList[i];
                        break;
                    }
                    Log.d("DevExtend", "CameraView.surfaceChanged: Not match format: iFmtList=" + iFmtList[i]);
                }
                if (iFmtSel >= 0) {
                    break;
                }
            }
            if (iFmtSel < 0) {
                m_iCameraOpenStatus = 0;
                return;
            }

            Log.d("DevExtend", "CameraView.surfaceChanged: Format=, iFmtSel=" + iFmtSel);
            Param.setPreviewFormat(iFmtSel);

            switch(iFmtSel) {
                case ImageFormat.NV16:
                    m_iCameraFormat = pgDevVideoIn.PG_DEV_VIDEO_IN_FMT_YUV422SP;
                    break;
                case ImageFormat.NV21:
                    m_iCameraFormat = pgDevVideoIn.PG_DEV_VIDEO_IN_FMT_NV21;
                    break;
                case ImageFormat.YUY2:
                    m_iCameraFormat = pgDevVideoIn.PG_DEV_VIDEO_IN_FMT_YUYV;
                    break;
                case ImageFormat.YV12:
                    m_iCameraFormat = pgDevVideoIn.PG_DEV_VIDEO_IN_FMT_YV12;
                    break;
            }

            // Set preview frame rates.
            int iRateInput = (1000 / m_iCameraFrmRate);
            int iRateSet = iRateInput;

            int iDeltaMin = 65536;
            List<Integer> listRate = m_Camera.getParameters().getSupportedPreviewFrameRates();
            for (int i = 0; i < listRate.size(); i++) {
                int iRateTemp = listRate.get(i);
                int iDeltaTemp = iRateTemp - iRateInput;
                if (iDeltaTemp < 0) {
                    iDeltaTemp = -iDeltaTemp;
                }
                if (iDeltaTemp < iDeltaMin) {
                    iDeltaMin = iDeltaTemp;
                    iRateSet = iRateTemp;
                }
                Log.d("DevExtend", "CameraView.surfaceChanged: Rate=" + iRateTemp);
            }

            Log.d("DevExtend", "CameraView.surfaceChanged: Param.setPreviewFrameRate, iRateSet=" + iRateSet);
            Param.setPreviewFrameRate(iRateSet);

            // Set rotate
            if (m_iRotate != 0) {
                Param.set("rotation", (m_iRotate % 360));
                m_Camera.setDisplayOrientation((m_iRotate % 360));
            }

            m_Camera.setParameters(Param);

            int iTempSize = (m_iCameraWidth * m_iCameraHeight);
            iTempSize = (iTempSize * ImageFormat.getBitsPerPixel(iFmtSel)) / 8;
            m_byBufOutData = new byte[iTempSize];
            m_Camera.setPreviewCallbackWithBuffer(this);
            m_Camera.addCallbackBuffer(m_byBufOutData);

            m_Camera.setPreviewDisplay(m_Holder);
            m_Camera.startPreview();

            // Start success.
            m_iCameraOpenStatus = 1;

            Log.d("DevExtend", "CameraView.surfaceChanged, startPreview.");
        }
        catch (Exception ex) {
            m_Camera.setPreviewCallback(null) ;
            m_Camera.stopPreview();
            m_Camera.release();
            m_Camera = null;
            m_byBufOutData = null;
            m_iCameraOpenStatus = 0;
            Log.d("DevExtend", "CameraView.surfaceChanged, ex=" + ex.toString());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceholder) {
        if (m_Camera != null) {
            try {
                m_Camera.setPreviewCallback(null) ;
                m_Camera.stopPreview();
                m_Camera.release();
                m_Camera = null;
                m_byBufOutData = null;
                m_iCameraCloseStatus = 1;
            }
            catch(Exception ex) {
                m_Camera = null;
                Log.d("DevExtend", "CameraView.surfaceDestroyed, ex=" + ex.toString());
                m_iCameraCloseStatus = 0;
            }
        }
        Log.d("DevExtend", "CameraView.surfaceDestroyed");
    }

    private long GetPresentationTime() {
        long lTime = 128 + m_iFrmCount * m_iCameraFrmRate * 1000;
        m_iFrmCount++;
        return lTime;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        try {
            if (m_iDevID < 0) {
                m_Camera.addCallbackBuffer(m_byBufOutData);
                return;
            }

            Log.d("DevExtend", "CameraView.onPreviewFrame, begin. dataSize=" + data.length);

            if (!m_bInputEnable || m_bStoped) {
                m_Camera.addCallbackBuffer(m_byBufOutData);
                return;
            }

            if (m_bHardwareEncode) {
                HardwareEncode(data);
            }
            else {
                // Uncompress callback.
                pgDevVideoIn.CaptureProcExt(m_iDevID, data, 0, data.length, m_iCameraFormat, 0);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        m_Camera.addCallbackBuffer(m_byBufOutData);
    }

    private void HardwareEncode(byte[] data) {

        //long lStamp1 = (new Date()).getTime();

        if (m_iHWEncodeFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
            // Convert Pixel format to YUV420SP.
            switch (m_iCameraFormat) {
                case pgDevVideoIn.PG_DEV_VIDEO_IN_FMT_YUV422SP:
                    NV16ToYUV420SP(data, m_iCameraWidth, m_iCameraHeight);
                    break;

                case pgDevVideoIn.PG_DEV_VIDEO_IN_FMT_NV21:
                    NV21ToYUV420SP(data, m_iCameraWidth, m_iCameraHeight);
                    break;

                case pgDevVideoIn.PG_DEV_VIDEO_IN_FMT_YUYV:
                    YUYVToYUV420SP(data, m_iCameraWidth, m_iCameraHeight);
                    break;

                case pgDevVideoIn.PG_DEV_VIDEO_IN_FMT_YV12:
                    YV12ToYUV420SP(data, m_iCameraWidth, m_iCameraHeight);
                    break;

                default:
                    Log.d("DevExtend", "CameraView.HardwareEncode, Invalid pixel format!");
                    return;
            }
        }
        else if (m_iHWEncodeFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar) {
            // Convert Pixel format to YUV420P.
            switch (m_iCameraFormat) {
                case pgDevVideoIn.PG_DEV_VIDEO_IN_FMT_YUV422SP:
                    NV16ToYUV420P(data, m_iCameraWidth, m_iCameraHeight);
                    break;

                case pgDevVideoIn.PG_DEV_VIDEO_IN_FMT_NV21:
                    NV21ToYUV420P(data, m_iCameraWidth, m_iCameraHeight);
                    break;

                case pgDevVideoIn.PG_DEV_VIDEO_IN_FMT_YUYV:
                    YUYVToYUV420P(data, m_iCameraWidth, m_iCameraHeight);
                    break;

                case pgDevVideoIn.PG_DEV_VIDEO_IN_FMT_YV12:
                    YV12ToYUV420P(data, m_iCameraWidth, m_iCameraHeight);
                    break;

                default:
                    Log.d("DevExtend", "CameraView.HardwareEncode, Invalid pixel format!");
                    return;
            }
        }
        else {
            Log.d("DevExtend", "CameraView.HardwareEncode, Not suppert YUV420SP or YUV420P encode format! m_iHWEncodeFormat=" + m_iHWEncodeFormat);
            return;
        }

        //long lStamp2 = (new Date()).getTime();

        // Compress with h264.
        ByteBuffer[] inputBuffers = m_MediaCodec.getInputBuffers();
        ByteBuffer[] outputBuffers = m_MediaCodec.getOutputBuffers();

        int inpuIndex = m_MediaCodec.dequeueInputBuffer(-1);
        if (inpuIndex >= 0) {
            inputBuffers[inpuIndex].clear();
            inputBuffers[inpuIndex].put(data, 0, data.length);

            int iQueFlag = 0;
            if (m_bKeyFrame) {
                m_bKeyFrame = false;

                // Generate a key frame.
                iQueFlag |= MediaCodec.BUFFER_FLAG_SYNC_FRAME;
            }

            m_MediaCodec.queueInputBuffer(inpuIndex, 0,
                    data.length, GetPresentationTime(), iQueFlag);
        }

        boolean bPushToSDK = false;
        int outputIndex = m_MediaCodec.dequeueOutputBuffer(m_bufferInfo, 20000);
        while (outputIndex >= 0) {

            int iOutDataSize = m_bufferInfo.size;

            if (m_bCombineSPS) {
                if (m_byBufSPS != null) {
                    if ((outputBuffers[outputIndex].get(4) & 0x1F) == H264NAL_TYPE_SLICE_IDR) {
                        // Combine SPS head to frame data.
                        System.arraycopy(m_byBufSPS, 0, m_byBufVideo, 0, m_byBufSPS.length);
                        outputBuffers[outputIndex].get(m_byBufVideo, m_byBufSPS.length, iOutDataSize);
                        int iSize = m_byBufSPS.length + iOutDataSize;

                        int iFlag = pgDevVideoIn.PG_DEV_VIDEO_IN_FLAG_KEY_FRAME;
                        pgDevVideoIn.CaptureProcExt(m_iDevID, m_byBufVideo, 0, iSize, pgDevVideoIn.PG_DEV_VIDEO_IN_FMT_H264, iFlag);
                        bPushToSDK = true;
                    }
                    else {
                        outputBuffers[outputIndex].get(m_byBufVideo, 0, iOutDataSize);
                        pgDevVideoIn.CaptureProcExt(m_iDevID, m_byBufVideo, 0, iOutDataSize, pgDevVideoIn.PG_DEV_VIDEO_IN_FMT_H264, 0);
                        bPushToSDK = true;
                    }
                }
                else {
                    // Backup SPS head.
                    if ((outputBuffers[outputIndex].get(4) & 0x1F) == H264NAL_TYPE_SPS
                            && outputBuffers[outputIndex].get(0) == 0x0
                            && outputBuffers[outputIndex].get(1) == 0x0
                            && outputBuffers[outputIndex].get(2) == 0x0
                            && outputBuffers[outputIndex].get(3) == 0x1)
                    {
                        Log.d("DevExtend", "CameraView.HardwareEncode: Backup SPS head");
                        m_byBufSPS = new byte[iOutDataSize];
                        outputBuffers[outputIndex].get(m_byBufSPS, 0, iOutDataSize);
                        dumpbuffer(m_byBufSPS);
                    }
                    else {
                        Log.d("DevExtend", "CameraView.HardwareEncode: There is no SPS head");
                    }
                }

                m_MediaCodec.releaseOutputBuffer(outputIndex, false);
                outputIndex = m_MediaCodec.dequeueOutputBuffer(m_bufferInfo, 0);

            }
            else {
                int iFlag = 0;
                if (outputBuffers[outputIndex].get(4) == 0x67) { // The key frame. It must has a SPS head.
                    iFlag |= pgDevVideoIn.PG_DEV_VIDEO_IN_FLAG_KEY_FRAME;
                }

                // Call peergine capture callback.
                outputBuffers[outputIndex].get(m_byBufVideo);
                pgDevVideoIn.CaptureProcExt(m_iDevID, m_byBufVideo, 0, iOutDataSize, pgDevVideoIn.PG_DEV_VIDEO_IN_FMT_H264, iFlag);
                bPushToSDK = true;

                Log.d("DevExtend", "CameraView.HardwareEncode, dataSize=" + iOutDataSize + ", iFlag=" + iFlag);

                m_MediaCodec.releaseOutputBuffer(outputIndex, false);
                outputIndex = m_MediaCodec.dequeueOutputBuffer(m_bufferInfo, 0);
            }
        }

        //long lStamp3 = (new Date()).getTime();
        //Log.d("DevExtend", "CameraView.HardwareEncode, Delta1=" + (lStamp2 - lStamp1) + ", Delta2=" + (lStamp3 - lStamp2));

        if (!bPushToSDK) {
            Log.d("DevExtend", "CameraView.HardwareEncode, Not push to SDK.");
        }
    }

    private void NV21ToYUV420SP(byte[] byData, int width, int height) {
        int iPixlSize = width * height;
        int iUVSize = iPixlSize + iPixlSize / 2;
        for (int iPos = iPixlSize; iPos < iUVSize; iPos += 2) {
            byte byTemp = byData[iPos];
            byData[iPos] = byData[iPos + 1];
            byData[iPos + 1] = byTemp;
        }
    }

    private void NV16ToYUV420SP(byte[] byData, int width, int height) {
        int iPixlSize = width * height;
        int iUVSize = iPixlSize + iPixlSize;
        for (int iPos = iPixlSize, iPos1 = iPixlSize; iPos < iUVSize; iPos += 4) {
            byData[iPos1] = byData[iPos];
            byData[iPos1 + 1] = byData[iPos + 1];
            iPos1 += 2;
        }
    }

    private void YUYVToYUV420SP(byte[] byData, int width, int height) {
        int iPixlSize = width * height;
        int iFrameSize = iPixlSize * 2;
        if (m_byBufConvert == null || m_byBufConvert.length < iFrameSize) {
            m_byBufConvert = new byte[iFrameSize];
        }

        System.arraycopy(byData, 0, m_byBufConvert, 0, iFrameSize);

        int iYPos = 0;
        int iUVPos = iFrameSize;
        for (int iPos = 0; iPos < iFrameSize; iPos += 2) {
            byData[iYPos] = m_byBufConvert[iPos];
            if ((iPos % 4) == 0) {
                byData[iUVPos] = m_byBufConvert[iPos + 1];
                iUVPos++;
            }
            iYPos++;
        }
    }

    private void YV12ToYUV420P(byte[] byData, int width, int height) {
        int iPixlSize = width * height;
        int iCopySize = iPixlSize / 2;
        if (m_byBufConvert == null || m_byBufConvert.length < iCopySize) {
            m_byBufConvert = new byte[iCopySize];
        }

        System.arraycopy(byData, iPixlSize, m_byBufConvert, 0, iCopySize);

        int iUVSize = iPixlSize / 4;
        int iUPos = 0;
        int iVPos = iUVSize;

        System.arraycopy(m_byBufConvert, iUPos, byData, (iPixlSize + iUVSize), iUVSize);
        System.arraycopy(m_byBufConvert, iVPos, byData, iPixlSize, iUVSize);
    }

    private void NV21ToYUV420P(byte[] byData, int width, int height) {
        int iPixlSize = width * height;
        int iCopySize = iPixlSize / 2;
        if (m_byBufConvert == null || m_byBufConvert.length < iCopySize) {
            m_byBufConvert = new byte[iCopySize];
        }

        System.arraycopy(byData, iPixlSize, m_byBufConvert, 0, iCopySize);

        int iUVSize = iPixlSize / 4;
        int iUPos = iPixlSize;
        int iVPos = iPixlSize + iUVSize;

        for (int iPos = 0; iPos < iCopySize; iPos += 2) {
            byData[iUPos++] = m_byBufConvert[iPos + 1];
            byData[iVPos++] = m_byBufConvert[iPos];
        }
    }

    private void NV16ToYUV420P(byte[] byData, int width, int height) {
        int iPixlSize = width * height;
        if (m_byBufConvert == null || m_byBufConvert.length < iPixlSize) {
            m_byBufConvert = new byte[iPixlSize];
        }

        System.arraycopy(byData, iPixlSize, m_byBufConvert, 0, iPixlSize);

        int iUVSize = iPixlSize / 4;
        int iUPos = iPixlSize;
        int iVPos = iPixlSize + iUVSize;

        for (int iPos = 0; iPos < iPixlSize; iPos += 4) {
            byData[iUPos++] = m_byBufConvert[iPos];
            byData[iVPos++] = m_byBufConvert[iPos + 1];
        }
    }

    private void YUYVToYUV420P(byte[] byData, int width, int height) {
        int iPixlSize = width * height;
        int iFrameSize = iPixlSize * 2;
        if (m_byBufConvert == null || m_byBufConvert.length < iFrameSize) {
            m_byBufConvert = new byte[iFrameSize];
        }

        System.arraycopy(byData, 0, m_byBufConvert, 0, iFrameSize);

        int iYPos = 0;
        int iUVPos = iFrameSize;
        for (int iPos = 0; iPos < iFrameSize; iPos += 2) {
            byData[iYPos] = m_byBufConvert[iPos];
            if ((iPos % 4) == 0) {
                byData[iUVPos] = m_byBufConvert[iPos + 1];
                iUVPos++;
            }
            iYPos++;
        }
    }

    private void YV12ToYUV420SP(byte[] byData, int width, int height) {
        int iPixlSize = width * height;
        int iCopySize = iPixlSize / 2;
        if (m_byBufConvert == null || m_byBufConvert.length < iCopySize) {
            m_byBufConvert = new byte[iCopySize];
        }

        System.arraycopy(byData, iPixlSize, m_byBufConvert, 0, iCopySize);

        int iUPos = 0;
        int iVPos = iPixlSize / 4;
        int iFrameSize = iPixlSize + iCopySize;
        for (int iPos = iPixlSize; iPos < iFrameSize; iPos += 2) {
            byData[iPos] = m_byBufConvert[iUPos];
            byData[iPos + 1] = m_byBufConvert[iVPos];
            iUPos++;
            iVPos++;
        }
    }

    public void dumpbuffer(byte[] byData) {
        int iInd = 0;
        while (iInd < byData.length) {
            String sLine = "";
            for (int iInd1 = 0; (iInd1 < 16 && iInd < byData.length); iInd1++, iInd++) {
                sLine += String.format("%02X ", byData[iInd]);
            }
            Log.d("DevExtend", "CameraView.dumpbuffer: " + sLine);
        }
    }
}


class ExterAudioIn {

    private int m_iDevID = 0;
    private AudioRecord m_Recorder = null;
    private byte[] m_byteData = null;
    private int m_iDataSize = 0;
    private boolean m_bPoll = false;

    public ExterAudioIn() {
    }

    public boolean Open(int iDevID, int uSampleBits, int uSampleRate, int uChannels, int uPackBytes, int iMicNo) {
        try {
            Log.d("plugin Exter", "ExterAudioIn.Open, uSampleBits=" + uSampleBits
                    + ", uSampleRate=" + uSampleRate + ", uPackBytes=" + uPackBytes);

            m_iDevID = iDevID;

            m_byteData = new byte[uPackBytes];
            m_iDataSize = 0;

            int iSampleFmt = (uSampleBits == 16) ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT;
            int iMinBufSize = AudioRecord.getMinBufferSize(uSampleRate, AudioFormat.CHANNEL_IN_MONO, iSampleFmt);
            if (iMinBufSize <= 0) {
                Log.d("plugin Exter", "ExterAudioIn.Open, failed, get min buffer sise");
                return false;
            }

            int iBufSize = uPackBytes * 12;
            if (iBufSize < iMinBufSize) {
                iBufSize = (iMinBufSize / uPackBytes) * uPackBytes + uPackBytes;
            }

            int iMicNoTemp = MediaRecorder.AudioSource.MIC;
            if (iMicNo != 0) {
                iMicNoTemp = iMicNo;
            }

            m_Recorder = new AudioRecord(iMicNoTemp, uSampleRate,
                    AudioFormat.CHANNEL_IN_MONO, iSampleFmt, iBufSize);
            if (m_Recorder.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.d("plugin Exter", "ExterAudioIn.Open, failed, not inited");
                return false;
            }

            m_Recorder.startRecording();

            m_bPoll = true;
            m_thread = new AudioReadThread();
            m_thread.start();

            Log.d("plugin Exter", "ExterAudioIn Open ok");
            return true;
        }
        catch (Exception ex) {
            Log.d("plugin Exter", "ExterAudioIn.Open Exception, ex=" + ex.toString());
            m_bPoll = false;
            return false;
        }
    }

    public void Close() {
        try {
            Log.d("plugin Exter", "ExterAudioIn.Close");

            m_bPoll = false;
            if (m_thread != null) {
                m_thread.join(500);
            }

            if (m_Recorder != null) {
                m_Recorder.setRecordPositionUpdateListener(null);
                m_Recorder.stop();
                m_Recorder.release();
                m_Recorder = null;
            }
        }
        catch (Exception ex) {
            Log.d("plugin Exter", "ExterAudioIn.Close Exception, ex=" + ex.toString());
        }
    }

    public int ReadData() {
        try {
            int iRead = m_Recorder.read(m_byteData, 0, m_byteData.length);
            if (iRead > 0) {
                pgDevAudioIn.RecordProc(m_iDevID, m_byteData,
                        pgDevAudioIn.PG_DEV_AUDIO_IN_FMT_PCM16, 0);
            }

            return iRead;
        }
        catch (Exception ex) {
            Log.d("plugin Exter", "ExterAudioIn.ReadData Exception, ex=" + ex.toString());
            return -1;
        }
    }

    Thread m_thread = null;

    class AudioReadThread extends Thread {
        @Override
        public void run() {
            ExterAudioIn.this.ThreadProc();
        }
    }

    public void ThreadProc() {
        try {
            android.os.Process.setThreadPriority(
                    android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            while (true) {
                ReadData();
                if (!m_bPoll) {
                    break;
                }
                Thread.sleep(10);
            }

            Log.d("plugin Exter", "ExterAudioIn.ThreadProc exit");
        }
        catch (Exception ex) {
            Log.d("plugin Exter", "ExterAudioIn.ThreadProc Exception, ex=" + ex.toString());
        }
    }
}
