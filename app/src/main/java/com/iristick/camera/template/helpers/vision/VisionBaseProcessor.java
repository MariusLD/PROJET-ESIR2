package com.iristick.camera.template.helpers.vision;

import android.graphics.Bitmap;
import android.media.Image;


import com.google.android.gms.tasks.Task;

public abstract class VisionBaseProcessor<T> {
    public abstract Task<T> detectInImage(Image imageProxy, Bitmap bitmap, int rotationDegrees);

    public abstract void stop();
}
