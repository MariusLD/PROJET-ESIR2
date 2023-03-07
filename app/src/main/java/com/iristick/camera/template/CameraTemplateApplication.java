package com.iristick.camera.template;

import android.app.Application;

import com.iristick.smartglass.support.app.IristickApp;

public class CameraTemplateApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        IristickApp.init(this);
    }
}