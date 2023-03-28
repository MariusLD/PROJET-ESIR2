package com.iristick.camera.template;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
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
import android.os.Handler;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.iristick.smartglass.core.Headset;
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

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {
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
    private int pictures_number;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGraphicOverlay = findViewById(R.id.graphic_overlay);

        mPreview = findViewById(R.id.preview);

        imageView = (ImageView) findViewById(R.id.image_view);

        cameraOffset = new Point(0,0);

        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if(pictures_number >= 18) {

                }
                // Call your function here
                takePicture();
                pictures_number++;

                TextView testView = (TextView) MainActivity.this.findViewById(R.id.test);
                testView.setText("Nombre photos : "+ pictures_number);

                // Call the runnable again after a specified delay
                handler.postDelayed(this, 3000); // 1000ms = 1 second
            }
        };

        Bitmap bitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setColor(Color.RED);

        canvas.drawRect(0, 0, 500, 500, paint);

        imageView.setImageBitmap(bitmap);

        handler.postDelayed(runnable, 3000);

        requestPermissions();
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
        //takePicture();
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

            mGraphicOverlay.clear();
            Image image = mImageReader.acquireLatestImage();


            if (image != null) {
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();

                ImageView imageView = (ImageView) findViewById(R.id.image_view);

                byte[] bytes = new byte[buffer.capacity()];
                buffer.get(bytes);
                Bitmap bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);

                mSelectedImage = bitmapImage;
                // Get the dimensions of the View
                Pair<Integer, Integer> targetedSize = getTargetedWidthHeight();

                int targetWidth = targetedSize.first;
                int maxHeight = targetedSize.second;

                // Determine how much to scale down the image
                float scaleFactor =
                        Math.max(
                                (float) mSelectedImage.getWidth() / (float) targetWidth,
                                (float) mSelectedImage.getHeight() / (float) maxHeight);

                Bitmap resizedBitmap =
                        Bitmap.createScaledBitmap(
                                mSelectedImage,
                                (int) (mSelectedImage.getWidth() / scaleFactor),
                                (int) (mSelectedImage.getHeight() / scaleFactor),
                                true);

                imageView.setImageBitmap(resizedBitmap);
                mSelectedImage = resizedBitmap;
                runFaceContourDetection();



//                try (OutputStream os = new FileOutputStream(file)) {
//                    Channels.newChannel(os).write(buffer);
//
//                    Toast.makeText(MainActivity.this, "Picture taken!", Toast.LENGTH_SHORT).show();
//                } catch (IOException e) {
//                    Toast.makeText(MainActivity.this, "Error taking picture", Toast.LENGTH_SHORT).show();
//                    e.printStackTrace();
//                }
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

    private void runFaceContourDetection() {
        InputImage image = InputImage.fromBitmap(mSelectedImage, 0);
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .enableTracking()
                        .build();

        // Permet de faire plusieurs détection dans une image
        // .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)

        FaceDetector detector = FaceDetection.getClient(options);
        detector.process(image)
                .addOnSuccessListener(
                        new OnSuccessListener<List<Face>>() {
                            @Override
                            public void onSuccess(List<Face> faces) {
                                processFaceContourDetectionResult(faces);
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                e.printStackTrace();
                            }
                        });

    }

    private void processFaceContourDetectionResult(List<Face> faces) {
        // Task completed successfully
        if (faces.size() == 0) {
            showToast("No face found");
            return;
        }
        else {
            mGraphicOverlay.clear();
            for (Face face: faces) {
                FaceContourGraphic faceGraphic = new FaceContourGraphic(mGraphicOverlay);
                mGraphicOverlay.add(faceGraphic);
                faceGraphic.updateFace(face);
            }
            showToast(faces.size() + " Visages");
        }
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

}