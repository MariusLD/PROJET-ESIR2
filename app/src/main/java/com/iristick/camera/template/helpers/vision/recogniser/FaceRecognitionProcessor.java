package com.iristick.camera.template.helpers.vision.recogniser;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.Image;
import android.text.Editable;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.iristick.camera.template.MainActivity;
import com.iristick.camera.template.helpers.vision.FaceGraphic;
import com.iristick.camera.template.helpers.vision.VisionBaseProcessor;
import com.iristick.camera.template.helpers.vision.GraphicOverlay;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FaceRecognitionProcessor extends VisionBaseProcessor<List<Face>> {

    class Person {
        public String name;
        public List<Float> faceVector;

        public Person(String name, List<Float> faceVector) {
            this.name = name;
            this.faceVector = faceVector;
        }
    }

    public interface FaceRecognitionCallback {
        void onFaceRecognised(Face face, Bitmap faceBitmap, float[] vector, float probability, String name);
        void onFaceDetected(Face face, Bitmap faceBitmap, float[] vector);
    }

    private static final DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference("Person");

    private static final String TAG = "FaceRecognitionProcessor";

    // Input image size for our facenet model
    private static final int FACENET_INPUT_IMAGE_SIZE = 112;

    private final FaceDetector detector;
    private final Interpreter faceNetModelInterpreter;
    private final ImageProcessor faceNetImageProcessor;
    private final GraphicOverlay graphicOverlay;
    private final FaceRecognitionCallback callback;

    public MainActivity activity;

    public FaceRecognitionProcessor(Interpreter faceNetModelInterpreter,
                                    GraphicOverlay graphicOverlay,
                                    FaceRecognitionCallback callback) {
        this.callback = callback;
        this.graphicOverlay = graphicOverlay;
        // initialize processors
        this.faceNetModelInterpreter = faceNetModelInterpreter;
        faceNetImageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(FACENET_INPUT_IMAGE_SIZE, FACENET_INPUT_IMAGE_SIZE, ResizeOp.ResizeMethod.BILINEAR))
                .add(new NormalizeOp(0f, 255f))
                .build();

        FaceDetectorOptions faceDetectorOptions = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                // to ensure we don't count and analyse same person again
                .enableTracking()
                .build();
        detector = FaceDetection.getClient(faceDetectorOptions);
    }

    //@OptIn(markerClass = ExperimentalGetImage.class)
    public Task<List<Face>> detectInImage(Image imageProxy, Bitmap bitmap, int rotationDegrees) {
        InputImage inputImage = InputImage.fromMediaImage(imageProxy, rotationDegrees);
        int rotation = rotationDegrees;

        // In order to correctly display the face bounds, the orientation of the analyzed
        // image and that of the viewfinder have to match. Which is why the dimensions of
        // the analyzed image are reversed if its rotation information is 90 or 270.
        boolean reverseDimens = rotation == 90 || rotation == 270;
        int width;
        int height;
        if (reverseDimens) {
            width = imageProxy.getHeight();
            height =  imageProxy.getWidth();
        } else {
            width = imageProxy.getWidth();
            height = imageProxy.getHeight();
        }
        return detector.process(inputImage)
                .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                    @Override
                    public void onSuccess(List<Face> faces) {
                        graphicOverlay.clear();
                        for (Face face : faces) {
                            FaceGraphic faceGraphic = new FaceGraphic(graphicOverlay, face, false, width, height);
                            Log.d(TAG, "face found, id: " + face.getTrackingId());
//
                            // now we have a face, so we can use that to analyse age and gender
                            Bitmap faceBitmap = cropToBBox(bitmap, face.getBoundingBox(), rotation);

                            if (faceBitmap == null) {
                                Log.d("GraphicOverlay", "Face bitmap null");
                                return;
                            }

                            TensorImage tensorImage = TensorImage.fromBitmap(faceBitmap);
                            ByteBuffer faceNetByteBuffer = faceNetImageProcessor.process(tensorImage).getBuffer();
                            float[][] faceOutputArray = new float[1][192];
                            faceNetModelInterpreter.run(faceNetByteBuffer, faceOutputArray);

                            Log.d(TAG, "output array: " + Arrays.deepToString(faceOutputArray));

                            if (callback != null) {
                                findNearestFace(faceOutputArray[0], new OnNearestFaceFoundListener() {
                                    @Override
                                    public void onNearestFaceFound(Pair<String, Float> result) {
                                        // if distance is within confidence
                                        if (result != null && result.second < 1.0f) {
                                            faceGraphic.name = result.first;
                                            callback.onFaceRecognised(face, faceBitmap, faceOutputArray[0], result.second, result.first);
                                        }
                                    }
                                });
                            }

                            graphicOverlay.add(faceGraphic);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // intentionally left empty
                    }
                });
    }


    // looks for the nearest vector in the dataset (using L2 norm)
    // and returns the pair <name, distance>
    private void findNearestFace(float[] vector, final OnNearestFaceFoundListener listener) {
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference("Person");

        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Pair<String, Float> ret = null;
                for (DataSnapshot personSnapshot : dataSnapshot.getChildren()) {
                    Person person = personSnapshot.getValue(Person.class);
                    final String name = person.name;
                    final float[] knownVector = new float[person.faceVector.size()];
                    for (int i = 0; i < person.faceVector.size(); i++) {
                        knownVector[i] = person.faceVector.get(i);
                    }

                    float distance = 0;
                    for (int i = 0; i < vector.length; i++) {
                        float diff = vector[i] - knownVector[i];
                        distance += diff * diff;
                    }
                    distance = (float) Math.sqrt(distance);
                    if (ret == null || distance < ret.second) {
                        ret = new Pair<>(name, distance);
                    }
                }
                listener.onNearestFaceFound(ret);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle errors here
            }
        });
    }

    public interface OnNearestFaceFoundListener {
        void onNearestFaceFound(Pair<String, Float> result);
    }



    public void stop() {
        detector.close();
    }

    private Bitmap cropToBBox(Bitmap image, Rect boundingBox, int rotation) {
        int shift = 0;
        if (rotation != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            image = Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);
        }
        if (boundingBox.top >= 0 && boundingBox.bottom <= image.getWidth()
                && boundingBox.top + boundingBox.height() <= image.getHeight()
                && boundingBox.left >= 0
                && boundingBox.left + boundingBox.width() <= image.getWidth()) {
            return Bitmap.createBitmap(
                    image,
                    boundingBox.left,
                    boundingBox.top + shift,
                    boundingBox.width(),
                    boundingBox.height()
            );
        } else return null;
    }

    // Register a name against the vector
    public void registerFace(Editable input, float[] tempVector) {
        List<Float> result = new ArrayList<Float>(tempVector.length);
        for (float f : tempVector) {
            result.add(f);
        }
        // Add Person to Firebase
        Person nPerson = new Person(input.toString(), result);
        mDatabase.push().setValue(nPerson);
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
