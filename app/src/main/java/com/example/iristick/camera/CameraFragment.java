package com.example.iristick.camera;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.iristick.R;
import com.iristick.smartglass.core.Headset;
import com.iristick.smartglass.core.camera.CameraCharacteristics;
import com.iristick.smartglass.core.camera.CameraDevice;
import com.iristick.smartglass.core.camera.CaptureRequest;
import com.iristick.smartglass.core.camera.CaptureSession;
import com.iristick.smartglass.support.app.IristickApp;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment handling one camera.
 * The camera index must be specified with the {@code auto:camera_index} XML attribute
 * in the layout.
 */
public class CameraFragment extends Fragment {

    private int mCameraIndex;
    private CameraPreview mPreview;
    private TextView mInfo;

    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;
    private CameraDevice mCamera;
    private CaptureSession mCaptureSession;

    /* Camera characteristics */
    private boolean mHasAutoFocus;
    private boolean mHasLaserAssistedAF;
    private float mMaxZoom;
    private Point mMaxOffset;

    /* Current settings */
    private Point mFrameSize;
    private float mZoom = 1.0f;
    private Point mOffset = new Point(0, 0);

    @Override
    public void onInflate(Context context, AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(context, attrs, savedInstanceState);
        /* Retrieve the camera index to open from the XML attributes. */
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CameraFragment);
        mCameraIndex = a.getInt(R.styleable.CameraFragment_camera_index, 0);
        a.recycle();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.camera_fragment, container, false);

        mPreview = view.findViewById(R.id.preview);
        mPreview.setSurfaceTextureListener(mSurfaceTextureListener);
        mPreview.setOnGestureListener(new CameraPreview.OnGestureListener() {
            @Override
            public void onZoom(float factor) {
                zoom(factor);
            }

            @Override
            public void onPan(int dx, int dy) {
                move(dx, dy);
            }
        });
        mPreview.setOnClickListener(v -> triggerAF());

        mInfo = view.findViewById(R.id.info);
        mInfo.setOnClickListener(v -> resetSettings());

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        /* Query the connected headset. */
        Headset headset = IristickApp.getHeadset();
        if (headset == null) {
            mInfo.setText(R.string.camera_waiting_for_headset);
            return;
        }

        /* Find camera. */
        String[] cameras = headset.getCameraIdList();
        if (mCameraIndex >= cameras.length ||
                getActivity().checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            mInfo.setText(R.string.camera_not_available);
            return;
        }
        String cameraId = cameras[mCameraIndex];
        CameraCharacteristics characteristics = headset.getCameraCharacteristics(cameraId);

        /* Find out smallest frame size. */
        CameraCharacteristics.StreamConfigurationMap streams = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Point[] sizes = streams.getSizes();
        mFrameSize = sizes[sizes.length - 1];

        /* Check whether this camera has auto focus control. */
        mHasAutoFocus = false;
        mHasLaserAssistedAF = false;
        if (characteristics.containsKey(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)) {
            int[] afModes = characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
            for (int afMode : afModes) {
                switch (afMode) {
                case CaptureRequest.CONTROL_AF_MODE_AUTO:
                    mHasAutoFocus = true;
                    break;
                case CaptureRequest.CONTROL_AF_MODE_LASER_ASSISTED:
                    mHasLaserAssistedAF = true;
                    break;
                }
            }
        }

        /* Get the maximum digital zoom level. */
        if (characteristics.containsKey(CameraCharacteristics.SCALER_MAX_ZOOM))
            mMaxZoom = characteristics.get(CameraCharacteristics.SCALER_MAX_ZOOM);
        else
            mMaxZoom = 1.0f;

        /* Get the maximum frame offset. */
        if (characteristics.containsKey(CameraCharacteristics.SCALER_MAX_OFFSET))
            mMaxOffset = characteristics.get(CameraCharacteristics.SCALER_MAX_OFFSET);
        else
            mMaxOffset = new Point(0, 0);

        /* Open the camera. */
        headset.openCamera(cameraId, mCameraListener, null);
    }

    @Override
    public void onStop() {
        /* Close the camera as soon as possible. */
        if (mCamera != null) {
            mCamera.close();
            mCaptureSession = null;
            mCamera = null;
        }
        super.onStop();
    }

    /**
     * Set up the TextureView transform matrix to preserve the image aspect ratio.
     * Do nothing if the frame size is unknown.
     */
    private void setupTransform(TextureView view) {
        if (mFrameSize == null)
            return;
        float disp_ratio = (float) view.getWidth() / (float) view.getHeight();
        float frame_ratio = (float) mFrameSize.x / (float) mFrameSize.y;
        Matrix transform = new Matrix();
        if (disp_ratio > frame_ratio)
            transform.setScale(frame_ratio/disp_ratio, 1.0f, view.getWidth()/2.0f, view.getHeight()/2.0f);
        else
            transform.setScale(1.0f, disp_ratio/frame_ratio, view.getWidth()/2.0f, view.getHeight()/2.0f);
        view.setTransform(transform);
    }

    /**
     * Try to create a Capture Session, if both the Iristick camera and
     * the texture surface are ready.
     */
    private void createCaptureSession() {
        if (mCamera == null || mSurface == null)
            return;

        /* Set the desired camera resolution. */
        mSurfaceTexture.setDefaultBufferSize(mFrameSize.x, mFrameSize.y);
        setupTransform(mPreview);

        /* Create the capture session. */
        mCaptureSession = null;
        List<Surface> outputs = new ArrayList<>();
        outputs.add(mSurface);
        mCamera.createCaptureSession(outputs, mCaptureSessionListener, null);
    }

    /**
     * Create a capture request with all current settings applied.
     * @param triggerAF True if the request should trigger auto focus (see {@link #triggerAF()}).
     */
    private CaptureRequest createCaptureRequest(boolean triggerAF) {
        /*
         * Create a builder, specifying the intended use through the template.
         * This sets some sane defaults for our use case.
         */
        CaptureRequest.Builder builder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

        /* Add target output. */
        builder.addTarget(mSurface);

        /* Set parameters. */
        builder.set(CaptureRequest.SCALER_ZOOM, mZoom);
        builder.set(CaptureRequest.SCALER_OFFSET, mOffset);
        if (mHasAutoFocus) {
            builder.set(CaptureRequest.CONTROL_AF_MODE,
                    mHasLaserAssistedAF ? CaptureRequest.CONTROL_AF_MODE_LASER_ASSISTED
                                        : CaptureRequest.CONTROL_AF_MODE_AUTO);
            if (triggerAF)
                builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
        }

        /* Build the capture request. */
        return builder.build();
    }

    /**
     * Start the stream by setting a repeating capture request.
     * Do nothing if the capture session is not configured.
     */
    private void setCapture() {
        if (mCaptureSession == null || mSurface == null)
            return;
        mCaptureSession.setRepeatingRequest(createCaptureRequest(false), null, null);

        /* Update info text. */
        StringBuilder str = new StringBuilder();
        if (mZoom > 1.0f) {
            str.append(getString(R.string.camera_info_zoom, (int) mZoom));
        }
        if (mOffset.x != 0 || mOffset.y != 0) {
            if (str.length() > 0)
                str.append("\n");
            str.append(getString(R.string.camera_info_offset, mOffset.x, mOffset.y));
        }
        mInfo.setText(str.toString());
    }

    /**
     * Trigger auto-focus.
     * Do nothing if the capture session is not configured or the camera does not support
     * auto-focus.
     */
    public void triggerAF() {
        if (mCaptureSession == null || mSurface == null || !mHasAutoFocus)
            return;
        /*
         * Note: CONTROL_AF_TRIGGER_START should only be specified for one frame.  Hence, the use
         * of capture() here.
         */
        mCaptureSession.capture(createCaptureRequest(true), null, null);
    }

    /** Reset capture settings */
    public void resetSettings() {
        if (mCaptureSession == null)
            return;
        mZoom = 1.0f;
        mOffset.set(0, 0);
        setCapture();
    }

    /** Adjust zoom factor */
    public void zoom(float factor) {
        if (mCaptureSession == null)
            return;
        mZoom *= factor;
        mZoom = Math.max(1.0f, Math.min(mZoom, mMaxZoom));
        setCapture();
    }

    /** Move the image offset */
    public void move(int dx, int dy) {
        if (mCaptureSession == null)
            return;
        mOffset.x += dx;
        mOffset.y += dy;
        mOffset.x = Math.max(-mMaxOffset.x, Math.min(mOffset.x, mMaxOffset.x));
        mOffset.y = Math.max(-mMaxOffset.y, Math.min(mOffset.y, mMaxOffset.y));
        setCapture();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Camera listeners implementations

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            mSurfaceTexture = surface;
            mSurface = new Surface(mSurfaceTexture);
            createCaptureSession();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            setupTransform(mPreview);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            if (mCaptureSession != null) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            mSurface.release();
            mSurface = null;
            mSurfaceTexture = null;
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    private final CameraDevice.Listener mCameraListener = new CameraDevice.Listener() {
        @Override
        public void onOpened(CameraDevice device) {
            mCamera = device;
            createCaptureSession();
        }

        @Override
        public void onClosed(CameraDevice device) {
            mCamera = null;
        }

        @Override
        public void onDisconnected(CameraDevice device) {
        }

        @Override
        public void onError(CameraDevice device, int error) {
            mInfo.setText(getString(R.string.camera_error, error));
        }
    };

    private final CaptureSession.Listener mCaptureSessionListener = new CaptureSession.Listener() {
        @Override
        public void onConfigured(CaptureSession session) {
            mCaptureSession = session;
            setCapture();
        }

        @Override
        public void onConfigureFailed(CaptureSession session, int error) {
            mInfo.setText(R.string.camera_error_configure);
        }

        @Override
        public void onClosed(CaptureSession session) {
            if (mCaptureSession == session)
                mCaptureSession = null;
        }

        @Override
        public void onActive(CaptureSession session) {
        }

        @Override
        public void onCaptureQueueEmpty(CaptureSession session) {
        }

        @Override
        public void onReady(CaptureSession session) {
        }
    };

}
