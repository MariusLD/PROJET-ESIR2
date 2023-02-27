package com.example.iristick.camera;

import android.view.Surface;
import android.widget.TextView;

import com.iristick.smartglass.core.camera.CaptureFailure;
import com.iristick.smartglass.core.camera.CaptureListener;
import com.iristick.smartglass.core.camera.CaptureRequest;
import com.iristick.smartglass.core.camera.CaptureResult;
import com.iristick.smartglass.core.camera.CaptureSession;

class MyCaptureListener implements CaptureListener {

    TextView mInfo;
    public MyCaptureListener(TextView mInfo_){
        mInfo = mInfo_;

    }


    @Override
    public void onCaptureStarted(CaptureSession session, CaptureRequest request,
                                 long timestamp, long frameNumber) {
        /* This is called when the camera device starts processing a capture
         * request. This is the appropriate time to play a shutter sound
         * effect or trigger UI indicators. */
    }

    @Override
    public void onCaptureBufferLost(CaptureSession session, CaptureRequest request,
                                    Surface surface, long frameNumber) {
        /* This is called when a buffer for capture data could not be sent to
         * its destination surface. */
    }

    @Override
    public void onCaptureCompleted(CaptureSession session, CaptureRequest request,
                                   CaptureResult result) {
        /* This is called when an image capture has completed successfully. */
        mInfo.setText("Frame number " + (int) result.getFrameNumber()) ;


    }

    @Override
    public void onCaptureFailed(CaptureSession session, CaptureRequest request,
                                CaptureFailure failure) {
        /* This is called when a capture failed. */
    }

    @Override
    public void onCaptureSequenceCompleted(CaptureSession session, int sequenceId,
                                           long frameNumber) {
        /* This is called when all captures of a capture sequence are
         * finished, i.e., onCaptureCompleted or onCaptureFailed has been
         * called for all capture requests in the sequence. */
    }

    @Override
    public void onCaptureSequenceAborted(CaptureSession session, int sequenceId) {
        /* This is called when a capture sequence aborts before any
         * CaptureResult or CaptureFailure for it have been returned
         * via this listener. */
    }
}