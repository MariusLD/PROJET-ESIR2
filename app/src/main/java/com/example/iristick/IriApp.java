package com.example.iristick;

import android.app.Application;
import com.iristick.smartglass.support.app.IristickApp;

public class IriApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        IristickApp.init(this);
    }

}
