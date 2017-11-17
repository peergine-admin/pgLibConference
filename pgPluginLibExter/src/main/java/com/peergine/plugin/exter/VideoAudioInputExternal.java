package com.peergine.plugin.exter;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
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
    Context context;
    public VideoAudioInputExternal(pgLibJNINode m_Node, LinearLayout m_View, Context context){
        this.m_Node= m_Node;
        this.m_View = m_View;
        this.context = context;
    }

    public void VideoInputExternalEnable(){
        pgDevVideoIn.SetCallback(m_oVideoInCB);
        m_CameraView = new CameraView(context);
        m_View.addView(m_CameraView);
        m_CameraView.setVisibility(View.GONE);
        Log.d("plugin Exter", "VideoStart: new camera view");

        m_CameraView.Initialize();
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
            //	if (iWidth != 320 || iHeight != 240) {
            //		return -1;
            //	}

            // The iDevID is '1234'.
            int iDevID = 1234;
            if (!m_CameraView.Start(iDevID, iDevNO, iWidth, iHeight, iFrmRate)) {
                return -1;
            }

            return iDevID;
        }

        @Override
        public void Close(int iDevID) {
            // TODO Auto-generated method stub

            m_CameraView.Stop();
            Log.d("plugin Exter", "pgDevVideoIn.OnCallback.Open: run close");
        }

        @Override
        public void Ctrl(int iDevID, int iCtrl, int iParam) {
            // TODO Auto-generated method stub

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
    private static final boolean HW_CODEC = false;

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

    public CameraView(Context ctx) {
        super(ctx);
    }

    public boolean Initialize() {
        try {
            Log.d("plugin Exter", "CameraView.Initialize");

            m_Handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    try {
                    }
                    catch (Exception ex) {
                        Log.d("plugin Exter", "handleMessage Exception");
                    }
                }
            };

            m_Holder = getHolder();
            m_Holder.addCallback(this);

            return true;
        }
        catch (Exception ex) {
            Log.d("plugin Exter", "CameraView.Start, ex=" + ex.toString());

            return false;
        }
    }


    public void Clean() {
        try {
            Log.d("plugin Exter", "CameraView.Clean");

            Stop();
        }
        catch (Exception ex) {
            Log.d("plugin Exter", "CameraView.Clean, ex=" + ex.toString());
        }
    }


    public boolean Start(int iDevID, int iCameraNo, int iW, int iH, int iFrmRate) {
        try {
            Log.d("plugin Exter", "CameraView.Start: iW=" + iW + ", iH=" + iH + ", iFrmRate=" + iFrmRate);

            m_iDevID = iDevID;
            m_iCameraNo = iCameraNo;
            m_iCameraWidth = iW;
            m_iCameraHeight = iH;
            m_iCameraFrmRate = iFrmRate;

            if (HW_CODEC) {
                m_MediaCodec = MediaCodec.createEncoderByType("video/avc");

                MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", m_iCameraWidth, m_iCameraHeight);
                mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 512000);
                mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, (1000 / iFrmRate));
                mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);
                mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 3);

                m_MediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                m_MediaCodec.start();
            }

            // Reset the open status.
            m_iCameraOpenStatus = -1;

            // Post the open camera handle to UI thread.
            m_Handler.post(new Runnable() {
                @Override
                public void run() {
                    Log.d("plugin Exter", "CameraView.Start, run PreviewOpen");
                    PreviewOpen();
                }
            });

            // wait and check the open result
            int i = 0;
            while (i < 20) {
                if (m_iCameraOpenStatus >= 0) {
                    break;
                }
                Thread.sleep(100);
                i++;
            }
            if (m_iCameraOpenStatus <= 0) {
                return false;
            }

            return true;
        }
        catch (Exception ex) {
            Log.d("plugin Exter", "CameraView.Start, ex=" + ex.toString());
            return false;
        }
    }

    public void Ctrl(int iDevID, int iCtrl, int iParam) {
        if (iDevID != m_iDevID) {
            return;
        }

        if (iCtrl == pgDevVideoIn.PG_DEV_VIDEO_IN_CTRL_PULL_KEY_FRAME) {

            // *** Important: Let's the encoder to output a key frame immidiately.
            // Add code here ...

            if (HW_CODEC) {

            }
        }
    }


    public void Stop() {
        try {
            Log.d("plugin Exter", "CameraView.Stop");

            // Reset the close status.
            m_iCameraCloseStatus = -1;

            // Post the close camera handle to UI thread.
            m_Handler.post(new Runnable() {
                @Override
                public void run() {
                    Log.d("plugin Exter", "CameraView.Stop, run PreviewClose");
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

            if (HW_CODEC) {
                if (m_MediaCodec != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        m_MediaCodec.stop();

                        m_MediaCodec.release();
                    }
                    m_MediaCodec = null;
                }
            }
        }
        catch (Exception ex) {
            Log.d("plugin Exter", "CameraView.Stop, ex=" + ex.toString());
        }
    }

    public void PreviewOpen() {
        try {
            Log.d("plugin Exter", "CameraView.PreviewOpen");

            getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

            if (getVisibility() != View.GONE) {
                setVisibility(View.GONE);
            }
            setVisibility(View.VISIBLE);
        }
        catch (Exception ex) {
            Log.d("plugin Exter", "CameraView.PreviewOpen, ex=" + ex.toString());
        }
    }

    public void PreviewClose() {
        try {
            Log.d("plugin Exter", "CameraView.PreviewClose");
            if (m_Camera != null) {
                if (getVisibility() != View.GONE) {
                    setVisibility(View.GONE);
                }
                getHolder().setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
                m_Camera = null;
            }
        }
        catch (Exception ex) {
            Log.d("plugin Exter", "CameraView.PreviewClose, ex=" + ex.toString());
        }
    }

    public void surfaceCreated(SurfaceHolder surfaceholder) {
        Log.d("plugin Exter", "CameraView.surfaceCreated");
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
                        Log.d("plugin Exter", "CameraView.surfaceChanged: Select iCameraNo=" + m_iCameraNo);
                        break;
                    }
                    if (iCameraIndFront < 0 && info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                        iCameraIndFront = iInd;
                    }
                }

                // If not 'iCameraNo' camera, select front camera.
                if (iCameraInd < 0 && iCameraIndFront >= 0) {
                    iCameraInd = iCameraIndFront;
                    Log.d("plugin Exter", "CameraView.surfaceChanged: Select front camera.");
                }

                // Try to open the selected camera.
                if (iCameraInd >= 0) {
                    cameraTemp = Camera.open(iCameraInd);
                }

                if (cameraTemp == null) {
                    cameraTemp = Camera.open(0);
                    if (cameraTemp == null) {
                        Log.d("plugin Exter", "CameraView.surfaceChanged, open camera failed.");
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
                Log.d("plugin Exter", "CameraView.surfaceChanged: Not match size: width=" + size.width + ", height=" + size.height);
            }
            if (!bHasSizeMode) {
                Log.d("plugin Exter", "CameraView.surfaceChanged: Not find valid size mode");
                m_iCameraOpenStatus = 0;
                return;
            }

            // Set preview size
            Param.setPreviewSize(m_iCameraWidth, m_iCameraHeight);

            // List all preview format.
            List<Integer> fmtList = Param.getSupportedPreviewFormats();
            for (int i = 0; i < fmtList.size(); i++) {
                Log.d("plugin Exter", "CameraView.surfaceChanged: Format=" + fmtList.get(i));
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
                }
                if (iFmtSel >= 0) {
                    break;
                }
            }
            if (iFmtSel < 0) {
                m_iCameraOpenStatus = 0;
                return;
            }

            Log.d("plugin Exter", "CameraView.surfaceChanged: Format=, iFmtSel=" + iFmtSel);
            Param.setPreviewFormat(iFmtSel);

            switch(iFmtSel) {
                case ImageFormat.NV16:
                    m_iCameraFormat = pgDevVideoIn.PG_DEV_VIDEO_IN_FMT_YUV422SP;
                    break;
                case ImageFormat.NV21:
                    m_iCameraFormat = pgDevVideoIn.PG_DEV_VIDEO_IN_FMT_YUV420SP;
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
                Log.d("plugin Exter", "CameraView.surfaceChanged: Rate=" + iRateTemp);
            }

            Log.d("plugin Exter", "CameraView.surfaceChanged: Param.setPreviewFrameRate, iRateSet=" + iRateSet);
            Param.setPreviewFrameRate(iRateSet);

            // Set rotate 90
            Param.set("rotation", 90);
            m_Camera.setDisplayOrientation(90);

            m_Camera.setParameters(Param);

            m_Camera.setPreviewCallback(this);
            m_Camera.setPreviewDisplay(m_Holder);

            m_Camera.startPreview();

            // Start success.
            m_iCameraOpenStatus = 1;

            Log.d("plugin Exter", "CameraView.surfaceChanged, startPreview.");
        }
        catch (Exception ex) {
            m_Camera.setPreviewCallback(null) ;
            m_Camera.stopPreview();
            m_Camera.release();
            m_Camera = null;
            m_iCameraOpenStatus = 0;
            Log.d("plugin Exter", "CameraView.surfaceChanged, ex=" + ex.toString());
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
                m_iCameraCloseStatus = 1;
            }
            catch(Exception ex) {
                m_Camera = null;
                Log.d("plugin Exter", "CameraView.surfaceDestroyed, ex=" + ex.toString());
                m_iCameraCloseStatus = 0;
            }
        }
        Log.d("plugin Exter", "CameraView.surfaceDestroyed");
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

        if (m_iDevID < 0) {
            return;
        }

        Log.d("plugin Exter", "CameraView.onPreviewFrame, begin. dataSize=" + data.length);

        if (HW_CODEC) {
            // Compress with h264.
            ByteBuffer[] inputBuffers = m_MediaCodec.getInputBuffers();
            ByteBuffer[] outputBuffers = m_MediaCodec.getOutputBuffers();

            int inpuIndex = m_MediaCodec.dequeueInputBuffer(-1);
            if (inpuIndex >= 0) {
                inputBuffers[inpuIndex].clear();
                inputBuffers[inpuIndex].put(data, 0, data.length);
                m_MediaCodec.queueInputBuffer(inpuIndex, 0, data.length, 0, 0);
            }

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputIndex = m_MediaCodec.dequeueOutputBuffer(bufferInfo, 0);
            while (outputIndex >= 0) {

                byte[] outData = new byte[bufferInfo.size];
                outputBuffers[outputIndex].get(outData);

                int iFlag = 0;
                if (outData[4] == 0x67) { // The key frame. It must has a SPS head.
                    iFlag |= pgDevVideoIn.PG_DEV_VIDEO_IN_FLAG_KEY_FRAME;
                }

                // Call peergine capture callback.
                pgDevVideoIn.CaptureProc(m_iDevID, outData, pgDevVideoIn.PG_DEV_VIDEO_IN_FMT_H264, iFlag);
                Log.d("plugin Exter", "CameraView.onPreviewFrame, dataSize=" + outData.length + ", iFlag=" + iFlag);

                m_MediaCodec.releaseOutputBuffer(outputIndex, false);
                outputIndex = m_MediaCodec.dequeueOutputBuffer(bufferInfo, 0);
            }
        }
        else {
            // Uncompress callback.
            pgDevVideoIn.CaptureProc(m_iDevID, data, m_iCameraFormat, 0);
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
