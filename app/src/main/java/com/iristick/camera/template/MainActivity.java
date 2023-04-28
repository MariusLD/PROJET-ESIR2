package com.iristick.camera.template;

import static com.iristick.smartglass.core.TouchEvent.GESTURE_TAP;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.mlkit.vision.face.Face;
import com.iristick.camera.template.helpers.MLVideoHelperActivity;
import com.iristick.camera.template.helpers.vision.GraphicOverlay;
import com.iristick.camera.template.helpers.vision.VisionBaseProcessor;
import com.iristick.camera.template.helpers.vision.recogniser.FaceRecognitionProcessor;
import com.iristick.smartglass.core.Headset;
import com.iristick.smartglass.core.TouchEvent;
import com.iristick.smartglass.core.VoiceEvent;
import com.iristick.smartglass.core.camera.Barcode;
import com.iristick.smartglass.core.camera.CameraCharacteristics;
import com.iristick.smartglass.core.camera.CameraDevice;
import com.iristick.smartglass.core.camera.CaptureListener;
import com.iristick.smartglass.core.camera.CaptureListener2;
import com.iristick.smartglass.core.camera.CaptureRequest;
import com.iristick.smartglass.core.camera.CaptureResult;
import com.iristick.smartglass.core.camera.CaptureSession;
import com.iristick.smartglass.support.app.IristickApp;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class MainActivity extends MLVideoHelperActivity implements FaceRecognitionProcessor.FaceRecognitionCallback {


    private Interpreter faceNetInterpreter;
    private FaceRecognitionProcessor faceRecognitionProcessor;

    private Face face;
    private Bitmap faceBitmap;
    private float[] faceVector;

    private static final String TAG = "MainActivity";
    private  ImageView imageView;
    private Bitmap mSelectedImage;
    private GraphicOverlay mGraphicOverlay;

    private enum VoiceCommand {
        TOGGLE_TORCH(R.string.toggle_flashlight),
        ZOOM_IN(R.string.zoom_in),
        ZOOM_OUT(R.string.zoom_out),
        CAMERA_UP(R.string.camera_up),
        CAMERA_CENTER(R.string.camera_center),
        CAMERA_DOWN(R.string.camera_down),
        FOCUS(R.string.focus),
        TAKE_PICTURE(R.string.take_picture),
        ;

        static final VoiceCommand[] VALUES = VoiceCommand.values();

        @StringRes
        final int resId;

        VoiceCommand(final int resId) {
            this.resId = resId;
        }
    }

    // static
    private static final int CENTER_CAMERA = 0;
    private static final int ZOOM_CAMERA = 1;
    private static final int FRAME_WIDTH = 1280;
    private static final int FRAME_HEIGHT = 960;

    public static final String[] PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    private static final String[] BARCODE_FORMATS = {
            CaptureRequest.POSTPROCESS_BARCODE_FORMAT_AZTEC, CaptureRequest.POSTPROCESS_BARCODE_FORMAT_CODE_128
    }; // here you can define all formats or just not use to scan all

    // private
    private boolean isTorchActive = false;
    private boolean canTakePicture = false;

    private int activeCamera = 0;

    private float zoomLevel = 1.0f;

    private Point cameraOffset;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());

    private ImageReader mImageReader;
    private Headset mHeadset;
    private TextureView mPreview;
    private SurfaceTexture mSurfaceTexture;
    private CameraDevice mCamera;
    private Surface mSurface;
    private CaptureSession mCaptureSession;

    private Button mAddFaceButton;
    private int pictures_number;

    private double start_timer = System.currentTimeMillis();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGraphicOverlay = findViewById(R.id.graphic_overlay);

        mPreview = findViewById(R.id.preview);

        mAddFaceButton = findViewById(R.id.addFaceButton);

        mAddFaceButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                onAddFaceClicked(v);
            }
        });

        imageView = (ImageView) findViewById(R.id.image_view);

        cameraOffset = new Point(0,0);

        Bitmap bitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setColor(Color.RED);

        canvas.drawRect(0, 0, 500, 500, paint);

        imageView.setImageBitmap(bitmap);

        faceRecognitionProcessor = (FaceRecognitionProcessor) setProcessor();

        requestPermissions();
        //makeAddFaceVisible();
    }

    private void requestPermissions() {
        List<String> ungranted = new ArrayList<>();
        for (String permissions : MainActivity.PERMISSIONS) {
            if (checkSelfPermission(permissions) != PackageManager.PERMISSION_GRANTED) {
                ungranted.add(permissions);
            }
        }
        if(ungranted.size() != 0) {
            requestPermissions(ungranted.toArray(new String[ungranted.size()]), 0);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        mHeadset = IristickApp.getHeadset();
        if (mHeadset != null) {
            String[] cameras = mHeadset.getCameraIdList();
            if (CENTER_CAMERA >= cameras.length ||
                    checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            String cameraId = cameras[CENTER_CAMERA];
            CameraCharacteristics characteristics = mHeadset.getCameraCharacteristics(cameraId);

            mPreview.setSurfaceTextureListener(mSurfaceTextureListener);

            openCamera();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHeadset = IristickApp.getHeadset();
        if (mHeadset != null) {
            //mHeadset.setLaserPointer(true);
            //mHeadset.setTorchMode(isTorchActive);

            mHeadset.registerTouchEventCallback(new TouchEvent.Callback() {
                @Override
                public void onTouchEvent(@NonNull TouchEvent event) {
                    /* Process the touch event here:
                     * event.getGestureCode() returns the simple gesture that was
                     *     recognized, or TouchEvent.GESTURE_NONE if none;
                     * event.getMotionEvent() returns the precise motion data or
                     *     null if such data is not available.
                     */
                    if(event.getGestureCode() == GESTURE_TAP) {
                        double timer = System.currentTimeMillis();
                        if(timer-start_timer > 1000) {
                            start_timer = timer;
                            takePicture();
                        }
                    }
                }
            }, null);

            String[] commands = new String[VoiceCommand.VALUES.length];
            for (int i = 0; i < VoiceCommand.VALUES.length; i++)
                commands[i] = getText(VoiceCommand.VALUES[i].resId).toString();
            mHeadset.registerVoiceCommands(commands, mVoiceCallback, null);
        }
    }

    @Override
    protected void onStop() {
        if (mCamera != null) {
            mCamera.close();
            mCaptureSession = null;
            mCamera = null;
        }
        super.onStop();
    }

    @Override
    protected void onPause() {
        mHeadset = IristickApp.getHeadset();
        if (mHeadset != null) {
            mHeadset.setLaserPointer(false);
            mHeadset.setTorchMode(false);

            mHeadset.unregisterVoiceCommands(mVoiceCallback);
        }
        super.onPause();
    }




    @Override
    public boolean onTouchEvent(MotionEvent event) {



        double timer = System.currentTimeMillis();
        if(timer-start_timer > 1000) {
            start_timer = timer;
            takePicture();
        }
        return super.onTouchEvent(event);
    }


    private void openCamera() {
        if (mHeadset == null)
            return;

        activeCamera = CENTER_CAMERA;

        final String id = mHeadset.getCameraIdList()[activeCamera];

        Point[] sizes = mHeadset.getCameraCharacteristics(id)
                .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                .getSizes(CaptureRequest.FORMAT_JPEG);

        mImageReader = ImageReader.newInstance(sizes[0].x, sizes[0].y,
                ImageFormat.JPEG, 50);

        mImageReader.setOnImageAvailableListener(null, null);

        mHeadset.openCamera(id, mCameraListener, null);
    }

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            mSurfaceTexture = surface;
            mSurface = new Surface(mSurfaceTexture);
            createCaptureSession();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private final CameraDevice.Listener mCameraListener = new CameraDevice.Listener() {
        @Override
        public void onOpened(CameraDevice cameraDevice) {
            mCamera = cameraDevice;
            createCaptureSession();
        }

        @Override
        public void onClosed(CameraDevice cameraDevice) {

        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {

        }

        @Override
        public void onError(CameraDevice cameraDevice, int i) {

        }
    };

    private final CaptureSession.Listener mCaptureSessionListener = new CaptureSession.Listener() {
        @Override
        public void onConfigured(CaptureSession captureSession) {
            mCaptureSession = captureSession;
            setCapture();
        }

        @Override
        public void onConfigureFailed(CaptureSession captureSession, int i) {

        }

        @Override
        public void onClosed(CaptureSession captureSession) {

        }

        @Override
        public void onActive(CaptureSession captureSession) {

        }

        @Override
        public void onCaptureQueueEmpty(CaptureSession captureSession) {

        }

        @Override
        public void onReady(CaptureSession captureSession) {

        }
    };

    private void setupTransform(TextureView view) {
        float disp_ratio = (float) view.getWidth() / (float) view.getHeight();
        float frame_ratio = (float) FRAME_WIDTH / (float) FRAME_HEIGHT;
        Matrix transform = new Matrix();
        if (disp_ratio > frame_ratio) {
            transform.setScale(frame_ratio / disp_ratio, 1.0f, view.getWidth() / 2.0f, view.getHeight() / 2.0f);
        }
        else {
            transform.setScale(1.0f, disp_ratio / frame_ratio, view.getWidth() / 2.0f, view.getHeight() / 2.0f);
        }
        view.setTransform(transform);
    }

    private void createCaptureSession() {
        if (mCamera == null || mSurface == null)
            return;

        /* Set the desired camera resolution. */
        mSurfaceTexture.setDefaultBufferSize(FRAME_WIDTH, FRAME_HEIGHT);
        setupTransform(mPreview);

        /* Create the capture session. */
        mCaptureSession = null;
        List<Surface> outputs = new ArrayList<>();
        outputs.add(mSurface);
        outputs.add(mImageReader.getSurface());

        mCamera.createCaptureSession(outputs, mCaptureSessionListener, null);
    }

    private void setupCaptureRequest(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
        builder.set(CaptureRequest.SCALER_ZOOM, zoomLevel);
        builder.set(CaptureRequest.SCALER_OFFSET, cameraOffset);
        builder.set(CaptureRequest.POSTPROCESS_BARCODE_COUNT, 20);
        builder.set(CaptureRequest.POSTPROCESS_BARCODE_FORMATS, BARCODE_FORMATS);
    }

    private void setCapture() {
        if (mCaptureSession == null || mSurface == null) {
            return;
        }
        CaptureRequest.Builder builder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        builder.addTarget(mSurface);

        setupCaptureRequest(builder);

        String zoomLevelString = Float.toString(zoomLevel);

        mCaptureSession.setRepeatingRequest(builder.build(), mCaptureListenerBarcode, null);
    }

    private void triggerAF() {
        if (mCaptureSession == null || mSurface == null) {
            return;
        }

        CaptureRequest.Builder builder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        builder.addTarget(mSurface);
        setupCaptureRequest(builder);

        builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
        mCaptureSession.capture(builder.build(), null, null);
    }

    private void takePicture() {
        if (mCaptureSession == null || mSurface == null) {
            return;
        }

        pictures_number++;

        CaptureRequest.Builder builder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        builder.addTarget(mImageReader.getSurface());

        setupCaptureRequest(builder);

        mCaptureSession.capture(builder.build(), mCaptureListenerTakePicture, null);


    }

    private final CaptureListener mCaptureListenerBarcode = new CaptureListener2() {
        @Override
        public void onPostProcessCompleted(CaptureSession session, CaptureRequest request, CaptureResult result) {

            Barcode[] barcodes = result.get(CaptureResult.POSTPROCESS_BARCODES);
            String barcodeValue = null;

            if (barcodes == null || barcodes.length == 0) {
                return;
            }

            for (int i = 0; i < barcodes.length; i++) {
                barcodeValue = barcodes[i].getValue();

                // do stuff with 'barcodeValue'
            }
        }
    };

    private final CaptureListener mCaptureListenerTakePicture = new CaptureListener2() {

        @Override
        public void onCaptureCompleted(@NonNull CaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult result) {
            //mGraphicOverlay.clear();
            Image image = mImageReader.acquireLatestImage();
            if (image != null) {

                ByteBuffer buffer = image.getPlanes()[0].getBuffer();


                ImageView imageView = (ImageView) findViewById(R.id.image_view);


                byte[] bytes = new byte[buffer.capacity()];
                buffer.get(bytes);

                mSelectedImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
                // Get the dimensions of the View
//                Pair<Integer, Integer> targetedSize = getTargetedWidthHeight();

//                int targetWidth = targetedSize.first;
//                int maxHeight = targetedSize.second;
//
//                // Determine how much to scale down the image
//                float scaleFactor =
//                        Math.max(
//                                (float) mSelectedImage.getWidth() / (float) targetWidth,
//                                (float) mSelectedImage.getHeight() / (float) maxHeight);
//
//                Bitmap resizedBitmap =
//                        Bitmap.createScaledBitmap(
//                                mSelectedImage,
//                                (int) (mSelectedImage.getWidth() / scaleFactor),
//                                (int) (mSelectedImage.getHeight() / scaleFactor),
//                                true);
//
//
                imageView.setImageBitmap(mSelectedImage);
//                mSelectedImage = resizedBitmap;

                Task<List<Face>> test = faceRecognitionProcessor.detectInImage(image, mSelectedImage,0);
                //writeError(face==null ? "non" : face.toString(), getFilesDir().toString());

                test.addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                    @Override
                    public void onSuccess(List<Face> faces) {
                        if(faces.size()>0) {
                            //onFaceDetected(faces.get(0), );
                        }
                        //writeError(String.valueOf(faces.size()), getFilesDir().toString());
                    }
                });
                //writeError("1" ,getFilesDir().toString());

            } else {
                Toast.makeText(MainActivity.this, "No picture taken", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private final VoiceEvent.Callback mVoiceCallback = event -> {
        switch (VoiceCommand.VALUES[event.getCommandIndex()]) {
            case TAKE_PICTURE:
                canTakePicture = true;
                takePicture();
                break;
            case TOGGLE_TORCH:
                toggleTorch();
                break;
            case ZOOM_IN:
                zoomIn(true);
                break;
            case ZOOM_OUT:
                zoomIn(false);
                break;
            case CAMERA_UP:
                cameraUp(true);
                break;
            case CAMERA_CENTER:
                cameraCenter();
                break;
            case CAMERA_DOWN:
                cameraUp(false);
                break;
            case FOCUS:
                triggerAF();
                break;
        }
    };

    private void toggleTorch() {
        isTorchActive = !isTorchActive;
        if (mHeadset != null) {
            mHeadset.setTorchMode(isTorchActive);
        }
    }

    private void zoomIn(boolean zoomingIn) {
        String camera = mHeadset.getCameraIdList()[CENTER_CAMERA];
        CameraCharacteristics mCameraCharacteristics = mHeadset.getCameraCharacteristics(camera);
        float maxZoom = mCameraCharacteristics.get(CameraCharacteristics.SCALER_MAX_ZOOM);

        if (zoomingIn) {
            zoomLevel = zoomLevel*2;
        } else {
            zoomLevel = zoomLevel/2;
        }

        if (zoomLevel >= maxZoom) {
            zoomLevel = maxZoom;
        }

        if (zoomLevel < 1.0f) {
            zoomLevel = 1.0f;
        }

        setCapture();
    }

    private void cameraUp(boolean cameraUp) {
        String camera = mHeadset.getCameraIdList()[CENTER_CAMERA];
        CameraCharacteristics mCameraCharacteristics = mHeadset.getCameraCharacteristics(camera);
        Point maxOffset = mCameraCharacteristics.get(CameraCharacteristics.SCALER_MAX_OFFSET);
        int stepsYOffset = maxOffset.y / 3; // device in e.g. 3 steps to move up and down. Make it as many steps as the slider you have on the Expert side.
        int newYOffset;

        if (cameraUp) {
            newYOffset = cameraOffset.y - stepsYOffset;

            if (newYOffset < -maxOffset.y) {
                newYOffset = -maxOffset.y;
            }

            cameraOffset.y = newYOffset;
        } else {
            newYOffset = cameraOffset.y + stepsYOffset;

            if (newYOffset > maxOffset.y) {
                newYOffset = maxOffset.y;
            }

            cameraOffset.y = newYOffset;
        }

        setCapture();
    }

    private void cameraCenter () {
        cameraOffset.y = 0;

        setCapture();
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    // Functions for loading images from app assets.


    // Gets the targeted width / height.
    private Pair<Integer, Integer> getTargetedWidthHeight() {
        int targetWidth;
        int targetHeight;
        int maxWidthForPortraitMode = imageView.getWidth();
        int maxHeightForPortraitMode = imageView.getHeight();
        targetWidth = maxWidthForPortraitMode;
        targetHeight = maxHeightForPortraitMode;
        return new Pair<>(targetWidth, targetHeight);
    }


    @Override
    protected VisionBaseProcessor setProcessor() {
        try {
            faceNetInterpreter = new Interpreter(FileUtil.loadMappedFile(this, "mobile_face_net.tflite"), new Interpreter.Options());
        } catch (IOException e) {
            writeError("interpreter failed", getFilesDir().toString());
            e.printStackTrace();
        }
        faceRecognitionProcessor = new FaceRecognitionProcessor(
                faceNetInterpreter,
                mGraphicOverlay,
                this
        );
        faceRecognitionProcessor.activity = this;
        return faceRecognitionProcessor;
    }

    public void setTestImage(Bitmap cropToBBox) {
        if (cropToBBox == null) {
            return;
        }
        runOnUiThread(() -> ((ImageView) findViewById(R.id.image_view)).setImageBitmap(cropToBBox));
    }

    @Override
    public void onFaceDetected(Face face, Bitmap faceBitmap, float[] faceVector) {
        this.face = face;
        this.faceBitmap = faceBitmap;
        this.faceVector = faceVector;
    }

    @Override
    public void onFaceRecognised(Face face, float probability, String name) {

    }

    @Override
    public void onAddFaceClicked(View view) {
        super.onAddFaceClicked(view);

        if (face == null || faceBitmap == null) {
            writeError("rate", getFilesDir().toString());
            return;
        }

        Face tempFace = face;
        Bitmap tempBitmap = faceBitmap;
        float[] tempVector = faceVector;

        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.activity_main, null);
        ((ImageView) dialogView.findViewById(R.id.image_view)).setImageBitmap(tempBitmap);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Editable input  = ((EditText) dialogView.findViewById(R.id.dlg_input)).getEditableText();
                if (input.length() > 0) {
                    faceRecognitionProcessor.registerFace(input, tempVector);
                }
            }
        });
        builder.show();
    }


    public static void writeError(String msg,String loc){
        try {
            File file = new File(loc + "/error");

            FileOutputStream stream = new FileOutputStream(file);

            stream.write(msg.getBytes());

            stream.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

}