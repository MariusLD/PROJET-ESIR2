package com.example.iristick;

import androidx.annotation.StringRes;

import com.example.iristick.barcode.BarcodeActivity;
import com.example.iristick.camera.CameraActivity;
import com.example.iristick.displaypresentation.DisplayPresentationActivity;
import com.example.iristick.displaysurface.DisplaySurfaceActivity;
import com.example.iristick.sensorscube.SensorsCubeActivity;
import com.example.iristick.sensorsreadout.SensorsReadoutActivity;
import com.example.iristick.touchpad.TouchpadActivity;

public enum Example {
    CAMERA(CameraActivity.class, R.string.camera_title, R.string.camera_description),
    BARCODE(BarcodeActivity.class, R.string.barcode_title, R.string.barcode_description),
    DISPLAY_PRESENTATION(DisplayPresentationActivity.class, R.string.displaypresentation_title, R.string.displaypresentation_description),
    DISPLAY_SURFACE(DisplaySurfaceActivity.class, R.string.displaysurface_title, R.string.displaysurface_description),
    SENSORS_CUBE(SensorsCubeActivity.class, R.string.sensorscube_title, R.string.sensorscube_description),
    SENSORS_READOUT(SensorsReadoutActivity.class, R.string.sensorsreadout_title, R.string.sensorsreadout_description),
    TOUCHPAD(TouchpadActivity.class, R.string.touchpad_title, R.string.touchpad_description),
    ;

    public final Class<? extends BaseActivity> activity;
    @StringRes
    public final int title;
    @StringRes public final int description;

    Example(Class<? extends BaseActivity> activity, int title, int description) {
        this.activity = activity;
        this.title = title;
        this.description = description;
    }

}
