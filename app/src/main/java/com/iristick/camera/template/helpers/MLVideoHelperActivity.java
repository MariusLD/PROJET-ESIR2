package com.iristick.camera.template.helpers;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.view.Surface;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;


import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;

import com.iristick.camera.template.R;
import com.iristick.camera.template.helpers.vision.VisionBaseProcessor;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import com.iristick.camera.template.helpers.vision.GraphicOverlay;
public abstract class MLVideoHelperActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA = 1001;

    protected GraphicOverlay graphicOverlay;
    private TextView outputTextView;
    private ExtendedFloatingActionButton addFaceButton;
    private Executor executor = Executors.newSingleThreadExecutor();

    private VisionBaseProcessor processor;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_video_helper_new);

        graphicOverlay = findViewById(R.id.graphic_overlay);
        outputTextView = findViewById(R.id.output_text_view);
        addFaceButton = findViewById(R.id.button_add_face);


        processor = setProcessor();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (processor != null) {
            processor.stop();
        }
    }


    protected void setOutputText(String text) {
        outputTextView.setText(text);
    }


    /**
     * The face detector provides face bounds whose coordinates, width and height depend on the
     * preview's width and height, which is guaranteed to be available after the preview starts
     * streaming.
     */
    private void setFaceDetector(int lensFacing) {

    }


    private Bitmap toBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }


    protected abstract VisionBaseProcessor setProcessor();

    public void makeAddFaceVisible() {
        addFaceButton.setVisibility(View.VISIBLE);
    }

    public void onAddFaceClicked(View view) {

    }
}
