package com.peergine.util;

import android.content.Context;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.File;

/**
 * Copyright (C) 2014-2017, Peergine, All rights reserved.
 * www.peergine.com, www.pptun.com
 * com.peergine.util
 *
 * @author ctkj
 * @date 2018/3/12.
 */

public final class Checker {


    public static boolean CameraCheck(Context context){
        try {
            return CameraTest(context);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return false;
        }
    }
    private static boolean CameraTest(Context context) throws Throwable {
        SurfaceView surfaceView = new SurfaceView(context);
        SurfaceHolder mHolder = surfaceView.getHolder();
        mHolder.addCallback(CALLBACK);
        Camera camera = null;
        try {
            camera = Camera.open();
            Camera.Parameters parameters = camera.getParameters();
            camera.setParameters(parameters);
            camera.setPreviewDisplay(mHolder);
            camera.setPreviewCallback(PREVIEW_CALLBACK);
            camera.startPreview();
            return true;
        } finally {
            if (camera != null) {
                camera.stopPreview();
                camera.setPreviewDisplay(null);
                camera.setPreviewCallback(null);
                camera.release();
            }
        }
    }

    private static final Camera.PreviewCallback PREVIEW_CALLBACK = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
        }
    };

    private static final SurfaceHolder.Callback CALLBACK = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
        }
    };
    public static boolean RecordAudioCheck(){
        try {
            return RecordAudioTest();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return false;
        }
    }
    private static boolean RecordAudioTest() throws Throwable {
        File mTempFile = null;
         MediaRecorder mRecorder = new MediaRecorder();
        try {
            mTempFile = File.createTempFile("permission", "test");

            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mRecorder.setOutputFile(mTempFile.getAbsolutePath());
            mRecorder.prepare();
            mRecorder.start();
            return true;
        } finally {
            if (mRecorder != null) {
                try {
                    mRecorder.stop();
                } catch (Exception ignored) {
                }
                try {
                    mRecorder.release();
                } catch (Exception ignored) {
                }
            }

            if (mTempFile != null && mTempFile.exists()) {
                // noinspection ResultOfMethodCallIgnored
                mTempFile.delete();
            }
        }
    }
}
