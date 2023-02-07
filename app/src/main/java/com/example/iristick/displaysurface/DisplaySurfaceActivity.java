package com.example.iristick.displaysurface;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Display;
import android.view.Surface;

import com.example.iristick.BaseActivity;
import com.example.iristick.R;
import com.iristick.smartglass.core.DisplayListener;
import com.iristick.smartglass.core.Headset;
import com.iristick.smartglass.support.app.IristickApp;

import java.util.ArrayList;

/**
 * This example draws a bunch of colored moving bubbles on the display,
 * using direct draw operations.
 */
public class DisplaySurfaceActivity extends BaseActivity implements DisplayListener {

    private static final String TAG = "DisplaySurfaceExample";
    private Renderer mRenderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.displaysurface_activity);
        getActionBar().setTitle(R.string.displaysurface_title);
    }

    /* Step 1: Open the display. */

    @Override
    protected void onStart() {
        super.onStart();

        Headset headset = IristickApp.getHeadset();
        if (headset != null)
            headset.openDisplaySurface(this, null);
    }

    @Override
    protected void onStop() {
        Headset headset = IristickApp.getHeadset();
        if (headset != null)
            headset.closeDisplay();
        super.onStop();
    }

    /* Step 2: Implement DisplayListener. */

    @Override
    public void onDisplayOpened(Display display) {
    }

    @Override
    public void onDisplaySurfaceOpened(Surface surface, int width, int height, int density) {
        mRenderer = new Renderer(surface, width, height);
    }

    @Override
    public void onDisplayClosed(int reason) {
        if (mRenderer != null) {
            mRenderer.stop();
            mRenderer = null;
        }
    }

    /* Step 3: Draw something. */

    class Renderer implements Runnable {
        private final Surface mSurface;
        private final int mSurfaceWidth;
        private final int mSurfaceHeight;
        private final ArrayList<Bubble> mBubbles;
        private final HandlerThread mHandlerThread;
        private final Handler mHandler;
        private long mPreviousTime;

        Renderer(Surface surface, int width, int height) {
            mSurface = surface;
            mSurfaceWidth = width;
            mSurfaceHeight = height;

            /* Create 40 colored bubbles. */
            mBubbles = new ArrayList<>();
            for (int i = 0; i < 40; ++i)
                mBubbles.add(new Bubble());

            /* Start rendering in a new thread. */
            mHandlerThread = new HandlerThread("RenderThread");
            mHandlerThread.start();
            mHandler = new Handler(mHandlerThread.getLooper());
            mPreviousTime = System.currentTimeMillis();
            mHandler.post(this);
        }

        void stop() {
            mHandlerThread.quit();
        }

        @Override
        public void run() {
            if (!mSurface.isValid())
                return;

            /* Calculate elapsed time. */
            long newTime = System.currentTimeMillis();
            float deltaT = (float) (newTime - mPreviousTime) / 1000;
            mPreviousTime = newTime;

            /* Draw scene. */
            try {
                Canvas canvas = mSurface.lockCanvas(null);
                canvas.drawARGB(255, 0, 0, 0);
                for (Bubble bubble : mBubbles)
                    bubble.updateAndDraw(deltaT, canvas);
                mSurface.unlockCanvasAndPost(canvas);
            } catch (Exception e) {
                Log.e(TAG, "Failed to draw frame", e);
                return;
            }

            /* Schedule next draw call (30 fps = 33 ms interval). */
            long millisToWait = Math.max((long) (33 - deltaT), 0);
            mHandler.postDelayed(this, millisToWait);
        }

        class Bubble {
            private final float mRadius;
            private float mPosX;
            private float mPosY;
            private float mDx;
            private float mDy;
            private final Paint mPaint;

            Bubble() {
                /* Randomize color. */
                mPaint = new Paint();
                mPaint.setARGB(255, (int) (Math.random() * 159 + 96),
                        (int) (Math.random() * 159 + 96),
                        (int) (Math.random() * 159 + 96));

                /* Randomize radius, starting position and velocity. */
                mRadius = (float) (Math.random() * 10 + 10);
                mPosX = (float) (Math.random() * (mSurfaceWidth - 2 * mRadius));
                mPosY = (float) (Math.random() * (mSurfaceHeight - 2 * mRadius));
                float speed = (float) (Math.random() * 50 + 30);
                double angle = Math.random() * 2 * Math.PI;
                mDx = (float) Math.cos(angle) * speed;
                mDy = (float) Math.sin(angle) * speed;
            }

            void updateAndDraw(float deltaT, Canvas canvas) {
                /* Update current position. */
                mPosX += mDx * deltaT;
                mPosY += mDy * deltaT;

                /* Wrap around screen bounds. */
                if (mPosX < -mRadius)
                    mPosX = mSurfaceWidth + mRadius;
                else if (mPosX > mSurfaceWidth + mRadius)
                    mPosX = -mRadius;
                if (mPosY < -mRadius)
                    mPosY = mSurfaceHeight + mRadius;
                else if (mPosY > mSurfaceHeight + mRadius)
                    mPosY = -mRadius;

                /* Draw onto the surface. */
                canvas.drawCircle(mPosX, mPosY, mRadius, mPaint);
            }
        }
    }

}